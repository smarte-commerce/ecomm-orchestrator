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
public class CouponAppliedEvent extends SagaEvent {
    private UUID couponId;
    private String couponCode;
    private BigDecimal discountAmount;
    private BigDecimal discountPercentage;
    private Instant validUntil;
    private boolean isApplied;
    private String errorMessage;
} 
