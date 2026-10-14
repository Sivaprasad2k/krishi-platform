# Krishi Platform — Implementation Log

This log records sequential architectural changes, migrations, security enhancements, and milestones implemented in the Krishi Platform codebase.

## [2026-10-14] Milestone 5A: Agricultural Advisory Rules Engine

### 1. Mandatory Architectural Invariants Implemented
- **Deterministic, Explainable Rules Engine:** Implemented `AgriculturalAdvisoryEngine` and `AgriculturalAdvisoryService` operating over `AgriculturalContextResponse`. All rules implement `AgriculturalRule` and produce explicit outcomes (`RULE_EVALUATION_SUCCESS`, `RULE_NOT_APPLICABLE`, `INSUFFICIENT_CONTEXT`, `RULE_EVALUATION_FAILURE`).
- **Non-Assumption of Safety:** Missing or stale data (e.g. weather `STALE`/`UNAVAILABLE`, unresolved growth stage) is strictly categorized as `INSUFFICIENT_CONTEXT` and never assumed to be safe conditions.
- **Isolated Rule Boundary:** Engine catches per-rule evaluation exceptions safely to prevent an unexpected error in one rule from aborting the evaluation of unrelated rules.
- **Initial High-Value Rules Implemented:**
  1. `IRRIGATION_RAINFALL_CONFLICT`: Withholds scheduled artificial irrigation when forecast precipitation on task due date is $\ge 20.0\,\text{mm}$ or probability $\ge 80\%$ (Standard ICAR & Agricultural Water Management Guidelines).
  2. `CHEMICAL_SPRAYING_WIND_RISK`: Postpones chemical applications when sustained wind $> 20.0\,\text{km/h}$ or gusts $> 30.0\,\text{km/h}$ (Provisional threshold based on FAO/CABI Agrochemical Application Guidelines).
  3. `OVERDUE_CULTIVATION_TASK`: Identifies operational delays ($\ge 1\,\text{day}$) and contextualizes risk with active growth stage (Krishi Cultivation Operations Standard).
  4. `STAGE_TASK_MISALIGNMENT`: Identifies pending tasks from earlier cultivation stages; evaluated only when `GrowthStageStatus == RESOLVED` (Certified ICAR Cultivation Plan Phenological Stage Boundaries).
- **Advisory Persistence & Idempotency (Flyway V22):** Persists advisories in `krishi.agricultural_advisories` with composite fingerprint `farm:planting:task:rule:date` and partial active unique index `WHERE status = 'ACTIVE'`. Repeated evaluations produce zero duplicate rows and zero duplicate notifications.
- **Complete Advisory Lifecycle:** Implemented `ACTIVE` $\rightarrow$ `ACKNOWLEDGED` $\rightarrow$ `RESOLVED` / `SUPERSEDED` / `EXPIRED`. Escalation marks previous advisory as `SUPERSEDED` and dispatches `[ESCALATION]` notification.
- **Workflow Safety:** Engine is strictly advisory and never mutates, completes, skips, or reschedules operational tasks.
- **Security & Tenant Isolation:** Enforced `ROLE_FARMER` and verified farm ownership on all APIs (`evaluate`, `getAdvisories`, `acknowledge`, `resolve`).
- **Audit Logging:** Published `AdvisoryCreatedEvent`, `AdvisoryAcknowledgedEvent`, and `AdvisoryResolvedEvent` persisted to `krishi.domain_events`.

### 2. Backend Implementation & Testing
- **New Database Migration:**
  - `V22__create_agricultural_advisories_table.sql`: Schema definition, foreign keys to `farms`, `crop_plantings`, `tasks`, performance indexes, and unique active index `uq_active_agri_advisory`.
- **New Domain Package (`com.krishi.backend.advisory`):**
  - Entities & Enums: `AgriculturalAdvisory`, `AdvisoryStatus`
  - Rules: `AgriculturalRule`, `RuleEvaluationOutcome`, `RuleEvaluationResult`, `AdvisoryProposal`, `IrrigationRainfallConflictRule`, `ChemicalSprayingWindRiskRule`, `OverdueCultivationTaskRule`, `StageTaskMisalignmentRule`
  - Engine & Service: `AgriculturalAdvisoryEngine`, `AgriculturalAdvisoryService`
  - DTOs & Mappers: `AgriculturalAdvisoryResponse`, `AdvisoryEvaluationSummaryResponse`, `RuleEvaluationReportItem`, `AgriculturalAdvisoryMapper`
  - Controller: `AgriculturalAdvisoryController` (`POST /api/v1/crop-plantings/{plantingId}/advisories/evaluate`, `GET /api/v1/crop-plantings/{plantingId}/advisories`, `PATCH /api/v1/advisories/{id}/acknowledge`, `PATCH /api/v1/advisories/{id}/resolve`)
  - Events: `AdvisoryCreatedEvent`, `AdvisoryAcknowledgedEvent`, `AdvisoryResolvedEvent`
- **Test Coverage:**
  - `IrrigationRainfallConflictRuleTest`: 8 unit tests.
  - `ChemicalSprayingWindRiskRuleTest`: 5 unit tests.
  - `OverdueCultivationTaskRuleTest`: 5 unit tests.
  - `StageTaskMisalignmentRuleTest`: 5 unit tests.
  - `AgriculturalAdvisoryEngineTest`: 2 unit tests.
  - `AgriculturalAdvisoryServiceTest`: 9 unit tests (including notification persistence failure resilience, linked-task auto-resolution, and condition-cleared auto-resolution).
  - `AgriculturalAdvisoryIntegrationTest`: 7 end-to-end MockMvc integration tests with PostgreSQL verifying evaluation, idempotency, filtering, lifecycle transitions, domain event auto-resolution auditing, tenant isolation, and weather failure resilience.

### 3. Post-Review Refinements & Hardening
- **Notification Transaction Boundary Isolation:**
  - Configured `@Transactional(propagation = Propagation.REQUIRES_NEW)` on `NotificationService.createNotification`.
  - An isolated physical transaction guarantees notification persistence errors cannot mark the outer advisory evaluation transaction rollback-only.
  - Added unit test `testAdvisoryPersistedWhenNotificationFails` verifying advisory persistence and event publication succeed despite notification failures.
- **API Response Consistency:**
  - Standardized all `AgriculturalAdvisoryController` endpoints (`/evaluate`, `/advisories`, `/{id}/acknowledge`, `/{id}/resolve`) to return `ResponseEntity<ApiResponse<T>>` with standard `success`, `message`, `data`, and `timestamp` fields.
- **N+1 Query Elimination in Agricultural Context:**
  - Updated `AgriculturalContextService.buildRelevantTasks` to invoke `taskRepository.findByCropPlantingIdWithTemplateTaskAndStage(plantingId)`.
  - Replaced ad-hoc relational lazy lookups with a single `LEFT JOIN FETCH` query across `Task`, `TemplateTask`, and `CultivationStage`.
  - Updated mock repository expectations across all 9 `AgriculturalContextServiceTest` test methods.
- **Audit Completeness on Automatic Resolution:**
  - Implemented synchronous publication of `AdvisoryResolvedEvent` with descriptive `resolutionReason` when advisories are automatically resolved due to linked task completion/cancellation/skip or when risk conditions clear clean.
  - Guaranteed repeat evaluation idempotency so that subsequent evaluations do not query already-resolved advisories or emit duplicate events.
- **Stable Advisory Fingerprint Generation:**
  - Stabilized task-based rule fingerprints (`OVERDUE_CULTIVATION_TASK`, `STAGE_TASK_MISALIGNMENT`) by binding to the intrinsic task `dueDate` (with a stable fallback) instead of volatile evaluation dates.
- **Full Test Suite Results:**
  - Backend: 137/137 tests passed (0 failures, 0 errors, 0 skipped).
  - Frontend: 27/27 Vitest tests passed across 9 suites.
  - Frontend Production Build: `vite build` completed cleanly with zero errors.
  - Git Hygiene: `git diff --check` passed with 0 conflicts or whitespace violations.

---

## [2026-10-14] Milestone 4B: Agricultural Context Foundation

### 1. Mandatory Architectural Invariants Implemented
- **Safe Read-Only Domain Aggregation (Zero Schema/DB Persistence):** Implemented `AgriculturalContextService` with transactional read-only semantics (`@Transactional(readOnly = true)`) aggregating existing domain models (`CropPlanting`, `CropCycle`, `Farm`, `CultivationPlan`, `Task`, `WeatherForecastCache`, `WeatherAdvisory`). Persisting derived agricultural context was rejected because it introduces cache invalidation hazards, relational desynchronization, and schema duplication.
- **Strict Ground Truth & Truthful Missing Data:** Never hallucinates or fabricates missing values. If crop variety or plot is unrecorded, nulls are preserved. Preserves all `GrowthStageStatus` domain semantics (`RESOLVED`, `NO_ACTIVE_PLAN`, `PRE_PLANTING`, `POST_HARVEST`, `STAGE_DEFINITION_ERROR`).
- **Explicit Ground-Truth Demarcation (Estimated vs Observed Stage):** Distinguishes algorithmic `estimatedStage` (derived from mathematical calendar-day offsets against published ICAR packages) from farmer-confirmed biological `observedStage` (unconfirmed / null). Includes explicit agronomic basis and discrepancy notice to prevent downstream AI advisory engines or farmers from conflating mathematical models with physical field observations.
- **4-Tier Environmental Weather Freshness:** Classifies weather context into `FRESH` (< 2h cache), `STALE` (> 2h cache served during outage), `UNAVAILABLE` (provider outage with empty cache), and `LOCATION_UNAVAILABLE` (farm GPS coordinates and district unconfigured). Pre-validates coordinate resolution and sets `@Transactional(noRollbackFor = {LocationUnavailableException.class, WeatherProviderException.class})` so provider or location failures never fail the caller's transaction.
- **Dual API Integration Strategy:**
  - Enriched existing `GET /api/v1/crop-plantings/{plantingId}/workspace` with optional `WeatherContext` to eliminate extra client round-trips for the React PWA.
  - Added dedicated endpoint `GET /api/v1/crop-plantings/{plantingId}/context` (alias `/agricultural-context`) tailored for headless rule engines and external AI services, omitting UI-heavy disease incident and treatment trees.
- **Tenant Isolation & Security:** Enforces `@PreAuthorize("hasRole('FARMER')")` and hierarchical farm ownership validation (`CropPlanting -> CropCycle -> Farm -> Owner == userDetails.getUserId()`) on both endpoints.

### 2. Backend Implementation & Testing
- **New Domain DTOs (`com.krishi.backend.cropplanting.dto.context`):**
  - `AgriculturalContextResponse.java`: Top-level composite agricultural context.
  - `CropIdentityContext.java`: Crop planting, cycle, variety, planting date, harvest date, and elapsed calendar days.
  - `GrowthStageContext.java`: Algorithmic estimate, field-observed stage, agronomic basis, discrepancy notice.
  - `ObservedGrowthStageContext.java`: Models farmer-observed biological stage.
  - `LocationContext.java`: Farm ID, name, address, state, district, resolved GPS/Centroid coordinates, coordinate source, optional plot.
  - `CultivationPlanSummaryContext.java`: Plan ID, version, status, template metadata, duration, total stages and tasks count.
  - `OperationalTaskContext.java`: Task ID, title, type, status, due date, overdue flag, days overdue, associated stage name.
  - `WeatherContext.java`: Weather freshness status, timestamps, observation summary, active advisories, attribution notice.
  - `WeatherFreshnessStatus.java`: Freshness enum (`FRESH`, `STALE`, `UNAVAILABLE`, `LOCATION_UNAVAILABLE`).
  - `WeatherObservationSummary.java`: Temperature, relative humidity, precipitation, wind speed, weather code, condition description.
- **Domain Services & Controllers:**
  - `AgriculturalContextService.java`: Safe domain aggregator service.
  - `CropPlantingService.java`: Enriched `getCropWorkspace` with weather context resolution.
  - `CropPlantingController.java`: Added `GET /api/v1/crop-plantings/{plantingId}/context` and `/agricultural-context`.
  - `WeatherForecastService.java`: Added `noRollbackFor` to `@Transactional` to guard against rollback-only marking on caller transactions.
  - `WeatherAdvisoryRepository.java`: Added `findByCropPlantingIdAndStatusInOrderByTargetDateAsc`.
- **Test Coverage:**
  - `AgriculturalContextServiceTest.java`: 10 comprehensive unit tests covering full valid context, missing plan, missing location coordinates/district, external provider failure, stale cache fallback, missing variety/plot, pre-planting date, post-harvest date, active advisories filtering, and IDOR tenant isolation.
  - `AgriculturalContextIntegrationTest.java`: 5 end-to-end MockMvc integration tests verifying valid planting with active plan, planting without active plan, missing farm location, cross-farmer IDOR security denial (HTTP 403), and backward compatibility of `/workspace` with weather payload.
  - **Full Backend Suite Regression:** 96/96 tests passing (0 failures, 0 errors, 0 skipped).
  - **Frontend Suite Regression:** 27/27 Vitest tests passing across 9 test suites.
  - **Frontend Production Build:** Vite build cleanly completed in 7.36s with zero errors.

---

## [2026-10-14] Milestone 4A: Dynamic Crop Growth-Stage Estimator

### 1. Mandatory Architectural Invariants Implemented
- **Deterministic Agronomic Resolver (Zero ML):** Implemented `AgronomicStageResolverService` utilizing versioned ICAR cultivation stages (`CultivationStage.startDayOffset` and `endDayOffset`) from active `CultivationPlan`. Zero machine learning or stochastic models.
- **Strict Calendar-Day Convention:** Calculated elapsed days from planting using `ChronoUnit.DAYS.between(plantingDate.toLocalDate(), asOfDate)`. Day 0 is explicitly defined as the planting date (`elapsedDays == 0`).
- **Closed-Interval Matching & Day 0 Transition:** Handled nursery-to-field transitions where nursery stage ends at Day 0 (`[-15, 0]`) and transplanting begins at Day 0 (`[0, 20]`). When `elapsedDays == 0`, the resolver deterministically resolves to the in-field stage (Stage 2) rather than nursery.
- **Non-Arbitrary Boundary & Error Safety:** In the event of conflicting stage overlaps or gaps in the stage catalogue, returns `STAGE_DEFINITION_ERROR` rather than silently picking an arbitrary stage.
- **Explicit Lifecycle States (`GrowthStageStatus`):** Explicitly models `RESOLVED`, `PRE_PLANTING`, `POST_HARVEST`, `NO_ACTIVE_PLAN`, `MISSING_PLANTING_DATE`, and `STAGE_DEFINITION_ERROR`. Never hallucinates or fabricates a confirmed stage.
- **Non-Invasive API Integration:** Integrated into `CropPlantingResponse` and `CropWorkspaceResponse` (both individual planting and workspace endpoints), accepting an optional `asOfDate` evaluation parameter while maintaining complete backward compatibility. Zero schema changes required.

### 2. Backend Implementation & Testing
- **New Domain DTOs & Services:**
  - `GrowthStageStatus.java`: Status enum for agronomic resolution.
  - `EstimatedGrowthStageResponse.java`: Detailed growth stage response DTO with stage boundaries, elapsed days, and clamped progress percentages.
  - `AgronomicStageResolverService.java`: Core estimation service.
- **Updated Controllers & Mappers:**
  - `CropPlantingResponse.java`: Added `estimatedGrowthStage` field.
  - `CropWorkspaceResponse.java`: Added `estimatedGrowthStage` field.
  - `CropPlantingMapper.java`: Overloaded `toResponse(planting, estimatedGrowthStage)`.
  - `CropPlantingService.java`: Injected resolver into create, update, getById, getByCropCycle, and getCropWorkspace.
  - `CropPlantingController.java`: Added optional `@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate` parameter to GET endpoints.
- **Test Coverage:**
  - `AgronomicStageResolverServiceTest.java`: 17 comprehensive unit tests covering Day 0, stage boundaries, consecutive transitions, missing plans, future plantings, post-harvest dates, gaps, overlaps, progress clamp, multiple revisions, mid-cycle shared boundary transitions, and consistent/inconsistent expected harvest date edge cases.
  - `AgronomicStageResolverIntegrationTest.java`: 4 comprehensive integration tests covering end-to-end API lifecycle, workspace aggregation, future dates, and IDOR/tenant isolation.
  - **Full Suite Regression:** 81/81 backend tests passing (0 failures, 0 errors, 0 skipped).
  - **Frontend Regression:** 27/27 Vitest tests passing across 9 test suites.

---

## [2026-10-13] Milestone 3B: Weather Intelligence & Contextual Advisories

### 1. Mandatory Architectural Corrections Implemented
- **Verified Open-Meteo Integration & Attribution:** Implemented Open-Meteo API integration (`https://api.open-meteo.com/v1/forecast`) via Spring Boot 4 `RestClient`. Verified non-commercial terms (10,000 calls/day, 5,000 calls/hour) and CC BY 4.0 license. Rendered mandatory attribution hyperlink in UI. Zero provider credentials required or logged.
- **Connection-Pinned PostgreSQL Advisory Locking:** Implemented `WeatherSyncScheduler` with connection-pinned `pg_try_advisory_lock(84729103)` ensuring lock acquisition and release run on the exact same physical JDBC connection from `DataSource`, preventing lock leakage across HikariCP pools and preventing overlapping executions.
- **Stable Advisory Identity & Deduplication:** Created composite deduplication fingerprint (`advisory_type + farm_id + crop_planting_id + task_id + advisory_date`) protected by PostgreSQL partial unique index `uq_active_weather_advisory` (`WHERE status = 'ACTIVE'`). Prevents duplicate advisories/notifications while permitting multi-crop coexistence and severity escalations (marking previous advisory `SUPERSEDED` via `saveAndFlush()`).
- **Normalized Spatial Grid Caching (~1.1 km):** Normalized query coordinates to 2 decimal places ($0.01^\circ \approx 1.1\text{ km}$), sharing forecasts across neighboring farms in the same village to conserve API calls. Added 2-hour fresh TTL, 15-minute manual refresh cooldown, and stale cache fallback on network failures. Documented elevation limitations in Western Ghats.
- **Verified District Provenance & HTTP 422 Fallback:** Seeded 14 verified Kerala district headquarters centroids from official Census of India / Government of Kerala data in `krishi.district_coordinates`. Unknown or unresolvable districts return HTTP 422 `LOCATION_UNAVAILABLE` without coordinate fabrication.
- **Farmer Sovereignty Invariant:** Guaranteed zero automated modification, rescheduling, completion, or cancellation of agricultural tasks. Weather advisories strictly provide contextual recommendations with optional farmer acknowledgment.

### 2. Database Schema Migrations
- **`V21__create_weather_and_district_registry.sql`**:
  - Created `krishi.district_coordinates` seeded with 14 verified Kerala district headquarters coordinates.
  - Created `krishi.weather_forecast_cache` keyed by `spatial_grid_key` (`{lat_2dec}_{lon_2dec}`) with forecast metrics, 7-day JSONB payload, freshness timestamps, and manual cooldown index.
  - Created `krishi.weather_advisories` with foreign keys to `farms`, `crop_plantings`, `tasks`, deduplication fingerprint, and partial unique index `uq_active_weather_advisory`.

### 3. Backend Implementation & Security
- **Domain Entities & Repositories:**
  - `DistrictCoordinate`, `WeatherForecastCache`, `WeatherAdvisory`.
  - Added `WEATHER_ADVISORY` to `NotificationType` enum.
  - Added `LOCATION_UNAVAILABLE` to `ErrorCode` enum and created `LocationUnavailableException` handled by `GlobalExceptionHandler`.
- **Weather Services:**
  - `OpenMeteoProviderClient`: Connect/read timeouts (3s/5s), attribution metadata, no logged credentials.
  - `WeatherNormalizationService`: Normalizes coordinates to 2 decimal places and constructs standardized grid keys and forecast DTOs.
  - `DistrictCentroidResolver`: Resolves farm coordinates via direct GPS or case-insensitive district registry match; throws `LocationUnavailableException` if unresolvable.
  - `WeatherAdvisoryEngine`: Evaluates 3 operational rules (`HEAVY_RAIN_IRRIGATION`, `HIGH_WIND_SPRAYING`, `EXTREME_HEAT_ALERT`). Handles deduplication, auto-expiry of past advisories, escalation with `saveAndFlush()`, and notification dispatching via existing notification infrastructure.
  - `WeatherForecastService`: Manages 2-hour cache TTL, 15-minute refresh cooldown, stale cache fallback, and farmer ownership verification (`entity.getFarmer().getId().equals(authenticatedUserId)`).
- **Concurrency & Scheduling:**
  - `WeatherSyncScheduler`: Daily synchronized background refresh at 06:00 IST using connection-pinned PostgreSQL advisory lock.
- **API Controller:**
  - `WeatherController`: Exposed `/api/v1/farms/{farmId}/weather`, `/api/v1/farms/{farmId}/weather/refresh`, `/api/v1/farms/{farmId}/advisories`, `/api/v1/advisories/{id}/acknowledge`, `/api/v1/tasks/{taskId}/advisories`.

### 4. Frontend Implementation & UI Integration
- **API Client:** `weatherApi.js` supporting weather retrieval, manual refresh, farm advisories, task advisories, and acknowledgment.
- **Components:**
  - `WeatherSummaryWidget.jsx`: Current weather, temperature, humidity, wind, 7-day strip, location precision badge (GPS / District), stale cache banner, refresh button, error handling (`LOCATION_UNAVAILABLE`), and CC BY 4.0 Open-Meteo attribution hyperlink.
  - `WeatherAdvisoryBanner.jsx`: Severity badges (`WARNING`, `ALERT`, `ADVISORY`), agricultural rationale, actionable recommendation, and Acknowledge button.
- **Workflow Integrations:**
  - `DashboardPage.jsx`: Renders weather summary widget and active weather advisories banner in the "Attention Required" section.
  - `FarmDetailsPage.jsx`: Renders farm weather conditions and farm-scoped active advisories.
  - `TaskDetailsModal.jsx`: Queries and displays task-specific operational weather advisories (e.g., heavy rain during irrigation).

### 5. Automated Verification
- **Backend Test Suite:** 60/60 tests passed across all modules, including 11 comprehensive tests in `WeatherIntegrationTest.java` (GPS resolution, district centroid fallback, location unavailable 422, 2-hour cache reuse, 15-min cooldown, stale fallback, heavy rain advisory, high wind advisory, extreme heat alert, advisory deduplication & escalation, IDOR farmer ownership protection).
- **Frontend Test Suite:** 27/27 tests passed across 9 Vitest suites, including 5 new unit tests in `weatherApi.test.js`.
- **Production Build:** `vite build` completed cleanly with code 0 (PWA precache bundle generated).
- **Git Whitespace Integrity:** `git diff --check` passed with code 0.

---

## [2026-10-13] Milestone 3A: Core Workflow Stabilization

### 1. Defect Confirmations & Architecture Audit
- **P0 Defect 1 (Dashboard Task Consolidation):** The Home dashboard rendered urgent tasks by executing a client-side recursive waterfall across farms, crop cycles, and crop plantings, missing manual tasks, treatment tasks, and cultivation plan automation tasks.
- **P0 Defect 2 (Overdue Task Actions Broken):** Overdue tasks were blocked in `TaskCard.jsx` and `TaskDetailsModal.jsx` due to strict `status === 'PENDING'` UI conditional guards, denying Start, Done, Reschedule, Skip, and Cancel actions that the backend permitted. Furthermore, skipping immediately triggered without capturing a valid agricultural reason.
- **P1 Gap 1 (Treatments API Waterfall):** The Treatments page executed a 4-tier client-side request waterfall across farms -> cycles -> plantings -> incidents -> treatments, resulting in excessive network roundtrips.
- **P1 Gap 2 (Farm Navigation & Structured Location):** `FarmDetailsPage.jsx` displayed disabled placeholder cards for Crops, Crop Problems, and Tasks instead of navigating with farm context; farm creation (`FarmFormWizard.jsx`) and editing (`EditFarmPage.jsx`) omitted structured `state`, `district`, and optional GPS coordinates.

### 2. Database Schema Migrations
- **`V20__add_task_skip_reason.sql`**:
  - Added `skip_reason VARCHAR(255)` to `krishi.tasks` to persist farmer agricultural rationale when field operations are skipped.

### 3. Backend Implementation & API Consolidation
- **Task Domain & Enriched DTOs:**
  - Added `skipReason` attribute to `Task` entity and `TaskResponse`.
  - Created `TaskSkipRequest` validating `@NotBlank String reason`.
  - Enriched `TaskResponse` with hierarchy metadata (`cropName`, `cropVariety`, `farmId`, `farmName`) via `TaskMapper.java`, eliminating client-side joins.
  - Updated `TaskService.skipTask` and `TaskController.skipTask` (`POST /api/v1/tasks/{id}/skip`) to accept an optional body and persist the validated skip reason.
- **Treatments Consolidation Endpoint:**
  - Implemented `GET /api/v1/treatments` in `TreatmentController.java` restricted to `ROLE_FARMER`.
  - Added `findByFarmerUserId` in `TreatmentRepository.java` utilizing eager `JOIN FETCH` across `diseaseIncident`, `cropPlanting`, `cropCycle`, and `farm`, executing in a single SQL query without N+1 overhead.
  - Enriched `TreatmentResponse` with `diseaseName`, `cropPlantingId`, `cropName`, `cropVariety`, `farmId`, and `farmName` via `TreatmentMapper.java`.
  - Enforced farmer ownership isolation (Farmer B receives empty list, cannot see Farmer A's treatments).

### 4. Frontend Implementation & Workflow Fixes
- **Dashboard Task Consolidation (`dashboardApi.js`, `DashboardPage.jsx`):**
  - Replaced recursive client-side waterfall in `fetchFarmerOperations` with direct call to authenticated `GET /api/v1/tasks`.
  - Filtered completed/cancelled/skipped tasks from urgent action queue.
  - Prioritized work by urgency: overdue tasks sorted first, followed by earliest due date.
  - Updated Dashboard card rendering with genuine crop names, farm badges, plan badges (`source: AUTOMATION`), and direct task links.
- **Overdue Task Actions & Skip Reason (`TaskCard.jsx`, `TaskDetailsModal.jsx`, `taskApi.js`):**
  - Updated `isOverdue` and action button guards to recognize `status === 'OVERDUE'`.
  - Restored Start, Done, Reschedule, Skip, and Cancel actions for overdue tasks matching permitted backend transitions.
  - Created `SkipTaskModal.jsx` featuring quick agronomic reason chips ("Adverse Weather / Rain", "Soil Moisture Adequate", "Crop Health Satisfactory", "Supplies / Labor Unavailable", "Completed Manually"), free text input, and validation before submission.
  - Updated `taskApi.skipTask` to send `{ reason }`.
- **Treatments API Consolidation (`treatmentApi.js`):**
  - Replaced 60-line 4-tier waterfall in `fetchAllTreatments()` with direct call to `GET /api/v1/treatments`.
- **Farm Navigation & Structured Location (`FarmDetailsPage.jsx`, `FarmFormWizard.jsx`, `EditFarmPage.jsx`, `TasksPage.jsx`, `CropsPage.jsx`, `DiseaseIncidentsPage.jsx`):**
  - Replaced disabled placeholder cards in `FarmDetailsPage.jsx` with active operational navigation linking to `/crops?farmId=${farmId}`, `/disease-incidents?farmId=${farmId}`, and `/tasks?farmId=${farmId}`.
  - Added structured location display (`state`, `district`, GPS coordinates) in `FarmDetailsPage.jsx`.
  - Updated `TasksPage.jsx`, `CropsPage.jsx`, and `DiseaseIncidentsPage.jsx` with `useSearchParams` to filter records by `farmId` and display dismissible filter banners.
  - Exposed `state`, `district`, `latitude` (-90 to 90), and `longitude` (-180 to 180) in Step 2 of `FarmFormWizard.jsx` and `EditFarmPage.jsx`.

### 5. Automated Verification
- **Backend Test Suite:** 49/49 tests passed (0 failures, 0 errors, 0 skipped), including new regression tests in `TaskIntegrationTest.java` (skip reason persistence, overdue task transitions, enriched hierarchy) and `TreatmentIntegrationTest.java` (consolidated endpoint, ownership isolation, enriched metadata).
- **Frontend Test Suite:** 22/22 tests passed across 8 Vitest suites (`taskApi.test.js`, `treatmentApi.test.js`, `dashboardApi.test.js`, `cultivationPlanApi.test.js`, `getCrop.test.js`, `authStore.test.js`, `i18n.test.js`, `axios.test.js`).
- **Production Build:** `vite build` completed cleanly with code 0 (PWA precache bundle generated).
- **Git Whitespace Integrity:** `git diff --check` passed with code 0.

---

## [2026-10-13] Milestone 2: Automatic Cultivation Planning & Branch Architecture Setup

### 1. Git Workflow & Dual-Developer Branch Initialization
- **Action:** Initialized isolated development branches from integration base commit `0003063`.
  - `feature/core-platform` (Developer A): Created and checked out to hold verified farm operations, cultivation planning, and frontend workflows.
  - `feature/ai-service` (Developer B): Created directly from `main` (`0003063`) ensuring complete isolation from in-flight core platform changes.
- **Safety Compliance:** No git reset, stash, clean, or auto-commit was performed; all 36 modified and 20 untracked files are preserved on `feature/core-platform`.

### 2. Database Schema Migrations
- **`V17__add_structured_farm_location.sql`**:
  - Added structured location columns to `krishi.farms`: `state VARCHAR(100)`, `district VARCHAR(100)`, `latitude NUMERIC(9,6)`, `longitude NUMERIC(9,6)`.
  - Preserved existing `location` column for backward API compatibility.
- **`V18__create_crop_knowledge_catalog_and_cultivation_plans.sql`**:
  - Created tables:
    - `krishi.cultivation_templates` (versioned crop packages, recommendation sources).
    - `krishi.cultivation_stages` (sequential growth stages with start/end day offsets).
    - `krishi.cultivation_template_tasks` (recommended agronomic operations with timing windows).
    - `krishi.cultivation_plans` (farmer crop planting plan instances).
  - Extended `krishi.tasks` with `cultivation_plan_id` and `template_task_id`.
  - Seeded genuine ICAR agronomic guidance:
    - **Rice (Paddy)**: ICAR Directorate of Rice Research `v2024.1` (135 days, 4 growth stages, 8 field tasks).
    - **Tomato**: ICAR Indian Institute of Horticultural Research `v2024.1` (110 days, 4 growth stages, 7 field tasks).
    - **Wheat**: ICAR Indian Institute of Wheat and Barley Research `v2024.1` (120 days, 4 growth stages, 6 field tasks).

- **`V19__enforce_single_active_cultivation_plan.sql`**:
  - Added partial unique index `uq_active_cultivation_plan_per_planting` on `krishi.cultivation_plans (crop_planting_id) WHERE status = 'ACTIVE'`.
  - Enforces database-level constraint preventing duplicate active plans during concurrent generation requests or race conditions.

### 3. Backend Implementation & Defect Resolution
- **Farm Location Validation:**
  - Added latitude (`-90` to `90`) and longitude (`-180` to `180`) range boundary checks in `FarmCreateRequest` and `FarmUpdateRequest`.
- **Cultivation Service & Controller:**
  - Created `CultivationTemplateService` exposing published catalog templates.
  - Created `CultivationPlanService` with idempotent plan generation, version recording, and task history preservation on revisions.
  - Implemented `CultivationTemplateController` (`/api/v1/cultivation-templates`).
  - Implemented `CultivationPlanController` (`/api/v1/crop-plantings/{plantingId}/cultivation-plan/generate`).
- **Defect Resolved — JPA Flush Ordering on Plan Revision:**
  - **Issue:** When superseding an active plan, Hibernate queued an `EntityInsertAction` for the new `ACTIVE` plan before the `EntityUpdateAction` for the superseded plan. This triggered PostgreSQL unique constraint violation `23505` against `uq_active_cultivation_plan_per_planting`.
  - **Correction:** Replaced `planRepository.save(existingPlan)` with `planRepository.saveAndFlush(existingPlan)` in `CultivationPlanService.java`, forcing the SQL update (`status = 'SUPERSEDED'`) to flush before inserting the new plan.
- **Task Workflow Extensions:**
  - Added task statuses `SKIPPED` and `OVERDUE` in `TaskStatus`.
  - Added source `PLAN` in `TaskSource`.
  - Added endpoints `POST /api/v1/tasks/{id}/skip` and `POST /api/v1/tasks/{id}/reschedule`.
- **Crop Planting & Workspace:**
  - Extended `CropPlantingCreateRequest` with optional `templateId` for auto-scheduling on crop creation.
  - Updated `CropWorkspaceResponse` to include the active `cultivationPlan`.

### 4. Frontend Implementation
- Created `cultivationPlanApi.js` for catalog querying and plan generation.
- Extended `taskApi.js` with `skipTask` and `rescheduleTask`.
- Built `RescheduleTaskModal.jsx` for interactive date rescheduling.
- Built `CultivationPlanCard.jsx` and `GeneratePlanModal.jsx` for plan visibility and on-demand plan generation.
- Built `CatalogBrowser.jsx` and tabbed `CropsPage.jsx` to allow browsing supported crops.
- Refactored `CropFormWizard.jsx` to dynamically consume catalog templates, removing all hardcoded crop duration days.
- Added `SKIPPED` status badge in `StatusBadge.jsx`.
- Cleaned up trailing whitespaces to ensure zero `git diff --check` warnings.

### 5. Automated Verification
- **Backend:** 46/46 tests passed (`CultivationPlanIntegrationTest` including database constraint enforcement test, `FarmModuleIntegrationTest`, `CropWorkspaceIntegrationTest`, `TaskIntegrationTest`, `SecurityRegressionTest`).
- **Frontend:** 16/16 tests passed across 6 Vitest suites (`cultivationPlanApi.test.js`, `taskApi.test.js`, `getCrop.test.js`, `authStore.test.js`, `i18n.test.js`, `axios.test.js`).
- **Build:** `vite build` completed cleanly with code 0 (PWA precache bundle generated).

---

## [2026-10-12] Milestone 1: Core Operational Stabilization

### 1. Method Security & IDOR Hardening
- Enabled `@EnableMethodSecurity` in `SecurityConfig.java`.
- Verified JWT authentication filter chain and explicit ownership checks across all service boundaries.
- Added `SecurityRegressionTest.java` validating token verification and unauthorized access denial.

### 2. Unified Crop Workspace Aggregation
- Implemented `GET /api/v1/crop-plantings/{plantingId}/workspace`.
- Eliminated sequential frontend multi-hop requests by returning composite crop planting, season, farm, pending tasks, disease incidents, and treatments in a single call.
- Refactored `CropDetailsPage.jsx` to consume `fetchCropWorkspaceDetails`.
