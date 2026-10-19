"""AI Doctor contract endpoint (Contract 3.2). STUB — returns the contract sample."""

from typing import Any

from fastapi import APIRouter, Depends
from pydantic import BaseModel, ConfigDict, Field

from app.core.security import require_internal_key
from app.services.contract_samples import DOCTOR_CONSULT_SAMPLE, stub_sample

router = APIRouter(prefix="/api/v1/ai", tags=["doctor"], dependencies=[Depends(require_internal_key)])


class DoctorConsultRequest(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    farmer_id: str = Field(alias="farmerId")
    query: str = Field(min_length=1)
    language: str = Field(default="en")
    crop_context: dict[str, Any] | None = Field(default=None, alias="cropContext")


@router.post("/doctor/consult")
async def consult_doctor(request: DoctorConsultRequest) -> dict:
    """STUB (LLM wiring lands in the next phase). Validates the transport contract only."""
    return stub_sample(DOCTOR_CONSULT_SAMPLE)
