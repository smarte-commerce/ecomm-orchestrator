package com.winnguyen1905.orchestrator.model.event;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "eventType")
@JsonSubTypes({
    @JsonSubTypes.Type(value = OrderCreatedEvent.class, name = "OrderCreated"),
    @JsonSubTypes.Type(value = ProductReservedEvent.class, name = "ProductReserved"),
    @JsonSubTypes.Type(value = ProductReservationFailedEvent.class, name = "ProductReservationFailed"),
    @JsonSubTypes.Type(value = PaymentProcessedEvent.class, name = "PaymentProcessed"),
    @JsonSubTypes.Type(value = PaymentFailedEvent.class, name = "PaymentFailed"),
    @JsonSubTypes.Type(value = OrderApprovedEvent.class, name = "OrderApproved"),
    @JsonSubTypes.Type(value = OrderCancelledEvent.class, name = "OrderCancelled"),
    @JsonSubTypes.Type(value = CouponAppliedEvent.class, name = "CouponApplied"),
    @JsonSubTypes.Type(value = SplitPaymentEvent.class, name = "SplitPayment")
})
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class SagaEvent {
  private UUID eventId;
  private UUID sagaId;
  private UUID orderId;
  private String eventType;
  private Instant timestamp;
  private int retryCount;

  // Used for tracing and logging
  private UUID correlationId;
  private UUID causationId;
}
