package com.winnguyen1905.orchestrator.model.event;

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
public class OrderCancelledEvent extends SagaEvent {
  private String reason;
  private String cancelledBy;
  private boolean shouldRefund;
  private boolean shouldReleaseInventory;
}
