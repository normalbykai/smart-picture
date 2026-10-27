from typing import Generic, TypeVar

from pydantic import BaseModel, ConfigDict, Field, field_validator

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


class ImageAnalysisRequest(BaseModel):
    model_config = ConfigDict(extra="forbid")
    image_url: str = Field(min_length=1, max_length=8192, repr=False)


class ImageAnalysisResult(BaseModel):
    model_config = ConfigDict(extra="forbid", strict=True)
    title: str = Field(min_length=1, max_length=60)
    tags: list[str] = Field(min_length=1, max_length=5)

    @field_validator("title")
    @classmethod
    def normalize_title(cls, value: str) -> str:
        value = value.strip()
        if not value or any(ord(c) < 32 for c in value):
            raise ValueError("Invalid title")
        return value

    @field_validator("tags")
    @classmethod
    def normalize_tags(cls, values: list[str]) -> list[str]:
        result = []
        for value in values:
            value = value.strip()
            if not 1 <= len(value) <= 12 or any(ord(c) < 32 for c in value):
                raise ValueError("Invalid tag")
            if value not in result:
                result.append(value)
        return result
