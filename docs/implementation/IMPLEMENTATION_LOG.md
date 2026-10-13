# Krishi Platform — Implementation Log

This log records sequential architectural changes, migrations, security enhancements, and milestones implemented in the Krishi Platform codebase.

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
