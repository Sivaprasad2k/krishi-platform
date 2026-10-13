package com.krishi.backend.weather.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.krishi.backend.weather.dto.DailyForecastItem;
import com.krishi.backend.weather.dto.HourlyForecastItem;
import com.krishi.backend.weather.dto.WeatherObservation;
import com.krishi.backend.weather.service.WeatherNormalizationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class OpenMeteoProviderClient implements WeatherDataProvider {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final WeatherNormalizationService normalizationService;
    private final String baseUrl;
    private final String apiKey;

    public OpenMeteoProviderClient(
            ObjectMapper objectMapper,
            WeatherNormalizationService normalizationService,
            @Value("${weather.open-meteo.base-url:https://api.open-meteo.com/v1/forecast}") String baseUrl,
            @Value("${weather.open-meteo.api-key:}") String apiKey
    ) {
        this.objectMapper = objectMapper;
        this.normalizationService = normalizationService;
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(3));
        requestFactory.setReadTimeout(Duration.ofSeconds(5));

        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public String getProviderName() {
        return "OPEN_METEO";
    }

    @Override
    public String getAttribution() {
        return "Weather data by Open-Meteo.com (CC BY 4.0)";
    }

    @Override
    public String getAttributionUrl() {
        return "https://open-meteo.com/";
    }

    @Override
    public ProviderForecastResult fetchForecast(double latitude, double longitude) {
        try {
            log.info("Fetching forecast from Open-Meteo for coordinates [{}, {}]", latitude, longitude);

            String uri = buildUri(latitude, longitude);

            String rawJson = restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(String.class);

            if (rawJson == null || rawJson.trim().isEmpty()) {
                throw new WeatherProviderException("Received empty response from Open-Meteo API");
            }

            JsonNode root = objectMapper.readTree(rawJson);
            OffsetDateTime now = OffsetDateTime.now();

            WeatherObservation current = parseCurrent(root, now);
            List<DailyForecastItem> daily = parseDaily(root);
            List<HourlyForecastItem> hourly = parseHourly(root);

            return ProviderForecastResult.builder()
                    .current(current)
                    .daily(daily)
                    .hourly(hourly)
                    .rawPayloadJson(rawJson)
                    .fetchedAt(now)
                    .build();

        } catch (WeatherProviderException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Failed to fetch weather forecast from Open-Meteo: {}", e.getMessage());
            throw new WeatherProviderException("Weather provider request failed: " + e.getMessage(), e);
        }
    }

    private String buildUri(double latitude, double longitude) {
        StringBuilder sb = new StringBuilder(baseUrl);
        sb.append("?latitude=").append(latitude)
                .append("&longitude=").append(longitude)
                .append("&current=temperature_2m,relative_humidity_2m,precipitation,weather_code,wind_speed_10m,wind_gusts_10m")
                .append("&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_sum,precipitation_probability_max,wind_speed_10m_max,wind_gusts_10m_max")
                .append("&hourly=temperature_2m,precipitation_probability,precipitation,wind_speed_10m,weather_code")
                .append("&timezone=auto")
                .append("&forecast_days=7");

        if (apiKey != null && !apiKey.trim().isEmpty()) {
            sb.append("&apikey=").append(apiKey.trim());
        }

        return sb.toString();
    }

    private WeatherObservation parseCurrent(JsonNode root, OffsetDateTime now) {
        JsonNode cur = root.path("current");
        if (cur.isMissingNode() || cur.isNull()) {
            return WeatherObservation.builder()
                    .observedAt(now)
                    .weatherCondition("Unknown")
                    .build();
        }

        Integer weatherCode = cur.hasNonNull("weather_code") ? cur.get("weather_code").asInt() : null;
        String condition = normalizationService.decodeWmoWeatherCode(weatherCode);

        return WeatherObservation.builder()
                .temperatureCelsius(cur.hasNonNull("temperature_2m") ? cur.get("temperature_2m").asDouble() : null)
                .relativeHumidity(cur.hasNonNull("relative_humidity_2m") ? cur.get("relative_humidity_2m").asInt() : null)
                .precipitationMm(cur.hasNonNull("precipitation") ? cur.get("precipitation").asDouble() : null)
                .windSpeedKmh(cur.hasNonNull("wind_speed_10m") ? cur.get("wind_speed_10m").asDouble() : null)
                .windGustsKmh(cur.hasNonNull("wind_gusts_10m") ? cur.get("wind_gusts_10m").asDouble() : null)
                .weatherCode(weatherCode)
                .weatherCondition(condition)
                .observedAt(now)
                .build();
    }

    private List<DailyForecastItem> parseDaily(JsonNode root) {
        List<DailyForecastItem> dailyList = new ArrayList<>();
        JsonNode daily = root.path("daily");
        if (daily.isMissingNode() || !daily.has("time")) {
            return dailyList;
        }

        JsonNode times = daily.get("time");
        JsonNode maxTemps = daily.path("temperature_2m_max");
        JsonNode minTemps = daily.path("temperature_2m_min");
        JsonNode precipSums = daily.path("precipitation_sum");
        JsonNode precipProbs = daily.path("precipitation_probability_max");
        JsonNode maxWinds = daily.path("wind_speed_10m_max");
        JsonNode maxGusts = daily.path("wind_gusts_10m_max");
        JsonNode codes = daily.path("weather_code");

        int count = times.size();
        for (int i = 0; i < count; i++) {
            LocalDate date = LocalDate.parse(times.get(i).asText());
            Integer code = (codes.isArray() && codes.size() > i && !codes.get(i).isNull()) ? codes.get(i).asInt() : null;

            dailyList.add(DailyForecastItem.builder()
                    .date(date)
                    .maxTempCelsius((maxTemps.isArray() && maxTemps.size() > i && !maxTemps.get(i).isNull()) ? maxTemps.get(i).asDouble() : null)
                    .minTempCelsius((minTemps.isArray() && minTemps.size() > i && !minTemps.get(i).isNull()) ? minTemps.get(i).asDouble() : null)
                    .precipitationMm((precipSums.isArray() && precipSums.size() > i && !precipSums.get(i).isNull()) ? precipSums.get(i).asDouble() : null)
                    .precipitationProbabilityMax((precipProbs.isArray() && precipProbs.size() > i && !precipProbs.get(i).isNull()) ? precipProbs.get(i).asInt() : null)
                    .maxWindSpeedKmh((maxWinds.isArray() && maxWinds.size() > i && !maxWinds.get(i).isNull()) ? maxWinds.get(i).asDouble() : null)
                    .maxWindGustsKmh((maxGusts.isArray() && maxGusts.size() > i && !maxGusts.get(i).isNull()) ? maxGusts.get(i).asDouble() : null)
                    .weatherCode(code)
                    .weatherCondition(normalizationService.decodeWmoWeatherCode(code))
                    .build());
        }

        return dailyList;
    }

    private List<HourlyForecastItem> parseHourly(JsonNode root) {
        List<HourlyForecastItem> hourlyList = new ArrayList<>();
        JsonNode hourly = root.path("hourly");
        if (hourly.isMissingNode() || !hourly.has("time")) {
            return hourlyList;
        }

        JsonNode times = hourly.get("time");
        JsonNode temps = hourly.path("temperature_2m");
        JsonNode precips = hourly.path("precipitation");
        JsonNode probs = hourly.path("precipitation_probability");
        JsonNode winds = hourly.path("wind_speed_10m");
        JsonNode codes = hourly.path("weather_code");

        // Parse up to 24 hours for responsiveness
        int limit = Math.min(times.size(), 24);
        for (int i = 0; i < limit; i++) {
            String timeStr = times.get(i).asText();
            OffsetDateTime time;
            try {
                time = OffsetDateTime.parse(timeStr);
            } catch (Exception e) {
                // If timezone offset missing, parse as LocalDateTime and assume system/UTC
                time = java.time.LocalDateTime.parse(timeStr).atOffset(java.time.ZoneOffset.UTC);
            }

            Integer code = (codes.isArray() && codes.size() > i && !codes.get(i).isNull()) ? codes.get(i).asInt() : null;

            hourlyList.add(HourlyForecastItem.builder()
                    .time(time)
                    .temperatureCelsius((temps.isArray() && temps.size() > i && !temps.get(i).isNull()) ? temps.get(i).asDouble() : null)
                    .precipitationMm((precips.isArray() && precips.size() > i && !precips.get(i).isNull()) ? precips.get(i).asDouble() : null)
                    .precipitationProbability((probs.isArray() && probs.size() > i && !probs.get(i).isNull()) ? probs.get(i).asInt() : null)
                    .windSpeedKmh((winds.isArray() && winds.size() > i && !winds.get(i).isNull()) ? winds.get(i).asDouble() : null)
                    .weatherCode(code)
                    .build());
        }

        return hourlyList;
    }
}
