package com.winnguyen1905.orchestrator.model.event;

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
public class ComprehensiveDiscountAppliedEvent extends SagaEvent {
  private List<ShopDiscountInfo> shopDiscounts;
  private ShippingDiscountInfo shippingDiscount;
  private GlobalDiscountInfo globalDiscount;
  private Double totalDiscountAmount;
  private Double finalOrderAmount;
  private boolean allDiscountsProcessed;

  @Data
  @SuperBuilder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ShopDiscountInfo {
    private UUID shopId;
    private UUID shopOrderId;
    private UUID discountId;
    private String discountCode;
    private Double discountAmount;
    private Double finalShopTotal;
    private boolean applied;
  }

  @Data
  @SuperBuilder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ShippingDiscountInfo {
    private UUID discountId;
    private String discountCode;
    private Double discountAmount;
    private Double finalShippingCost;
    private boolean applied;
  }

  @Data
  @SuperBuilder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class GlobalDiscountInfo {
    private UUID discountId;
    private String discountCode;
    private List<UUID> applicableShopIds;
    private Double totalDiscountAmount;
    private boolean applied;
  }
}
