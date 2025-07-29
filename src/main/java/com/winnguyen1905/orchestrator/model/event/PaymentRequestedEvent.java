package com.winnguyen1905.orchestrator.model.event;

import java.math.BigDecimal;
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
public class PaymentRequestedEvent extends SagaEvent {
  private BigDecimal amount;
  private String currency;
  private String callbackTopic;
  private String paymentMethod;
  private UUID customerId;
  private String description;
  private Instant requestedAt;
} 
