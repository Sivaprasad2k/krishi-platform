"""Versioned agronomic datasets for the deterministic fertilizer engine.

This module is the DATA LAYER ONLY: thresholds, rates, products, stage schedules and
citations live here. Calculation logic lives in app/services/fertilizer.py and MUST NOT
contain agronomic numeric literals.

Yardstick (contract underpinning all entries below):
  engine version = krishi-fertilizer-datasets-2.0.0, verified 2026-10-19.

Dataset 1 - Rice (Paddy)  [VERIFIED]
    Crop & production context : transplanted puddled lowland rice, short-duration
        varieties, tracts OUTSIDE the Cauvery delta and Coimbatore tract (Tamil Nadu
        production system).
    Applicable geographic region : Tamil Nadu (TNAU blanket recommendation). The
        integration contract request carries NO region/season/variety field (integration
        note for Developer A: without such fields only this blanket row can be selected).
    Soil conditions & assumptions : The source explicitly frames this dose as the
        fallback ("blanket recommendation") for when per-soil-test recommendations cannot
        be followed; the engine therefore emits the blanket dose independently of the
        soil classification category (see limitations).
    Soil-test method & reporting conventions :
        N : alkaline permanganate (general chart), elemental N in kg/ha
        P : general chart "Available Phosphorus (P)", elemental P in kg/ha
        K : general chart "Available potassium (K)", elemental K in kg/ha
        INTEGRATION NOTE: the request schema field names (nitrogenKgPerHa,
        phosphorusKgPerHa, potassiumKgPerHa) are interpreted literally as ELEMENTAL
        values. The contract does not state the P/K reporting basis; this must be
        confirmed with Developer A before client integration (never converted silently).
    Recommended nutrient rates (kg/ha) : N 120, P2O5 40, K2O 40.
    Fertilizer products & declared composition :
        UREA 46 % N; SINGLE SUPER PHOSPHATE 16 % P2O5; MURIATE OF POTASH 60 % K2O
        (standard declared commercial grades; verify against the purchased product label).
    Growth-stage allocation (source-quoted) :
        N and K in FOUR EQUAL splits at basal, tillering, panicle initiation and heading
        stages; P 100% basal ("P may be applied as basal and incorporated").
    References (accessed 2026-10-19) :
        - TNAU Agritech Portal, "Crop Production :: Cereals :: Rice :: Transplanted
          Puddled Lowland Rice - Nutrient Management" (page updated Dec 2022):
          https://agritech.tnau.ac.in/agriculture/agri_cropproduction_cereals_rice_tranpudlow_mainfield_nutrient_mgmt_inorganic.html
        - TNAU Agritech Portal, "Resource Management :: Soil :: Soil Rating Chart"
          (© TNAU-2016):
          https://agritech.tnau.ac.in/agriculture/agri_soil_soilratingchart.html
    Known limitations / applicability restrictions :
        - Blanket dose NOT conditioned on classification category: category-specific
          dose adjustment (soil-test-crop-response, "STCR", calibration) is NOT
          implemented and MUST NOT be invented.
        - Classification uses the TNAU GENERAL soil rating chart, not rice-calibrated
          critical limits.
        - Zinc scheduling, green-manure/FYM integration, rock-phosphate substitution,
          region/season/variety-duration context are NOT modelled.
        - pH and organic carbon are validated and recorded for interpretation only;
          they do NOT modify rates.
        - Dose-row selection rule (documented, not agronomic): the blanket row
          120:40:40 (short duration, other tracts) is used because the contract cannot
          disambiguate variety duration / season / tract.

Dataset 2 - Wheat  [VERIFIED]
    Crop & production context : wheat (Triticum aestivum), general TNAU production
        system; blanket recommendation applies when no soil-test-based dose is available.
    Applicable geographic region : Tamil Nadu (TNAU production system).
    Soil conditions & assumptions : as with rice, the source's blanket recommendation
        ("If soil test recommendation is not available, adopt 80:40:40 NPK kg/ha") is the
        documented fallback path the engine implements; no category-to-rate calibration.
    Soil-test method & reporting conventions :
        Dose basis N : P2O5 : K2O. Cross-corroborated by (a) the source's own wording
        "full dose of P2O5 and K2O basally" and (b) the ICAR Rabi Agro-Advisory 2021-22
        which states wheat doses as "kg N, 60 kg P2O5, 40 kg K2O". Classification bands
        are the TNAU general chart (elemental labels) as for rice.
    Recommended nutrient rates (kg/ha) : N 80, P2O5 40, K2O 40.
    Fertilizer products & declared composition : UREA 46 % N; SINGLE SUPER PHOSPHATE
        16 % P2O5; MURIATE OF POTASH 60 % K2O (standard declared commercial grades).
    Growth-stage allocation (source-quoted) :
        N  : half basal before sowing (incorporated in the sowing line), remaining half
             at crown root initiation stage (15-20 DAS).
        P2O5/K2O : full dose basal before sowing, incorporated.
    References (accessed 2026-10-19) :
        - TNAU Agritech Portal, "Nutrient Management :: Wheat" (© TNAU-2016):
          https://agritech.tnau.ac.in/agriculture/agri_nutrientmgt_wheat.html
    Known limitations / applicability restrictions :
        - No verified category-to-rate calibration (STCR); blanket dose emitted
          independently of classification.
        - FYM/compost 12.5 t/ha, lime and micronutrients are NOT modelled.
        - Classification thresholds are the general TNAU soil rating chart, not
          wheat-calibrated critical limits.
        - The ICAR Rabi Agro-Advisory range (120-150:60:40 for timely-sown irrigated
          wheat) differs from the TNAU blanket 80:40:40; the engine uses the TNAU row
          because the request carries no zone/sowing-window context to select between
          them (integration note for Developer A).

Crops registered WITHOUT a verified dataset in v2.0.0 (hard-gated, never estimated):
    Tomato : two distinct TNAU regimens were retrieved (agritech.tnau.ac.in/
        govt_schemes_services/aas/tomato.html and horti_vegetables_tomato_irrigation.html:
        (a) conventional: "N 75 kg, P 100 kg, K 50 kg as basal + 75 kg N on day 30";
        (b) hybrid: "NPK 50:300:50 kg/ha basal + N and K each 150 kg in 3 splits".
        Blocking gap: regimen (a) prints "P"/"K" WITHOUT the P2O5/K2O subscript, so the
        reporting basis cannot be verified from the retrieved text (a 100 kg/ha dose is
        elementally very different from 100 kg/ha P2O5); regimen (b) is a drip-fertigation
        hybrid context with a total-dose structure the current engine does not model.
        Un-gating requires a written basis confirmation from the source or Developer A.

Multi-nutrient products (e.g. DAP 18-46-0): deliberately NOT registered in v1.0.0.
Dataset validation rejects them so nutrient double-counting cannot occur silently;
supporting blended products requires an explicit residual-tracker design plus verified
composition data (future work).
"""

from __future__ import annotations

from dataclasses import dataclass
from typing import Final

DATASET_VERSION: Final[str] = "krishi-fertilizer-datasets-2.0.0"
DATASET_VERIFIED_ON: Final[str] = "2026-10-19"

# ---------------------------------------------------------------------------
# Unit and chemical constants (exact, derived -- no magic numbers)
# ---------------------------------------------------------------------------

# Statutory exact conversion: 1 international acre = 4046.8564224 m^2 = 0.40468564224 ha.
HECTARES_PER_ACRE: Final[float] = 0.40468564224

# Molar masses (g/mol) used to derive the elemental/oxide ratios documented below.
MOLAR_MASS_P: Final[float] = 30.973762
MOLAR_MASS_O: Final[float] = 15.999
MOLAR_MASS_K: Final[float] = 39.0983

# Elemental fraction of the oxide: P = P2O5 x PHOSPHORUS_OXIDE_RATIO (published ~0.4364),
# derived exactly from molar masses in tests.
PHOSPHORUS_OXIDE_P_TO_P2O5: Final[float] = (2 * MOLAR_MASS_P) / (2 * MOLAR_MASS_P + 5 * MOLAR_MASS_O)
POTASSIUM_OXIDE_K_TO_K2O: Final[float] = (2 * MOLAR_MASS_K) / (2 * MOLAR_MASS_K + MOLAR_MASS_O)

# ---------------------------------------------------------------------------
# Data structures
# ---------------------------------------------------------------------------


@dataclass(frozen=True)
class NutrientBand:
    """Soil-test interpretation band.

    Boundary semantics (documented, deterministic, mutually exclusive, gapless):
        value <  deficient_upper      -> DEFICIENT
        deficient_upper <= value <= medium_upper -> MEDIUM
        value >  medium_upper         -> OPTIMAL
    Units/agronomic provenance are carried in ``method``/``source`` for traceability.
    """

    method: str  # soil-test method or provenance note
    unit: str  # kg/ha, etc.
    deficient_upper: float  # exclusive
    medium_upper: float  # inclusive


@dataclass(frozen=True)
class FertilizerProduct:
    """Declared commercial grade of a single fertilizer product.

    ``nutrients`` maps Nutrient -> declared mass FRACTION in the same basis as the
    product grade label (P2O5 for phosphatic grades, K2O for potassic grades).
    """

    code: str  # response fertilizerType (contract naming style)
    display_name: str
    nutrients: tuple[tuple[str, float], ...]


@dataclass(frozen=True)
class StageSchedule:
    """Fractional allocation of a nutrient total across the stages in ``stages``."""

    stages: tuple[str, ...]  # contract splitStage labels ("Basal", ...)
    fractions: tuple[float, ...]
    method: str  # application method text for the response
    source_note: str


@dataclass(frozen=True)
class CropNutrientDataset:
    """A complete, traceable recommendation dataset for one canonical crop."""

    crop_canonical_name: str
    dose_kg_per_ha: dict[str, float]  # keys: "N", "P2O5", "K2O" (target nutrient rate)
    classification: dict[str, NutrientBand]  # keys: "N" (elemental), "P", "K"
    products_by_nutrient: dict[str, FertilizerProduct]  # keys: "N", "P2O5", "K2O"
    schedules: dict[str, StageSchedule]  # keys: "N", "P2O5", "K2O"
    guidance_source: str  # response guidanceSource value
    region_context: str
    selection_rule: str
    references: tuple[str, ...]
    limitations: tuple[str, ...]


class AgronomicConfigError(Exception):
    """Raised when a dataset is internally inconsistent (fail loudly, never silently)."""


# ---------------------------------------------------------------------------
# Dataset validation (defence-in-depth: invalid data can never reach the engine)
# ---------------------------------------------------------------------------


def _validate_dataset(dataset: CropNutrientDataset) -> CropNutrientDataset:
    for nutrient_key, schedule in dataset.schedules.items():
        if not schedule.stages:
            raise AgronomicConfigError(f"{dataset.crop_canonical_name}/{nutrient_key}: empty stage list")
        # Multi-nutrient products (e.g. DAP 18-46-0) are REJECTED at configuration time
        # so a blend can never silently double-count N/P/K through its co-nutrients.
        if len(dataset.products_by_nutrient[nutrient_key].nutrients) != 1:
            raise AgronomicConfigError(
                f"{dataset.crop_canonical_name}/{nutrient_key}: multi-nutrient products are "
                "rejected until a residual-tracking solver is implemented"
            )
        if len(schedule.fractions) != len(schedule.stages):
            raise AgronomicConfigError(
                f"{dataset.crop_canonical_name}/{nutrient_key}: fractions/stages length mismatch"
            )
        if any(f <= 0.0 or f > 1.0 for f in schedule.fractions):
            raise AgronomicConfigError(
                f"{dataset.crop_canonical_name}/{nutrient_key}: fractions must be in (0, 1]"
            )
        total = sum(schedule.fractions)
        if abs(total - 1.0) > 1e-9:
            raise AgronomicConfigError(
                f"{dataset.crop_canonical_name}/{nutrient_key}: allocations sum to {total}, expected 1.0"
            )
        if len(set(schedule.stages)) != len(schedule.stages):
            raise AgronomicConfigError(
                f"{dataset.crop_canonical_name}/{nutrient_key}: duplicate stage labels"
            )
        if sum(f for _, f in dataset.products_by_nutrient[nutrient_key].nutrients) > 1.0:
            raise AgronomicConfigError(
                f"{dataset.crop_canonical_name}/{nutrient_key}: product nutrient mass fractions "
                "sum exceeds 1.0"
            )
        for _, fraction in dataset.products_by_nutrient[nutrient_key].nutrients:
            if not 0.0 < fraction <= 1.0:
                raise AgronomicConfigError(
                    f"{dataset.crop_canonical_name}/{nutrient_key}: invalid nutrient mass fraction "
                    f"{fraction}"
                )
    return dataset


# ---------------------------------------------------------------------------
# Verified dataset: Rice (Paddy)
# ---------------------------------------------------------------------------

RICE_DATASET: Final[CropNutrientDataset] = CropNutrientDataset(
    crop_canonical_name="Rice (Paddy)",
    # TNAU blanket recommendation: short duration varieties (dry season), tracts other
    # than Cauvery delta & Coimbatore tract: 120 N : 40 P2O5 : 40 K2O kg/ha.
    dose_kg_per_ha={"N": 120.0, "P2O5": 40.0, "K2O": 40.0},
    classification={
        # TNAU general soil rating chart (kg/ha; elemental reporting per chart labels).
        "N": NutrientBand(
            method="TNAU general chart - available N (alkaline permanganate basis)",
            unit="kg/ha",
            deficient_upper=240.0,
            medium_upper=480.0,
        ),
        "P": NutrientBand(
            method="TNAU general chart - available P",
            unit="kg/ha",
            deficient_upper=11.0,
            medium_upper=22.0,
        ),
        "K": NutrientBand(
            method="TNAU general chart - available K",
            unit="kg/ha",
            deficient_upper=110.0,
            medium_upper=280.0,
        ),
    },
    products_by_nutrient={
        "N": FertilizerProduct("UREA", "Urea", (("N", 0.46),)),
        "P2O5": FertilizerProduct(
            "SINGLE_SUPER_PHOSPHATE", "Single Super Phosphate", (("P2O5", 0.16),)
        ),
        "K2O": FertilizerProduct("MURIATE_OF_POTASH", "Muriate of Potash (KCl)", (("K2O", 0.60),)),
    },
    schedules={
        # "Apply N and K in four equal splits viz., basal, tillering, panicle initiation
        # and heading stages." (source: TNAU rice nutrient-management portal page)
        "N": StageSchedule(
            stages=("Basal", "Tillering", "Panicle Initiation", "Heading"),
            fractions=(0.25, 0.25, 0.25, 0.25),
            method="Broadcast uniformly on the moist field as a split dose",
            source_note="TNAU: N and K in four equal splits (basal/tillering/PI/heading)",
        ),
        "P2O5": StageSchedule(
            # "P may be applied as basal and incorporated." (same source)
            stages=("Basal",),
            fractions=(1.0,),
            method="Apply as basal dose and incorporate into the soil",
            source_note="TNAU: P applied as basal and incorporated",
        ),
        "K2O": StageSchedule(
            stages=("Basal", "Tillering", "Panicle Initiation", "Heading"),
            fractions=(0.25, 0.25, 0.25, 0.25),
            method="Broadcast uniformly on the moist field as a split dose",
            source_note="TNAU: N and K in four equal splits (basal/tillering/PI/heading)",
        ),
    },
    guidance_source=(
        "TNAU Agritech Portal - Transplanted Puddled Lowland Rice, Nutrient Management "
        "(blanket recommendation), updated Dec 2022; dataset krishi-fertilizer-datasets-2.0.0"
    ),
    region_context="Tamil Nadu (TNAU production system); tracts outside Cauvery delta / Coimbatore",
    selection_rule=(
        "Request carries no variety/season/region field; TNAU short-duration 'other tracts' "
        "blanket row 120:40:40 selected by documented rule, not by agronomic inference"
    ),
    references=(
        "https://agritech.tnau.ac.in/agriculture/"
        "agri_cropproduction_cereals_rice_tranpudlow_mainfield_nutrient_mgmt_inorganic.html",
        "https://agritech.tnau.ac.in/agriculture/agri_soil_soilratingchart.html",
    ),
    limitations=(
        "Blanket dose is not conditioned on the soil classification category "
        "(no verified soil-test crop response calibration, none invented)",
        "Classification thresholds are the general TNAU soil rating chart, not "
        "rice-calibrated critical limits",
        "Zinc scheduling, green manure/FYM integration and rock phosphate substitution "
        "are not modelled",
        "pH and organic carbon are validated and recorded for interpretation only; "
        "they do not modify rates",
    ),
)

RICE_DATASET: Final[CropNutrientDataset] = _validate_dataset(RICE_DATASET)

# ---------------------------------------------------------------------------
# Verified dataset: Wheat
# ---------------------------------------------------------------------------

WHEAT_DATASET: Final[CropNutrientDataset] = CropNutrientDataset(
    crop_canonical_name="Wheat",
    # TNAU blanket recommendation (Nutrient Management :: Wheat): "80:40:40 NPK kg/ha"
    # (N : P2O5 : K2O), documented for when a soil-test recommendation is not available.
    dose_kg_per_ha={"N": 80.0, "P2O5": 40.0, "K2O": 40.0},
    classification={
        # TNAU general soil rating chart (kg/ha; same protective interpretation as rice).
        "N": NutrientBand(
            method="TNAU general chart - available N (alkaline permanganate basis)",
            unit="kg/ha",
            deficient_upper=240.0,
            medium_upper=480.0,
        ),
        "P": NutrientBand(
            method="TNAU general chart - available P",
            unit="kg/ha",
            deficient_upper=11.0,
            medium_upper=22.0,
        ),
        "K": NutrientBand(
            method="TNAU general chart - available K",
            unit="kg/ha",
            deficient_upper=110.0,
            medium_upper=280.0,
        ),
    },
    products_by_nutrient={
        "N": FertilizerProduct("UREA", "Urea", (("N", 0.46),)),
        "P2O5": FertilizerProduct(
            "SINGLE_SUPER_PHOSPHATE", "Single Super Phosphate", (("P2O5", 0.16),)
        ),
        "K2O": FertilizerProduct("MURIATE_OF_POTASH", "Muriate of Potash (KCl)", (("K2O", 0.60),)),
    },
    schedules={
        "N": StageSchedule(
            stages=("Basal", "Crown Root Initiation"),
            fractions=(0.5, 0.5),
            method=(
                "Apply half the total N basally before sowing, incorporated in the sowing "
                "line; broadcast the remainder uniformly on the moist field"
            ),
            source_note=(
                "TNAU: half of N basal before sowing; remaining half at crown root "
                "initiation stage (15-20 DAS)"
            ),
        ),
        "P2O5": StageSchedule(
            stages=("Basal",),
            fractions=(1.0,),
            method="Apply full dose basally before sowing and incorporate in the sowing line",
            source_note="TNAU: full dose of P2O5 basally before sowing and incorporated",
        ),
        "K2O": StageSchedule(
            stages=("Basal",),
            fractions=(1.0,),
            method="Apply full dose basally before sowing and incorporate in the sowing line",
            source_note="TNAU: full dose of K2O basally before sowing and incorporated",
        ),
    },
    guidance_source=(
        "TNAU Agritech Portal - Nutrient Management :: Wheat (blanket recommendation "
        "80:40:40), TNAU-2016; dataset krishi-fertilizer-datasets-2.0.0"
    ),
    region_context="Tamil Nadu (TNAU production system)",
    selection_rule=(
        "Request carries no zone/sowing-window context; TNAU blanket row 80:40:40 "
        "selected by documented rule (ICAR Rabi Advisory range 120-150:60:40 not "
        "selectable without that context)"
    ),
    references=(
        "https://agritech.tnau.ac.in/agriculture/agri_nutrientmgt_wheat.html",
    ),
    limitations=(
        "Blanket dose is not conditioned on the soil classification category "
        "(no verified soil-test crop response calibration, none invented)",
        "FYM/compost 12.5 t/ha, lime and micronutrient recommendations are not modelled",
        "Classification thresholds are the general TNAU soil rating chart, not "
        "wheat-calibrated critical limits",
        "Dose basis follows the source NPK convention (N : P2O5 : K2O), corroborated by "
        "the source wording and the ICAR Rabi Agro-Advisory 2021-22",
    ),
)

WHEAT_DATASET: Final[CropNutrientDataset] = _validate_dataset(WHEAT_DATASET)

# ---------------------------------------------------------------------------
# Registry
# ---------------------------------------------------------------------------

# Canonical crops recognised by the engine (mirrors contract supportedCrops order
# reported by /api/v1/ai/version). Canonical names are exact; no silent aliasing
# (e.g. "paddy" or "Rice" are NOT accepted as "Rice (Paddy)").
SUPPORTED_CROPS: Final[tuple[str, ...]] = ("Rice (Paddy)", "Tomato", "Wheat")

# Fully verified datasets available for the success path.
VERIFIED_DATASETS: Final[dict[str, CropNutrientDataset]] = {
    RICE_DATASET.crop_canonical_name: RICE_DATASET,
    WHEAT_DATASET.crop_canonical_name: WHEAT_DATASET,
}

# Crops the contract recognises but for which no verified dataset exists yet.
CROPS_WITH_MISSING_CALIBRATION: Final[dict[str, tuple[str, ...]]] = {
    "Tomato": (  # future candidates, never silently substituted
        "Two TNAU regimens retrieved but the nutrient reporting basis cannot be verified "
        "from the source text ('P 100 kg' without a P2O5 subscript vs the hybrid drip "
        "total-dose structure); see module docstring for the exact blocking gap"
    ),
}


def soil_reaction_class(ph: float) -> str:
    """TNAU soil rating chart pH classes (interpretive context only, no rate effect).

    Source table: Acids <6.0; "Normal to Saline" 6.0-8.5; "Tending to become Alkaline"
    8.9-9.0; Alkaline >9.0. The 8.9 lower bound is treated as a typo for 8.5 (values in
    (8.5, 9.0] report "tending alkaline"); the anomaly is retained in the limitation note.
    """
    if ph < 6.0:
        return "ACIDIC"
    if ph <= 8.5:
        return "NORMAL_TO_SALINE"
    if ph <= 9.0:
        return "TENDING_ALKALINE"
    return "ALKALINE"


def organic_carbon_class(percent: float) -> str:
    """TNAU soil rating chart organic-carbon classes (interpretive context only).

    Source row (verbatim): "<0.5 %", "0.5 - 7.5 %", ">0.75 %". The "7.5" middle band is
    treated as a typo for 0.75 - corroborated by the source's own High band ">0.75 %",
    which is only coherent if the medium band ends at 0.75. Boundaries: 0.5 -> MEDIUM,
    0.75 -> MEDIUM, >0.75 -> HIGH.
    """
    if percent < 0.5:
        return "LOW"
    if percent <= 0.75:
        return "MEDIUM"
    return "HIGH"
