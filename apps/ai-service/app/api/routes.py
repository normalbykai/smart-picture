from fastapi import APIRouter, Depends, Request

from app.core.security import require_api_key
from app.schemas import ApiResponse, HealthData, ServiceInfo

health_router = APIRouter(tags=["health"])
internal_router = APIRouter(
    prefix="/api/v1", tags=["internal"], dependencies=[Depends(require_api_key)]
)


@health_router.get("/health", response_model=ApiResponse[HealthData])
def health(request: Request) -> ApiResponse[HealthData]:
    return ApiResponse(data=HealthData(), request_id=request.state.request_id)


@internal_router.get("/info", response_model=ApiResponse[ServiceInfo])
def service_info(request: Request) -> ApiResponse[ServiceInfo]:
    return ApiResponse(data=ServiceInfo(), request_id=request.state.request_id)
