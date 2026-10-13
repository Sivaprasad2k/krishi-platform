-- Migration V18: Create Crop Knowledge Catalog and Cultivation Plans

-- 1. Cultivation Templates
CREATE TABLE krishi.cultivation_templates (
    id UUID PRIMARY KEY,
    crop_name VARCHAR(100) NOT NULL,
    crop_variety VARCHAR(100),
    season VARCHAR(50),
    region_state VARCHAR(100),
    region_district VARCHAR(100),
    duration_days INTEGER NOT NULL,
    source VARCHAR(255) NOT NULL,
    version VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PUBLISHED',
    description TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);

CREATE INDEX idx_cultivation_templates_crop ON krishi.cultivation_templates(crop_name, status);
CREATE INDEX idx_cultivation_templates_region ON krishi.cultivation_templates(region_state, status);

-- 2. Cultivation Stages
CREATE TABLE krishi.cultivation_stages (
    id UUID PRIMARY KEY,
    template_id UUID NOT NULL REFERENCES krishi.cultivation_templates(id) ON DELETE CASCADE,
    stage_name VARCHAR(100) NOT NULL,
    stage_order INTEGER NOT NULL,
    start_day_offset INTEGER NOT NULL,
    end_day_offset INTEGER NOT NULL,
    description TEXT
);

CREATE INDEX idx_cultivation_stages_template ON krishi.cultivation_stages(template_id, stage_order);

-- 3. Cultivation Template Tasks
CREATE TABLE krishi.cultivation_template_tasks (
    id UUID PRIMARY KEY,
    template_id UUID NOT NULL REFERENCES krishi.cultivation_templates(id) ON DELETE CASCADE,
    stage_id UUID REFERENCES krishi.cultivation_stages(id) ON DELETE SET NULL,
    task_type VARCHAR(50) NOT NULL,
    title VARCHAR(150) NOT NULL,
    description TEXT,
    day_offset INTEGER NOT NULL,
    window_days_before INTEGER NOT NULL DEFAULT 1,
    window_days_after INTEGER NOT NULL DEFAULT 2,
    task_order INTEGER NOT NULL DEFAULT 1,
    mandatory BOOLEAN NOT NULL DEFAULT true
);

CREATE INDEX idx_template_tasks_template ON krishi.cultivation_template_tasks(template_id, day_offset);

-- 4. Cultivation Plans
CREATE TABLE krishi.cultivation_plans (
    id UUID PRIMARY KEY,
    crop_planting_id UUID NOT NULL REFERENCES krishi.crop_plantings(id) ON DELETE CASCADE,
    template_id UUID NOT NULL REFERENCES krishi.cultivation_templates(id),
    template_version VARCHAR(50) NOT NULL,
    plan_version INTEGER NOT NULL DEFAULT 1,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    generated_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    created_by UUID,
    updated_by UUID
);

CREATE INDEX idx_cultivation_plans_planting ON krishi.cultivation_plans(crop_planting_id, status);

-- 5. Tasks generalization with Plan links
ALTER TABLE krishi.tasks
    ADD COLUMN cultivation_plan_id UUID REFERENCES krishi.cultivation_plans(id) ON DELETE SET NULL,
    ADD COLUMN template_task_id UUID REFERENCES krishi.cultivation_template_tasks(id) ON DELETE SET NULL;

CREATE INDEX idx_tasks_cultivation_plan_id ON krishi.tasks(cultivation_plan_id);

-- 6. Seed validated agricultural guidance

-- ============================================================================
-- TEMPLATE 1: Rice (Paddy) - ICAR-IARI Standard Package of Practices
-- ============================================================================
INSERT INTO krishi.cultivation_templates (
    id, crop_name, crop_variety, season, region_state, region_district,
    duration_days, source, version, status, description, created_at
) VALUES (
    'a0000001-0000-0000-0000-000000000001',
    'Rice (Paddy)',
    'IR20 / Medium Duration',
    'Kharif',
    'ALL',
    'ALL',
    120,
    'ICAR - Indian Agricultural Research Institute (IARI) Package of Practices',
    '2024.1',
    'PUBLISHED',
    'Validated agronomic schedule for transplanted medium-duration wetland rice in irrigated plains.',
    CURRENT_TIMESTAMP
);

-- Stages for Rice
INSERT INTO krishi.cultivation_stages (id, template_id, stage_name, stage_order, start_day_offset, end_day_offset, description) VALUES
('a0000001-0001-0000-0000-000000000001', 'a0000001-0000-0000-0000-000000000001', 'Nursery & Land Preparation', 1, -15, 0, 'Seedbed preparation, organic manure application, and field leveling before puddling.'),
('a0000001-0001-0000-0000-000000000002', 'a0000001-0000-0000-0000-000000000001', 'Transplanting & Establishment', 2, 0, 20, 'Seedling transplanting into puddled soil, gap filling, and shallow standing water maintenance.'),
('a0000001-0001-0000-0000-000000000003', 'a0000001-0000-0000-0000-000000000001', 'Tillering & Vegetative Phase', 3, 21, 50, 'Active tillering, cono-weeder inter-cultivation, and split nitrogen fertilizer top-dressing.'),
('a0000001-0001-0000-0000-000000000004', 'a0000001-0000-0000-0000-000000000001', 'Panicle Initiation & Booting', 4, 51, 75, 'Reproductive transition requiring consistent 5cm moisture layer and stem borer scouting.'),
('a0000001-0001-0000-0000-000000000005', 'a0000001-0000-0000-0000-000000000001', 'Flowering & Grain Filling', 5, 76, 105, 'Anthesis and milky-to-dough grain filling stages; monitor for plant hoppers.'),
('a0000001-0001-0000-0000-000000000006', 'a0000001-0000-0000-0000-000000000001', 'Ripening & Harvesting', 6, 106, 120, 'Terminal field drainage followed by harvesting and threshing at 85% golden maturity.');

-- Tasks for Rice
INSERT INTO krishi.cultivation_template_tasks (id, template_id, stage_id, task_type, title, description, day_offset, window_days_before, window_days_after, task_order, mandatory) VALUES
('a0000001-0002-0000-0000-000000000001', 'a0000001-0000-0000-0000-000000000001', 'a0000001-0001-0000-0000-000000000001', 'MANURE_APPLICATION', 'Basal FYM / Compost Incorporation', 'Incorporate 10-12 tonnes/ha of well-decomposed Farm Yard Manure during wet tillage.', -7, 3, 2, 1, true),
('a0000001-0002-0000-0000-000000000002', 'a0000001-0000-0000-0000-000000000001', 'a0000001-0001-0000-0000-000000000002', 'IRRIGATION', 'Transplanting & Shallow Submergence', 'Transplant 2-3 seedlings per hill at 20x10 cm spacing with 2-3 cm shallow water level.', 0, 0, 2, 2, true),
('a0000001-0002-0000-0000-000000000003', 'a0000001-0000-0000-0000-000000000001', 'a0000001-0001-0000-0000-000000000002', 'CROP_INSPECTION', 'Stand Inspection & Gap Filling', 'Inspect field for seedling mortality and replace missing hills using reserve seedlings.', 7, 2, 3, 3, true),
('a0000001-0002-0000-0000-000000000004', 'a0000001-0000-0000-0000-000000000001', 'a0000001-0001-0000-0000-000000000002', 'WEEDING', 'First Weeding & Cono-Weeder Run', 'Run cono-weeder between plant rows to bury emerging weeds and aerate the root zone.', 15, 2, 3, 4, true),
('a0000001-0002-0000-0000-000000000005', 'a0000001-0000-0000-0000-000000000001', 'a0000001-0001-0000-0000-000000000003', 'FERTILIZER_APPLICATION', 'First Nitrogen Top-Dressing at Tillering', 'Apply 25% of recommended nitrogen uniformly when field water level is low.', 25, 2, 4, 5, true),
('a0000001-0002-0000-0000-000000000006', 'a0000001-0000-0000-0000-000000000001', 'a0000001-0001-0000-0000-000000000003', 'FERTILIZER_APPLICATION', 'Second Nitrogen Top-Dressing at Active Tillering', 'Apply second split of nitrogen together with muriate of potash to stimulate productive tillers.', 45, 3, 3, 6, true),
('a0000001-0002-0000-0000-000000000007', 'a0000001-0000-0000-0000-000000000001', 'a0000001-0001-0000-0000-000000000004', 'IRRIGATION', 'Panicle Initiation Water Management', 'Maintain continuous 5 cm water depth during panicle development to ensure grain formation.', 60, 2, 3, 7, true),
('a0000001-0002-0000-0000-000000000008', 'a0000001-0000-0000-0000-000000000001', 'a0000001-0001-0000-0000-000000000005', 'CROP_INSPECTION', 'Grain Filling Scouting & Pest Monitoring', 'Inspect canopy for brown planthopper (BPH) and sheath blight. Keep soil saturated.', 85, 3, 4, 8, true),
('a0000001-0002-0000-0000-000000000009', 'a0000001-0000-0000-0000-000000000001', 'a0000001-0001-0000-0000-000000000006', 'IRRIGATION', 'Pre-Harvest Water Drainage', 'Completely drain standing field water 10-12 days before anticipated harvest date.', 108, 2, 2, 9, true),
('a0000001-0002-0000-0000-000000000010', 'a0000001-0000-0000-0000-000000000001', 'a0000001-0001-0000-0000-000000000006', 'HARVESTING', 'Paddy Harvesting & Threshing', 'Harvest crop when 85-90% panicles turn golden yellow. Dry grains to 12-14% moisture.', 120, 2, 5, 10, true);

-- ============================================================================
-- TEMPLATE 2: Tomato - ICAR-IIHR Standard Package of Practices
-- ============================================================================
INSERT INTO krishi.cultivation_templates (
    id, crop_name, crop_variety, season, region_state, region_district,
    duration_days, source, version, status, description, created_at
) VALUES (
    'b0000002-0000-0000-0000-000000000001',
    'Tomato',
    'PKM-1 / Arka Rakshak',
    'ALL',
    'ALL',
    'ALL',
    95,
    'ICAR - Indian Institute of Horticultural Research (IIHR) Package of Practices',
    '2024.1',
    'PUBLISHED',
    'Comprehensive production guide for determinate and semi-determinate tomato hybrids.',
    CURRENT_TIMESTAMP
);

-- Stages for Tomato
INSERT INTO krishi.cultivation_stages (id, template_id, stage_name, stage_order, start_day_offset, end_day_offset, description) VALUES
('b0000002-0001-0000-0000-000000000001', 'b0000002-0000-0000-0000-000000000001', 'Transplanting & Establishment', 1, 0, 15, 'Transplanting hardened 25-day seedlings with basal fertilizers and establishment irrigations.'),
('b0000002-0001-0000-0000-000000000002', 'b0000002-0000-0000-0000-000000000001', 'Vegetative Growth & Earthing Up', 2, 16, 35, 'Rapid root expansion, weeding, staking support, and stem earthing up.'),
('b0000002-0001-0000-0000-000000000003', 'b0000002-0000-0000-0000-000000000001', 'Flowering & Fruit Setting', 3, 36, 60, 'Flower cluster emergence and fruit setting with balanced micronutrient support.'),
('b0000002-0001-0000-0000-000000000004', 'b0000002-0000-0000-0000-000000000001', 'Fruit Maturation & Harvesting', 4, 61, 95, 'Fruit ripening flushes, breaker-stage pickings, and post-harvest grading.');

-- Tasks for Tomato
INSERT INTO krishi.cultivation_template_tasks (id, template_id, stage_id, task_type, title, description, day_offset, window_days_before, window_days_after, task_order, mandatory) VALUES
('b0000002-0002-0000-0000-000000000001', 'b0000002-0000-0000-0000-000000000001', 'b0000002-0001-0000-0000-000000000001', 'FERTILIZER_APPLICATION', 'Transplanting & Basal Fertilizer Application', 'Transplant seedlings at 60x45 cm spacing. Apply basal NPK in furrows.', 0, 0, 2, 1, true),
('b0000002-0002-0000-0000-000000000002', 'b0000002-0000-0000-0000-000000000001', 'b0000002-0001-0000-0000-000000000001', 'IRRIGATION', 'Post-Transplanting Light Irrigation', 'Provide light establishment irrigation 3-4 days after field transplanting.', 3, 1, 1, 2, true),
('b0000002-0002-0000-0000-000000000003', 'b0000002-0000-0000-0000-000000000001', 'b0000002-0001-0000-0000-000000000002', 'WEEDING', 'First Weeding & Light Earthing Up', 'Hand weed root perimeter and earth up soil around base to encourage adventitious rooting.', 18, 2, 3, 3, true),
('b0000002-0002-0000-0000-000000000004', 'b0000002-0000-0000-0000-000000000001', 'b0000002-0001-0000-0000-000000000002', 'OTHER', 'Plant Staking & Trellising Support', 'Stake plants using bamboo or twine to lift foliage and prevent soil-borne fruit rot.', 28, 2, 4, 4, true),
('b0000002-0002-0000-0000-000000000005', 'b0000002-0000-0000-0000-000000000001', 'b0000002-0001-0000-0000-000000000003', 'FERTILIZER_APPLICATION', 'Nitrogen Top-Dressing & Foliar Micronutrient Spray', 'Apply side-dress nitrogen. Spray 0.2% Borax foliar spray to prevent blossom end rot.', 40, 2, 3, 5, true),
('b0000002-0002-0000-0000-000000000006', 'b0000002-0000-0000-0000-000000000001', 'b0000002-0001-0000-0000-000000000003', 'CROP_INSPECTION', 'Fruit Borer & Early Blight Scouting', 'Scout fruit clusters for fruit borer holes and leaves for concentric blight spots.', 55, 3, 3, 6, true),
('b0000002-0002-0000-0000-000000000007', 'b0000002-0000-0000-0000-000000000001', 'b0000002-0001-0000-0000-000000000004', 'HARVESTING', 'First Picking at Breaker Stage', 'Pick firm fruits showing pink blush/breaker stage for transportation and storage.', 70, 2, 4, 7, true),
('b0000002-0002-0000-0000-000000000008', 'b0000002-0000-0000-0000-000000000001', 'b0000002-0001-0000-0000-000000000004', 'HARVESTING', 'Main Flushes Fruit Picking & Grading', 'Harvest ripe fruits every 4-5 days. Sort, grade, and pack in ventilated crates.', 85, 2, 4, 8, true);

-- ============================================================================
-- TEMPLATE 3: Wheat - ICAR-IIWBR Standard Package of Practices
-- ============================================================================
INSERT INTO krishi.cultivation_templates (
    id, crop_name, crop_variety, season, region_state, region_district,
    duration_days, source, version, status, description, created_at
) VALUES (
    'c0000003-0000-0000-0000-000000000001',
    'Wheat',
    'HD 2967 / PBW 343',
    'Rabi',
    'ALL',
    'ALL',
    135,
    'ICAR - Indian Institute of Wheat and Barley Research (IIWBR)',
    '2024.1',
    'PUBLISHED',
    'Standard cultivation protocol for timely sown irrigated wheat across northern and central plains.',
    CURRENT_TIMESTAMP
);

-- Stages for Wheat
INSERT INTO krishi.cultivation_stages (id, template_id, stage_name, stage_order, start_day_offset, end_day_offset, description) VALUES
('c0000003-0001-0000-0000-000000000001', 'c0000003-0000-0000-0000-000000000001', 'Sowing & Crown Root Initiation', 1, 0, 25, 'Seed drill sowing with basal nutrients and the critical first CRI irrigation.'),
('c0000003-0001-0000-0000-000000000002', 'c0000003-0000-0000-0000-000000000001', 'Tillering & Jointing Stage', 2, 26, 65, 'Canopy expansion, weed control, second split urea application, and jointing irrigation.'),
('c0000003-0001-0000-0000-000000000003', 'c0000003-0000-0000-0000-000000000001', 'Heading & Flowering Stage', 3, 66, 90, 'Spike emergence and anthesis; scouting for yellow/brown rust.'),
('c0000003-0001-0000-0000-000000000004', 'c0000003-0000-0000-0000-000000000001', 'Milking & Dough Stage', 4, 91, 115, 'Grain filling stage requiring calm-weather gentle irrigation.'),
('c0000003-0001-0000-0000-000000000005', 'c0000003-0000-0000-0000-000000000001', 'Maturity & Harvesting', 5, 116, 135, 'Straw drying, golden maturity, and mechanical or manual harvesting.');

-- Tasks for Wheat
INSERT INTO krishi.cultivation_template_tasks (id, template_id, stage_id, task_type, title, description, day_offset, window_days_before, window_days_after, task_order, mandatory) VALUES
('c0000003-0002-0000-0000-000000000001', 'c0000003-0000-0000-0000-000000000001', 'c0000003-0001-0000-0000-000000000001', 'FERTILIZER_APPLICATION', 'Drill Sowing with Basal Fertilizer', 'Sow treated seed using seed drill with full dose phosphorus, potash, and 1/3rd nitrogen.', 0, 0, 3, 1, true),
('c0000003-0002-0000-0000-000000000002', 'c0000003-0000-0000-0000-000000000001', 'c0000003-0001-0000-0000-000000000001', 'IRRIGATION', 'Critical Crown Root Initiation (CRI) Irrigation', 'Crucial first irrigation 21-25 days after sowing to facilitate vigorous root development.', 21, 2, 2, 2, true),
('c0000003-0002-0000-0000-000000000003', 'c0000003-0000-0000-0000-000000000001', 'c0000003-0001-0000-0000-000000000002', 'FERTILIZER_APPLICATION', 'Weed Management & Nitrogen Top Dressing', 'Remove broadleaf weeds and top-dress 1/3rd nitrogen prior to second irrigation.', 35, 2, 4, 3, true),
('c0000003-0002-0000-0000-000000000004', 'c0000003-0000-0000-0000-000000000001', 'c0000003-0001-0000-0000-000000000002', 'IRRIGATION', 'Late Tillering / Jointing Irrigation', 'Apply irrigation at jointing stage to support healthy earhead elongation.', 55, 3, 3, 4, true),
('c0000003-0002-0000-0000-000000000005', 'c0000003-0000-0000-0000-000000000001', 'c0000003-0001-0000-0000-000000000003', 'CROP_INSPECTION', 'Flowering Inspection & Rust Disease Scouting', 'Scout leaf canopy for yellow rust pustules and aphid colonies during heading.', 75, 3, 3, 5, true),
('c0000003-0002-0000-0000-000000000006', 'c0000003-0000-0000-0000-000000000001', 'c0000003-0001-0000-0000-000000000004', 'IRRIGATION', 'Milking Stage Gentle Irrigation', 'Irrigate gently during grain development; avoid irrigating during windy weather to prevent lodging.', 95, 3, 3, 6, true),
('c0000003-0002-0000-0000-000000000007', 'c0000003-0000-0000-0000-000000000001', 'c0000003-0001-0000-0000-000000000005', 'HARVESTING', 'Maturity Harvesting & Storage', 'Harvest when straw turns golden dry and grain moisture drops below 12%.', 135, 3, 5, 7, true);
