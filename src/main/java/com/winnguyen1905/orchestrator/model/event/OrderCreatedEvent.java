package com.winnguyen1905.orchestrator.model.event;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreatedEvent extends SagaEvent {
  private UUID sagaId;
  private UUID eventId;
  private String eventType;
  private Instant timestamp;
  private int retryCount;
  private UUID correlationId;

  // Order details for orchestrator
  private UUID customerId;
  private String orderNumber;
  private String paymentMethod = "ONLINE";
  private String currency;
  private String shippingAddress;
  private String billingAddress;
  private List<CheckoutItem> checkoutItems;
  private UUID shippingDiscountId;
  private UUID globalProductDiscountId;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class CheckoutItem {
    private UUID shopId;
    private UUID orderId;
    private String notes;
    private List<OrderItem> items;
    private UUID shopProductDiscountId;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class OrderItem {
    private UUID productId;
    private UUID variantId;
    private String productSku;
    private Integer quantity;
    private Double weight;
    private String dimensions;
    private String taxCategory;
  }
}
