package com.krishi.backend.farm.repository;

import com.krishi.backend.farm.entity.Farm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;
import java.util.List;

@Repository
public interface FarmRepository extends JpaRepository<Farm, UUID> {

    /**
     * Find all farms owned by a specific user.
     *
     * @param ownerId the UUID of the owner
     * @return a list of farms
     */
    List<Farm> findByOwnerId(UUID ownerId);

    long countByOwnerId(UUID ownerId);
}
