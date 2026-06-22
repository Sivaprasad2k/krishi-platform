package com.krishi.backend.farm.dto;

import com.krishi.backend.farm.entity.FarmStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class FarmResponse {
    private UUID id;
    private String name;
    private Double sizeHectares;
    private String location;
    private UUID ownerId;
    private FarmStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
