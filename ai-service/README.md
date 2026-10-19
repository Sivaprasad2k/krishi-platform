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
| `/api/v1/ai/fertilizer/recommend` | POST | key | Contract stub - deterministic ICAR calibration next |
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
