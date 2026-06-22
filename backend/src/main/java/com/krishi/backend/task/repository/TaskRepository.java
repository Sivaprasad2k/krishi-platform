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
     * Find all tasks associated with a specific treatment.
     *
     * @param treatmentId the treatment ID
     * @return the list of tasks
     */
    List<Task> findByTreatmentId(UUID treatmentId);

    @Query("SELECT COUNT(t) FROM Task t WHERE t.treatment.diseaseIncident.cropPlanting.cropCycle.farm.owner.id = :ownerId AND t.status IN :statuses")
    long countPendingTasksByOwner(@Param("ownerId") UUID ownerId, @Param("statuses") List<TaskStatus> statuses);
}
