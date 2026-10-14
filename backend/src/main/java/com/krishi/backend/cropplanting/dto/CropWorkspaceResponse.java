package com.krishi.backend.cropplanting.dto;

import com.krishi.backend.cropcycle.dto.CropCycleResponse;
import com.krishi.backend.diseaseincident.dto.DiseaseIncidentResponse;
import com.krishi.backend.farm.dto.FarmResponse;
import com.krishi.backend.task.dto.TaskResponse;
import com.krishi.backend.treatment.dto.TreatmentResponse;
import com.krishi.backend.cultivation.dto.EstimatedGrowthStageResponse;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class CropWorkspaceResponse {
    private CropPlantingResponse crop;
    private CropCycleResponse season;
    private FarmResponse farm;
    private String farmName;
    private UUID farmId;
    private List<TaskResponse> tasks;
    private List<DiseaseIncidentResponse> incidents;
    private List<DiseaseIncidentResponse> openIncidents;
    private List<TreatmentResponse> treatments;
    private com.krishi.backend.cultivation.dto.CultivationPlanResponse cultivationPlan;
    private EstimatedGrowthStageResponse estimatedGrowthStage;
    private com.krishi.backend.cropplanting.dto.context.WeatherContext weather;
}
