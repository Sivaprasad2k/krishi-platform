"""Deterministic fertilizer recommendation engine (Phase 2).

Calculation pipeline (all agronomic values come from app.services.agronomic_data):

  1. Canonicalise + registry lookup     (UNSUPPORTED_CROP / MISSING_CALIBRATION_DATA gates)
  2. Target nutrient rate               (dataset dose in kg/ha)
  3. Field area conversion              (acres -> ha, applied EXACTLY ONCE)
  4. Field-level nutrient requirement   (rate x area_ha; never a soil-test subtraction)
  5. Nutrient -> product mass           (requirement / declared product nutrient fraction)
  6. Growth-stage allocation            (dataset fractions; totals preserved exactly)
  7. Soil classification                (TNAU general chart bands; reported status only)

Invariants (each enforced and tested):
  * identical (inputs, dataset version) -> identical response (pure, no I/O in path);
  * the area conversion is applied exactly once on the request scale;
  * every emitted quantity is finite and non-negative;
  * stage quantities sum exactly to the rounded total (largest-remainder rounding);
  * multi-nutrient products are rejected at dataset validation time, which makes N/P/K
    double-counting impossible by construction.
"""

from __future__ import annotations

import logging
import math
from typing import Final

from app.core.errors import KrishiAiError
from app.services.agronomic_data import (
    CROPS_WITH_MISSING_CALIBRATION,
    CropNutrientDataset,
    DATASET_VERSION,
    SUPPORTED_CROPS,
    VERIFIED_DATASETS,
    organic_carbon_class,
    soil_reaction_class,
)
from app.services.agronomic_data import HECTARES_PER_ACRE

logger: Final = logging.getLogger("krishi-ai.fertilizer")

ROUND_DIGITS: Final[int] = 2  # documented presentation boundary: 2 decimals for kg


def field_area_hectares(field_area_acres: float) -> float:
    """Convert the request area from acres to hectares (applied once, no hidden retries)."""
    return field_area_acres * HECTARES_PER_ACRE


def classify_soil_nutrient(value: float, deficient_upper: float, medium_upper: float) -> str:
    """Classify a soil nutrient concentration against a documented band.

    Semantics (deterministic, mutually exclusive, gapless):
        value <  deficient_upper        -> "DEFICIENT"
        deficient_upper <= value <= medium_upper -> "MEDIUM"
        value >  medium_upper           -> "OPTIMAL"
    """
    if value < deficient_upper:
        return "DEFICIENT"
    if value <= medium_upper:
        return "MEDIUM"
    return "OPTIMAL"


def _assert_finite(value: float, label: str) -> float:
    if not math.isfinite(value):
        raise KrishiAiError(
            message=f"Non-finite {label} result; refusing to emit a physical quantity.",
            error_code="INTERNAL_ERROR",
            status_code=500,
        )
    return value


def _round_preserving_total(total: float, fractions: tuple[float, ...]) -> list[float]:
    """Split ``total`` across ``fractions`` and round to ROUND_DIGITS such that the
    rounded stage quantities sum EXACTLY to round(total, ROUND_DIGITS).

    Largest-remainder distribution with deterministic tie-breaking (lowest index wins).
    Paths: ``fractions`` must already be validated to sum to 1.0 (dataset layer).
    """
    scale = 10**ROUND_DIGITS
    total_rounded = round(total, ROUND_DIGITS)
    exact = [total * f for f in fractions]
    rounded = [round(e, ROUND_DIGITS) for e in exact]
    residual_units = int(round((total_rounded - sum(rounded)) * scale))
    step = 1.0 / scale
    while residual_units > 0:
        remainders = [exact[idx] - rounded[idx] for idx in range(len(exact))]
        winner = max(range(len(exact)), key=lambda idx: (remainders[idx], -idx))
        rounded[winner] = round(rounded[winner] + step, ROUND_DIGITS)
        residual_units -= 1
    while residual_units < 0:
        remainders = [exact[idx] - rounded[idx] for idx in range(len(exact))]
        loser = min(range(len(exact)), key=lambda idx: (remainders[idx], idx))
        rounded[loser] = round(rounded[loser] - step, ROUND_DIGITS)
        residual_units += 1
    return rounded


def _canonicalise_crop(crop_name: str | None) -> str:
    """Map a request crop name to its canonical dataset key (no silent aliasing)."""
    if crop_name is None:
        raise KrishiAiError(message="cropName is required.", error_code="VALIDATION_ERROR")
    stripped = crop_name.strip()
    if not stripped:
        raise KrishiAiError(message="cropName must not be blank.", error_code="VALIDATION_ERROR")
    for canonical in SUPPORTED_CROPS:
        if stripped.casefold() == canonical.casefold():
            return canonical
    raise KrishiAiError(
        message=f"Unsupported crop '{stripped}'. Supported crops: {', '.join(SUPPORTED_CROPS)}.",
        error_code="UNSUPPORTED_CROP",
    )


def _resolve_dataset(canonical_crop: str) -> CropNutrientDataset:
    """Strict gate: known-but-unverified crops fail loudly with enumerated data gaps."""
    missing_reasons = CROPS_WITH_MISSING_CALIBRATION.get(canonical_crop)
    if missing_reasons is not None:
        joined = "; ".join(missing_reasons)
        raise KrishiAiError(
            message=(
                f"No verified agronomic dataset ({DATASET_VERSION}) for '{canonical_crop}': "
                f"{joined}. Recommendation intentionally withheld rather than estimated."
            ),
            error_code="MISSING_CALIBRATION_DATA",
            status_code=501,
        )
    dataset = VERIFIED_DATASETS.get(canonical_crop)
    if dataset is None:  # defensive: registry/contract drift
        raise KrishiAiError(
            message=f"No dataset registered for '{canonical_crop}'.",
            error_code="INTERNAL_ERROR",
            status_code=500,
        )
    return dataset


def compute_recommendation(
    crop_name: str,
    field_area_acres: float,
    soil_nitrogen: float,
    soil_phosphorus: float,
    soil_potassium: float,
    ph: float,
    organic_carbon_percent: float,
) -> dict:
    """Pure deterministic recommendation of the contract 3.3 response shape.

    Parameters are already validated by the API schema; this orchestration remains
    pure: no I/O, no clocks, no randomness.
    """
    canonical_crop = _canonicalise_crop(crop_name)
    dataset = _resolve_dataset(canonical_crop)

    area_ha = _assert_finite(
        field_area_hectares(field_area_acres), "field area (hectares)"
    )

    # Soil-test classification is interpretive status ONLY (TNAU general chart); it does
    # not modify the blanket dose (no verified category-to-rate calibration exists).
    deficiency_status = {
        "nitrogen": classify_soil_nutrient(
            soil_nitrogen, dataset.classification["N"].deficient_upper, dataset.classification["N"].medium_upper
        ),
        "phosphorus": classify_soil_nutrient(
            soil_phosphorus,
            dataset.classification["P"].deficient_upper,
            dataset.classification["P"].medium_upper,
        ),
        "potassium": classify_soil_nutrient(
            soil_potassium,
            dataset.classification["K"].deficient_upper,
            dataset.classification["K"].medium_upper,
        ),
    }
    # pH / organic carbon recorded for interpretation; deliberately not rate-modifying.
    _interpretation_context = {
        "soilReaction": soil_reaction_class(ph),
        "organicCarbon": organic_carbon_class(organic_carbon_percent),
    }

    # Dose keys per dataset convention: N (elemental), P2O5, K2O (oxide basis).
    # Product grade fractions are declared on the same basis as the dose key, so
    # nutrient->product conversion stays coherent without silent basis switching.
    applications: list[dict] = []
    for dose_key, nutrient_response_key in (
        ("N", "nitrogen"),
        ("P2O5", "phosphorus"),
        ("K2O", "potassium"),
    ):
        product = dataset.products_by_nutrient[dose_key]
        schedule = dataset.schedules[dose_key]
        (_, product_fraction) = product.nutrients[0]
        rate_kg_per_ha = dataset.dose_kg_per_ha[dose_key]
        field_requirement_kg = _assert_finite(rate_kg_per_ha * area_ha, "nutrient requirement")
        total_product_kg = _assert_finite(
            field_requirement_kg / product_fraction, "product mass"
        )
        if total_product_kg < 0:
            raise KrishiAiError(
                message="Negative product quantity computed; refusing to emit.",
                error_code="INTERNAL_ERROR",
                status_code=500,
            )
        stage_quantities = _round_preserving_total(total_product_kg, schedule.fractions)
        for stage, quantity in zip(schedule.stages, stage_quantities):
            applications.append(
                {
                    "fertilizerType": product.code,
                    "quantityKg": round(quantity, ROUND_DIGITS),
                    "splitStage": stage,
                    "method": schedule.method,
                }
            )

    logger.info(
        "Fertilizer recommendation generated: crop=%s dataset=%s context=%s",
        canonical_crop,
        DATASET_VERSION,
        _interpretation_context,
    )

    return {
        "success": True,
        "deficiencyStatus": deficiency_status,
        "recommendedApplications": applications,
        "guidanceSource": dataset.guidance_source,
    }
