import os

os.environ.setdefault("KRISHI_AI_INTERNAL_KEY", "test-internal-key")
os.environ.setdefault("KRISHI_AI_ENV", "test")

import pytest
from fastapi.testclient import TestClient

from app.core.config import reset_settings_cache
from app.main import app

reset_settings_cache()


@pytest.fixture
def client() -> TestClient:
    return TestClient(app)


@pytest.fixture
def auth_headers() -> dict:
    return {"X-Krishi-Internal-Key": os.environ["KRISHI_AI_INTERNAL_KEY"]}
