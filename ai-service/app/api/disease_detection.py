"""Disease detection contract endpoint (Contract 3.1). STUB — returns the contract sample."""

from fastapi import APIRouter, Depends, File, Form, UploadFile

from app.core.catalog import ALLOWED_IMAGE_CONTENT_TYPES, MAX_IMAGE_BYTES
from app.core.errors import KrishiAiError
from app.core.security import require_internal_key
from app.services.contract_samples import DISEASE_DIAGNOSIS_SAMPLE, stub_sample

router = APIRouter(prefix="/api/v1/ai", tags=["disease-detection"], dependencies=[Depends(require_internal_key)])


@router.post("/disease-detection/diagnose")
async def diagnose_disease(
    image: UploadFile = File(...),
    crop_name: str = Form(..., alias="cropName"),
    crop_variety: str | None = Form(default=None, alias="cropVariety"),
    growth_stage: str | None = Form(default=None, alias="growthStage"),
    observed_symptoms: str | None = Form(default=None, alias="observedSymptoms"),
) -> dict:
    """STUB (Milestone 5 will swap in real ONNX inference). Validates the transport contract only."""
    payload = await image.read()
    if image.content_type not in ALLOWED_IMAGE_CONTENT_TYPES:
        raise KrishiAiError(
            message="Only JPEG or PNG foliage images are accepted.",
            error_code="INVALID_IMAGE_PAYLOAD",
        )
    if len(payload) > MAX_IMAGE_BYTES:
        raise KrishiAiError(
            message="Image exceeds the 10 MB limit.",
            error_code="INVALID_IMAGE_PAYLOAD",
        )
    return stub_sample(DISEASE_DIAGNOSIS_SAMPLE)
