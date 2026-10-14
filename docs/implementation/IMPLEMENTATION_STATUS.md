# Krishi Platform — Implementation Status

**Document Version:** 1.0.0
**Last Updated:** October 2026
**Integration Baseline:** `main` (`0003063`)
**Active Development Branches:**
- **Developer A (Core Platform):** `feature/core-platform`
- **Developer B (AI Service):** `feature/ai-service`

---

## 1. Executive Summary

Krishi is an agricultural operations and intelligence platform designed to guide farmers through end-to-end cultivation, farm management, disease mitigation, and automated task execution.

The platform employs a dual-stream development architecture:
1. **Core Platform (Developer A):** Monolithic Spring Boot backend and React Vite PWA handling relational operations, identity, agronomic scheduling, and farm workflows.
2. **AI Service (Developer B):** Dedicated Python microservice providing high-precision computer vision (disease diagnosis), LLM-assisted agronomy guidance (AI Doctor), and soil nutrient recommendations.

---

## 2. Milestone Roadmap & Progress

| Milestone | Domain | Owner | Status | Test Coverage |
| :--- | :--- | :--- | :--- | :--- |
| **Milestone 1** | Core Operational Stabilization (Method Security, Workspace Endpoint, Regression Testing) | Developer A | **Complete** | Unit, Security & Integration (Passing) |
| **Milestone 2** | Automatic Cultivation Planning (Farm Location, ICAR Knowledge Catalog, Plan Generation, Task Workflows, UI) | Developer A | **Complete** | 46 Backend Tests, 16 Frontend Tests (Passing) |
| **Milestone 3A** | Core Workflow Stabilization (P0 Dashboard Consolidation, P0 Overdue Task Actions & Skip Reason, P1 Treatments API Consolidation, P1 Farm Navigation & Structured Location) | Developer A | **Complete** | 49 Backend Tests, 22 Frontend Tests (Passing) |
| **Milestone 3B** | Weather Intelligence & Contextual Agricultural Advisories | Developer A | **Complete** | 60 Backend Tests, 27 Frontend Tests (Passing) |
| **Milestone 4A** | Dynamic Crop Growth-Stage Estimator (Deterministic Resolver, Boundary Semantics, Day-Count Convention, Workspace Integration) | Developer A | **Complete** | 81 Backend Tests (17 Unit + 4 Integration), 27 Frontend Tests (Passing) |
| **Milestone 4B** | Agricultural Context Foundation (Domain Aggregation, Estimated vs Observed Stages, Weather Freshness, Dual API Endpoints) | Developer A | **Complete** | 96 Backend Tests (10 Unit + 5 Integration), 27 Frontend Tests (Passing) |
| **Milestone 5A** | Agricultural Advisory Rules Engine (Deterministic Rules, Non-Assumption of Safety, 4 Core Rules, Idempotency, V22 Migration, Complete Lifecycle, Post-Review Hardening) | Developer A | **Complete** | 137 Backend Tests (34 Unit + 7 Integration), 27 Frontend Tests (Passing) |
| **Milestone 4** | Python AI Service Foundation & Contract Setup | Developer B | **Pending Setup** | Service Contract Defined |
| **Milestone 5** | AI Disease Detection & Computer Vision Pipeline | Developer B | **Planned** | Model Evaluation Suite Pending |
| **Milestone 6** | Government Schemes Discovery & Subsidies | Developer A | **Planned** | Pending Implementation |
| **Milestone 7** | Community Knowledge & Agricultural Marketplace | Developer A | **Planned** | Pending Implementation |

---

## 3. Subsystem Breakdown & Ownership

### A. Core Platform (Developer A Ownership)

| Component | Modules / Packages | Status | Verification Detail |
| :--- | :--- | :--- | :--- |
| **Identity & Security** | `com.krishi.backend.identity`, `com.krishi.backend.common.security` | Production Ready | JWT Auth, Method Security (`@PreAuthorize`), Farmer Ownership IDOR checks. |
| **Farm Management** | `com.krishi.backend.farm` | Verified (V17 Migration) | State, district, optional GPS coordinates (`latitude`/`longitude`), plot hierarchy, active operational navigation (`/crops`, `/disease-incidents`, `/tasks`). |
| **Crop Cycles & Plantings** | `com.krishi.backend.cropcycle`, `com.krishi.backend.cropplanting` | Verified | Seasonal groupings, planting lifecycles, aggregated workspace endpoint (`/workspace`), and farm-context filtering. |
| **Crop Knowledge Catalog** | `com.krishi.backend.cultivation` | Verified (V18 Migration) | Versioned ICAR agronomic packages for Rice (Paddy), Tomato, and Wheat. Zero hardcoding in UI. |
| **Cultivation Plan Generator** | `com.krishi.backend.cultivation.service.CultivationPlanService` | Verified (V19 Migration) | Idempotent generation, version recording, partial unique active plan index, history preservation on revisions. |
| **Crop Growth-Stage Estimator** | `com.krishi.backend.cultivation.service.AgronomicStageResolverService` | Production Ready (Milestone 4A) | Deterministic calendar-day offset matching, Day 0 boundary resolution, closed integer intervals, progress calculations, workspace integration. Zero ML. |
| **Agricultural Context Foundation** | `com.krishi.backend.cropplanting.service.AgriculturalContextService` | Production Ready (Milestone 4B) | Safe read-only domain aggregation, estimated vs field-observed separation, 4-tier weather freshness, `/context` endpoint, enriched workspace, N+1 query elimination. Zero DB persistence. |
| **Advisory Rules Engine** | `com.krishi.backend.advisory` | Production Ready (Milestone 5A) | 4 deterministic rules, non-assumption of safety on stale/missing data, V22 migration, partial active index, complete lifecycle (ACTIVE, ACKNOWLEDGED, RESOLVED, SUPERSEDED, EXPIRED), in-app notifications (isolated transaction safety), domain event auditing (auto-resolution reasons), stable task due-date fingerprints, uniform `ApiResponse<T>` envelope. Zero automated task mutations. |
| **Task Management** | `com.krishi.backend.task` | Verified (V20 Migration) | Lifecycle transitions (`PENDING`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED`, `SKIPPED`, `OVERDUE`), overdue actions unlocked in UI, skip reason persistence (`POST /tasks/{id}/skip`), rescheduling (`/reschedule`). |
| **Disease Incidents & Treatments**| `com.krishi.backend.diseaseincident`, `com.krishi.backend.treatment` | Production Ready | Field observation tracking, consolidated `GET /api/v1/treatments` endpoint with eager JOIN FETCH hierarchy, waterfall eliminated. |
| **Weather Intelligence** | `com.krishi.backend.weather`, `frontend/src/features/weather` | Verified (V21 Migration) | Open-Meteo CC BY 4.0 provider abstraction, 2-decimal normalized spatial grid caching (~1.1 km), Kerala district centroid registry, idempotent advisory engine (`HEAVY_RAIN_IRRIGATION`, `HIGH_WIND_SPRAYING`, `EXTREME_HEAT_ALERT`), connection-pinned PostgreSQL advisory locking. |
| **Frontend PWA** | `frontend/src` | Production Ready | Responsive React 19 + Tailwind PWA, consolidated operational dashboard, WeatherSummaryWidget, WeatherAdvisoryBanner, SkipTaskModal, farm navigation & filter persistence. Production build verified. |

### B. AI Microservice (Developer B Ownership)

| Component | Responsibility | Status | Dependencies |
| :--- | :--- | :--- | :--- |
| **Python Service Architecture** | FastAPI REST runtime, health checks, version reporting | Scaffold Ready | Python 3.11+, PyTorch/ONNX runtime. |
| **Disease Detection** | Leaf/crop image classification and symptom segmentation | Specification Ready | REST multipart upload contract defined. |
| **AI Doctor** | Contextual advisory chat for agricultural queries | Specification Ready | LLM integration, prompt sanitization. |
| **Fertilizer Recommendation** | N-P-K soil and growth-stage nutrient dosing | Specification Ready | ICAR soil chemistry calibration guidelines. |

---

## 4. Current Verification Baseline

- **Database Engine:** PostgreSQL 16 on `localhost:5432` (`krishi_test_db`).
- **Flyway Migrations:** All 22 migrations applied and validated (V17 through V22).
- **Backend Test Suite:** 137 tests executed across all modules, 0 failures, 0 errors, 0 skipped (including 9 unit tests in `AgriculturalAdvisoryServiceTest` and 7 integration tests in `AgriculturalAdvisoryIntegrationTest`).
- **Frontend Test Suite:** 27 tests executed via Vitest across 9 suites, 0 failures, 0 errors.
- **Frontend Build:** `vite build` completed cleanly with zero bundle errors (PWA service worker generated).
- **Git State:**
  - `main`: Clean integration reference (`0003063`).
  - `feature/core-platform`: Working branch containing all verified Milestone 1, 2, 3A, 3B, 4A, 4B & 5A additions and post-review hardening.
  - `feature/ai-service`: Clean branch initialized from `main` (`0003063`).
