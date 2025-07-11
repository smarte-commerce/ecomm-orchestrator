package com.winnguyen1905.orchestrator.model.request;

import java.math.BigDecimal;
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
public class SplitPaymentRequest {
  private UUID orderId;
  private UUID customerId;
  private BigDecimal totalAmount;
  private List<PaymentMethod> paymentMethods;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class PaymentMethod {
    private String type; // CREDIT_CARD, GIFT_CARD, LOYALTY_POINTS, etc.
    private String cardNumber;
    private String cardHolderName;
    private String expiryDate;
    private String cvv;
    private BigDecimal amount;
    private String currency;
  }
}
