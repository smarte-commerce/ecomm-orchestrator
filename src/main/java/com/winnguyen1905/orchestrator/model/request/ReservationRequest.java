package com.winnguyen1905.orchestrator.model.request;

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
public class ReservationRequest {
  private UUID sagaId;
  private List<ReservationItem> items;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ReservationItem {
    private UUID productId;
    private String sku;
    private int quantity;
  }
}
