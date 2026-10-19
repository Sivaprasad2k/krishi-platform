"""Unit, boundary and invariant tests for the deterministic fertilizer engine.

Exact expected values are derived from the verified TNAU rice dataset fixture
(app/services/agronomic_data.py) and hand-computed below.
"""

import math

import pytest

from app.core.errors import KrishiAiError
from app.services import fertilizer as engine
from app.services.agronomic_data import (
    AgronomicConfigError,
    CropNutrientDataset,
    FertilizerProduct,
    HECTARES_PER_ACRE,
    PHOSPHORUS_OXIDE_P_TO_P2O5,
    POTASSIUM_OXIDE_K_TO_K2O,
    RICE_DATASET,
    StageSchedule,
    _validate_dataset,
    organic_carbon_class,
    soil_reaction_class,
)

# ---------------------------------------------------------------------------
# Hand-computed verified fixture: contract sample soil with 2.5 acres
# area_ha   = 2.5 x 0.40468564224          = 1.0117141056
# N need    = 120 x 1.0117141056           = 121.405692672 kg  ->  urea / 0.46
# urea total= 121.405692672 / 0.46         = 263.92541...      -> 263.93 (2 dp)
# per split = 65.981354713...              -> 65.98 each; residual 0.01 -> Basal 65.99
# P2O5 need = 40 x 1.0117141056            = 40.468564224 kg -> SSP / 0.16 = 252.9285264
# MOP total = 40.468564224 / 0.60          = 67.44760704 -> 67.45; stages 16.86190176
#                                              -> 16.86 each; residual 0.01 -> Basal 16.87
# ---------------------------------------------------------------------------

FIXTURE_SOIL = dict(nitrogen=300.0, phosphorus=10.0, potassium=300.0, ph=6.8, organic_carbon=0.45)


def recommend(**overrides):
    args = {**FIXTURE_SOIL, **overrides}
    return engine.compute_recommendation(
        crop_name=overrides.pop("crop_name", "Rice (Paddy)"),
        field_area_acres=overrides.pop("field_area_acres", 2.5),
        soil_nitrogen=args["nitrogen"],
        soil_phosphorus=args["phosphorus"],
        soil_potassium=args["potassium"],
        ph=args["ph"],
        organic_carbon_percent=args["organic_carbon"],
    )


def applications_for(resp, fertilizer_type):
    return [a for a in resp["recommendedApplications"] if a["fertilizerType"] == fertilizer_type]


# ---------------------------------------------------------------------------
# Constants and dataset-level guarantees
# ---------------------------------------------------------------------------


def test_acre_conversion_is_the_exact_statutory_constant():
    assert HECTARES_PER_ACRE == 0.40468564224  # 1 acre = 4046.8564224 m^2 exactly


@pytest.mark.parametrize(
    ("derived", "published", "tolerance"),
    [
        (PHOSPHORUS_OXIDE_P_TO_P2O5, 0.4364, 5e-4),
        (POTASSIUM_OXIDE_K_TO_K2O, 0.8301, 5e-4),
    ],
)
def test_oxide_ratios_match_published_stoichiometry(derived, published, tolerance):
    assert math.isclose(derived, published, abs_tol=tolerance)


def test_rice_dataset_stage_allocations_sum_to_one():
    for nutrient_key, schedule in RICE_DATASET.schedules.items():
        assert sum(schedule.fractions) == pytest.approx(1.0, abs=1e-9), (nutrient_key,)


def test_multi_nutrient_products_are_rejected_at_config_time():
    dap = FertilizerProduct("DI_AMMONIUM_PHOSPHATE", "DAP", (("N", 0.18), ("P2O5", 0.46)))
    bad = CropNutrientDataset(
        crop_canonical_name="FixtureCrop",
        dose_kg_per_ha=RICE_DATASET.dose_kg_per_ha,
        classification=RICE_DATASET.classification,
        products_by_nutrient={"N": dap, "P2O5": RICE_DATASET.products_by_nutrient["P2O5"],
                              "K2O": RICE_DATASET.products_by_nutrient["K2O"]},
        schedules=RICE_DATASET.schedules,
        guidance_source="fixture",
        region_context="fixture",
        selection_rule="fixture",
        references=(),
        limitations=(),
    )
    with pytest.raises(AgronomicConfigError):
        _validate_dataset(bad)


def test_inconsistent_stage_allocation_fails_config_validation():
    bad = CropNutrientDataset(
        crop_canonical_name="FixtureCrop",
        dose_kg_per_ha=RICE_DATASET.dose_kg_per_ha,
        classification=RICE_DATASET.classification,
        products_by_nutrient=RICE_DATASET.products_by_nutrient,
        schedules={
            "N": StageSchedule(stages=("A", "B"), fractions=(0.8, 0.1), method="m", source_note="s"),
            "P2O5": RICE_DATASET.schedules["P2O5"],
            "K2O": RICE_DATASET.schedules["K2O"],
        },
        guidance_source="fixture",
        region_context="fixture",
        selection_rule="fixture",
        references=(),
        limitations=(),
    )
    with pytest.raises(AgronomicConfigError, match="allocations sum"):
        _validate_dataset(bad)


# ---------------------------------------------------------------------------
# Classification boundary semantics (exact + immediately-adjacent values)
# ---------------------------------------------------------------------------


@pytest.mark.parametrize(
    ("nutrient", "value", "expected"),
    [
        # N: chart "<240 low, 240-480 medium, >480 high" (bounds inclusive-lower/exclusive-upper)
        ("N", 0.0, "DEFICIENT"),
        ("N", 239.9, "DEFICIENT"),
        ("N", 240.0, "MEDIUM"),
        ("N", 480.0, "MEDIUM"),  # exact upper threshold stays MEDIUM (>480 is high)
        ("N", 480.1, "OPTIMAL"),
        ("N", 10_000.0, "OPTIMAL"),
        # P: "<11 low, 11-22 medium, >22 high"
        ("P", 0.0, "DEFICIENT"),
        ("P", 10.9, "DEFICIENT"),
        ("P", 11.0, "MEDIUM"),
        ("P", 22.0, "MEDIUM"),
        ("P", 22.1, "OPTIMAL"),
        # K: "<110 low, 110-280 medium, >280 high"
        ("K", 0.0, "DEFICIENT"),
        ("K", 109.9, "DEFICIENT"),
        ("K", 110.0, "MEDIUM"),
        ("K", 280.0, "MEDIUM"),
        ("K", 280.1, "OPTIMAL"),
    ],
)
def test_classification_boundaries_are_mutually_exclusive(nutrient, value, expected):
    band = RICE_DATASET.classification[nutrient]
    assert engine.classify_soil_nutrient(value, band.deficient_upper, band.medium_upper) == expected


@pytest.mark.parametrize(
    ("value", "expected"),
    [(0.0, "LOW"), (0.49, "LOW"), (0.5, "MEDIUM"), (0.75, "MEDIUM"), (0.8, "HIGH")],
)
def test_organic_carbon_boundaries(value, expected):
    assert organic_carbon_class(value) == expected


@pytest.mark.parametrize(
    ("value", "expected"),
    [
        (0.0, "ACIDIC"),
        (5.9, "ACIDIC"),
        (6.0, "NORMAL_TO_SALINE"),
        (8.5, "NORMAL_TO_SALINE"),
        (9.0, "TENDING_ALKALINE"),
        (9.5, "ALKALINE"),
    ],
)
def test_soil_reaction_boundaries(value, expected):
    assert soil_reaction_class(value) == expected


# ---------------------------------------------------------------------------
# Area conversion and quantity scaling
# ---------------------------------------------------------------------------


def test_acre_conversion_applied_once():
    assert engine.field_area_hectares(2.5) == pytest.approx(1.0117141056, abs=1e-12)


def test_doubling_area_doubles_unrounded_requirements():
    single = RICE_DATASET.dose_kg_per_ha["N"] * engine.field_area_hectares(1.0)
    double = RICE_DATASET.dose_kg_per_ha["N"] * engine.field_area_hectares(2.0)
    assert double == pytest.approx(2 * single, rel=1e-12)


def test_product_quantities_are_finite_and_nonnegative():
    for area in (0.5, 2.5, 100.0):
        resp = recommend(field_area_acres=area)
        for application in resp["recommendedApplications"]:
            q = application["quantityKg"]
            assert math.isfinite(q) and q >= 0.0


# ---------------------------------------------------------------------------
# Exact verified values (TNAU rice dataset) and stage-total preservation
# ---------------------------------------------------------------------------


def test_rice_recommendation_exact_application_plan():
    resp = recommend()
    urea = applications_for(resp, "UREA")
    ssp = applications_for(resp, "SINGLE_SUPER_PHOSPHATE")
    mop = applications_for(resp, "MURIATE_OF_POTASH")
    # Urea: 4 equal N splits; residual 0.01 kg distributed to Basal (largest remainder)
    assert [(a["splitStage"], a["quantityKg"]) for a in urea] == [
        ("Basal", 65.99),
        ("Tillering", 65.98),
        ("Panicle Initiation", 65.98),
        ("Heading", 65.98),
    ]
    # SSP: full dose basal and incorporated
    assert [(a["splitStage"], a["quantityKg"]) for a in ssp] == [("Basal", 252.93)]
    # MOP: 4 equal K splits; residual 0.01 kg distributed to Basal
    assert [(a["splitStage"], a["quantityKg"]) for a in mop] == [
        ("Basal", 16.87),
        ("Tillering", 16.86),
        ("Panicle Initiation", 16.86),
        ("Heading", 16.86),
    ]


def test_wheat_recommendation_exact_application_plan():
    # Hand-computed TNAU wheat fixture (2.5 acres, dose 80:40:40 N:P2O5:K2O kg/ha):
    # area = 1.0117141056 ha; N need = 80 x area = 80.937128448 kg
    #   urea total = /0.46 = 175.9502809 -> 175.95; per stage 87.97514 -> 87.98 each,
    #   sum 175.96 vs total 175.95 => NEGATIVE residual -0.01 -> Basal 87.97 (lowest index)
    #   [exercises the negative-residual branch of the rounding algorithm]
    # SSP total = 40 x area / 0.16 = 252.9285264 -> 252.93 (single basal)
    # MOP total = 40 x area / 0.60 = 67.44760704 -> 67.45 (single basal)
    resp = recommend(crop_name="Wheat")
    assert [(a["splitStage"], a["quantityKg"]) for a in applications_for(resp, "UREA")] == [
        ("Basal", 87.97),
        ("Crown Root Initiation", 87.98),
    ]
    assert [
        (a["splitStage"], a["quantityKg"])
        for a in applications_for(resp, "SINGLE_SUPER_PHOSPHATE")
    ] == [("Basal", 252.93)]
    assert [
        (a["splitStage"], a["quantityKg"])
        for a in applications_for(resp, "MURIATE_OF_POTASH")
    ] == [("Basal", 67.45)]
    assert resp["success"] is True


def test_wheat_stage_quantities_sum_exactly_to_total():
    resp = recommend(crop_name="Wheat")
    urea_total = round(80.0 * 1.0117141056 / 0.46, 2)
    assert round(sum(a["quantityKg"] for a in applications_for(resp, "UREA")), 2) == urea_total == 175.95
    assert round(sum(a["quantityKg"] for a in applications_for(resp, "SINGLE_SUPER_PHOSPHATE")), 2) == 252.93
    assert round(sum(a["quantityKg"] for a in applications_for(resp, "MURIATE_OF_POTASH")), 2) == 67.45


def test_stage_quantities_sum_exactly_to_total():
    resp = recommend()
    by_type = {}
    for a in resp["recommendedApplications"]:
        by_type.setdefault(a["fertilizerType"], []).append(a["quantityKg"])
    urea_total = round(120.0 * 1.0117141056 / 0.46, 2)
    ssp_total = round(40.0 * 1.0117141056 / 0.16, 2)
    mop_total = round(40.0 * 1.0117141056 / 0.60, 2)
    assert round(sum(by_type["UREA"]), 2) == urea_total == 263.93
    assert round(sum(by_type["SINGLE_SUPER_PHOSPHATE"]), 2) == ssp_total == 252.93
    assert round(sum(by_type["MURIATE_OF_POTASH"]), 2) == mop_total == 67.45


def test_product_nutrient_contribution_reconciles_with_requirement():
    # SSP grade 16% P2O5: field P2O5 must equal quantity x 0.16 (within rounding).
    resp = recommend()
    ssp_total = sum(a["quantityKg"] for a in applications_for(resp, "SINGLE_SUPER_PHOSPHATE"))
    expected_p2o5 = 40.0 * 1.0117141056
    assert ssp_total * 0.16 == pytest.approx(expected_p2o5, abs=5e-2)
    # Cross-check the elemental convention: document, never silently switch basis.
    elemental_p = ssp_total * 0.16 * PHOSPHORUS_OXIDE_P_TO_P2O5
    assert elemental_p < expected_p2o5  # sanity: elemental mass is a fraction of oxide mass


def test_classification_does_not_modify_blanket_dose():
    # No verified category->rate calibration exists; the blanket dose must be identical
    # regardless of category outcomes.
    low = recommend(nitrogen=0.0, phosphorus=0.0, potassium=0.0)
    high = recommend(nitrogen=10_000.0, phosphorus=10_000.0, potassium=10_000.0)
    assert [a["quantityKg"] for a in low["recommendedApplications"]] == [
        a["quantityKg"] for a in high["recommendedApplications"]
    ]
    assert low["deficiencyStatus"] == {
        "nitrogen": "DEFICIENT",
        "phosphorus": "DEFICIENT",
        "potassium": "DEFICIENT",
    }
    assert high["deficiencyStatus"] == {
        "nitrogen": "OPTIMAL",
        "phosphorus": "OPTIMAL",
        "potassium": "OPTIMAL",
    }


def test_identical_inputs_produce_identical_responses():
    first, second = recommend(), recommend()
    assert first == second


# ---------------------------------------------------------------------------
# Deterministic gates
# ---------------------------------------------------------------------------


def test_unsupported_crop_is_rejected_explicitly():
    with pytest.raises(KrishiAiError) as excinfo:
        recommend(crop_name="Maize")
    err = excinfo.value
    assert err.error_code == "UNSUPPORTED_CROP"
    assert err.status_code == 400
    assert "Maize" in err.message
    assert "Rice (Paddy)" in err.message  # supported list surfaced to the caller


@pytest.mark.parametrize("canonical_alias", ["rice (paddy)", "RICE (PADDY)"])
def test_canonical_crop_matching_is_case_insensitive_exact(canonical_alias):
    resp = engine.compute_recommendation(
        crop_name=canonical_alias, field_area_acres=2.5, soil_nitrogen=300.0,
        soil_phosphorus=10.0, soil_potassium=300.0, ph=6.8, organic_carbon_percent=0.45,
    )
    assert resp["success"] is True


@pytest.mark.parametrize("bare_alias", ["Rice", "paddy", "Paddy Rice"])
def test_bare_aliases_are_not_silently_mapped(bare_alias):
    with pytest.raises(KrishiAiError) as excinfo:
        recommend(crop_name=bare_alias)
    assert excinfo.value.error_code == "UNSUPPORTED_CROP"


@pytest.mark.parametrize("crop", ["Tomato"])
def test_crops_with_missing_calibration_fail_loudly(crop):
    with pytest.raises(KrishiAiError) as excinfo:
        recommend(crop_name=crop)
    err = excinfo.value
    assert err.error_code == "MISSING_CALIBRATION_DATA"
    assert err.status_code == 501
    assert crop in err.message
    assert "krishi-fertilizer-datasets-2.0.0" in err.message


def test_missing_calibration_gate_mechanism_for_any_unverified_dataset(monkeypatch):
    """The gate mechanism itself stays covered even after crops graduate to v2 datasets.

    Note: non-contract crop names are rejected upstream as UNSUPPORTED_CROP (they must
    never be re-interpreted as missing data), so the gate is probed at its unit level.
    """
    from app.services import fertilizer as engine
    from app.services.agronomic_data import CROPS_WITH_MISSING_CALIBRATION

    monkeypatch.setitem(CROPS_WITH_MISSING_CALIBRATION, "FixtureCrop", ("no complete verified data",))
    with pytest.raises(KrishiAiError) as excinfo:
        engine._resolve_dataset("FixtureCrop")
    err = excinfo.value
    assert err.error_code == "MISSING_CALIBRATION_DATA"
    assert err.status_code == 501
    assert "FixtureCrop" in err.message
