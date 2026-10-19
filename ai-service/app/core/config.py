"""Application configuration via environment variables."""

import os
from functools import lru_cache


class Settings:
    """Simple environment-backed settings with sane dev defaults."""

    def __init__(self) -> None:
        self.internal_api_key: str = os.environ.get("KRISHI_AI_INTERNAL_KEY", "dev-internal-key")
        self.env: str = os.environ.get("KRISHI_AI_ENV", "dev")
        self.disease_model_version: str = os.environ.get("KRISHI_AI_DISEASE_MODEL_VERSION", "plant-vision-v2.1")
        self.doctor_model: str = os.environ.get("KRISHI_AI_DOCTOR_MODEL", "agri-llm-lora-v1")
        self.service_name: str = "krishi-ai-service"
        self.service_version: str = "1.0.0"


@lru_cache
def get_settings() -> Settings:
    return Settings()


def reset_settings_cache() -> None:
    """Clear the cached settings (used by tests to reload env)."""
    get_settings.cache_clear()
