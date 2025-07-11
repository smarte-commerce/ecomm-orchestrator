package com.winnguyen1905.orchestrator.model.request;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundRequest {
    private UUID orderId;
    private Double amount;
    private String reason;
    private boolean isFullRefund;
} 