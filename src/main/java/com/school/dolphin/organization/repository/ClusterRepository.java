package com.school.whale.organization.repository;

import com.school.whale.organization.entity.Cluster;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ClusterRepository extends JpaRepository<Cluster, UUID> {

    boolean existsByRegionIdAndCode(
            UUID regionId,
            String code
    );

    List<Cluster> findAllByRegionId(UUID regionId);
}