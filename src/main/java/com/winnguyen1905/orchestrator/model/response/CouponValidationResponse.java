package com.winnguyen1905.orchestrator.model.response;

import java.math.BigDecimal;
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
public class CouponValidationResponse {
    private UUID couponId;
    private String couponCode;
    private boolean isValid;
    private String invalidReason;
    private BigDecimal discountAmount;
    private BigDecimal discountPercentage;
    private Instant validUntil;
    private UUID orderId;
    private UUID customerId;
}
