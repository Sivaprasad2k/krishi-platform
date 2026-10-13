-- Migration V21: Weather Intelligence, District Registry, and Contextual Advisories

-- 1. District Coordinates Registry
CREATE TABLE krishi.district_coordinates (
    id UUID PRIMARY KEY,
    state VARCHAR(100) NOT NULL,
    district VARCHAR(100) NOT NULL,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    provenance VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uq_district_state_name ON krishi.district_coordinates (LOWER(state), LOWER(district));

-- 2. Weather Forecast Cache (Spatial Grid Normalized)
CREATE TABLE krishi.weather_forecast_cache (
    id UUID PRIMARY KEY,
    grid_key VARCHAR(64) NOT NULL UNIQUE,
    normalized_latitude DOUBLE PRECISION NOT NULL,
    normalized_longitude DOUBLE PRECISION NOT NULL,
    temperature_celsius DOUBLE PRECISION,
    relative_humidity INTEGER,
    precipitation_probability INTEGER,
    precipitation_mm DOUBLE PRECISION,
    wind_speed_kmh DOUBLE PRECISION,
    wind_gusts_kmh DOUBLE PRECISION,
    weather_condition VARCHAR(100),
    weather_code INTEGER,
    forecast_payload JSONB NOT NULL,
    fetched_at TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_weather_cache_expires_at ON krishi.weather_forecast_cache(expires_at);
CREATE INDEX idx_weather_cache_grid_key ON krishi.weather_forecast_cache(grid_key);

-- 3. Weather Advisories (Contextual, Idempotent, Task-Aware)
CREATE TABLE krishi.weather_advisories (
    id UUID PRIMARY KEY,
    advisory_fingerprint VARCHAR(160) NOT NULL,
    farm_id UUID NOT NULL REFERENCES krishi.farms(id) ON DELETE CASCADE,
    crop_planting_id UUID REFERENCES krishi.crop_plantings(id) ON DELETE CASCADE,
    task_id UUID REFERENCES krishi.tasks(id) ON DELETE SET NULL,
    rule_code VARCHAR(64) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    title VARCHAR(200) NOT NULL,
    message TEXT NOT NULL,
    recommended_action TEXT NOT NULL,
    target_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_evaluated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    acknowledged_at TIMESTAMP WITH TIME ZONE
);

CREATE UNIQUE INDEX uq_active_weather_advisory ON krishi.weather_advisories (advisory_fingerprint) WHERE status = 'ACTIVE';
CREATE INDEX idx_weather_advisories_farm_status ON krishi.weather_advisories(farm_id, status);
CREATE INDEX idx_weather_advisories_task ON krishi.weather_advisories(task_id) WHERE task_id IS NOT NULL;
CREATE INDEX idx_weather_advisories_target_date ON krishi.weather_advisories(target_date);

-- 4. Seed Verified Kerala District Headquarters Coordinates
-- Provenance: Official District Headquarters Coordinates, Government of Kerala / NIC District Portals & Census of India
INSERT INTO krishi.district_coordinates (id, state, district, latitude, longitude, provenance) VALUES
    ('d0000001-0000-0000-0000-000000000001', 'Kerala', 'Thiruvananthapuram', 8.524100, 76.936600, 'Census of India 2011 / NIC District Collectorate Portal'),
    ('d0000001-0000-0000-0000-000000000002', 'Kerala', 'Kollam', 8.893200, 76.614100, 'Census of India 2011 / NIC District Collectorate Portal'),
    ('d0000001-0000-0000-0000-000000000003', 'Kerala', 'Pathanamthitta', 9.264800, 76.787000, 'Census of India 2011 / NIC District Collectorate Portal'),
    ('d0000001-0000-0000-0000-000000000004', 'Kerala', 'Alappuzha', 9.498100, 76.338800, 'Census of India 2011 / NIC District Collectorate Portal'),
    ('d0000001-0000-0000-0000-000000000005', 'Kerala', 'Kottayam', 9.591600, 76.522200, 'Census of India 2011 / NIC District Collectorate Portal'),
    ('d0000001-0000-0000-0000-000000000006', 'Kerala', 'Idukki', 9.849400, 76.971000, 'Census of India 2011 / NIC District Collectorate Portal (Painavu)'),
    ('d0000001-0000-0000-0000-000000000007', 'Kerala', 'Ernakulam', 9.981600, 76.299900, 'Census of India 2011 / NIC District Collectorate Portal (Kochi/Kakkanad)'),
    ('d0000001-0000-0000-0000-000000000008', 'Kerala', 'Thrissur', 10.527600, 76.214400, 'Census of India 2011 / NIC District Collectorate Portal (Ayyanthole)'),
    ('d0000001-0000-0000-0000-000000000009', 'Kerala', 'Palakkad', 10.786700, 76.654800, 'Census of India 2011 / NIC District Collectorate Portal (Civil Station)'),
    ('d0000001-0000-0000-0000-000000000010', 'Kerala', 'Malappuram', 11.051000, 76.071100, 'Census of India 2011 / NIC District Collectorate Portal (Civil Station)'),
    ('d0000001-0000-0000-0000-000000000011', 'Kerala', 'Kozhikode', 11.258800, 75.780400, 'Census of India 2011 / NIC District Collectorate Portal (Civil Station)'),
    ('d0000001-0000-0000-0000-000000000012', 'Kerala', 'Wayanad', 11.685400, 76.132000, 'Census of India 2011 / NIC District Collectorate Portal (Kalpetta)'),
    ('d0000001-0000-0000-0000-000000000013', 'Kerala', 'Kannur', 11.874500, 75.370400, 'Census of India 2011 / NIC District Collectorate Portal (Collectorate)'),
    ('d0000001-0000-0000-0000-000000000014', 'Kerala', 'Kasaragod', 12.510200, 74.985200, 'Census of India 2011 / NIC District Collectorate Portal (Vidyanagar)');
