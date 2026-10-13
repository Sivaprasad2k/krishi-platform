# Krishi Platform — Implementation Log

This log records sequential architectural changes, migrations, security enhancements, and milestones implemented in the Krishi Platform codebase.

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
