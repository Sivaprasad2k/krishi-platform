# Krishi Platform — AI Service Integration Contract

**Document Version:** 1.0.0
**Consumers:** Developer A (Core Platform) & Developer B (AI Microservice)
**Protocol:** HTTP/1.1 REST + JSON (Multipart for images)
**Security:** Shared Secret API Key (`X-Krishi-Internal-Key`)

---

## 1. Architectural Relationship & Data Ownership

```
  +----------------------+                     +----------------------+
  |    Core Platform     |                     |     AI Service       |
  |     (Developer A)    |                     |    (Developer B)     |
  |                      |                     |                      |
  |  Spring Boot Backend |                     |   FastAPI Microservice
  |                      |                     |                      |
  |  Owns Persistence:   |                     |  Stateless Engine:   |
  |  - disease_incidents | ===== HTTP REST ===>|  - ResNet / Vision   |
  |  - treatments        |   (Signed Header)   |  - LLM Doctor        |
  |  - tasks & plans     |                     |  - N-P-K Calibrator  |
  +----------------------+                     +----------------------+
```

### Invariants:
1. **Stateless AI Processing:** The Python AI service does **not** read from or write to the Core PostgreSQL database directly. All context is passed via request parameters.
2. **Persistence Boundary:** The Core Platform is the sole system of record. When disease or diagnosis results return, the Core Platform creates `DiseaseIncident`, `Treatment`, and associated `Task` entities.
3. **Graceful Degradation:** If the AI service times out or is unreachable, the Core Platform must fail gracefully (e.g. allowing manual incident logging and predefined ICAR reference fallback).

---

## 2. Service Health & Identification Endpoints

### 2.1 Health Probe
- **Endpoint:** `GET /health`
- **Response:** `200 OK`
```json
{
  "status": "UP",
  "service": "krishi-ai-service",
  "timestamp": "2026-10-13T10:00:00Z"
}
```

### 2.2 Model Version Probe
- **Endpoint:** `GET /api/v1/ai/version`
- **Response:** `200 OK`
```json
{
  "serviceVersion": "1.0.0",
  "diseaseModelVersion": "plant-vision-v2.1",
  "doctorModel": "agri-llm-lora-v1",
  "supportedCrops": ["Rice (Paddy)", "Tomato", "Wheat"]
}
```

---

## 3. Core Functional Interfaces

### 3.1 Computer Vision Disease Diagnosis
Used when a farmer photographs a diseased crop or distressed foliage.

- **Endpoint:** `POST /api/v1/ai/disease-detection/diagnose`
- **Content-Type:** `multipart/form-data`
- **Headers:** `X-Krishi-Internal-Key: <configured-secret>`

#### Request Parameters:
| Field | Type | Required | Description |
| :--- | :--- | :--- | :--- |
| `image` | Binary File | Yes | JPEG or PNG image of infected foliage (max 10MB) |
| `cropName` | String | Yes | Crop name (e.g. `Rice (Paddy)`, `Tomato`, `Wheat`) |
| `cropVariety` | String | No | Crop variety if known (e.g. `Samba Masoori`) |
| `growthStage` | String | No | Current stage (e.g. `Tillering`, `Vegetative`) |
| `observedSymptoms` | String | No | Optional notes supplied by farmer |

#### Success Response (`200 OK`):
```json
{
  "success": true,
  "diagnosisId": "ai-diag-88492024",
  "diseaseDetected": true,
  "diseaseCode": "RICE_BLAST",
  "diseaseName": "Blast Disease (Magnaporthe oryzae)",
  "confidence": 0.94,
  "severity": "HIGH",
  "symptomsIdentified": [
    "Spindle-shaped lesions with grayish centers",
    "Brown margins on leaf blade"
  ],
  "recommendedTreatment": {
    "treatmentName": "Tricyclazole 75% WP Spray",
    "organicAlternative": "Pseudomonas fluorescens 0.5% foliar spray",
    "dosageInstructions": "Apply Tricyclazole @ 0.6 g/liter of water upon early symptom appearance.",
    "precautions": "Ensure protective gear during application. Do not spray during windy hours.",
    "packageSource": "ICAR Directorate of Rice Research"
  }
}
```

---

### 3.2 AI Doctor Consultative Advisory
Provides contextual question-answering for cultivation questions, pest inquiries, and anomalous crop conditions.

- **Endpoint:** `POST /api/v1/ai/doctor/consult`
- **Content-Type:** `application/json`
- **Headers:** `X-Krishi-Internal-Key: <configured-secret>`

#### Request Body:
```json
{
  "farmerId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "query": "My tomato leaves are curling upwards and turning yellow at the margins. What should I do?",
  "language": "en",
  "cropContext": {
    "cropName": "Tomato",
    "cropVariety": "Arka Rakshak",
    "daysSincePlanting": 45,
    "currentGrowthStage": "Flowering"
  }
}
```

#### Success Response (`200 OK`):
```json
{
  "success": true,
  "responseId": "ai-doc-19382",
  "advice": "Upward leaf curling with marginal chlorosis in tomato commonly indicates Tomato Leaf Curl Virus (ToLCV) transmitted by whiteflies, or severe moisture stress.",
  "recommendedActions": [
    "Inspect the undersides of leaves for tiny whitefly vectors.",
    "If whiteflies are present, deploy yellow sticky traps (10-12 traps/acre).",
    "Apply Neem oil (5ml/liter) or systemic insecticide as per ICAR package of practices if vector population is high.",
    "Rogue out and destroy severely stunted infected plants to prevent viral spread."
  ],
  "warningDisclaimer": "This advice is generated based on certified agronomic guidelines. For commercial chemical applications, consult your local Krishi Vigyan Kendra (KVK).",
  "source": "ICAR Indian Institute of Horticultural Research"
}
```

---

### 3.3 Fertilizer & Nutrient Recommendation
Calculates precise N-P-K mineral application dosages based on soil test parameters and field size.

- **Endpoint:** `POST /api/v1/ai/fertilizer/recommend`
- **Content-Type:** `application/json`

#### Request Body:
```json
{
  "cropName": "Rice (Paddy)",
  "fieldAreaAcres": 2.5,
  "soilTest": {
    "nitrogenKgPerHa": 180.0,
    "phosphorusKgPerHa": 14.0,
    "potassiumKgPerHa": 210.0,
    "ph": 6.8,
    "organicCarbonPercent": 0.45
  },
  "growthStage": "Tillering"
}
```

#### Success Response (`200 OK`):
```json
{
  "success": true,
  "deficiencyStatus": {
    "nitrogen": "DEFICIENT",
    "phosphorus": "MEDIUM",
    "potassium": "OPTIMAL"
  },
  "recommendedApplications": [
    {
      "fertilizerType": "UREA",
      "quantityKg": 32.5,
      "splitStage": "Active Tillering Split",
      "method": "Broadcast in moist soil following weeding"
    },
    {
      "fertilizerType": "ZINC_SULPHATE_HEPTAHYDRATE",
      "quantityKg": 12.5,
      "splitStage": "Basal / Early Tillering",
      "method": "Soil application (do not mix directly with phosphatic fertilizers)"
    }
  ],
  "guidanceSource": "ICAR Nutrient Management Guidelines"
}
```

---

## 4. SLA, Timeouts, and Error Handling

| Service Call | Timeout Threshold | Retry Policy | Fallback Behavior |
| :--- | :--- | :--- | :--- |
| Disease Detection | 8,000 ms | 1 retry with exponential backoff | Allow farmer to manually report problem with photo attached |
| AI Doctor | 6,000 ms | 0 retries (immediate timeout) | Offer emergency hotline & ICAR manual advisory lookup |
| Fertilizer Recommend | 4,000 ms | 1 retry | Fall back to standard basal dosage from `CultivationTemplate` |

### Error Format:
```json
{
  "success": false,
  "errorCode": "INVALID_IMAGE_PAYLOAD",
  "message": "Foliage could not be identified in the uploaded image. Please ensure clear lighting and centered focus on affected leaves."
}
```
