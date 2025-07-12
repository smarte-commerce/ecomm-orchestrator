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
public class CreateShopOrderRequest {
  private UUID sagaId;
  private UUID parentOrderId;
  private UUID shopId;
  private UUID customerId;
  private String orderNumber;
  private String currency;
  private String shippingAddress;
  private String billingAddress;
  private String paymentMethod;
  private List<OrderItem> items;
  private UUID shopProductDiscountId;
  private Double shopSubtotal;
  private Double shopDiscountAmount;
  private Double shopTaxAmount;
  private Double shopShippingAmount;
  private Double shopTotalAmount;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class OrderItem {
    private UUID productId;
    private UUID variantId;
    private String productSku;
    private Integer quantity;
    private Double unitPrice;
    private Double lineTotal;
    private Double weight;
    private String dimensions;
    private String taxCategory;
    private UUID reservationId;
  }
}
