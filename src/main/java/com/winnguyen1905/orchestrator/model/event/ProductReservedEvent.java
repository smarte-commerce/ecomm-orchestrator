package com.winnguyen1905.orchestrator.model.event;

import java.time.Instant;
import java.util.List;
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
public class ProductReservedEvent extends SagaEvent {
    private UUID reservationId;
    private List<ReservedProduct> products;
    private Instant expirationTime;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @SuperBuilder
    public static class ReservedProduct {
        private String sku;
        private Long productId;
        private int quantity;
    }
} 