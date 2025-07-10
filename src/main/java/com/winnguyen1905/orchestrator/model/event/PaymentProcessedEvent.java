package com.winnguyen1905.orchestrator.model.event;

import java.time.Instant;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentProcessedEvent extends SagaEvent {
  private UUID paymentId;
  private double amount;
  private String transactionId;
  private String paymentMethod;
  private Instant processedAt;
}
