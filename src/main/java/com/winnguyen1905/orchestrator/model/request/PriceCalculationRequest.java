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
public class PriceCalculationRequest {
  private UUID sagaId;
  private UUID customerId;
  private List<ShopCheckoutItem> shopItems;
  private UUID shippingDiscountId;
  private UUID globalProductDiscountId;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ShopCheckoutItem {
    private UUID shopId;
    private List<ProductItem> items;
    private UUID shopProductDiscountId;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductItem {
      private UUID productId;
      private UUID variantId;
      private String productSku;
      private Integer quantity;
      private Double weight;
      private String dimensions;
      private String taxCategory;
    }
  }
}
