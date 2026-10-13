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
- **Workflows:** Direct transitions, interactive date rescheduling (`/reschedule`), and explicit skipping (`/skip`).

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
- **Current Migration Level:** `V19` (V17: Structured farm location; V18: Crop knowledge catalog, stages, templates, and cultivation plans; V19: Partial unique index enforcing single active cultivation plan per planting).
- **Storage Constraints & Invariants:**
  - `krishi.cultivation_plans`: Guaranteed single `ACTIVE` plan per `crop_planting_id` via PostgreSQL partial index (`uq_active_cultivation_plan_per_planting`).
  - `krishi.cultivation_templates`: Stage order and relative task day offsets enforced via relational keys and check constraints.
- **Test Database Invariant:** Integration tests strictly require and validate `jdbc:postgresql://localhost:5432/krishi_test_db`. Destruction or recreation of production data is prevented by initializer URL validation guards.
