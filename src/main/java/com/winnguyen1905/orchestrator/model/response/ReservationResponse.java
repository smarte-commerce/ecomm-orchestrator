package com.winnguyen1905.orchestrator.model.response;

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
public class ReservationResponse {
    private UUID reservationId;
    private UUID orderId;
    private boolean success;
    private String message;
    private Instant expirationDate;
    private List<ReservedItem> items;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReservedItem {
        private String sku;
        private int quantity;
        private Long productId;
        private boolean reserved;
        private String message;
    }
} 