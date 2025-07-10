package com.winnguyen1905.orchestrator.util;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import com.winnguyen1905.orchestrator.model.response.ReservationResponse;
import com.winnguyen1905.orchestrator.model.response.ReserveInventoryResponse;

public class ReservationResponseConverter {
  /**
   * Converts ReserveInventoryResponse to ReservationResponse
   * for backward compatibility with existing orchestrator code
   */
  public static ReservationResponse toReservationResponse(ReserveInventoryResponse reserveResponse, UUID orderId) {
    List<ReservationResponse.ReservedItem> reservedItems = reserveResponse.getItems().stream()
        .map(item -> ReservationResponse.ReservedItem.builder()
            .sku(item.getVariantId().toString()) // Convert variantId back to sku
            .quantity(item.getQuantity())
            .productId(item.getProductId() != null ? item.getProductId().getMostSignificantBits() : null)
            .reserved(true) // Assume successful if item is in response
            .message("Successfully reserved")
            .build())
        .collect(Collectors.toList());

    return ReservationResponse.builder()
        .reservationId(reserveResponse.getReservationId())
        .orderId(orderId)
        .success(reserveResponse.isStatus()) // Map status to success
        .message(reserveResponse.isStatus() ? "Reservation successful" : "Reservation failed")
        .expirationDate(reserveResponse.getExpiresAt())
        .items(reservedItems)
        .build();
  }
}
