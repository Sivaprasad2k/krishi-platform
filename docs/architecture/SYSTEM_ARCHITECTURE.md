# Krishi Platform — System Architecture

**Document Version:** 1.0.0
**Status:** Approved Architecture Baseline

---

## 1. System Topology Overview

Krishi is composed of two primary application tiers and an independent machine learning service:

```
 +-------------------------------------------------------------------------+
 |                               Farmer UI                                 |
 |                 React 19 + Tailwind CSS + Lucide (PWA)                  |
 +-----------------------------------+-------------------------------------+
                                     |
                                     | HTTPS / REST (JWT)
                                     v
 +-------------------------------------------------------------------------+
 |                         Krishi Core Backend                             |
 |           Spring Boot 4.x (Java 17) + Spring Security (Method Level)     |
 |                                                                         |
 |  [Identity & Auth]       [Farm Management]     [Crop Knowledge Catalog]  |
 |  [Crop Planting & Plan]  [Task Scheduler]      [Incidents & Treatments]  |
 +-------------------+-----------------------------------+-----------------+
                     |                                   |
                     | PostgreSQL JDBC                   | Internal REST (mTLS / HMAC)
                     v                                   v
 +-----------------------------------+   +---------------------------------+
 |         PostgreSQL 16 DB          |   |     Python AI Microservice      |
 | Schema: 'krishi' (Flyway Managed) |   |    FastAPI + PyTorch / ONNX     |
 |                                   |   |                                 |
 | - users & roles                   |   | - Disease Detection Engine      |
 | - farms & plots                   |   | - AI Doctor Consultation Engine |
 | - crop_cycles & plantings         |   | - Fertilizer Recommendation     |
 | - cultivation_templates & plans   |   +---------------------------------+
 | - tasks, incidents, treatments    |
 +-----------------------------------+
```

---

## 2. Core Platform Components (Developer A)

### A. Identity & Access Control
- **Mechanism:** Stateless JWT with claims (`sub`, `roles`, `farmerId`).
- **Authorization:** Two-tiered enforcement:
  1. **Spring Method Security:** `@PreAuthorize("hasRole('FARMER')")` at controller entry points.
  2. **Domain IDOR Guard:** Explicit validation verifying `entity.getFarmer().getId().equals(authenticatedUserId)` in service layers before read or write operations.

### B. Farm & Plot Management
- **Entity Hierarchy:** `Farmer` (1) -> `Farm` (N) -> `Plot` (N).
- **Location Attributes:**
  - `state` (`VARCHAR(100)`): Agricultural region classification.
  - `district` (`VARCHAR(100)`): Agro-climatic zone mapping.
  - `latitude`, `longitude` (`NUMERIC(9,6)`): Optional GPS boundary coordinates validated within standard terrestrial bounds (`[-90, 90]`, `[-180, 180]`).
  - `location`: Human-readable village/town string.

### C. Crop Knowledge Catalog & Cultivation Engine
- **Catalog Design:**
  - `CultivationTemplate`: Versioned agronomic guides (e.g. `ICAR Directorate of Rice Research v2024.1`). Includes regional applicability and duration days.
  - `CultivationStage`: Logical growth milestones (e.g. Nursery, Tillering, Panicle Initiation, Harvesting).
  - `CultivationTemplateTask`: Actionable agronomic activities (e.g., Nursery Bed Preparation, Zinc Sulphate Application) with relative `offsetDays`, priority, and permissible timing windows.
- **Plan Generation Lifecycle:**
  1. Creation of `CropPlanting` invokes `CultivationPlanService.generatePlanForPlanting()`.
  2. System resolves published template matching crop name and variety.
  3. Concrete `Task` instances are created with calculated `dueDate = plantingDate + offsetDays`.
  4. Template version is pinned to `CultivationPlan` (`template_id`, `template_version`).
  5. **Revision Invariant:** When a plan is revised, in-progress, completed, and skipped tasks are preserved for auditability; only unstarted pending tasks are rescheduled or superseded.

### D. Task Scheduling & Operations
- **Task States:** `PENDING`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED`, `SKIPPED`, `OVERDUE`.
- **Sources:** `MANUAL` (farmer created), `PLAN` (cultivation plan generated), `AUTOMATION` (system rule triggered).
### E. Weather Intelligence & Contextual Advisories
- **Provider:** Open-Meteo Integration via Spring `RestClient` (`https://api.open-meteo.com/v1/forecast`).
- **Attribution & Licensing:** Open-access tier (max 10,000 calls/day), CC BY 4.0 license; attribution hyperlink explicitly displayed on all weather UI views. Zero credentials required or logged.
- **Location Resolution & Fallback:** Direct farm GPS coordinates when present; verified district headquarters centroid fallback via `krishi.district_coordinates` (14 verified Kerala districts); HTTP 422 `LOCATION_UNAVAILABLE` when unresolvable.
- **Spatial Grid Caching:** Coordinates normalized to 2 decimal places (~1.1 km grid), 2-hour fresh TTL, 15-minute manual cooldown, stale cache fallback on network/provider outage. Farm-level authorization enforced at API boundary.
- **Operational Advisory Rules:** Evaluates 3 verified operational rules (`HEAVY_RAIN_IRRIGATION`, `HIGH_WIND_SPRAYING`, `EXTREME_HEAT_ALERT`). Disease microclimate models and frost warnings deferred pending sensor calibration.
- **Advisory Identity & Deduplication:** Composite fingerprint (`advisory_type + farm_id + crop_planting_id + task_id + advisory_date`) protected by PostgreSQL partial index `uq_active_weather_advisory` (`WHERE status = 'ACTIVE'`). Prevents duplicates while supporting multi-crop coexistence and escalation.
- **Concurrency & Lock Safety:** `WeatherSyncScheduler` pins a single physical JDBC `Connection` from `DataSource` for `pg_try_advisory_lock` and `pg_advisory_unlock`, guaranteeing lock release in all execution paths and preventing overlapping scheduled jobs.
- **Farmer Sovereignty Invariant:** Zero automatic modification, rescheduling, completion, or cancellation of agricultural tasks. Advisories provide actionable recommendations with optional farmer acknowledgment.

### F. Dynamic Crop Growth-Stage Estimator (Milestone 4A)
- **Service:** `com.krishi.backend.cultivation.service.AgronomicStageResolverService`.
- **Convention:** Strict calendar-day calculation (`ChronoUnit.DAYS.between(plantingDate.toLocalDate(), asOfDate)`). Day 0 represents the day of sowing/planting (`elapsedDays == 0`).
- **Closed Integer Intervals:** Evaluates `[startDayOffset, endDayOffset]`. On Day 0, the crop enters the field, resolving to Stage 2 (`Transplanting & Establishment`) rather than nursery.
- **Error & Edge States:**
  - `PRE_PLANTING`: Evaluated before planting date or before the earliest stage offset (`elapsedDays < 0`).
  - `POST_HARVEST`: Evaluated after expected harvest date or beyond final stage end.
  - `NO_ACTIVE_PLAN`: Planting exists without an active cultivation plan.
  - `STAGE_DEFINITION_ERROR`: Triggered when stages overlap erroneously or contain gaps, preventing silent arbitrary selection.
- **Non-Invasive API Integration:** Embedded in `CropPlantingResponse` and `CropWorkspaceResponse` with optional `asOfDate` query parameter. Never modifies existing task records or DB schemas.

### G. Agricultural Context Foundation (Milestone 4B)
- **Domain Aggregator:** `com.krishi.backend.cropplanting.service.AgriculturalContextService` (`@Transactional(readOnly = true)`).
- **Core Role:** Unifies operational planting, variety, temporal offsets, estimated stage, field location, active cultivation plan, pending operational tasks, weather observations, and active weather advisories into a single read-only DTO. Zero database persistence of derived context.
- **Data Integrity & Ground Truth:**
  - Distinguishes algorithmic `estimatedStage` from field-observed `observedStage` (unconfirmed / null).
  - Preserves nulls for unrecorded variety and optional plot without inventing placeholder values.
  - Exposes 4-tier weather freshness: `FRESH`, `STALE`, `UNAVAILABLE`, `LOCATION_UNAVAILABLE`.
  - Transaction-safe weather resolution (`noRollbackFor = {LocationUnavailableException.class, WeatherProviderException.class}`) guarantees provider outages or unconfigured coordinates never fail operational requests.
- **Endpoints:**
  - `GET /api/v1/crop-plantings/{plantingId}/workspace`: Enriched with non-breaking `WeatherContext` to prevent redundant client network trips.
  - `GET /api/v1/crop-plantings/{plantingId}/context` (alias `/agricultural-context`): Dedicated, lightweight contract for future AI inference engines and headless rules.
- **Security:** Strict farmer role validation (`@PreAuthorize("hasRole('FARMER')")`) and tenant IDOR checks (`CropPlanting -> CropCycle -> Farm -> Owner`).

---

## 3. AI Service Microservice Architecture (Developer B)

### A. Separation of Concerns
- The Python AI microservice is decoupled from core transactional database logic.
- The AI service does **not** connect directly to the relational database; all context is passed via signed HTTP payloads.
- Core Platform coordinates persistence; AI service returns structured evaluation metadata and confidence scores.

### B. Functional Endpoints
1. **Computer Vision Disease Diagnosis (`/api/v1/ai/disease-detection/diagnose`):**
   - Consumes leaf photos via multipart form data.
   - Outputs detected pathology, confidence threshold, and recommended treatment category.
2. **AI Doctor Advisory (`/api/v1/ai/doctor/consult`):**
   - Consumes structured agronomic questions with crop and growth stage context.
   - Outputs verified guidance formatted for farmer clarity.
3. **Fertilizer Recommendation (`/api/v1/ai/fertilizer/recommend`):**
   - Consumes N-P-K soil test values, target crop, and field dimensions.
   - Outputs balanced nutrient application schedule aligned with ICAR guidelines.

---

## 4. Database Schema Architecture

- **Schema Namespace:** `krishi` (isolated PostgreSQL schema).
- **Migration Engine:** Flyway version-controlled scripts located in `backend/src/main/resources/db/migration/`.
- **Current Migration Level:** `V22`
  - `V17`: Structured farm location (`state`, `district`, `latitude`, `longitude`).
  - `V18`: Crop knowledge catalog, stages, templates, and cultivation plans with ICAR guidance.
  - `V19`: Partial unique index enforcing single active cultivation plan per planting.
  - `V20`: Task skip reason persistence (`skip_reason VARCHAR(255)`).
  - `V21`: Verified district coordinates registry, normalized spatial weather forecast cache, and weather advisories table with partial active index.
  - `V22`: Contextual agricultural advisories table (`krishi.agricultural_advisories`) with partial unique index enforcing idempotent active advisories (`uq_active_agri_advisory`).
- **Storage Constraints & Invariants:**
  - `krishi.cultivation_plans`: Guaranteed single `ACTIVE` plan per `crop_planting_id` via PostgreSQL partial index (`uq_active_cultivation_plan_per_planting`).
  - `krishi.weather_advisories`: Guaranteed single `ACTIVE` advisory per composite fingerprint via PostgreSQL partial index (`uq_active_weather_advisory`).
  - `krishi.agricultural_advisories`: Guaranteed single `ACTIVE` advisory per composite fingerprint (`farm:planting:task:rule:date`) via PostgreSQL partial index (`uq_active_agri_advisory`).
  - `krishi.cultivation_templates`: Stage order and relative task day offsets enforced via relational keys and check constraints.
- **Test Database Invariant:** Integration tests strictly require and validate `jdbc:postgresql://localhost:5432/krishi_test_db`. Destruction or recreation of production data is prevented by initializer URL validation guards.

---

## 5. Agricultural Advisory Rules Engine Architecture (Milestone 5A)

### A. Engine Philosophy & Non-Assumption of Safety
The rules engine evaluates the transient, read-only `AgriculturalContextResponse` snapshot. It adheres to strict deterministic outcomes:
- `RULE_EVALUATION_SUCCESS`: Evaluated successfully; advisory proposals generated or benign condition confirmed.
- `RULE_NOT_APPLICABLE`: Pre-conditions not met (e.g. no irrigation tasks scheduled).
- `INSUFFICIENT_CONTEXT`: Context missing or stale (e.g. stale weather cache, unresolved growth stage). **Missing inputs are never interpreted as evidence that conditions are safe.**
- `RULE_EVALUATION_FAILURE`: Unhandled rule failure safely isolated so remaining rules continue evaluating.

### B. Implemented Rules & Provenance
1. **`IRRIGATION_RAINFALL_CONFLICT`**: Suspends artificial irrigation when precipitation on task due date is $\ge 20\,\text{mm}$ or probability $\ge 80\%$ (Standard ICAR & Agricultural Water Management Guidelines).
2. **`CHEMICAL_SPRAYING_WIND_RISK`**: Postpones chemical applications when sustained wind $> 20\,\text{km/h}$ or gusts $> 30\,\text{km/h}$ (*Provisional threshold* based on FAO/CABI Agrochemical Spray Drift Guidelines).
3. **`OVERDUE_CULTIVATION_TASK`**: Alerts overdue tasks ($\ge 1\,\text{day}$) and contextualizes risk with the active growth stage (Krishi Cultivation Operations Standard).
4. **`STAGE_TASK_MISALIGNMENT`**: Identifies pending tasks belonging to prior elapsed growth stages; evaluates **only** when `GrowthStageStatus == RESOLVED` (Certified ICAR Cultivation Plan Phenological Stage Boundaries).

### C. Advisory Lifecycle & Workflow Integration
- **States:** `ACTIVE` $\rightarrow$ `ACKNOWLEDGED` $\rightarrow$ `RESOLVED` / `SUPERSEDED` / `EXPIRED`.
- **Idempotency:** Composite fingerprint `farm:planting:task:rule:date` (with task rule dates bound to task `dueDate`) ensures repeated evaluations produce zero duplicate rows or redundant notifications.
- **Escalation:** Escalating severity supersedes previous advisory and triggers `[ESCALATION]` notification.
- **Non-Mutating Guard:** Rules engine never automatically completes, skips, or reschedules operational tasks.
- **Auto-Resolution Domain Events:** Clearing conditions or completed/cancelled/skipped tasks trigger auto-resolution and synchronously emit `AdvisoryResolvedEvent` with explicit resolution reason; idempotent queries prevent duplicate audit events on re-evaluation.
- **Transaction Safety:** Notification persistence executes under `@Transactional(propagation = Propagation.REQUIRES_NEW)` to prevent notification errors from marking the outer advisory transaction rollback-only.
- **API Consistency:** Controller endpoints consistently return standardized `ApiResponse<T>` envelopes across evaluation, query, acknowledge, and resolve workflows.
- **Performance Optimization:** `AgriculturalContextService` fetches operational tasks using `TaskRepository.findByCropPlantingIdWithTemplateTaskAndStage` with `LEFT JOIN FETCH` to prevent N+1 query cascades.
