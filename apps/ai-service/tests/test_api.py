import pytest
from fastapi.testclient import TestClient
from pydantic import BaseModel, ValidationError

from app.core.config import Settings
from app.main import create_app

TEST_KEY = "test-key-" + "a" * 32


@pytest.fixture
def app():
    return create_app(Settings(api_key=TEST_KEY, docs_enabled=False, _env_file=None))


def test_health_and_request_id(app):
    with TestClient(app) as client:
        response = client.get("/health", headers={"X-Request-ID": "java-request-123"})
    assert response.status_code == 200
    assert response.json()["data"] == {"status": "ok"}
    assert response.json()["request_id"] == response.headers["X-Request-ID"]
    assert response.headers["X-Request-ID"] == "java-request-123"


@pytest.mark.parametrize("key", [None, "incorrect-key"])
def test_internal_endpoint_requires_key(app, key):
    with TestClient(app) as client:
        response = client.get("/api/v1/info", headers={"X-API-Key": key} if key else {})
    assert response.status_code == 401
    assert response.json()["code"] == 401
    assert response.json()["data"] is None


def test_authenticated_info(app):
    with TestClient(app) as client:
        response = client.get("/api/v1/info", headers={"X-API-Key": TEST_KEY})
    assert response.status_code == 200
    assert response.json()["data"]["capabilities"] == []
    assert TEST_KEY not in response.text


def test_invalid_request_id_is_replaced(app):
    with TestClient(app) as client:
        response = client.get("/health", headers={"X-Request-ID": "x" * 100})
    assert len(response.headers["X-Request-ID"]) == 32


def test_docs_disabled_and_404_enveloped(app):
    with TestClient(app) as client:
        for path in ["/docs", "/openapi.json", "/missing"]:
            response = client.get(path)
            assert response.status_code == 404
            assert response.json()["code"] == 404


def test_openapi_security():
    app = create_app(Settings(api_key=TEST_KEY, docs_enabled=True, _env_file=None))
    with TestClient(app) as client:
        schema = client.get("/openapi.json").json()
    assert schema["paths"]["/api/v1/info"]["get"]["security"] == [{"APIKeyHeader": []}]


def test_errors_do_not_expose_inputs_or_internals(app):
    class Input(BaseModel):
        count: int

    @app.post("/test-validation")
    def validate(body: Input):
        return body

    @app.get("/test-error")
    def fail():
        raise RuntimeError("private-provider-details")

    with TestClient(app) as client:
        invalid = client.post("/test-validation", json={"count": "private-input"})
        failure = client.get("/test-error")
    assert invalid.status_code == 422
    assert "private-input" not in invalid.text
    assert failure.status_code == 500
    assert failure.json()["code"] == 500
    assert "private-provider-details" not in failure.text
    assert failure.json()["request_id"] == failure.headers["X-Request-ID"]


@pytest.mark.parametrize("key", ["", "short", " " * 32])
def test_rejects_weak_configuration(key):
    with pytest.raises(ValidationError):
        Settings(api_key=key, _env_file=None)


def test_missing_key_fails_closed(monkeypatch):
    monkeypatch.delenv("AI_SERVICE_API_KEY", raising=False)
    with pytest.raises(ValidationError):
        Settings(_env_file=None)
