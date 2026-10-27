from fastapi import APIRouter, Depends, Request

from app.core.security import require_api_key
from app.schemas import (
    ApiResponse,
    HealthData,
    ImageAnalysisRequest,
    ImageAnalysisResult,
    ServiceInfo,
)

health_router = APIRouter(tags=["health"])
internal_router = APIRouter(
    prefix="/api/v1", tags=["internal"], dependencies=[Depends(require_api_key)]
)


@health_router.get("/health", response_model=ApiResponse[HealthData])
def health(request: Request) -> ApiResponse[HealthData]:
    return ApiResponse(data=HealthData(), request_id=request.state.request_id)


@internal_router.get("/info", response_model=ApiResponse[ServiceInfo])
def service_info(request: Request) -> ApiResponse[ServiceInfo]:
    configured = bool(request.app.state.settings.deepseek_api_key.get_secret_value().strip())
    return ApiResponse(
        data=ServiceInfo(capabilities=["image-analysis"] if configured else []),
        request_id=request.state.request_id,
    )


@internal_router.post("/images/analyze", response_model=ApiResponse[ImageAnalysisResult])
async def analyze_image(
    body: ImageAnalysisRequest, request: Request
) -> ApiResponse[ImageAnalysisResult]:
    result = await request.app.state.image_analysis.analyze(body.image_url)
    return ApiResponse(data=result, request_id=request.state.request_id)
