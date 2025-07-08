package com.winnguyen1905.orchestrator.persistance.entity;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "saga_states")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ESagaState {

  @Id
  private UUID sagaId;

  @Column(nullable = false)
  private UUID orderId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private SagaStatus status;

  @Column(columnDefinition = "TEXT")
  private String currentStep;

  @Column(columnDefinition = "TEXT")
  private String payload;

  @Column(columnDefinition = "TEXT")
  private String errorMessage;

  @Column
  private int retryCount;

  @Version
  private long version;

  @CreationTimestamp
  @Column(updatable = false)
  private Instant createdAt;

  @UpdateTimestamp
  private Instant updatedAt;

  @Column
  private Instant completedAt;

  public enum SagaStatus {
    STARTED,
    PRODUCT_RESERVING,
    PRODUCT_RESERVED,
    PAYMENT_PROCESSING,
    PAYMENT_PROCESSED,
    COMPLETING,
    COMPLETED,
    FAILED,
    COMPENSATING,
    COMPENSATED
  }
}
