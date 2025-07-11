package com.winnguyen1905.orchestrator.model.response;

import java.math.BigDecimal;
import java.time.Instant;
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
public class OrderResponse {
    private UUID id;
    private String orderNumber;
    private UUID customerId;
    private String customerName;
    private String customerEmail;
    private String status;
    private BigDecimal subtotalAmount;
    private BigDecimal taxAmount;
    private BigDecimal shippingAmount;
    private BigDecimal discountAmount;
    private BigDecimal totalAmount;
    private String couponCode;
    private UUID couponId;
    private Instant createdAt;
    private Instant updatedAt;
    private List<OrderItemResponse> orderItems;
    private PaymentDetailsResponse paymentDetails;
    private ShippingDetailsResponse shippingDetails;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemResponse {
        private UUID id;
        private UUID productId;
        private String productName;
        private String productSku;
        private BigDecimal unitPrice;
        private int quantity;
        private BigDecimal totalPrice;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentDetailsResponse {
        private String paymentMethod;
        private String paymentStatus;
        private String transactionId;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ShippingDetailsResponse {
        private String address;
        private String city;
        private String state;
        private String zipCode;
        private String country;
        private String shippingMethod;
        private String trackingNumber;
    }
}
