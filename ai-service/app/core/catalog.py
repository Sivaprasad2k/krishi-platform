"""Static reference data shared across the service (no ML model logic yet)."""

from typing import Final

SUPPORTED_CROPS: Final[list[str]] = ["Rice (Paddy)", "Tomato", "Wheat"]

MAX_IMAGE_BYTES: Final[int] = 10 * 1024 * 1024  # Contract 3.1: max 10MB
ALLOWED_IMAGE_CONTENT_TYPES: Final[set[str]] = {"image/jpeg", "image/png"}
