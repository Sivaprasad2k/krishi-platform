package com.krishi.backend.cropplanting.dto.context;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CropIdentityContext {
    private UUID plantingId;
    private UUID cropCycleId;
    private String cropName;
    private String cropVariety;
    private LocalDateTime plantingDate;
    private LocalDateTime expectedHarvestDate;
    private Long elapsedDays;
}
