package com.winnguyen1905.orchestrator.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;

/**
 * DTO for Kafka messages between orchestrator and other services
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KafkaOrchestratorResponseDTO {
  private UUID orderId;
  private UUID paymentId;
  private UUID transactionId;
  private UUID productId;
  private UUID couponId;
  private String status;
  private String message;
  private Map<String, Object> metadata;
}
