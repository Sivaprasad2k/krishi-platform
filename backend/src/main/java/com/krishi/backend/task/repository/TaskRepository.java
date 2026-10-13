package com.krishi.backend.task.repository;

import com.krishi.backend.task.entity.Task;
import com.krishi.backend.task.entity.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TaskRepository extends JpaRepository<Task, UUID> {

    /**
     * Find all tasks associated with a specific crop planting.
     *
     * @param cropPlantingId the crop planting ID
     * @return the list of tasks
     */
    List<Task> findByCropPlantingId(UUID cropPlantingId);

    /**
     * Find all tasks associated with a specific treatment.
     *
     * @param treatmentId the treatment ID
     * @return the list of tasks
     */
    List<Task> findByTreatmentId(UUID treatmentId);

    /**
     * Find all tasks associated with a specific cultivation plan.
     */
    List<Task> findByCultivationPlanId(UUID cultivationPlanId);

    /**
     * Find all tasks for a planting and cultivation plan.
     */
    List<Task> findByCropPlantingIdAndCultivationPlanId(UUID cropPlantingId, UUID cultivationPlanId);

    /**
     * Find all tasks belonging to any crop planting on farms owned by the given user.
     *
     * @param ownerId the farmer user ID
     * @return the list of tasks
     */
    List<Task> findByCropPlantingCropCycleFarmOwnerId(UUID ownerId);

    /**
     * Count pending/in-progress tasks for farms owned by the given user.
     *
     * @param ownerId the farmer user ID
     * @param statuses list of active statuses (PENDING, IN_PROGRESS)
     * @return count of active tasks
     */
    @Query("SELECT COUNT(t) FROM Task t WHERE t.cropPlanting.cropCycle.farm.owner.id = :ownerId AND t.status IN :statuses")
    long countPendingTasksByOwner(@Param("ownerId") UUID ownerId, @Param("statuses") List<TaskStatus> statuses);
}
