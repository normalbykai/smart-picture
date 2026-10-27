from pathlib import Path
from urllib.parse import urlsplit

from pydantic import Field, SecretStr, field_validator
from pydantic_settings import BaseSettings, SettingsConfigDict

SERVICE_ROOT = Path(__file__).resolve().parents[2]


class Settings(BaseSettings):
    model_config = SettingsConfigDict(
        env_prefix="AI_SERVICE_",
        env_file=SERVICE_ROOT / ".env",
        env_file_encoding="utf-8",
        extra="ignore",
    )

    api_key: SecretStr
    docs_enabled: bool = False
    deepseek_api_key: SecretStr = SecretStr("")
    deepseek_base_url: str = "https://api.deepseek.com"
    deepseek_model: str = "deepseek-flash"
    image_allowed_base_urls: list[str] = ["http://127.0.0.1:9000/smart-image/"]
    image_max_bytes: int = Field(default=10 * 1024 * 1024, ge=1024, le=20 * 1024 * 1024)
    analysis_concurrency: int = Field(default=2, ge=1, le=16)

    @field_validator("image_allowed_base_urls")
    @classmethod
    def validate_storage_bases(cls, values: list[str]) -> list[str]:
        if not values:
            raise ValueError("At least one storage base URL is required")
        for value in values:
            parsed = urlsplit(value)
            if (
                parsed.scheme not in ("http", "https")
                or not parsed.hostname
                or parsed.username
                or parsed.password
                or parsed.query
                or parsed.fragment
                or not parsed.path.endswith("/")
                or parsed.path == "/"
            ):
                raise ValueError(
                    "Storage base URLs must include an explicit bucket path ending in /"
                )
        return values

    @field_validator("deepseek_base_url")
    @classmethod
    def validate_provider_url(cls, value: str) -> str:
        parsed = urlsplit(value)
        if (
            parsed.scheme != "https"
            or not parsed.hostname
            or parsed.username
            or parsed.password
            or parsed.query
            or parsed.fragment
        ):
            raise ValueError("DeepSeek base URL must use HTTPS without credentials/query/fragment")
        return value.rstrip("/")

    @field_validator("api_key")
    @classmethod
    def validate_api_key(cls, value: SecretStr) -> SecretStr:
        key = value.get_secret_value()
        if len(key) < 32 or not key.isascii() or any(char.isspace() for char in key):
            raise ValueError("API key must contain at least 32 non-whitespace ASCII characters")
        return value
