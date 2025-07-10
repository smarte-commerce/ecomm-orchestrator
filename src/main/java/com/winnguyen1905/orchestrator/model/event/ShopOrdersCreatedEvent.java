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
public class ShopOrdersCreatedEvent extends SagaEvent {
  private UUID parentOrderId;
  private List<ShopOrderInfo> shopOrders;
  private boolean allOrdersCreated;

  @Data
  @SuperBuilder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ShopOrderInfo {
    private UUID shopId;
    private UUID orderId;
    private String orderNumber;
    private Double shopTotal;
    private String status;
    private List<OrderItemInfo> items;

    @Data
    @SuperBuilder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemInfo {
      private UUID productId;
      private UUID variantId;
      private String productSku;
      private Integer quantity;
      private Double unitPrice;
      private Double lineTotal;
      private UUID reservationId;
    }
  }
}
