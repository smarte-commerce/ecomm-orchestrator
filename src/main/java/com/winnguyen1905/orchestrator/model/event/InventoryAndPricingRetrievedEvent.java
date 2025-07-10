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
public class InventoryAndPricingRetrievedEvent extends SagaEvent {
  private List<ShopInventoryReservation> shopReservations;
  private Double totalOrderSubtotal;
  private boolean allInventoryReserved;

  @Data
  @SuperBuilder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ShopInventoryReservation {
    private UUID shopId;
    private List<ProductReservation> productReservations;
    private Double shopSubtotal;

    @Data
    @SuperBuilder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductReservation {
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
}
