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
public class ReserveInventoryResponse {
  private UUID reservationId;
  private boolean status;
  private List<Item> items;
  private Instant expiresAt;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Item {
    private UUID productId;
    private UUID variantId;
    private int quantity;
  }
} 
