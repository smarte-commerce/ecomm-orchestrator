package com.winnguyen1905.orchestrator.model.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class SplitPaymentEvent extends SagaEvent {
    private List<PaymentPart> payments;
    private BigDecimal totalAmount;
    private boolean completed;
    private Instant processedAt;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentPart {
        private UUID paymentId;
        private String paymentMethod;
        private String transactionId;
        private BigDecimal amount;
        private String status;
    }
}
