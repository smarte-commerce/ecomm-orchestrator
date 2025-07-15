package com.winnguyen1905.orchestrator.model.event;

import java.math.BigDecimal;
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
public class ShippingCalculatedEvent extends SagaEvent {
  private List<ShopShippingInfo> shopShippingDetails;
  private BigDecimal totalShippingCost;
  private boolean allShippingCalculated;
  private String shippingProvider;

  @Data
  @SuperBuilder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ShopShippingInfo {
    private UUID shopId;
    private UUID shopOrderId;
    private String shippingProvider;
    private String shippingService;
    private BigDecimal shippingCost;
    private String currency;
    private Integer estimatedDays;
    private String trackingSupported;
    private String quoteId;
    private boolean calculated;
  }
}
