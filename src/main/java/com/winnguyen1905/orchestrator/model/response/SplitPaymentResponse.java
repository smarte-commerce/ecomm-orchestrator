package com.winnguyen1905.orchestrator.model.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SplitPaymentResponse {
    private boolean success;
    private String errorMessage;
    private List<PaymentResult> paymentResults;
    private Instant processedAt;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentResult {
        private UUID paymentId;
        private String type;
        private String transactionId;
        private BigDecimal amount;
        private String status;
        private String errorMessage;
    }
}
