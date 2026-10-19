"""Deep-copied contract example responses used by the Phase-1 stub endpoints.

Each sample is the verbatim success response from docs/integration/AI_INTEGRATION_CONTRACT.md
sections 3.1, 3.2 and 3.3 so that Developer A can build the Core Platform client against the
exact shapes before real model inference lands in Milestones 5/4.
"""

import copy
from typing import Any

DISEASE_DIAGNOSIS_SAMPLE: dict[str, Any] = {
    "success": True,
    "diagnosisId": "ai-diag-88492024",
    "diseaseDetected": True,
    "diseaseCode": "RICE_BLAST",
    "diseaseName": "Blast Disease (Magnaporthe oryzae)",
    "confidence": 0.94,
    "severity": "HIGH",
    "symptomsIdentified": [
        "Spindle-shaped lesions with grayish centers",
        "Brown margins on leaf blade",
    ],
    "recommendedTreatment": {
        "treatmentName": "Tricyclazole 75% WP Spray",
        "organicAlternative": "Pseudomonas fluorescens 0.5% foliar spray",
        "dosageInstructions": "Apply Tricyclazole @ 0.6 g/liter of water upon early symptom appearance.",
        "precautions": "Ensure protective gear during application. Do not spray during windy hours.",
        "packageSource": "ICAR Directorate of Rice Research",
    },
}

DOCTOR_CONSULT_SAMPLE: dict[str, Any] = {
    "success": True,
    "responseId": "ai-doc-19382",
    "advice": "Upward leaf curling with marginal chlorosis in tomato commonly indicates Tomato Leaf Curl "
    "Virus (ToLCV) transmitted by whiteflies, or severe moisture stress.",
    "recommendedActions": [
        "Inspect the undersides of leaves for tiny whitefly vectors.",
        "If whiteflies are present, deploy yellow sticky traps (10-12 traps/acre).",
        "Apply Neem oil (5ml/liter) or systemic insecticide as per ICAR package of practices if vector "
        "population is high.",
        "Rogue out and destroy severely stunted infected plants to prevent viral spread.",
    ],
    "warningDisclaimer": "This advice is generated based on certified agronomic guidelines. For commercial "
    "chemical applications, consult your local Krishi Vigyan Kendra (KVK).",
    "source": "ICAR Indian Institute of Horticultural Research",
}

FERTILIZER_RECOMMENDATION_SAMPLE: dict[str, Any] = {
    "success": True,
    "deficiencyStatus": {
        "nitrogen": "DEFICIENT",
        "phosphorus": "MEDIUM",
        "potassium": "OPTIMAL",
    },
    "recommendedApplications": [
        {
            "fertilizerType": "UREA",
            "quantityKg": 32.5,
            "splitStage": "Active Tillering Split",
            "method": "Broadcast in moist soil following weeding",
        },
        {
            "fertilizerType": "ZINC_SULPHATE_HEPTAHYDRATE",
            "quantityKg": 12.5,
            "splitStage": "Basal / Early Tillering",
            "method": "Soil application (do not mix directly with phosphatic fertilizers)",
        },
    ],
    "guidanceSource": "ICAR Nutrient Management Guidelines",
}


def stub_sample(sample: dict[str, Any]) -> dict[str, Any]:
    """Return a deep copy so no handler can mutate shared sample state."""
    return copy.deepcopy(sample)
