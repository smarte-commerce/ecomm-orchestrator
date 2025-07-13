package com.winnguyen1905.orchestrator.model.request;

import java.math.BigDecimal;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponValidationRequest {
    private UUID orderId;
    private UUID customerId;
    private String couponCode;
    private BigDecimal orderAmount;
} 
