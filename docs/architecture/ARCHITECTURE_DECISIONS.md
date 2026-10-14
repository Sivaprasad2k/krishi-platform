# Krishi Platform — Architecture Decision Records (ADRs)

---

## ADR 001: Method-Level Security and Explicit IDOR Authorization

- **Status:** Accepted & Enforced
- **Context:** Initial architecture relied primarily on URL-based filter security, which left internal service methods vulnerable to Insecure Direct Object References (IDOR) if endpoints omitted manual user ID verification.
- **Decision:** Enable `@EnableMethodSecurity` globally in Spring Security. Require dual enforcement:
  1. Role validation at endpoint boundaries (`@PreAuthorize("hasRole('FARMER')")`).
  2. Explicit farmer ownership verification within domain services comparing entity ownership to `SecurityContextHolder` authenticated principal.
- **Consequences:** Eliminates IDOR cross-tenant access. Prevents unauthorized farmers from viewing, modifying, or rescheduling tasks, farms, crops, or treatments owned by other users.

---

## ADR 002: Unified Crop Workspace Aggregation Endpoint

- **Status:** Accepted & Enforced
- **Context:** The frontend Crop Details workflow previously made multiple sequential HTTP requests (crop planting, crop cycle, farm, tasks, disease incidents, treatments) creating an API waterfall, slow paint times, and race conditions.
- **Decision:** Implement `GET /api/v1/crop-plantings/{plantingId}/workspace` returning an aggregated payload containing the complete operational workspace (crop, season, farm, tasks, incidents, treatments, active cultivation plan).
- **Consequences:** Reduces network round trips from 6+ sequential calls to 1 atomic request. Simplifies frontend state management and provides cohesive offline caching boundaries.

---

## ADR 003: Versioned Crop Knowledge Catalog and ICAR Agronomic Guidance

- **Status:** Accepted & Enforced
- **Context:** The platform requires cultivation schedules for farmers, but hardcoding schedules in frontend components violates domain integrity, makes regional adaptation impossible, and creates maintenance bottlenecks.
- **Decision:** Establish a relational, versioned knowledge catalog in PostgreSQL (`cultivation_templates`, `cultivation_stages`, `cultivation_template_tasks`) seeded exclusively from validated Indian Council of Agricultural Research (ICAR) packages of practice (Rice/Paddy, Tomato, Wheat).
- **Consequences:** UI components dynamically render catalog guidance without hardcoded durations or invented chemical/fertilizer dosages. Extension officers can version and publish updated packages without requiring frontend deployments.

---

## ADR 004: Cultivation Plan Generation, Idempotency, and Task History Preservation

- **Status:** Accepted & Enforced
- **Context:** When farmers create plantings or revise cultivation parameters, generating tasks must not create duplicates upon network retries, and revising plans must not destroy existing work records.
- **Decision:**
  1. Make `generatePlanForPlanting()` strictly idempotent: retrying with the same template ID returns the active plan without duplicate task generation.
  2. When a farmer revises an existing plan with a new template, archive the old plan as `SUPERSEDED`, preserve historical tasks (`COMPLETED`, `IN_PROGRESS`, `SKIPPED`), and supersede only unstarted `PENDING` plan tasks.
- **Consequences:** Guarantees historical auditability of field work while allowing dynamic plan adjustments.

---

## ADR 005: Dual-Branch Development Model (Core Platform vs. Python AI Service)

- **Status:** Accepted & Enforced
- **Context:** Two developers are working concurrently: Developer A on core operational platform (Java / Spring Boot / React) and Developer B on AI computer vision and advisory models (Python / PyTorch). Shared commits on a single branch risk unstable baselines and merge conflicts.
- **Decision:**
  - `main`: Clean integration branch containing only verified, regression-tested production baselines.
  - `feature/core-platform`: Working branch for Developer A (farm operations, tasks, knowledge catalog, frontend).
  - `feature/ai-service`: Working branch for Developer B (independent Python microservice, ML models, AI doctor).
- **Consequences:** Complete isolation of dependencies (Java 17 vs Python 3.11). Developers coordinate via documented REST contracts (`docs/integration/AI_INTEGRATION_CONTRACT.md`) without blocking each other's local repositories.

---

## ADR 006: Dedicated Test Database Guard and Flyway Validation Invariants

- **Status:** Accepted & Enforced
- **Context:** Integration tests run against PostgreSQL and execute Flyway schema migrations. Connecting to a shared or production database could cause catastrophic data corruption.
- **Decision:** `TestDatabaseInitializer` strictly validates that the test datasource targets `jdbc:postgresql://localhost:5432/krishi_test_db`. If the URL targets any other database, tests immediately abort before executing migrations or queries.
- **Consequences:** Eliminates risks of test fixtures contaminating production or development databases.

---

## ADR 007: Database-Level Enforced Single Active Cultivation Plan Invariant & JPA Flush Ordering

- **Status:** Accepted & Enforced
- **Context:** If concurrent HTTP requests attempt to generate cultivation plans for the same crop planting, application-level checks alone cannot prevent race conditions. Duplicate `ACTIVE` plans would result in orphaned tasks and non-deterministic task scheduling. Additionally, under Hibernate/JPA, replacing an entity constrained by a partial unique index can fail if Hibernate queues the `INSERT` before the `UPDATE` of the old entity.
- **Decision:**
  1. Enforce a PostgreSQL partial unique index via forward migration `V19__enforce_single_active_cultivation_plan.sql`:
     `CREATE UNIQUE INDEX IF NOT EXISTS uq_active_cultivation_plan_per_planting ON krishi.cultivation_plans (crop_planting_id) WHERE status = 'ACTIVE';`
  2. In `CultivationPlanService.java`, execute `planRepository.saveAndFlush(existingPlan)` prior to constructing and saving the new plan, ensuring the SQL `UPDATE` to `SUPERSEDED` is executed immediately on the database connection before the `INSERT` of the new `ACTIVE` plan.
- **Consequences:** Provides hard database guarantees against duplicate active cultivation plans. Prevents constraint violation exceptions during plan revisions.

---

## ADR 008: Weather Intelligence Provider Abstraction & Open-Meteo Integration

- **Status:** Accepted & Implemented (Milestone 3B)
- **Context:** Krishi requires accurate meteorological forecasts and agricultural parameters (precipitation, temperature, humidity, wind, and soil conditions) mapped to Indian farm coordinates to power operational advisories and field activity planning. Hardcoding a single commercial vendor or relying on APIs that require high fees or complex authentication creates operational friction and vendor lock-in.
- **Decision:**
  1. Define a domain interface `WeatherDataProvider` that abstracts weather retrieval, returning canonical domain objects (`WeatherObservation`, `DailyForecast`, `HourlyForecast`).
  2. Implement an `OpenMeteoProviderClient` leveraging Open-Meteo's API with ECMWF IFS / GFS numerical models. Open-Meteo provides global coverage, high resolution over India, agricultural variables, 10,000 free requests/day for non-commercial use, and full CC BY 4.0 attribution compliance with UI hyperlink. Support optional API keys for commercial tiers.
  3. Implement a resilient spatial caching layer (`krishi.weather_forecast_cache`) normalized to 2 decimal places (~1.1 km precision) with a 2-hour TTL and 15-minute manual refresh cooldown. If the external provider is unreachable or times out, serve valid cached data with a `stale: true` indicator. Never fabricate synthetic meteorological values.
  4. Use farm GPS coordinates when available; fall back to verified district centroid coordinates seeded from Census of India 2011 / NIC portals. If neither is available, return an explicit HTTP 422 `LOCATION_UNAVAILABLE` error prompt.
- **Consequences:** Decouples core platform from external weather APIs. Ensures low-latency, resilient responses while keeping operating costs zero during early adoption.

---

## ADR 009: Contextual Agricultural Advisories and Non-Intrusive Task Guidance

- **Status:** Accepted & Implemented (Milestone 3B)
- **Context:** Adverse meteorological events (heavy downpours, strong winds, extreme heat) impact pending field work (irrigation, spraying, harvesting). However, automatically rescheduling, cancelling, or completing tasks without farmer knowledge violates agronomic sovereignty and could result in disastrous operational decisions if conditions vary locally.
- **Decision:**
  1. Evaluate weather forecasts against a defensible, ICAR-aligned rules engine producing `WeatherAdvisory` records. Start in V1 with 3 strictly verified operational warning categories:
     - `HEAVY_RAIN_IRRIGATION`: Rain >= 20mm or prob >= 80% on pending irrigation tasks.
     - `HIGH_WIND_SPRAYING`: Sustained wind > 20 km/h or gusts > 30 km/h on pending spraying/fertilizing tasks.
     - `EXTREME_HEAT_ALERT`: Max temperature >= 40.0°C for active plantings.
  2. Defer unverified agronomic predictions (fungal blast microclimate models, frost warnings) pending cultivar-level sensor calibration.
  3. When an advisory impacts a scheduled task, link the advisory to the task (`task_id`) and render contextual warning chips on task cards and modal guidance. Never mutate task state, due dates, or completion data automatically.
  4. Enforce idempotency and deduplication via unique composite fingerprints (`farmId:plantingId:taskId:ruleCode:targetDate`) backed by a partial unique index (`WHERE status = 'ACTIVE'`).
  5. If conditions worsen, supersede the previous advisory, insert the escalated alert, and dispatch a notification. If conditions remain stable, update `lastEvaluatedAt` without re-notifying the farmer.
- **Consequences:** Empowers farmers with actionable contextual intelligence while preserving operational control and preventing alert fatigue.

---

## ADR 010: Concurrency-Safe Scheduler Advisory Locking & Immediate Action Queue Flushing

- **Status:** Accepted & Implemented (Milestone 3B)
- **Context:** Scheduled background jobs (e.g. daily weather forecast sync across registered farms) in a multi-instance or clustered environment risk race conditions, redundant API consumption, and rate-limit exhaustion if multiple workers execute simultaneously. Furthermore, Hibernate's action queue delays entity UPDATE operations relative to INSERTs within a transaction, which triggers unique constraint violations on partial indexes (`WHERE status = 'ACTIVE'`) during entity supersession.
- **Decision:**
  1. Guard the scheduled weather synchronization job with a 64-bit PostgreSQL session-level advisory lock (`SELECT pg_try_advisory_lock(?)`) pinned to a single physical JDBC `Connection` via `DataSource.getConnection()`.
  2. Wrap lock acquisition and execution in a `try-with-resources` construct with an explicit `pg_advisory_unlock` in the `finally` block on the exact same connection instance. If the connection drops or is closed, PostgreSQL guarantees automatic lock release, completely eliminating orphaned locks and overlapping executions.
  3. When superseding active entities (e.g. transitioning an advisory from `ACTIVE` to `SUPERSEDED` upon severity escalation), call `repository.saveAndFlush(existing)` prior to creating and saving the new `ACTIVE` entity. This forces the SQL `UPDATE` to execute on the database connection immediately, preventing unique constraint collisions against the partial index.
- **Consequences:** Guarantees zero overlapping scheduled jobs without requiring distributed lock brokers like Redis or ZooKeeper, and prevents unique constraint violations during entity escalation.

---

## ADR 011: Deterministic Agronomic Growth-Stage Estimator and Boundary Semantics

- **Status:** Accepted & Implemented (Milestone 4A)
- **Context:** Krishi requires dynamic calculation of the current crop growth stage from planting dates and published cultivation templates (ICAR packages). A machine learning or stochastic approach introduces non-determinism, hallucinations, and unreliability in agricultural operations. Additionally, growth stages define integer day offsets (`startDayOffset`, `endDayOffset`) where boundary overlaps (e.g. Nursery ending at day 0 and Transplanting starting at day 0) require explicit, non-arbitrary domain semantics.
- **Decision:**
  1. **Day-Count Convention:** Standard calendar-day difference calculated via `ChronoUnit.DAYS.between(plantingDate.toLocalDate(), asOfDate)`. Day 0 is strictly defined as the calendar day of planting (`elapsedDays == 0`). Negative `elapsedDays` indicates pre-planting time.
  2. **Closed-Interval Matching & Day 0 Transition:** Evaluate stages based on closed integer intervals `[startDayOffset, endDayOffset]`. In ICAR cultivation templates (e.g., Rice), the Nursery stage spans `[-15, 0]` and Transplanting & Establishment spans `[0, 20]`. When `elapsedDays == 0`, the crop enters the field; thus the resolver deterministically selects the field stage (Stage 2) rather than remaining in nursery.
  3. **No Silent or Arbitrary Selection:** If non-consecutive or erroneous stages overlap (e.g. `[10, 30]` and `[20, 50]`) or if gaps exist in the active plan's stage catalogue, the resolver explicitly returns `STAGE_DEFINITION_ERROR` with a descriptive message rather than silently selecting an arbitrary stage.
  4. **Explicit Lifecycle Statuses:** Model stage estimates using `GrowthStageStatus`:
     - `RESOLVED`: Stage successfully determined from active cultivation plan.
     - `PRE_PLANTING`: Evaluated before planting date or before the earliest stage offset (`elapsedDays < 0`).
     - `POST_HARVEST`: Evaluated after `expectedHarvestDate` or past the template's final stage `endDayOffset`.
     - `NO_ACTIVE_PLAN`: Planting exists but lacks an active cultivation plan.
     - `MISSING_PLANTING_DATE`: Planting date is null.
     - `STAGE_DEFINITION_ERROR`: Inconsistent or overlapping stage definitions detected.
  5. **Progress Calculation:** Calculate `stageProgressPercentage` as `(elapsedDays - startDay) / (endDay - startDay) * 100` clamped to `[0.0, 100.0]`. Calculate `overallProgressPercentage` as `elapsedDays / totalDurationDays * 100` clamped to `[0.0, 100.0]`.
  6. **API Integration:** Expose `estimatedGrowthStage` in `CropPlantingResponse` and `CropWorkspaceResponse` with an optional `asOfDate` query parameter (defaulting to current date), preserving backward compatibility.
- **Consequences:** Fully deterministic, testable growth-stage resolution without external dependencies or machine learning. Prevents mistaking an estimate for a farmer-confirmed observation.

---

## ADR 012: Agricultural Context Foundation & Safe Domain Aggregation

- **Status:** Accepted & Implemented (Milestone 4B)
- **Context:** Future agronomic advisory rules and external AI services (disease detection, doctor advisory, nutrient planning) require a comprehensive snapshot of crop state, planting lifecycle, active cultivation plans, operational tasks, field coordinates, weather conditions, and active advisories. Creating separate or persisted database models for context would introduce stale data duplication, relational synchronization hazards, and schema bloat.
- **Decision:**
  1. **Transient Domain Aggregation Service:** Implement `AgriculturalContextService` with read-only transactional semantics (`@Transactional(readOnly = true)`) aggregating existing domain models (`CropPlanting`, `CropCycle`, `Farm`, `CultivationPlan`, `Task`, `WeatherForecastCache`, `WeatherAdvisory`). Zero persistence of derived context.
  2. **Strict Missing-Data Preservation:** Never invent missing values. If crop variety or plot is unrecorded, return `null`. Preserve all `GrowthStageStatus` semantics (`RESOLVED`, `NO_ACTIVE_PLAN`, `PRE_PLANTING`, `POST_HARVEST`, `STAGE_DEFINITION_ERROR`).
  3. **Clear Distinction between Estimated vs Observed Growth Stage:** Expose `estimatedStage` (derived from mathematical calendar-day offsets against ICAR templates) alongside `observedStage` (null/unconfirmed biological field observation), with an explicit notice to prevent AI models or farmers from conflating algorithmic estimates with ground truth.
  4. **Defensive Environmental Weather Resolution:** Weather provider outages or unconfigured farm coordinates must never fail operational agricultural requests. Implement a 4-tier freshness classification (`FRESH`, `STALE`, `UNAVAILABLE`, `LOCATION_UNAVAILABLE`). Pre-validate coordinate resolution and configure `@Transactional(noRollbackFor = {LocationUnavailableException.class, WeatherProviderException.class})` to prevent unexpected rollback of outer read-only transactions.
  5. **Dual Endpoint Architecture:**
     - Extend existing `GET /api/v1/crop-plantings/{plantingId}/workspace` with optional `weather` context to eliminate extra frontend hops.
     - Expose dedicated `GET /api/v1/crop-plantings/{plantingId}/context` (alias `/agricultural-context`) for AI services and headless rule engines, omitting UI-specific incident/treatment payloads.
  6. **Tenant Isolation:** Enforce farmer role verification (`@PreAuthorize("hasRole('FARMER')")`) and hierarchical farm ownership validation across both workspace and context endpoints.
- **Consequences:** Clean, deterministic context provider supporting both interactive UI workflows and future AI integrations with zero schema changes, zero external dependencies, and complete failure resilience.

---

## ADR 013: Deterministic Agricultural Advisory Rules Engine & Lifecycle Semantics

- **Status:** Accepted & Implemented (Milestone 5A)
- **Context:** Krishi requires an explainable, deterministic rules engine that converts contextual agricultural intelligence (`AgriculturalContextResponse`) into actionable recommendations without becoming another disconnected CRUD module. Recommendations must explicitly document rule assumptions, provenance, and lifecycle states while preserving farmer ownership and preventing duplicate alerts.
- **Decision:**
  1. **Deterministic Rule Taxonomy & Non-Assumption of Safety:** Every rule implements `AgriculturalRule` and returns a `RuleEvaluationResult` categorized into four explicit outcomes:
     - `RULE_EVALUATION_SUCCESS`: Rule evaluated successfully; advisory generated or conditions confirmed benign.
     - `RULE_NOT_APPLICABLE`: Rule does not apply to the current crop, stage, or task context.
     - `INSUFFICIENT_CONTEXT`: Required inputs are missing or stale. Missing or stale data is **never** interpreted as evidence that conditions are safe.
     - `RULE_EVALUATION_FAILURE`: Runtime exception caught safely without crashing batch or single-planting evaluations.
  2. **Isolated Rule Boundary:** `AgriculturalAdvisoryEngine` evaluates each rule in an isolated `try/catch` boundary, ensuring an unhandled exception in one rule does not abort remaining rule evaluations.
  3. **Initial Rule Set & Provenance:**
     - `IRRIGATION_RAINFALL_CONFLICT`: Withhold scheduled artificial irrigation when forecast precipitation on task due date is $\ge 20.0\,\text{mm}$ or probability $\ge 80\%$ (Provenance: Standard ICAR & Agricultural Water Management Guidelines).
     - `CHEMICAL_SPRAYING_WIND_RISK`: Postpone chemical applications when forecast sustained wind $> 20.0\,\text{km/h}$ or gusts $> 30.0\,\text{km/h}$ (Provenance: *Provisional threshold* based on FAO/CABI Agrochemical Spray Drift Guidelines).
     - `OVERDUE_CULTIVATION_TASK`: Alert overdue tasks ($\ge 1\,\text{day}$) and contextualize agronomic risk against current growth stage (Provenance: Krishi Cultivation Operations Standard).
     - `STAGE_TASK_MISALIGNMENT`: Warn when pending tasks belong to prior elapsed cultivation stages; evaluated **only** when `GrowthStageStatus == RESOLVED` (Provenance: Certified ICAR Cultivation Plan Phenological Stage Boundaries).
  4. **Advisory Persistence & Idempotency:** Persist advisories in `krishi.agricultural_advisories` via Flyway migration `V22`. Deduplication via composite fingerprint (`farm:planting:task:rule:date`) backed by a partial unique index `WHERE status = 'ACTIVE'`. Concurrent creation conflicts resolve safely to the existing active record.
  5. **Explicit Advisory Lifecycle:** Support five discrete lifecycle states: `ACTIVE`, `ACKNOWLEDGED`, `RESOLVED`, `SUPERSEDED`, and `EXPIRED`. Escalation supersedes lower-severity active advisories and dispatches `[ESCALATION]` notifications. Conditions abating or tasks completing automatically transition advisories to `RESOLVED`.
  6. **Workflow Non-Interference:** The rules engine is advisory-only. It never automatically mutates, reschedules, completes, or skips farmer tasks.
  7. **Security & Auditability:** Enforce `ROLE_FARMER` and verified farm ownership on all advisory APIs. Publish domain events (`AGRICULTURAL_ADVISORY_CREATED`, `AGRICULTURAL_ADVISORY_ACKNOWLEDGED`, `AGRICULTURAL_ADVISORY_RESOLVED`) persisted synchronously to `krishi.domain_events`.
   8. **Notification Transaction Boundary Isolation:** Configure `NotificationService.createNotification` with `@Transactional(propagation = Propagation.REQUIRES_NEW)` to ensure notification database failures execute in an isolated transaction. The outer advisory transaction catches and logs notification errors without being marked rollback-only, guaranteeing that the advisory remains persisted.
   9. **API Response Contract Consistency:** All endpoints in `AgriculturalAdvisoryController` adhere to Krishi's standard `ApiResponse<T>` envelope (`success`, `message`, `data`, `timestamp`) across evaluation, listing, acknowledgment, and resolution.
   10. **N+1 Query Elimination in Task Context Loading:** `AgriculturalContextService.buildRelevantTasks` leverages `taskRepository.findByCropPlantingIdWithTemplateTaskAndStage(plantingId)` with eager `LEFT JOIN FETCH` on template tasks and cultivation stages, eliminating relational query cascades during context resolution.
   11. **Audit Completeness & Fingerprint Stabilization:** Automated rules-engine resolutions (due to linked task completion, cancellation, skipping, or risk conditions clearing clean) publish `AdvisoryResolvedEvent` with descriptive `resolutionReason`. Status transitions to `RESOLVED` prevent duplicate resolution events during repeated evaluations. Task rule fingerprints bind to task `dueDate` rather than volatile evaluation timestamps to ensure deterministic identity.
- **Consequences:** Provides robust, transparent, reproducible decision support for farmers with zero external AI dependencies, transaction-safe notification dispatch, and strong idempotency guarantees.
