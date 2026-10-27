from typing import Generic, TypeVar

from pydantic import BaseModel, Field

T = TypeVar("T")


class ApiResponse(BaseModel, Generic[T]):
    code: int = 0
    message: str = "ok"
    data: T | None = None
    request_id: str


class HealthData(BaseModel):
    status: str = "ok"


class ServiceInfo(BaseModel):
    name: str = "smart-picture-ai-service"
    version: str = "0.1.0"
    capabilities: list[str] = Field(default_factory=list)
