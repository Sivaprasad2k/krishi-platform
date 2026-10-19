"""Shared-secret authentication for the internal key."""

import secrets

from fastapi import Header

from app.core.config import get_settings
from app.core.errors import KrishiAiError


def require_internal_key(
    x_krishi_internal_key: str | None = Header(default=None, alias="X-Krishi-Internal-Key"),
) -> None:
    """Validate the shared secret (Contract: Security: Shared Secret API Key)."""
    expected = get_settings().internal_api_key
    if not expected or not x_krishi_internal_key or not secrets.compare_digest(x_krishi_internal_key, expected):
        raise KrishiAiError(
            message="Missing or invalid X-Krishi-Internal-Key header.",
            error_code="UNAUTHORIZED",
            status_code=401,
        )
