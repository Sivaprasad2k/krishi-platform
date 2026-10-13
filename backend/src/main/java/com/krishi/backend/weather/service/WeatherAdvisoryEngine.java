package com.krishi.backend.weather.service;

import com.krishi.backend.cropcycle.entity.CropCycleStatus;
import com.krishi.backend.cropplanting.entity.CropPlanting;
import com.krishi.backend.cropplanting.repository.CropPlantingRepository;
import com.krishi.backend.farm.entity.Farm;
import com.krishi.backend.notification.entity.NotificationType;
import com.krishi.backend.notification.service.NotificationService;
import com.krishi.backend.task.entity.Task;
import com.krishi.backend.task.entity.TaskStatus;
import com.krishi.backend.task.repository.TaskRepository;
import com.krishi.backend.weather.dto.DailyForecastItem;
import com.krishi.backend.weather.entity.AdvisorySeverity;
import com.krishi.backend.weather.entity.AdvisoryStatus;
import com.krishi.backend.weather.entity.WeatherAdvisory;
import com.krishi.backend.weather.repository.WeatherAdvisoryRepository;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WeatherAdvisoryEngine {

    public static final String RULE_HEAVY_RAIN_IRRIGATION = "HEAVY_RAIN_IRRIGATION";
    public static final String RULE_HIGH_WIND_SPRAYING = "HIGH_WIND_SPRAYING";
    public static final String RULE_EXTREME_HEAT_ALERT = "EXTREME_HEAT_ALERT";

    private final CropPlantingRepository cropPlantingRepository;
    private final TaskRepository taskRepository;
    private final WeatherAdvisoryRepository weatherAdvisoryRepository;
    private final NotificationService notificationService;

    @Transactional
    public List<WeatherAdvisory> evaluateAdvisories(Farm farm, List<DailyForecastItem> dailyForecasts) {
        if (dailyForecasts == null || dailyForecasts.isEmpty()) {
            return Collections.emptyList();
        }

        Map<LocalDate, DailyForecastItem> forecastByDate = dailyForecasts.stream()
                .filter(d -> d.getDate() != null)
                .collect(Collectors.toMap(DailyForecastItem::getDate, d -> d, (a, b) -> a));

        List<CropPlanting> activePlantings = cropPlantingRepository.findByCropCycleFarmIdAndCropCycleStatus(
                farm.getId(), CropCycleStatus.ACTIVE
        );

        List<Task> activeTasks = taskRepository.findByFarmIdAndStatusIn(
                farm.getId(), List.of(TaskStatus.PENDING, TaskStatus.IN_PROGRESS, TaskStatus.OVERDUE)
        );

        List<AdvisoryProposal> proposals = new ArrayList<>();

        // 1. Evaluate HEAVY_RAIN_IRRIGATION
        evaluateHeavyRainIrrigation(farm, activeTasks, forecastByDate, proposals);

        // 2. Evaluate HIGH_WIND_SPRAYING
        evaluateHighWindSpraying(farm, activeTasks, forecastByDate, proposals);

        // 3. Evaluate EXTREME_HEAT_ALERT
        evaluateExtremeHeatAlert(farm, activePlantings, dailyForecasts, proposals);

        // Process proposals idempotently with deduplication
        List<WeatherAdvisory> processedAdvisories = new ArrayList<>();
        for (AdvisoryProposal proposal : proposals) {
            WeatherAdvisory advisory = processProposal(farm, proposal);
            if (advisory != null) {
                processedAdvisories.add(advisory);
            }
        }

        // Expire past advisories
        expirePastAdvisories(farm.getId());

        return processedAdvisories;
    }

    private void evaluateHeavyRainIrrigation(
            Farm farm,
            List<Task> activeTasks,
            Map<LocalDate, DailyForecastItem> forecastByDate,
            List<AdvisoryProposal> proposals
    ) {
        for (Task task : activeTasks) {
            if (!isIrrigationTask(task)) {
                continue;
            }

            LocalDate targetDate = task.getDueDate() != null ? task.getDueDate().toLocalDate() : LocalDate.now();
            DailyForecastItem forecast = forecastByDate.get(targetDate);
            if (forecast == null) {
                continue;
            }

            double precipMm = forecast.getPrecipitationMm() != null ? forecast.getPrecipitationMm() : 0.0;
            int precipProb = forecast.getPrecipitationProbabilityMax() != null ? forecast.getPrecipitationProbabilityMax() : 0;

            if (precipMm >= 20.0 || precipProb >= 80) {
                AdvisorySeverity severity = (precipMm >= 50.0) ? AdvisorySeverity.HIGH : AdvisorySeverity.MEDIUM;
                String cropName = getCropName(task);
                UUID plantingId = task.getCropPlanting() != null ? task.getCropPlanting().getId() : null;

                String fingerprint = buildFingerprint(farm.getId(), plantingId, task.getId(), RULE_HEAVY_RAIN_IRRIGATION, targetDate);

                proposals.add(AdvisoryProposal.builder()
                        .fingerprint(fingerprint)
                        .ruleCode(RULE_HEAVY_RAIN_IRRIGATION)
                        .severity(severity)
                        .title("Heavy Rain Forecast — Review Irrigation")
                        .message(String.format("Heavy rainfall (%.1f mm, %d%% probability) is forecast on %s for %s. Suspend artificial irrigation to prevent waterlogging and root asphyxiation.", precipMm, precipProb, targetDate, cropName))
                        .recommendedAction("Withhold scheduled irrigation until soil moisture dries down. Inspect drainage bunds to prevent ponding.")
                        .targetDate(targetDate)
                        .cropPlanting(task.getCropPlanting())
                        .task(task)
                        .build());
            }
        }
    }

    private void evaluateHighWindSpraying(
            Farm farm,
            List<Task> activeTasks,
            Map<LocalDate, DailyForecastItem> forecastByDate,
            List<AdvisoryProposal> proposals
    ) {
        for (Task task : activeTasks) {
            if (!isSprayingOrTreatmentTask(task)) {
                continue;
            }

            LocalDate targetDate = task.getDueDate() != null ? task.getDueDate().toLocalDate() : LocalDate.now();
            DailyForecastItem forecast = forecastByDate.get(targetDate);
            if (forecast == null) {
                continue;
            }

            double maxWind = forecast.getMaxWindSpeedKmh() != null ? forecast.getMaxWindSpeedKmh() : 0.0;
            double maxGust = forecast.getMaxWindGustsKmh() != null ? forecast.getMaxWindGustsKmh() : 0.0;

            if (maxWind > 20.0 || maxGust > 30.0) {
                AdvisorySeverity severity = (maxWind > 35.0 || maxGust > 45.0) ? AdvisorySeverity.HIGH : AdvisorySeverity.MEDIUM;
                String cropName = getCropName(task);
                UUID plantingId = task.getCropPlanting() != null ? task.getCropPlanting().getId() : null;

                String fingerprint = buildFingerprint(farm.getId(), plantingId, task.getId(), RULE_HIGH_WIND_SPRAYING, targetDate);

                proposals.add(AdvisoryProposal.builder()
                        .fingerprint(fingerprint)
                        .ruleCode(RULE_HIGH_WIND_SPRAYING)
                        .severity(severity)
                        .title("High Wind Warning — Chemical Spraying Risk")
                        .message(String.format("High winds (sustained %.1f km/h, gusts up to %.1f km/h) are expected on %s for %s. Chemical spraying in windy conditions causes severe drift, wastage, and inhalation risk.", maxWind, maxGust, targetDate, cropName))
                        .recommendedAction("Postpone spraying or top-dressing until wind speeds subside below 15 km/h, preferably during calm early morning hours.")
                        .targetDate(targetDate)
                        .cropPlanting(task.getCropPlanting())
                        .task(task)
                        .build());
            }
        }
    }

    private void evaluateExtremeHeatAlert(
            Farm farm,
            List<CropPlanting> activePlantings,
            List<DailyForecastItem> dailyForecasts,
            List<AdvisoryProposal> proposals
    ) {
        for (CropPlanting planting : activePlantings) {
            for (DailyForecastItem day : dailyForecasts) {
                if (day.getMaxTempCelsius() != null && day.getMaxTempCelsius() >= 40.0 && day.getDate() != null) {
                    double maxTemp = day.getMaxTempCelsius();
                    LocalDate targetDate = day.getDate();
                    AdvisorySeverity severity = (maxTemp >= 43.0) ? AdvisorySeverity.HIGH : AdvisorySeverity.MEDIUM;
                    String cropName = planting.getCropName();

                    String fingerprint = buildFingerprint(farm.getId(), planting.getId(), null, RULE_EXTREME_HEAT_ALERT, targetDate);

                    proposals.add(AdvisoryProposal.builder()
                            .fingerprint(fingerprint)
                            .ruleCode(RULE_EXTREME_HEAT_ALERT)
                            .severity(severity)
                            .title("Extreme Heat Alert — Crop Heat Stress")
                            .message(String.format("Extreme high temperature of %.1f°C is forecast on %s for %s. Heat above 40°C increases evapotranspiration and risks pollen sterility.", maxTemp, targetDate, cropName))
                            .recommendedAction("Maintain light standing water or surface mulch. Avoid transplanting, weeding, or heavy spraying during peak afternoon heat.")
                            .targetDate(targetDate)
                            .cropPlanting(planting)
                            .task(null)
                            .build());
                }
            }
        }
    }

    private WeatherAdvisory processProposal(Farm farm, AdvisoryProposal proposal) {
        Optional<WeatherAdvisory> activeOpt = weatherAdvisoryRepository.findByAdvisoryFingerprintAndStatus(
                proposal.getFingerprint(), AdvisoryStatus.ACTIVE
        );

        if (activeOpt.isPresent()) {
            WeatherAdvisory existing = activeOpt.get();

            // Check if severity escalated
            if (proposal.getSeverity().ordinal() > existing.getSeverity().ordinal()) {
                log.info("Advisory [{}] escalated from {} to {}. Superseding old advisory.",
                        proposal.getFingerprint(), existing.getSeverity(), proposal.getSeverity());

                existing.setStatus(AdvisoryStatus.SUPERSEDED);
                weatherAdvisoryRepository.saveAndFlush(existing);

                WeatherAdvisory escalated = createAndSaveAdvisory(farm, proposal);

                // Dispatch notification for escalated advisory
                try {
                    notificationService.createNotification(
                            farm.getOwner().getId(),
                            "[ESCALATION] " + proposal.getTitle(),
                            proposal.getMessage(),
                            NotificationType.WEATHER_ADVISORY
                    );
                } catch (Exception notifEx) {
                    log.warn("Failed to dispatch notification for escalated advisory [{}]: {}",
                            proposal.getFingerprint(), notifEx.getMessage());
                }

                return escalated;
            } else {
                // Condition unchanged or lower: update lastEvaluatedAt without re-notifying
                existing.setLastEvaluatedAt(OffsetDateTime.now());
                return weatherAdvisoryRepository.save(existing);
            }
        }

        // Check if previously acknowledged with equal or higher severity
        Optional<WeatherAdvisory> latestOpt = weatherAdvisoryRepository.findFirstByAdvisoryFingerprintOrderByCreatedAtDesc(
                proposal.getFingerprint()
        );

        if (latestOpt.isPresent()) {
            WeatherAdvisory latest = latestOpt.get();
            if (latest.getStatus() == AdvisoryStatus.ACKNOWLEDGED
                    && latest.getSeverity().ordinal() >= proposal.getSeverity().ordinal()) {
                // Farmer already acknowledged this warning for this target date. Do not harass.
                return latest;
            }
        }

        // New active advisory
        WeatherAdvisory newAdvisory = createAndSaveAdvisory(farm, proposal);

        // Dispatch notification
        try {
            notificationService.createNotification(
                    farm.getOwner().getId(),
                    proposal.getTitle(),
                    proposal.getMessage(),
                    NotificationType.WEATHER_ADVISORY
            );
        } catch (Exception notifEx) {
            log.warn("Failed to dispatch notification for advisory [{}]: {}",
                    proposal.getFingerprint(), notifEx.getMessage());
        }

        return newAdvisory;
    }

    private WeatherAdvisory createAndSaveAdvisory(Farm farm, AdvisoryProposal proposal) {
        WeatherAdvisory advisory = new WeatherAdvisory();
        advisory.setId(UUID.randomUUID());
        advisory.setAdvisoryFingerprint(proposal.getFingerprint());
        advisory.setFarm(farm);
        advisory.setCropPlanting(proposal.getCropPlanting());
        advisory.setTask(proposal.getTask());
        advisory.setRuleCode(proposal.getRuleCode());
        advisory.setSeverity(proposal.getSeverity());
        advisory.setTitle(proposal.getTitle());
        advisory.setMessage(proposal.getMessage());
        advisory.setRecommendedAction(proposal.getRecommendedAction());
        advisory.setTargetDate(proposal.getTargetDate());
        advisory.setStatus(AdvisoryStatus.ACTIVE);
        advisory.setCreatedAt(OffsetDateTime.now());
        advisory.setLastEvaluatedAt(OffsetDateTime.now());

        return weatherAdvisoryRepository.save(advisory);
    }

    private void expirePastAdvisories(UUID farmId) {
        LocalDate today = LocalDate.now();
        List<WeatherAdvisory> expired = weatherAdvisoryRepository.findByFarmIdAndTargetDateBeforeAndStatus(
                farmId, today, AdvisoryStatus.ACTIVE
        );
        for (WeatherAdvisory adv : expired) {
            adv.setStatus(AdvisoryStatus.RESOLVED);
            weatherAdvisoryRepository.save(adv);
        }
    }

    private boolean isIrrigationTask(Task task) {
        if (task.getTaskType() == com.krishi.backend.task.entity.TaskType.IRRIGATION) {
            return true;
        }
        String title = task.getTitle() != null ? task.getTitle().toLowerCase() : "";
        return title.contains("irrigation") || title.contains("watering") || title.contains("submergence");
    }

    private boolean isSprayingOrTreatmentTask(Task task) {
        if (task.getTaskType() != null) {
            com.krishi.backend.task.entity.TaskType type = task.getTaskType();
            if (type == com.krishi.backend.task.entity.TaskType.FERTILIZER_APPLICATION
                    || type == com.krishi.backend.task.entity.TaskType.DISEASE_TREATMENT
                    || type == com.krishi.backend.task.entity.TaskType.PEST_CONTROL) {
                return true;
            }
        }
        String title = task.getTitle() != null ? task.getTitle().toLowerCase() : "";
        return title.contains("spray") || title.contains("top-dressing") || title.contains("fertilizer")
                || title.contains("fungicide") || title.contains("pesticide") || title.contains("chemical");
    }

    private String getCropName(Task task) {
        if (task.getCropPlanting() != null && task.getCropPlanting().getCropName() != null) {
            return task.getCropPlanting().getCropName();
        }
        return "crop";
    }

    public String buildFingerprint(UUID farmId, UUID plantingId, UUID taskId, String ruleCode, LocalDate targetDate) {
        String pId = plantingId != null ? plantingId.toString() : "GLOBAL";
        String tId = taskId != null ? taskId.toString() : "GLOBAL";
        return farmId + ":" + pId + ":" + tId + ":" + ruleCode + ":" + targetDate;
    }

    @Data
    @Builder
    private static class AdvisoryProposal {
        private String fingerprint;
        private String ruleCode;
        private AdvisorySeverity severity;
        private String title;
        private String message;
        private String recommendedAction;
        private LocalDate targetDate;
        private CropPlanting cropPlanting;
        private Task task;
    }
}
