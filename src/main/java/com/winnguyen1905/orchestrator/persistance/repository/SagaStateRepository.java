package com.winnguyen1905.orchestrator.persistance.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.winnguyen1905.orchestrator.persistance.entity.ESagaState;
import com.winnguyen1905.orchestrator.persistance.entity.ESagaState.SagaStatus;

@Repository
public interface SagaStateRepository extends JpaRepository<ESagaState, UUID> {
    
    Optional<ESagaState> findByOrderId(UUID orderId);
    
    List<ESagaState> findByStatus(SagaStatus status);
    
    @Query("SELECT s FROM ESagaState s WHERE s.status = :status AND s.updatedAt < :timestamp")
    List<ESagaState> findStuckSagas(SagaStatus status, java.time.Instant timestamp);
} 