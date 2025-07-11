package com.winnguyen1905.orchestrator.model.request;

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
public class ComprehensiveDiscountRequest {
  private UUID sagaId;
  private UUID customerId;
  private String eventType;
  private UUID globalProductDiscountId;
  private UUID globalShippingDiscountId;
  private List<DrawOrder> checkoutItems;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class DrawOrder {
    private UUID shopId;
    private UUID orderId;
    private List<DrawOrderItem> items;
    private UUID shopProductDiscountId;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class DrawOrderItem {
    private UUID productId;
    private UUID variantId;
    private Integer quantity;
    private Double unitPrice;
    private String productSku;
  }
}
