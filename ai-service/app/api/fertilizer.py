"""Fertilizer recommendation contract endpoint (Contract 3.3).

Thin HTTP adapter: request validation (schema below), auth via router dependency,
service invocation, response serialization. Domain logic lives in
app/services/fertilizer.py; agronomic data in app/services/agronomic_data.py.

Response shape is unchanged from the contract sample (success / deficiencyStatus /
recommendedApplications / guidanceSource). No additive fields are emitted.
"""

from fastapi import APIRouter, Depends
from pydantic import BaseModel, ConfigDict, Field, field_validator

from app.core.security import require_internal_key
from app.services import fertilizer as fertilizer_service

router = APIRouter(prefix="/api/v1/ai", tags=["fertilizer"], dependencies=[Depends(require_internal_key)])


class SoilTest(BaseModel):
    # Units documented in app/services/agronomic_data.py:
    #   elemental N, P, K in kg/ha; pH on the technically valid 0-14 scale;
    #   organic carbon as a percentage (0-100), NOT a 0-1 fraction (never converted).
    model_config = ConfigDict(extra="ignore")

    nitrogenKgPerHa: float = Field(ge=0, allow_inf_nan=False)
    phosphorusKgPerHa: float = Field(ge=0, allow_inf_nan=False)
    potassiumKgPerHa: float = Field(ge=0, allow_inf_nan=False)
    # Technically valid pH scale here (0-14). Crop agronomic applicability gating is
    # NOT implemented (no verified crop pH response dataset); pH stays interpretation-only.
    ph: float = Field(ge=0, le=14, allow_inf_nan=False)
    organicCarbonPercent: float = Field(ge=0, le=100, allow_inf_nan=False)


class FertilizerRecommendationRequest(BaseModel):
    model_config = ConfigDict(extra="ignore")

    cropName: str = Field(min_length=1)
    fieldAreaAcres: float = Field(gt=0, allow_inf_nan=False)
    soilTest: SoilTest
    # Retained for contract compatibility; the engine currently returns the full split
    # plan and does not stage-filter (documented integration note for Developer A).
    growthStage: str | None = None

    @field_validator("cropName")
    @classmethod
    def reject_blank_crop_name(cls, value: str) -> str:
        if not value.strip():
            raise ValueError("cropName must not be blank")
        return value


@router.post("/fertilizer/recommend")
async def recommend_fertilizer(request: FertilizerRecommendationRequest) -> dict:
    """POST /api/v1/ai/fertilizer/recommend (Contract 3.3). Deterministic, no I/O."""
    soil = request.soilTest
    return fertilizer_service.compute_recommendation(
        crop_name=request.cropName,
        field_area_acres=request.fieldAreaAcres,
        soil_nitrogen=soil.nitrogenKgPerHa,
        soil_phosphorus=soil.phosphorusKgPerHa,
        soil_potassium=soil.potassiumKgPerHa,
        ph=soil.ph,
        organic_carbon_percent=soil.organicCarbonPercent,
    )
