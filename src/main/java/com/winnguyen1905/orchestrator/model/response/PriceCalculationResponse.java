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
public class PriceCalculationResponse {
  private List<ShopPricing> shopPricings;
  // private PricingSummary totalPricing;
  private boolean allInventoryReserved;
  private boolean allDiscountsApplied;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ShopPricing {
    private UUID shopId;
    private Double subtotal;
    private Double taxAmount;
    private List<ProductPricing> productPricings;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductPricing {
      private UUID productId;
      private UUID variantId;
      private String productSku;
      private Integer quantity;
      private Double unitPrice;
      private Double lineTotal;
      private boolean inventoryReserved;
      private UUID reservationId;
    }
  }

  // @Data
  // @Builder
  // @NoArgsConstructor
  // @AllArgsConstructor
  // public static class PricingSummary {
  //   private Double orderSubtotal;
  //   private Double totalShopDiscounts;
  //   private Double shippingDiscount;
  //   private Double globalDiscount;
  //   private Double totalDiscount;
  //   private Double shippingAmount;
  //   private Double taxAmount;
  //   private Double finalTotal;
  // }
}
