# Krishi Platform — Weather Integration & Contextual Advisories Specification

**Document Version:** 1.0.0  
**Status:** Approved Integration Specification  
**Component:** Weather Intelligence Engine (`com.krishi.backend.weather`)

---

## 1. Provider Integration Architecture

### 1.1 Provider Selection & API Contract
Krishi integrates with the **Open-Meteo Weather Forecast API** (`https://api.open-meteo.com/v1/forecast`) via Spring Boot 4's `RestClient` abstraction.

- **Endpoint:** `GET https://api.open-meteo.com/v1/forecast`
- **Request Parameters:**
  - `latitude`, `longitude`: Decimal coordinates (normalized to 2 decimal places).
  - `current`: `temperature_2m,relative_humidity_2m,precipitation,weather_code,wind_speed_10m`
  - `daily`: `temperature_2m_max,temperature_2m_min,precipitation_sum,precipitation_probability_max,wind_speed_10m_max,weather_code`
  - `hourly`: `temperature_2m,relative_humidity_2m,precipitation_probability,precipitation,wind_speed_10m,weather_code`
  - `timezone`: `auto`
  - `forecast_days`: `7`
- **Client Configuration:**
  - Connect Timeout: 3,000 ms
  - Read Timeout: 5,000 ms
  - Factory: `JdkClientHttpRequestFactory`

### 1.2 Terms of Service, Licensing & Attribution
- **Licence:** Open-Meteo data is licensed under the **Creative Commons Attribution 4.0 International (CC BY 4.0)** license.
- **Attribution Obligation:** Any user-facing interface displaying weather forecast data derived from Open-Meteo must clearly display attribution with a direct hyperlink to `https://open-meteo.com/`.
  - Frontend implementation: Rendered prominently in `WeatherSummaryWidget.jsx` footer:
    > "Weather data provided by [Open-Meteo](https://open-meteo.com/) (CC BY 4.0)"
- **Usage Limits:** The free non-commercial tier permits up to **10,000 API calls per day** and a rate limit of **5,000 calls per hour**.
- **Credential Safety:** The Open-Meteo standard tier requires zero API keys. No credentials, tokens, or private secrets exist in client-side code, logs, or repositories.

---

## 2. Spatial Resolution, Normalization & Caching Policy

### 2.1 Spatial Resolution
Coordinates are normalized using `BigDecimal.setScale(2, RoundingMode.HALF_UP)`:
- Latitude: $0.01^\circ \approx 1.11\text{ km}$
- Longitude (at $10^\circ\text{N}$, Kerala): $0.01^\circ \times \cos(10^\circ) \times 111.32\text{ km} \approx 1.09\text{ km}$

**Agronomic & Meteorological Rationale:**
- Numerical Weather Prediction (NWP) models backing Open-Meteo operate on discrete grids: ECMWF IFS ($\approx 9\text{ km}$), GFS ($\approx 13-28\text{ km}$), ICON ($\approx 13\text{ km}$). Querying fractional coordinates at micro-meter precision provides false precision and causes cache fragmentation.
- Rounding to 2 decimal places ($\approx 1.1\text{ km}$) ensures farms within the same village or panchayat share cached forecast data, keeping daily API calls well below the 10,000 limit.

**Limitations:**
- In high-relief mountainous topography (e.g., Western Ghats in Idukki and Wayanad), rapid elevation gradients within a 1.1 km horizontal radius may experience temperature differences (lapse rate $\approx 6.5^\circ\text{C}/\text{km}$ elevation) or localized rain-shadow effects not captured by standard grid interpolation.

### 2.2 Cache Lifecycle & Stale Data Fallback
- **Cache Storage:** `krishi.weather_forecast_cache` table keyed by `spatial_grid_key` (`{lat_2dec}_{lon_2dec}`).
- **Freshness TTL:** 2 hours (`expires_at = fetched_at + INTERVAL '2 hours'`).
- **Manual Refresh Cooldown:** 15 minutes (`cooldown_until = fetched_at + INTERVAL '15 minutes'`). Requests within the cooldown window return the cached forecast.
- **Stale Fallback:** When Open-Meteo encounters upstream errors, timeouts, or network disconnection, the system retrieves the most recent expired cache entry and sets `stale = true`. The UI alerts the farmer that weather data is cached and may not reflect real-time conditions.

---

## 3. Location Resolution & Centroid Fallback

### 3.1 Location Resolution Pipeline
1. **Direct GPS Coordinates:** If the farm has valid `latitude` and `longitude`, coordinates are normalized and queried directly (`coordinate_source = GPS`).
2. **Verified District Headquarters Centroid:** If GPS coordinates are null, the farm's `district` (and optional `state`) is matched case-insensitively against `krishi.district_coordinates` (`coordinate_source = DISTRICT_CENTROID`).
3. **Location Unavailable:** If neither GPS coordinates nor a verified district centroid are found, the system throws `LocationUnavailableException` returning **HTTP 422 Unprocessable Entity** with error code `LOCATION_UNAVAILABLE`. Coordinates are **never** fabricated.

### 3.2 Verified Kerala District Headquarters Reference Coordinates
Seeded in Flyway migration `V21` with provenance from the **Census of India 2011 / Government of Kerala District Collectorate Portals**.
> [!NOTE]
> These coordinates represent the administrative **District Headquarters / Collectorate / Civil Station** reference points, not the geometric center (centroid polygon) of the district. Administrative headquarters provide the civic baseline used by IMD and state disaster management authorities (KSDMA) for district-level weather advisories.

| District | Latitude | Longitude | Reference Location / Provenance |
| :--- | :--- | :--- | :--- |
| Thiruvananthapuram | 8.5241 | 76.9366 | District Collectorate, Kudappanakunnu (Census 2011 / NIC) |
| Kollam | 8.8932 | 76.6141 | District Collectorate, Civil Station, Kollam (Census 2011 / NIC) |
| Pathanamthitta | 9.2648 | 76.7870 | District Collectorate, Pathanamthitta (Census 2011 / NIC) |
| Alappuzha | 9.4981 | 76.3388 | District Collectorate, Civil Station, Alappuzha (Census 2011 / NIC) |
| Kottayam | 9.5916 | 76.5222 | District Collectorate, Kottayam (Census 2011 / NIC) |
| Idukki | 9.8494 | 76.9710 | District Headquarters, Painavu (Census 2011 / NIC) |
| Ernakulam | 9.9816 | 76.2999 | District Collectorate, Kakkanad (Census 2011 / NIC) |
| Thrissur | 10.5276 | 76.2144 | District Collectorate, Ayyanthole (Census 2011 / NIC) |
| Palakkad | 10.7867 | 76.6548 | District Collectorate, Civil Station, Palakkad (Census 2011 / NIC) |
| Malappuram | 11.0510 | 76.0711 | District Collectorate, Civil Station, Malappuram (Census 2011 / NIC) |
| Kozhikode | 11.2588 | 75.7804 | District Collectorate, Civil Station, Kozhikode (Census 2011 / NIC) |
| Wayanad | 11.6854 | 76.1320 | District Headquarters, Kalpetta (Census 2011 / NIC) |
| Kannur | 11.8745 | 75.3704 | District Collectorate, Civil Station, Kannur (Census 2011 / NIC) |
| Kasaragod | 12.5102 | 74.9852 | District Collectorate, Vidyanagar, Kasaragod (Census 2011 / NIC) |

---

## 4. Contextual Operational Advisory Rules

The advisory engine evaluates operational risks without modifying task states or schedules.

### 4.1 Validated V1 Rules

#### Rule 1: Heavy Rain Warning for Scheduled Irrigation (`HEAVY_RAIN_IRRIGATION`)
- **Applicability:** Farm has a scheduled task matching irrigation operations (`IRRIGATION`, "watering", "submergence") on forecast date $T$.
- **Agronomic Threshold:**
  - `MEDIUM` Severity: Forecast precipitation $\ge 20.0\text{ mm}$ OR precipitation probability $\ge 80\%$.
  - `HIGH` Severity: Forecast precipitation $\ge 50.0\text{ mm}$.
- **Agronomic Evidence & Source:** IMD Agromet Advisory Service (AAS) & ICAR Rice Knowledge Management Portal guidelines. Daily precipitation $\ge 20\text{ mm}$ satisfies multiple days of crop evapotranspiration ($3.5-5.5\text{ mm/day}$ in humid tropics), making artificial irrigation redundant. Irrigation during heavy rainfall triggers root zone waterlogging/hypoxia in upland crops and nutrient runoff in flooded rice.
- **Recommended Action:** "Withhold scheduled irrigation until soil moisture dries down. Inspect drainage bunds to prevent ponding."
- **Known Limitations:** Evaluated without real-time in-situ soil moisture sensor data or soil texture differentiation (heavy clay vs sandy loam). Assumes farmer can defer watering.

#### Rule 2: High Wind Warning for Spraying / Application (`HIGH_WIND_SPRAYING`)
- **Applicability:** Farm has a scheduled task matching chemical or fertilizer application (`FERTILIZER_APPLICATION`, `DISEASE_TREATMENT`, `PEST_CONTROL`, "spray", "top-dressing") on forecast date $T$.
- **Agronomic Threshold:**
  - `MEDIUM` Severity: Sustained wind speed $> 20.0\text{ km/h}$ OR max gusts $> 30.0\text{ km/h}$.
  - `HIGH` Severity: Sustained wind speed $> 35.0\text{ km/h}$ OR max gusts $> 45.0\text{ km/h}$.
- **Agronomic Evidence & Source:** FAO Guidelines on Good Practice for Ground Application of Pesticides (FAO AGP: 1998/M/2) and Central Insecticides Board & Registration Committee (CIBRC) standards. Wind speeds above $15-20\text{ km/h}$ cause severe droplet drift, uneven deposit, operator exposure, and rapid pesticide wastage.
- **Recommended Action:** "Postpone spraying or top-dressing until wind speeds subside below 15 km/h, preferably during calm early morning hours."
- **Known Limitations:** Does not distinguish coarse anti-drift nozzles from fine mist nozzles, nor tree-crop spraying (coconut/arecanut) from ground canopy crops.

#### Rule 3: Extreme Heat Alert (`EXTREME_HEAT_ALERT`)
- **Applicability:** Farm has at least one active `CropPlanting` on forecast date $T$.
- **Agronomic Threshold:**
  - `MEDIUM` Severity: Daily maximum temperature $\ge 40.0^\circ\text{C}$.
  - `HIGH` Severity: Daily maximum temperature $\ge 43.0^\circ\text{C}$.
- **Agronomic Evidence & Source:** IMD Heatwave Criteria for Indian Peninsular Plains ($T_{max} \ge 40^\circ\text{C}$) and ICAR-National Institute of Abiotic Stress Management (NIASM) guidelines. High heat above $40^\circ\text{C}$ sharply accelerates crop evapotranspiration, induces floret sterility during anthesis in rice (*Oryza sativa*), and causes flower abortion in solanaceous vegetables (*Solanum lycopersicum*).
- **Recommended Action:** "Maintain light standing water or surface mulch. Avoid transplanting, weeding, or heavy spraying during peak afternoon heat."
- **Known Limitations:** Uses an ambient macro-temperature threshold. Sensitive crops (tomato, cowpea) may experience pollen stress at $36-38^\circ\text{C}$, while C4 crops (sorghum, maize) tolerate $>40^\circ\text{C}$. Does not dynamically adjust for growth stage.

### 4.2 Explicitly Deferred Agronomic Rules
- **Fungal Disease Microclimate Model (Rice Blast / Late Blight):** Fungal infection prediction based on macro-forecast relative humidity ($>90\%$) and temperature is disabled/deferred. Validated epidemiological models require canopy-level temperature, microclimate leaf wetness duration, and continuous degree-hour tracking. Macro NWP forecasts cannot substitute for canopy sensors without high false-positive rates.
- **Frost / Cold Shock Model:** Frost prediction is disabled as agricultural lowlands across Kerala do not experience sub-zero freezing outside high-altitude tea plantations in Munnar/Idukki.

---

## 5. Advisory Identity, Deduplication & Concurrency Control

### 5.1 Advisory Identity Fingerprint
To allow multiple crop plantings and distinct field tasks to receive advisories without colliding, the composite fingerprint is defined as:
```
fingerprint = advisory_type + ":" + farm_id + ":" +
              (crop_planting_id != null ? crop_planting_id : "ALL") + ":" +
              (task_id != null ? task_id : "NONE") + ":" +
              advisory_date
```

### 5.2 Database Deduplication Invariant
Flyway migration `V21` enforces a PostgreSQL partial unique index:
```sql
CREATE UNIQUE INDEX uq_active_weather_advisory
    ON krishi.weather_advisories (deduplication_fingerprint)
    WHERE status = 'ACTIVE';
```

- **De-duplication:** If an active advisory exists with identical severity, it is preserved without duplicate notifications.
- **Escalation:** If severity escalates (e.g., `ADVISORY` to `WARNING`), the existing active advisory is transitioned to `SUPERSEDED` using `saveAndFlush()` before persisting the new `ACTIVE` advisory, preventing Hibernate action queue unique index conflicts.
- **Auto-Expiry:** Advisories for past dates are automatically transitioned to `EXPIRED`.
- **Farmer Acknowledgment:** Farmer can acknowledge an active advisory (`status = 'ACKNOWLEDGED'`).

### 5.3 Scheduler Advisory Locking
Scheduled forecast synchronization runs via `WeatherSyncScheduler`:
- **Lock Acquisition:** Pinned to a single dedicated physical JDBC `Connection` from `DataSource`.
- **Advisory Lock Key:** `pg_try_advisory_lock(84729103)`
- **Safety Guarantee:** Lock acquisition and release execute within the identical physical database session in a `try-with-resources` block with `finally { pg_advisory_unlock(...) }`. Prevents session lock leakage across HikariCP connection pools.

---

## 6. Farmer Sovereignty Invariant
Advisories provide actionable guidance. Under no circumstances does the weather intelligence system:
1. Automatically change task statuses (`PENDING`, `IN_PROGRESS`, `COMPLETED`).
2. Automatically alter task due dates or reschedule operations.
3. Automatically mark tasks as skipped or cancelled.

Farmers retain full operational authority over all farm decisions.
