package com.winnguyen1905.orchestrator.model.response;

import java.time.Instant;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundResponse {
  private UUID refundId;
  private UUID paymentId;
  private UUID orderId;
  private Double amount;
  private String status;
  private String refundTransactionId;
  private Instant processedAt;
  private String reason;
  private String errorMessage;
}
