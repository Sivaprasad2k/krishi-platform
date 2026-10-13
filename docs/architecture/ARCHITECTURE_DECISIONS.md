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
