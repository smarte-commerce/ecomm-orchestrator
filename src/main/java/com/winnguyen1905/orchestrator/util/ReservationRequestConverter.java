package com.winnguyen1905.orchestrator.util;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import com.winnguyen1905.orchestrator.model.request.ReservationRequest;
import com.winnguyen1905.orchestrator.model.request.ReserveInventoryRequest;

public class ReservationRequestConverter {

    /**
     * Converts ReservationRequest to ReserveInventoryRequest
     * Maps sku to variantId for compatibility
     */
    public static ReserveInventoryRequest toReserveInventoryRequest(ReservationRequest reservationRequest) {
        List<ReserveInventoryRequest.Item> items = reservationRequest.getItems().stream()
            .map(item -> ReserveInventoryRequest.Item.builder()
                .productId(item.getProductId())
                // Map sku to variantId (assuming they represent the same thing in the context)
                .variantId(parseSkuToVariantId(item.getSku()))
                .quantity(item.getQuantity())
                .build())
            .collect(Collectors.toList());

        return ReserveInventoryRequest.builder()
            .reservationId(reservationRequest.getSagaId()) // Use sagaId as reservationId
            .items(items)
            .expiresAt(Instant.now().plus(30, ChronoUnit.MINUTES)) // Default 30 minute expiration
            .build();
    }

    /**
     * Attempts to parse SKU to UUID for variantId
     * If SKU is already a UUID, use it directly
     * If not, you might need to look up the variantId from a service
     */
    private static UUID parseSkuToVariantId(String sku) {
        try {
            // Try to parse as UUID first
            return UUID.fromString(sku);
        } catch (IllegalArgumentException e) {
            // If SKU is not a UUID, you might need to:
            // 1. Look up the variant from a product service
            // 2. Use a default mapping
            // 3. Generate a deterministic UUID from the SKU
            
            // For now, generate a deterministic UUID from SKU
            // This is a simple approach - you might want to replace with actual lookup
            return UUID.nameUUIDFromBytes(sku.getBytes());
        }
    }
} 
