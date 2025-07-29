package com.winnguyen1905.orchestrator.model.request;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentStatusUpdateRequest {
  
  @NotNull(message = "Order ID cannot be null")
  private UUID orderId;
  
  @NotNull(message = "Payment ID cannot be null") 
  private UUID paymentId;
  
  @NotBlank(message = "Payment status cannot be blank")
  private String paymentStatus; // PENDING, COMPLETED, FAILED, CANCELLED
  
  private BigDecimal amount;
  
  private String currency;
  
  private String transactionId;
  
  private String paymentMethod;
  
  private String failureReason;
  
  private String gatewayResponse;
  
  private Instant processedAt;
  
  private String metadata;
} 
