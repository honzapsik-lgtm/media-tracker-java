package com.mediatracker.repository;

import com.mediatracker.model.entity.SystemLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SystemLogRepository extends JpaRepository<SystemLogEntity, String> {
    List<SystemLogEntity> findTop100ByOrderByCreatedAtDesc();
}
