"""Model version probe (Contract section 2.2)."""

from fastapi import APIRouter, Depends

from app.core.config import get_settings
from app.core.security import require_internal_key

router = APIRouter(prefix="/api/v1/ai", dependencies=[Depends(require_internal_key)])


@router.get("/version")
def version() -> dict:
    settings = get_settings()
    return {
        "serviceVersion": settings.service_version,
        "diseaseModelVersion": settings.disease_model_version,
        "doctorModel": settings.doctor_model,
        "supportedCrops": ["Rice (Paddy)", "Tomato", "Wheat"],
    }
