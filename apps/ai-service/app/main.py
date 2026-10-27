import logging
import re
from time import perf_counter
from uuid import uuid4

from fastapi import FastAPI, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse
from starlette.exceptions import HTTPException

from app.api.routes import health_router, internal_router
from app.core.config import Settings
from app.schemas import ApiResponse

logger = logging.getLogger("uvicorn.error")
REQUEST_ID_PATTERN = re.compile(r"[A-Za-z0-9._-]{1,64}")


def error_response(request: Request, status: int, message: str) -> JSONResponse:
    body = ApiResponse[None](code=status, message=message, request_id=request.state.request_id)
    return JSONResponse(status_code=status, content=body.model_dump())


def create_app(settings: Settings | None = None) -> FastAPI:
    settings = settings if settings is not None else Settings()
    app = FastAPI(
        title="Smart Picture AI Service",
        version="0.1.0",
        docs_url="/docs" if settings.docs_enabled else None,
        redoc_url=None,
        openapi_url="/openapi.json" if settings.docs_enabled else None,
    )
    app.state.settings = settings

    @app.middleware("http")
    async def trace_request(request: Request, call_next):
        supplied_id = request.headers.get("X-Request-ID", "")
        request.state.request_id = (
            supplied_id if REQUEST_ID_PATTERN.fullmatch(supplied_id) else uuid4().hex
        )
        started = perf_counter()
        try:
            response = await call_next(request)
        except Exception:
            logger.exception("Unhandled error request_id=%s", request.state.request_id)
            response = error_response(request, 500, "Internal server error")
        response.headers["X-Request-ID"] = request.state.request_id
        logger.info(
            "request_id=%s method=%s status=%s duration_ms=%.2f",
            request.state.request_id,
            request.method,
            response.status_code,
            (perf_counter() - started) * 1000,
        )
        return response

    @app.exception_handler(HTTPException)
    async def handle_http_error(request: Request, exc: HTTPException):
        response = error_response(request, exc.status_code, str(exc.detail))
        if exc.headers:
            response.headers.update(exc.headers)
        return response

    @app.exception_handler(RequestValidationError)
    async def handle_validation_error(request: Request, exc: RequestValidationError):
        # Avoid reflecting request bodies, image URLs or credentials in errors.
        return error_response(request, 422, "Request validation failed")

    app.include_router(health_router)
    app.include_router(internal_router)
    return app
