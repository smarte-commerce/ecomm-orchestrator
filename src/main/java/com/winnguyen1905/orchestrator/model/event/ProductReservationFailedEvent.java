package com.winnguyen1905.orchestrator.model.event;

import java.util.List;

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
public class ProductReservationFailedEvent extends SagaEvent {
    private String reason;
    private List<FailedProduct> products;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @SuperBuilder
    public static class FailedProduct {
        private String sku;
        private Long productId;
        private int requestedQuantity;
        private int availableQuantity;
        private String reason;
    }
} 