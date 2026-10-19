# Krishi AI Service

FastAPI microservice owned by **Developer B**, implementing the AI side of
`docs/integration/AI_INTEGRATION_CONTRACT.md`.

- Stateless: the AI service never touches the Core PostgreSQL database. All context arrives via request parameters. The Core Platform (Developer A) remains the sole system of record (incidents, treatments, tasks).
- Auth: every endpoint except `GET /health` requires the shared secret header `X-Krishi-Internal-Key`.
- Errors: every failure renders the contract section-4 envelope `{"success": false, "errorCode": "...", "message": "..."}`.

## Endpoints (Milestone 4 - Phase 1 complete)

| Endpoint | Method | Auth | Status |
| :--- | :--- | :--- | :--- |
| `/health` | GET | none | Implemented (contract 2.1) |
| `/api/v1/ai/version` | GET | key | Implemented, stub model values (contract 2.2) |
| `/api/v1/ai/fertilizer/recommend` | POST | key | **Implemented** - verified TNAU datasets for Rice (Paddy) and Wheat; Tomato hard-gated (MISSING_CALIBRATION_DATA, basis unverified in source text) |
| `/api/v1/ai/doctor/consult` | POST | key | Contract stub - LLM wiring next |
| `/api/v1/ai/disease-detection/diagnose` | POST | key | Contract stub - ONNX inference in Milestone 5 |

The three functional endpoints are **contract stubs**: they validate the transport
(multipart / JSON), image constraints (10 MB, JPEG/PNG), required fields, and return the
verbatim `docs/integration/AI_INTEGRATION_CONTRACT.md` sample success responses so that
Developer A can build and test the Core Platform client today.

## Run

```bash
cd ai-service
docker compose up --build
```

Service runs on `http://localhost:8000`. Interactive docs at `/docs`.

## Test

```bash
docker compose run --rm ai pytest -q
```

## Configuration

| Env var | Purpose | Default |
| :--- | :--- | :--- |
| `KRISHI_AI_INTERNAL_KEY` | Shared secret with the Core Platform; must match Developer A's configured value | `dev-internal-key` (dev only) |
| `KRISHI_AI_ENV` | `dev` \| `prod` | `dev` |
| `KRISHI_AI_DISEASE_MODEL_VERSION` | Reported by `/api/v1/ai/version` | `plant-vision-v2.1` |
| `KRISHI_AI_DOCTOR_MODEL` | Reported by `/api/v1/ai/version` | `agri-llm-lora-v1` |

SLA expectations (contract section 4, enforced by the Core Platform client):

| Service | Timeout | Retries |
| :--- | :--- | :--- |
| Disease detection | 8000 ms | 1 retry, exponential backoff |
| AI Doctor | 6000 ms | none |
| Fertilizer recommend | 4000 ms | 1 retry |

## Fertilizer engine (Phase 2)

Architecture: `app/api/fertilizer.py` is a thin HTTP adapter; calculation logic lives in
`app/services/fertilizer.py`; all agronomic values (doses, thresholds, products, stage
splits, citations) live in `app/services/agronomic_data.py` (dataset
`krishi-fertilizer-datasets-2.0.0`, verified 2026-10-19). The calculation is pure and
I/O-free: identical inputs and dataset version always produce the identical response.

### Verified data (Rice (Paddy))

- Dose: 120 N : 40 P2O5 : 40 K2O kg/ha - TNAU blanket recommendation, transplanted
  puddled lowland rice, short-duration varieties outside the Cauvery delta /
  Coimbatore tract (source updates Dec 2022).
- Stage schedule: N and K in four equal splits at basal, tillering, panicle initiation
  and heading; P 100% basal and incorporated (same source).
- Soil classification: TNAU general soil rating chart (N <240 low / 240-480 medium /
  >480 high; P <11 / 11-22 / >22; K <110 / 110-280 / >280 kg/ha; OC <0.5% / 0.5-0.75% /
  >0.75%) - GENERAL bands, not rice-calibrated critical limits.
- Products: UREA (46% N), SINGLE_SUPER_PHOSPHATE (16% P2O5), MURIATE_OF_POTASH (60% K2O)
  - standard declared commercial grades; verify against the purchased product label.
- Conversions: 1 acre = 0.40468564224 ha (exact statutory constant, applied exactly
  once); P2O5->P = x 0.4364 and K2O->K = x 0.8301 derived from molar masses in tests.

### Documented assumptions (confirm with Developer A before client integration)

1. The contract does not state the P/K reporting basis; request fields
   `phosphorusKgPerHa` / `potassiumKgPerHa` are interpreted literally as ELEMENTAL P and
   K, matching the TNAU chart labels. They are never silently converted.
2. `organicCarbonPercent` is a percentage (0-100), never a 0-1 fraction; no silent x100.
3. The request has no region/season/variety-duration field (integration note for
   Developer A); the TNAU 120:40:40 blanket row is selected by documented rule, not
   agronomic inference.
4. `growthStage` is accepted for contract compatibility; the response always contains
   the full split plan (no stage filtering).

### Hard gates (never fabricate)

- Unknown crop -> 400 `UNSUPPORTED_CROP`.
- Wheat (verified in v2, see below) -> full recommendation path.
- Tomato -> 501 `MISSING_CALIBRATION_DATA`: two TNAU regimens were retrieved
  (`govt_schemes_services/aas/tomato.html`), but the conventional one prints its dose as
  "P 100 kg, K 50 kg" WITHOUT a P2O5/K2O subscript, so the reporting basis cannot be
  verified from the source text and the hybrid regimen is a drip total-dose structure the
  engine does not model. Recommendation withheld rather than guessed. The contract's
  documented fallback applies (Developer A: standard basal dosage from CultivationTemplate).
- Multi-nutrient products (e.g. DAP) are rejected at dataset validation so N/P/K
  double-counting is impossible by construction.

### Verified data (Wheat)

- Dose: 80 N : 40 P2O5 : 40 K2O kg/ha - TNAU blanket recommendation
  ("Nutrient Management :: Wheat", for when no soil-test-based dose is available).
- Stage schedule: half the N basal before sowing (incorporated in the sowing line) and
  the remaining half at crown root initiation stage (15-20 DAS); full P2O5 and K2O basal.
- Basis corroboration: source wording "full dose of P2O5 and K2O basally"; the ICAR Rabi
  Agro-Advisory 2021-22 quotes wheat as "kg N, 60 kg P2O5, 40 kg K2O" (a non-selectable
  120-150:60:40 range without zone/sowing-window context - documentation note for
  Developer A).
- Hand-computed fixture (2.5 acres): urea total 175.95 kg split Basal 87.97 /
  Crown Root Initiation 87.98; SSP 252.93 kg; MOP 67.45 kg (tests pin these values).

### Known limitations

- Classification is diagnostic only: no verified category-to-rate calibration (STCR)
  exists, so the blanket dose is emitted regardless of category and factors are NOT
  invented.
- pH and organic carbon are validated and interpreted (TNAU soil classes) but do not
  modify rates.
- For Wheat, FYM/compost 12.5 t/ha and micronutrients are not modelled; for Rice, zinc
  scheduling, green manure/FYM integration and rock-phosphate substitution are not
  modelled.
- `docs/integration/AI_INTEGRATION_CONTRACT.md` is now vendored on this branch for
  self-verification; the timeout/retry fallback documented there remains the agreed
  mechanism for `MISSING_CALIBRATION_DATA` responses (confirmation with Developer A
  pending - see audit finding C2).
- Measured latency (test evidence, `test_fertilizer_performance.py`): pure calculation
  ~0.015 ms mean; full endpoint ~4.3 ms mean / <10 ms max - under 0.25% of the 4000 ms
  budget (in-process measurement, not a deployed-environment SLA verification).
