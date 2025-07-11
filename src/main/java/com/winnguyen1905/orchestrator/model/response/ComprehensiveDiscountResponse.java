package com.winnguyen1905.orchestrator.model.response;

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
public class ComprehensiveDiscountResponse {
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
    private UUID customerId;
    // private String notes;
    private List<DrawOrderItem> items;
    private UUID shopProductDiscountId;
    private Double totalOrderBeforeDiscounts;
    private Double totalShopDiscounts;
    private Double totalShippingDiscounts;
    private Double totalGlobalDiscounts;
    private Double totalOrderAfterDiscounts;
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
    private boolean isEligibleForDiscount;
  }

  // @Data
  // @Builder
  // @NoArgsConstructor
  // @AllArgsConstructor
  // public static class DiscountSummary {
  //   private Double totalShopDiscounts;
  //   private Double totalShippingDiscounts;
  //   private Double totalGlobalDiscounts;
  //   private Double totalDiscountAmount;
  //   private Double orderSubtotalBeforeDiscounts;
  //   private Double orderSubtotalAfterDiscounts;
  // }
}
