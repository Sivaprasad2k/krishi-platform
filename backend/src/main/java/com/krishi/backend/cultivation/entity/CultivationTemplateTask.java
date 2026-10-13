package com.krishi.backend.cultivation.entity;

import com.krishi.backend.task.entity.TaskType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "cultivation_template_tasks", schema = "krishi")
public class CultivationTemplateTask {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "template_id", nullable = false)
    private CultivationTemplate template;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stage_id")
    private CultivationStage stage;

    @Enumerated(EnumType.STRING)
    @Column(name = "task_type", nullable = false)
    private TaskType taskType;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "day_offset", nullable = false)
    private Integer dayOffset;

    @Column(name = "window_days_before", nullable = false)
    private Integer windowDaysBefore;

    @Column(name = "window_days_after", nullable = false)
    private Integer windowDaysAfter;

    @Column(name = "task_order", nullable = false)
    private Integer taskOrder;

    @Column(name = "mandatory", nullable = false)
    private Boolean mandatory;
}
