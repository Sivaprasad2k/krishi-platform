"""Fertilizer recommendation contract endpoint (Contract 3.3). STUB — returns the contract sample."""

from fastapi import APIRouter, Depends
from pydantic import BaseModel, Field

from app.core.security import require_internal_key
from app.services.contract_samples import FERTILIZER_RECOMMENDATION_SAMPLE, stub_sample

router = APIRouter(prefix="/api/v1/ai", tags=["fertilizer"], dependencies=[Depends(require_internal_key)])


class SoilTest(BaseModel):
    nitrogenKgPerHa: float
    phosphorusKgPerHa: float
    potassiumKgPerHa: float
    ph: float = Field(ge=0, le=14)
    organicCarbonPercent: float


class FertilizerRecommendationRequest(BaseModel):
    cropName: str = Field(min_length=1)
    fieldAreaAcres: float = Field(gt=0)
    soilTest: SoilTest
    growthStage: str | None = None


@router.post("/fertilizer/recommend")
async def recommend_fertilizer(request: FertilizerRecommendationRequest) -> dict:
    """STUB (deterministic ICAR calibration lands in the next phase). Transport contract only."""
    return stub_sample(FERTILIZER_RECOMMENDATION_SAMPLE)
