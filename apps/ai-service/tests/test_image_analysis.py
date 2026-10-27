import asyncio
import io
import json

import httpx
import pytest
from fastapi import HTTPException
from fastapi.testclient import TestClient
from PIL import Image

from app.core.config import Settings
from app.main import create_app
from app.services.image_analysis import ImageAnalysisService, prepare_image, validate_image_url

KEY = "test-" + "a" * 32
SIGNED = (
    "http://127.0.0.1:9000/smart-image/test.png"
    "?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Signature=test-signature"
)


def settings(**kwargs):
    return Settings(api_key=KEY, deepseek_api_key="test-provider-key", _env_file=None, **kwargs)


def png():
    output = io.BytesIO()
    Image.new("RGB", (32, 24), "blue").save(output, format="PNG")
    return output.getvalue()


def model_response(content=None, reason="stop"):
    return {
        "choices": [
            {
                "finish_reason": reason,
                "message": {
                    "content": content
                    or json.dumps(
                        {"title": " 蓝色图片 ", "tags": ["蓝色", "蓝色", "纯色"]},
                        ensure_ascii=False,
                    )
                },
            }
        ]
    }


def test_end_to_end_with_mocked_storage_and_provider():
    requests = []

    def handle(request):
        requests.append(request)
        if request.url.host == "127.0.0.1":
            assert "authorization" not in request.headers
            assert "x-api-key" not in request.headers
            return httpx.Response(200, content=png())
        assert str(request.url) == "https://api.deepseek.com/chat/completions"
        body = json.loads(request.content)
        assert body["model"] == "deepseek-flash"
        assert body["thinking"] == {"type": "disabled"}
        assert body["response_format"] == {"type": "json_object"}
        assert body["messages"][0]["content"][1]["image_url"]["url"].startswith(
            "data:image/jpeg;base64,"
        )
        assert b"test-signature" not in request.content
        assert request.headers["Authorization"] == "Bearer test-provider-key"
        return httpx.Response(200, json=model_response())

    app = create_app(settings())
    app.state.image_analysis = ImageAnalysisService(settings(), httpx.MockTransport(handle))
    with TestClient(app) as client:
        response = client.post(
            "/api/v1/images/analyze",
            json={"image_url": SIGNED},
            headers={"X-API-Key": KEY, "X-Request-ID": "analysis-test"},
        )
    assert response.status_code == 200
    assert response.json()["data"] == {"title": "蓝色图片", "tags": ["蓝色", "纯色"]}
    assert response.json()["request_id"] == "analysis-test"
    assert len(requests) == 2


def test_missing_key_and_authentication_fail_before_download():
    app = create_app(Settings(api_key=KEY, deepseek_api_key="", _env_file=None))
    with TestClient(app) as client:
        assert client.post("/api/v1/images/analyze", json={"image_url": SIGNED}).status_code == 401
        response = client.post(
            "/api/v1/images/analyze", json={"image_url": SIGNED}, headers={"X-API-Key": KEY}
        )
    assert response.status_code == 503
    assert "not configured" in response.json()["message"]
    assert "test-signature" not in response.text


@pytest.mark.parametrize(
    "url",
    [
        SIGNED.replace("127.0.0.1", "evil.example"),
        SIGNED.replace("9000", "9001"),
        SIGNED.replace("/smart-image/", "/other-bucket/"),
        SIGNED.replace("/test.png", "/../other/test.png"),
        SIGNED.replace("/test.png", "/%2e%2e/other/test.png"),
        SIGNED.replace("127.0.0.1", "user:password@127.0.0.1"),
        SIGNED.split("?")[0],
        SIGNED + "#fragment",
    ],
)
def test_rejects_untrusted_or_unsigned_urls(url):
    with pytest.raises(HTTPException) as error:
        validate_image_url(url, settings())
    assert error.value.status_code == 422


@pytest.mark.parametrize(
    "status,headers,content,expected",
    [
        (302, {"location": "http://169.254.169.254/"}, b"", 502),
        (403, {}, b"expired", 502),
        (200, {"content-length": "2000"}, b"", 413),
        (200, {}, b"x" * 2048, 413),
        (200, {}, b"not an image", 422),
    ],
)
def test_download_failure_never_calls_model(status, headers, content, expected):
    calls = []

    def handle(request):
        calls.append(request)
        assert request.url.host == "127.0.0.1"
        return httpx.Response(status, headers=headers, content=content)

    service = ImageAnalysisService(settings(image_max_bytes=1024), httpx.MockTransport(handle))
    with pytest.raises(HTTPException) as error:
        asyncio.run(service.analyze(SIGNED))
    assert error.value.status_code == expected
    assert len(calls) == 1


@pytest.mark.parametrize(
    "response,expected",
    [
        (httpx.Response(401, text="private-provider-details"), 502),
        (httpx.Response(429, text="private-provider-details"), 429),
        (httpx.Response(500, text="private-provider-details"), 502),
        (httpx.Response(200, json=model_response("not json")), 502),
        (httpx.Response(200, json=model_response('{"title":"ok","tags":[]}')), 502),
        (httpx.Response(200, json=model_response('{"title":"ok","tags":[123]}')), 502),
        (httpx.Response(200, json=model_response(reason="length")), 502),
    ],
)
def test_provider_errors_are_sanitized_and_not_retried(response, expected):
    count = 0

    def handle(request):
        nonlocal count
        count += 1
        return response

    service = ImageAnalysisService(settings(), httpx.MockTransport(handle))
    with pytest.raises(HTTPException) as error:
        asyncio.run(service.call_model("data:image/jpeg;base64,test"))
    assert error.value.status_code == expected
    assert "private-provider-details" not in error.value.detail
    assert count == 1


def test_timeouts_and_busy_limit():
    def timeout(request):
        raise httpx.ReadTimeout("secret-url")

    service = ImageAnalysisService(settings(analysis_concurrency=1), httpx.MockTransport(timeout))
    with pytest.raises(HTTPException) as error:
        asyncio.run(service.analyze(SIGNED))
    assert error.value.status_code == 504

    async def busy():
        await service.slots.acquire()
        try:
            await service.analyze(SIGNED)
        finally:
            service.slots.release()

    with pytest.raises(HTTPException) as error:
        asyncio.run(busy())
    assert error.value.status_code == 429


def test_rejects_large_dimensions_before_decoding():
    output = io.BytesIO()
    Image.new("RGB", (8193, 1)).save(output, "PNG")
    with pytest.raises(HTTPException) as error:
        prepare_image(output.getvalue())
    assert error.value.status_code == 413
