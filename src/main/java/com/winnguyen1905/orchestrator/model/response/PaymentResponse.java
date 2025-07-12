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
public class PaymentResponse {
    private UUID paymentId;
    private UUID orderId;
    private Long customerId;
    private Double amount;
    private String paymentMethod;
    private String status;
    private String transactionId;
    private String currency;
    private Instant processedAt;
    private String description;
    private String errorMessage;
} 