package com.winnguyen1905.orchestrator.core.feign;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import com.winnguyen1905.orchestrator.model.request.CreateShopOrderRequest;
import com.winnguyen1905.orchestrator.model.response.OrderResponse;
import com.winnguyen1905.orchestrator.model.response.RestResponse;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;

@FeignClient(name = "order-service", url = "${microservices.order-service.url:http://order-service:8091}")
@CircuitBreaker(name = "orderService")
@Retry(name = "orderService")
public interface OrderServiceClient {

  /**
   * Get order by ID
   */
  @GetMapping("/api/orders/{orderId}")
  ResponseEntity<RestResponse<OrderResponse>> getOrderById(@PathVariable("orderId") UUID orderId);

  /**
   * Update order status
   */
  @PutMapping("/api/orders/{orderId}/status")
  ResponseEntity<RestResponse<Void>> updateOrderStatus(
      @PathVariable("orderId") UUID orderId,
      @RequestParam("status") String status,
      @RequestParam("reason") String reason);

  /**
   * Update order discount
   */
  @PutMapping("/api/orders/{orderId}/discount")
  ResponseEntity<RestResponse<Void>> updateOrderDiscount(
      @PathVariable("orderId") UUID orderId,
      @RequestBody BigDecimal discountAmount);

  /**
   * Create separate orders for each shop
   */
  @PostMapping("/api/orders/create-shop-orders")
  ResponseEntity<RestResponse<List<OrderResponse>>> createShopOrders(@RequestBody List<CreateShopOrderRequest> requests);

  /**
   * Update order pricing information including subtotal, discounts, taxes
   */
  @PutMapping("/api/orders/{orderId}/pricing")
  ResponseEntity<RestResponse<Void>> updateOrderPricing(
      @PathVariable("orderId") UUID orderId,
      @RequestBody OrderPricingUpdate pricingUpdate);

  /**
   * Batch update multiple orders status
   */
  @PutMapping("/api/orders/batch-status")
  ResponseEntity<RestResponse<Void>> batchUpdateOrderStatus(@RequestBody List<OrderStatusUpdate> updates);

  /**
   * Update order payment information
   */
  @PutMapping("/api/orders/{orderId}/payment")
  ResponseEntity<RestResponse<Void>> updateOrderPayment(
      @PathVariable("orderId") UUID orderId,
      @RequestBody OrderPaymentUpdate paymentUpdate);

  /**
   * DTO for order pricing updates
   */
  public static class OrderPricingUpdate {
    private BigDecimal subtotal;
    private BigDecimal taxAmount;
    private BigDecimal shippingAmount;
    private BigDecimal discountAmount;
    private BigDecimal totalAmount;

    // Constructors, getters and setters
    public OrderPricingUpdate() {}

    public OrderPricingUpdate(BigDecimal subtotal, BigDecimal taxAmount, BigDecimal shippingAmount, BigDecimal discountAmount, BigDecimal totalAmount) {
      this.subtotal = subtotal;
      this.taxAmount = taxAmount;
      this.shippingAmount = shippingAmount;
      this.discountAmount = discountAmount;
      this.totalAmount = totalAmount;
    }

    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
    public BigDecimal getTaxAmount() { return taxAmount; }
    public void setTaxAmount(BigDecimal taxAmount) { this.taxAmount = taxAmount; }
    public BigDecimal getShippingAmount() { return shippingAmount; }
    public void setShippingAmount(BigDecimal shippingAmount) { this.shippingAmount = shippingAmount; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
  }

  /**
   * DTO for batch order status updates
   */
  public static class OrderStatusUpdate {
    private UUID orderId;
    private String status;
    private String reason;

    public OrderStatusUpdate() {}

    public OrderStatusUpdate(UUID orderId, String status, String reason) {
      this.orderId = orderId;
      this.status = status;
      this.reason = reason;
    }

    public UUID getOrderId() { return orderId; }
    public void setOrderId(UUID orderId) { this.orderId = orderId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
  }

  /**
   * DTO for order payment updates
   */
  public static class OrderPaymentUpdate {
    private UUID paymentId;
    private String paymentStatus;
    private BigDecimal amountPaid;
    private String paymentMethod;

    public OrderPaymentUpdate() {}

    public OrderPaymentUpdate(UUID paymentId, String paymentStatus, BigDecimal amountPaid, String paymentMethod) {
      this.paymentId = paymentId;
      this.paymentStatus = paymentStatus;
      this.amountPaid = amountPaid;
      this.paymentMethod = paymentMethod;
    }

    public UUID getPaymentId() { return paymentId; }
    public void setPaymentId(UUID paymentId) { this.paymentId = paymentId; }
    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }
    public BigDecimal getAmountPaid() { return amountPaid; }
    public void setAmountPaid(BigDecimal amountPaid) { this.amountPaid = amountPaid; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
  }
}
