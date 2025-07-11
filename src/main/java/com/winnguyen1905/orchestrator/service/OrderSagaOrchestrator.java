package com.winnguyen1905.orchestrator.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.winnguyen1905.orchestrator.core.feign.OrderServiceClient;
import com.winnguyen1905.orchestrator.core.feign.PaymentServiceClient;
import com.winnguyen1905.orchestrator.core.feign.ProductServiceClient;
import com.winnguyen1905.orchestrator.core.feign.PromotionServiceClient;
import com.winnguyen1905.orchestrator.messaging.KafkaProducer;
import com.winnguyen1905.orchestrator.model.event.CouponAppliedEvent;
import com.winnguyen1905.orchestrator.model.event.OrderApprovedEvent;
import com.winnguyen1905.orchestrator.model.event.OrderCancelledEvent;
import com.winnguyen1905.orchestrator.model.event.OrderCreatedEvent;
import com.winnguyen1905.orchestrator.model.event.PaymentFailedEvent;
import com.winnguyen1905.orchestrator.model.event.PaymentProcessedEvent;
import com.winnguyen1905.orchestrator.model.event.ProductReservationFailedEvent;
import com.winnguyen1905.orchestrator.model.event.ProductReservedEvent;
import com.winnguyen1905.orchestrator.model.event.SagaEvent;
import com.winnguyen1905.orchestrator.model.request.ComprehensiveDiscountRequest;
import com.winnguyen1905.orchestrator.model.request.CouponValidationRequest;
import com.winnguyen1905.orchestrator.model.request.CreateShopOrderRequest;
import com.winnguyen1905.orchestrator.model.request.PaymentRequest;
import com.winnguyen1905.orchestrator.model.request.PriceCalculationRequest;
import com.winnguyen1905.orchestrator.model.request.RefundRequest;
import com.winnguyen1905.orchestrator.model.request.ReservationRequest;
import com.winnguyen1905.orchestrator.model.request.ReserveInventoryRequest;
import com.winnguyen1905.orchestrator.model.response.ComprehensiveDiscountResponse;
import com.winnguyen1905.orchestrator.model.response.CouponValidationResponse;
import com.winnguyen1905.orchestrator.model.response.KafkaOrchestratorResponseDTO;
import com.winnguyen1905.orchestrator.model.response.OrderResponse;
import com.winnguyen1905.orchestrator.model.response.PaymentResponse;
import com.winnguyen1905.orchestrator.model.response.PriceCalculationResponse;
import com.winnguyen1905.orchestrator.model.response.ReservationResponse;
import com.winnguyen1905.orchestrator.model.response.ReserveInventoryResponse;
import com.winnguyen1905.orchestrator.model.response.RestResponse;
import com.winnguyen1905.orchestrator.util.ReservationRequestConverter;
import com.winnguyen1905.orchestrator.util.ReservationResponseConverter;
import com.winnguyen1905.orchestrator.persistance.entity.ESagaState;
import com.winnguyen1905.orchestrator.persistance.entity.ESagaState.SagaStatus;
import com.winnguyen1905.orchestrator.persistance.repository.SagaStateRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderSagaOrchestrator {

  private final SagaStateRepository sagaStateRepository;
  private final KafkaTemplate<String, SagaEvent> kafkaTemplate;
  private final ObjectMapper objectMapper;
  private final OrderServiceClient orderServiceClient;
  private final ProductServiceClient productServiceClient;
  private final PaymentServiceClient paymentServiceClient;
  private final PromotionServiceClient promotionServiceClient;
  private final KafkaProducer kafkaProducer;

  // Kafka topics
  @Value("${topic.name.order.created}")
  private String orderCreatedTopic;
  @Value("${topic.name.stock.reserved}")
  private String productReservedTopic;
  @Value("${topic.name.stock.cancel}")
  private String productReservationFailedTopic;
  @Value("${topic.name.payment.out}")
  private String paymentProcessedTopic;
  @Value("${topic.name.payment.cancel}")
  private String paymentFailedTopic;
  @Value("${topic.name.order.approve}")
  private String orderApprovedTopic;
  @Value("${topic.name.order.reject}")
  private String orderCancelledTopic;
  @Value("${topic.name.promotion.out}")
  private String couponValidatedTopic;

  /**
   * Starts the SAGA for a new order
   */
  @Transactional
  public UUID startOrderSaga(UUID orderId) {
    // Create a new saga ID
    UUID sagaId = UUID.randomUUID();

    try {
      // Get order details from Order service
      RestResponse<OrderResponse> response = orderServiceClient.getOrderById(orderId).getBody();
      OrderResponse order = response.data();

      if (order == null) {
        throw new RuntimeException("Order not found: " + orderId);
      }

      // Create and persist the initial saga state
      ESagaState sagaState = ESagaState.builder()
          .sagaId(sagaId)
          .orderId(orderId)
          .status(SagaStatus.STARTED)
          .currentStep("START_SAGA")
          .build();

      sagaStateRepository.save(sagaState);

      // Create and publish the OrderCreated event
      OrderCreatedEvent event = OrderCreatedEvent.builder()
          .eventId(UUID.randomUUID())
          .sagaId(sagaId)
          .orderId(orderId)
          .eventType("OrderCreated")
          .timestamp(Instant.now())
          .retryCount(0)
          .correlationId(sagaId)
          .customerId(order.getCustomerId() != null ? order.getCustomerId() : UUID.randomUUID())
          .orderNumber(order.getOrderNumber())
          .paymentMethod("ONLINE")
          .currency("USD")
          .shippingAddress(order.getShippingDetails() != null ? order.getShippingDetails().getAddress() : "")
          .billingAddress(order.getShippingDetails() != null ? order.getShippingDetails().getAddress() : "")
          .build();

      kafkaTemplate.send(orderCreatedTopic, event);

      log.info("Started order saga for orderId: {}, sagaId: {}", orderId, sagaId);
      return sagaId;

    } catch (Exception e) {
      log.error("Failed to start order saga for orderId: " + orderId, e);
      throw new RuntimeException("Failed to start order saga", e);
    }
  }

  /**
   * Process the OrderCreated event
   */
  @Transactional
  public void processOrderCreated(OrderCreatedEvent event) {
    handleOrderCreated(event);
  }

  /**
   * Process the ProductReserved event
   */
  @Transactional
  public void processProductReserved(ProductReservedEvent event) {
    handleProductReserved(event);
  }

  /**
   * Process the ProductReservationFailed event
   */
  @Transactional
  public void processProductReservationFailed(ProductReservationFailedEvent event) {
    handleProductReservationFailed(event);
  }

  /**
   * Process the PaymentProcessed event
   */
  @Transactional
  public void processPaymentProcessed(PaymentProcessedEvent event) {
    handlePaymentProcessed(event);
  }

  /**
   * Process the CouponApplied event
   */
  @Transactional
  public void processCouponApplied(CouponAppliedEvent event) {
    handleCouponApplied(event);
  }

  @Transactional
  public void handleCreateOrder_New(OrderCreatedEvent event) {
    UUID sagaId = event.getSagaId();
    UUID orderId = event.getOrderId();

    log.info("Handling new OrderCreated event for orderId: {}, sagaId: {} - Enhanced multi-shop flow", orderId, sagaId);

    try {
      // Update saga state
      ESagaState sagaState = sagaStateRepository.findById(sagaId)
          .orElseThrow(() -> new RuntimeException("Saga not found: " + sagaId));

      sagaState.setStatus(SagaStatus.PRODUCT_RESERVING);
      sagaState.setCurrentStep("STEP_1_RESERVE_INVENTORY_PER_SHOP");
      sagaStateRepository.save(sagaState);

      // ensure all items are reserved
      // STEP 1: Reserve inventory for each shop's orders
      List<ReservationRequest.ReservationItem> allReservationItems = new ArrayList<>();
      for (OrderCreatedEvent.CheckoutItem checkoutItem : event.getCheckoutItems()) {
        for (OrderCreatedEvent.OrderItem item : checkoutItem.getItems()) {
          allReservationItems.add(ReservationRequest.ReservationItem.builder()
              .productId(item.getProductId())
              .sku(item.getProductSku())
              .quantity(item.getQuantity())
              .build());
        }
      }

      ReservationRequest reservationRequest = ReservationRequest.builder()
          .sagaId(sagaId)
          .items(allReservationItems)
          .build();

      // Convert to ReserveInventoryRequest and call service
      ReserveInventoryRequest reserveRequest = ReservationRequestConverter
          .toReserveInventoryRequest(reservationRequest);
      RestResponse<ReserveInventoryResponse> reserveResponse = productServiceClient
          .reserveInventoryBatch(reserveRequest).getBody();

      // Convert response back to ReservationResponse for compatibility
      ReservationResponse reservationResponse = null;
      if (reserveResponse != null && reserveResponse.data() != null) {
        reservationResponse = ReservationResponseConverter.toReservationResponse(reserveResponse.data(), orderId);
      }

      if (reservationResponse == null || !reservationResponse.isSuccess()) {
        handleInventoryReservationFailure(sagaId, orderId, event.getEventId(),
            "Failed to reserve inventory for all items");
        return;
      }

      // ensure all items are priced
      // STEP 2: Calculate subtotal for each shop's orders using pricing service
      sagaState.setCurrentStep("STEP_2_CALCULATE_PRICING_PER_SHOP");
      sagaStateRepository.save(sagaState);

      List<PriceCalculationRequest.ShopCheckoutItem> shopItems = event.getCheckoutItems().stream()
          .map(checkoutItem -> PriceCalculationRequest.ShopCheckoutItem.builder()
              .shopId(checkoutItem.getShopId())
              .shopProductDiscountId(checkoutItem.getShopProductDiscountId())
              .items(checkoutItem.getItems().stream()
                  .map(item -> PriceCalculationRequest.ShopCheckoutItem.ProductItem.builder()
                      .productId(item.getProductId())
                      .variantId(item.getVariantId())
                      .productSku(item.getProductSku())
                      .quantity(item.getQuantity())
                      .weight(item.getWeight() != null ? item.getWeight() : 0.0)
                      .dimensions(item.getDimensions() != null ? item.getDimensions() : "")
                      .taxCategory(item.getTaxCategory() != null ? item.getTaxCategory() : "STANDARD")
                      .build())
                  .collect(Collectors.toList()))
              .build())
          .collect(Collectors.toList());

      PriceCalculationRequest pricingRequest = PriceCalculationRequest.builder()
          .sagaId(sagaId)
          .customerId(event.getCustomerId())
          .shopItems(shopItems)
          .shippingDiscountId(event.getShippingDiscountId())
          .globalProductDiscountId(event.getGlobalProductDiscountId())
          .build();

      RestResponse<PriceCalculationResponse> pricingResponse = productServiceClient.calculatePricing(pricingRequest)
          .getBody();

      if (pricingResponse == null || pricingResponse.data() == null) {
        handlePricingCalculationFailure(sagaId, orderId, event.getEventId(), "Failed to calculate pricing for shops");
        return;
      }

      // ensure all items are discounted
      // STEP 3-5: Apply comprehensive discounts (shop, shipping, global)
      sagaState.setCurrentStep("STEP_3_5_APPLY_COMPREHENSIVE_DISCOUNTS");
      sagaStateRepository.save(sagaState);

      ComprehensiveDiscountRequest discountRequest;
      ComprehensiveDiscountResponse discountData;
      
      try {
        discountRequest = buildComprehensiveDiscountRequest(event, pricingResponse.data());
        
        log.info("Calling promotion service for comprehensive discounts with sagaId: {}", sagaId);
        RestResponse<ComprehensiveDiscountResponse> discountResponse = promotionServiceClient
            .applyComprehensiveDiscounts(discountRequest).getBody();

        if (discountResponse == null || discountResponse.data() == null) {
          log.error("Promotion service returned null or empty response for sagaId: {}", sagaId);
          handleDiscountApplicationFailure(sagaId, orderId, event.getEventId(), 
              "Promotion service returned null response");
          return;
        }
        
        discountData = discountResponse.data();
        log.info("Successfully applied comprehensive discounts for sagaId: {}, processed {} shops", 
                 sagaId, discountData.getCheckoutItems().size());
        
      } catch (Exception e) {
        log.error("Failed to apply comprehensive discounts for sagaId: {}", sagaId, e);
        handleDiscountApplicationFailure(sagaId, orderId, event.getEventId(), 
            "Failed to apply discounts: " + e.getMessage());
        return;
      }

      // Create shop orders with pricing and discount information
      sagaState.setCurrentStep("STEP_6_CREATE_SHOP_ORDERS");
      sagaStateRepository.save(sagaState);

      List<CreateShopOrderRequest> shopOrderRequests = buildShopOrderRequests(event, pricingResponse.data(),
          discountData, reservationResponse);

      // TODO: IMPLEMENTATION: SHIPPING API LOGIC HERE , Currently we are not using
      // shipping service
      // ___________________________________

      // CREATE ORDER BUT PAYMENT IS NOT PROCESSED YET
      RestResponse<List<OrderResponse>> shopOrdersResponse = orderServiceClient.createShopOrders(shopOrderRequests)
          .getBody();

      if (shopOrdersResponse == null || shopOrdersResponse.data() == null || shopOrdersResponse.data().isEmpty()) {
        handleShopOrderCreationFailure(sagaId, orderId, event.getEventId(), "Failed to create shop orders");
        return;
      }

      // STEP 6: Process payment based on payment method
      if ("ONLINE".equals(event.getPaymentMethod())) {
        sagaState.setCurrentStep("STEP_7_PROCESS_PAYMENTS");
        sagaStateRepository.save(sagaState);

        processPaymentsForShopOrders(event, shopOrdersResponse.data(), discountData);
      } else {
        // For COD or other payment methods, mark orders as confirmed
        sagaState.setCurrentStep("STEP_7_UPDATE_ORDER_STATUS");
        sagaStateRepository.save(sagaState);

        updateShopOrdersStatus(shopOrdersResponse.data(), "CONFIRMED", "Order confirmed - COD payment");
      }

      // Publish ShopOrdersCreated event
      publishShopOrdersCreatedEvent(sagaId, orderId, shopOrdersResponse.data(), event.getEventId());

      log.info("Successfully processed new order creation for orderId: {}, sagaId: {}", orderId, sagaId);

    } catch (Exception e) {
      log.error("Error in new order creation flow for orderId: " + orderId, e);
      handleOrderCreationFailure(sagaId, orderId, event.getEventId(), e.getMessage());
    }
  }

  /**
   * Enhanced OrderCreated handler - reserves inventory AND gets pricing
   * information
   */
  @Transactional
  public void handleOrderCreated(OrderCreatedEvent event) {
    UUID sagaId = event.getSagaId();
    UUID orderId = event.getOrderId();

    log.info("Handling OrderCreated event for orderId: {}, sagaId: {} - Enhanced flow with pricing", orderId, sagaId);

    try {
      // Update saga state
      ESagaState sagaState = sagaStateRepository.findById(sagaId)
          .orElseThrow(() -> new RuntimeException("Saga not found: " + sagaId));

      sagaState.setStatus(SagaStatus.PRODUCT_RESERVING);
      sagaState.setCurrentStep("RESERVE_INVENTORY_AND_GET_PRICING");
      sagaStateRepository.save(sagaState);

      // Get order details from Order service to get complete information
      RestResponse<OrderResponse> orderResponse = orderServiceClient.getOrderById(orderId).getBody();
      OrderResponse order = orderResponse.data();

      if (order == null) {
        throw new RuntimeException("Order not found: " + orderId);
      }

      // STEP 1: Reserve inventory
      // Prepare enhanced reservation request with pricing information
      List<ReservationRequest.ReservationItem> items = new ArrayList<>();

      // Convert from Order structure to include pricing request
      for (OrderResponse.OrderItemResponse item : order.getOrderItems()) {
        items.add(ReservationRequest.ReservationItem.builder()
            .sku(item.getProductSku())
            .productId(item.getProductId())
            .quantity(item.getQuantity())
            .build());
      }

      ReservationRequest reservationRequest = ReservationRequest.builder()
          .sagaId(sagaId)
          .items(items)
          .build();

      // Convert to ReserveInventoryRequest and call service
      ReserveInventoryRequest reserveRequest = ReservationRequestConverter
          .toReserveInventoryRequest(reservationRequest);
      RestResponse<ReserveInventoryResponse> response = productServiceClient.reserveInventory(reserveRequest).getBody();

      // Convert response back to ReservationResponse for compatibility
      ReservationResponse reservationResponse = null;
      if (response != null && response.data() != null) {
        reservationResponse = ReservationResponseConverter.toReservationResponse(response.data(), orderId);
      }

      if (reservationResponse != null && reservationResponse.isSuccess()) {
        // Inventory reservation successful
        handleProductReserved(ProductReservedEvent.builder()
            .eventId(UUID.randomUUID())
            .sagaId(sagaId)
            .orderId(orderId)
            .eventType("ProductReserved")
            .timestamp(Instant.now())
            .retryCount(0)
            .correlationId(sagaId)
            .causationId(event.getEventId())
            .reservationId(reservationResponse.getReservationId())
            .expirationTime(reservationResponse.getExpirationDate())
            .build());
      } else {
        // Inventory reservation failed
        handleProductReservationFailed(ProductReservationFailedEvent.builder()
            .eventId(UUID.randomUUID())
            .sagaId(sagaId)
            .orderId(orderId)
            .eventType("ProductReservationFailed")
            .timestamp(Instant.now())
            .retryCount(0)
            .correlationId(sagaId)
            .causationId(event.getEventId())
            .reason("Insufficient inventory")
            .build());
      }
    } catch (Exception e) {
      log.error("Error handling OrderCreated event for orderId: " + orderId, e);

      // Update saga state to failed
      ESagaState sagaState = sagaStateRepository.findById(sagaId)
          .orElseThrow(() -> new RuntimeException("Saga not found: " + sagaId));

      sagaState.setStatus(SagaStatus.FAILED);
      sagaState.setErrorMessage("Failed to reserve inventory: " + e.getMessage());
      sagaStateRepository.save(sagaState);

      // Create and publish OrderCancelled event
      OrderCancelledEvent cancelEvent = OrderCancelledEvent.builder()
          .eventId(UUID.randomUUID())
          .sagaId(sagaId)
          .orderId(orderId)
          .eventType("OrderCancelled")
          .timestamp(Instant.now())
          .retryCount(0)
          .correlationId(sagaId)
          .causationId(event.getEventId())
          .reason("Failed to reserve inventory: " + e.getMessage())
          .cancelledBy("SYSTEM")
          .shouldRefund(false)
          .shouldReleaseInventory(false)
          .build();

      kafkaTemplate.send(orderCancelledTopic, cancelEvent);
    }
  }

  /**
   * Handles ProductReserved event by validating coupon and discount
   */
  @Transactional
  public void handleProductReserved(ProductReservedEvent event) {
    UUID sagaId = event.getSagaId();
    UUID orderId = event.getOrderId();

    log.info("Handling ProductReserved event for orderId: {}, sagaId: {}", orderId, sagaId);

    try {
      // Update saga state
      ESagaState sagaState = sagaStateRepository.findById(sagaId)
          .orElseThrow(() -> new RuntimeException("Saga not found: " + sagaId));

      sagaState.setStatus(SagaStatus.PRODUCT_RESERVED);
      sagaState.setCurrentStep("VALIDATE_COUPON");
      sagaStateRepository.save(sagaState);

      // Publish ProductReserved event to Kafka
      kafkaTemplate.send(productReservedTopic, event);

      // Get order details
      RestResponse<OrderResponse> response = orderServiceClient.getOrderById(orderId).getBody();
      OrderResponse order = response.data();

      if (order == null) {
        throw new RuntimeException("Order not found: " + orderId);
      }

      // Check if there's a coupon code in the order
      if (order.getCouponCode() != null && !order.getCouponCode().isEmpty()) {
        // Prepare coupon validation request
        CouponValidationRequest couponRequest = CouponValidationRequest.builder()
            .orderId(orderId)
            .customerId(order.getCustomerId())
            .couponCode(order.getCouponCode())
            .orderAmount(order.getTotalAmount())
            .build();

        // Call Promotion service to validate coupon
        RestResponse<CouponValidationResponse> couponResponse = promotionServiceClient.validateCoupon(couponRequest)
            .getBody();
        CouponValidationResponse couponValidation = couponResponse.data();

        if (couponValidation != null && couponValidation.isValid()) {
          // Coupon validation successful
          CouponAppliedEvent couponEvent = CouponAppliedEvent.builder()
              .eventId(UUID.randomUUID())
              .sagaId(sagaId)
              .orderId(orderId)
              .eventType("CouponApplied")
              .timestamp(Instant.now())
              .retryCount(0)
              .correlationId(sagaId)
              .causationId(event.getEventId())
              .couponId(couponValidation.getCouponId())
              .couponCode(order.getCouponCode())
              .discountAmount(couponValidation.getDiscountAmount())
              .discountPercentage(couponValidation.getDiscountPercentage())
              .validUntil(couponValidation.getValidUntil())
              .isApplied(true)
              .build();

          handleCouponApplied(couponEvent);
        } else {
          // Coupon validation failed, but we continue with payment processing
          log.warn("Coupon validation failed for order: {}, continuing with original amount", orderId);

          // Process payment with original amount
          processPayment(sagaId, orderId, order, event.getEventId());
        }
      } else {
        // No coupon code, proceed directly to payment
        processPayment(sagaId, orderId, order, event.getEventId());
      }
    } catch (Exception e) {
      log.error("Error handling ProductReserved event for orderId: " + orderId, e);

      // Update saga state to failed
      ESagaState sagaState = sagaStateRepository.findById(sagaId)
          .orElseThrow(() -> new RuntimeException("Saga not found: " + sagaId));

      sagaState.setStatus(SagaStatus.FAILED);
      sagaState.setErrorMessage("Failed to process discount: " + e.getMessage());
      sagaStateRepository.save(sagaState);

      // Create and publish OrderCancelled event
      OrderCancelledEvent cancelEvent = OrderCancelledEvent.builder()
          .eventId(UUID.randomUUID())
          .sagaId(sagaId)
          .orderId(orderId)
          .eventType("OrderCancelled")
          .timestamp(Instant.now())
          .retryCount(0)
          .correlationId(sagaId)
          .causationId(event.getEventId())
          .reason("Failed to process discount: " + e.getMessage())
          .cancelledBy("SYSTEM")
          .shouldRefund(false)
          .shouldReleaseInventory(true)
          .build();

      kafkaTemplate.send(orderCancelledTopic, cancelEvent);
    }
  }

  /**
   * Handles CouponApplied event by processing payment
   */
  @Transactional
  public void handleCouponApplied(CouponAppliedEvent event) {
    UUID sagaId = event.getSagaId();
    UUID orderId = event.getOrderId();

    log.info("Handling CouponApplied event for orderId: {}, sagaId: {}", orderId, sagaId);

    try {
      // Update saga state
      ESagaState sagaState = sagaStateRepository.findById(sagaId)
          .orElseThrow(() -> new RuntimeException("Saga not found: " + sagaId));

      sagaState.setStatus(SagaStatus.COUPON_APPLIED);
      sagaState.setCurrentStep("PROCESS_PAYMENT");
      sagaStateRepository.save(sagaState);

      // Publish CouponApplied event to Kafka
      kafkaTemplate.send(couponValidatedTopic, event);

      // Get order details
      RestResponse<OrderResponse> response = orderServiceClient.getOrderById(orderId).getBody();
      OrderResponse order = response.data();

      if (order == null) {
        throw new RuntimeException("Order not found: " + orderId);
      }

      // Update order with discount information
      orderServiceClient.updateOrderDiscount(orderId, event.getDiscountAmount());

      // Process payment with discounted amount
      processPayment(sagaId, orderId, order, event.getEventId());

    } catch (Exception e) {
      log.error("Error handling CouponApplied event for orderId: " + orderId, e);

      // Update saga state to failed
      ESagaState sagaState = sagaStateRepository.findById(sagaId)
          .orElseThrow(() -> new RuntimeException("Saga not found: " + sagaId));

      sagaState.setStatus(SagaStatus.FAILED);
      sagaState.setErrorMessage("Failed to apply discount: " + e.getMessage());
      sagaStateRepository.save(sagaState);

      // Send cancel coupon message
      KafkaOrchestratorResponseDTO cancelCouponPayload = KafkaOrchestratorResponseDTO.builder()
          .orderId(orderId)
          .couponId(event.getCouponId())
          .status("CANCELLED")
          .message("Failed to apply discount: " + e.getMessage())
          .build();

      kafkaProducer.sendCancelCoupon(cancelCouponPayload);

      // Create and publish OrderCancelled event
      OrderCancelledEvent cancelEvent = OrderCancelledEvent.builder()
          .eventId(UUID.randomUUID())
          .sagaId(sagaId)
          .orderId(orderId)
          .eventType("OrderCancelled")
          .timestamp(Instant.now())
          .retryCount(0)
          .correlationId(sagaId)
          .causationId(event.getEventId())
          .reason("Failed to apply discount: " + e.getMessage())
          .cancelledBy("SYSTEM")
          .shouldRefund(false)
          .shouldReleaseInventory(true)
          .build();

      kafkaTemplate.send(orderCancelledTopic, cancelEvent);
    }
  }

  /**
   * Process payment with either original or discounted amount
   */
  private void processPayment(UUID sagaId, UUID orderId, OrderResponse order, UUID causationId) {
    // Prepare payment request
    PaymentRequest paymentRequest = PaymentRequest.builder()
        .orderId(orderId)
        .customerId(order.getCustomerId())
        .amount(order.getTotalAmount())
        .paymentMethod("CREDIT_CARD") // This would come from the order in a real implementation
        .currency("USD")
        .description("Payment for order " + order.getOrderNumber())
        .build();

    // Call Payment service to process payment
    RestResponse<PaymentResponse> paymentResponse = paymentServiceClient.processPayment(paymentRequest).getBody();
    PaymentResponse payment = paymentResponse.data();

    if (payment != null && "COMPLETED".equals(payment.getStatus())) {
      // Payment successful
      handlePaymentProcessed(PaymentProcessedEvent.builder()
          .eventId(UUID.randomUUID())
          .sagaId(sagaId)
          .orderId(orderId)
          .eventType("PaymentProcessed")
          .timestamp(Instant.now())
          .retryCount(0)
          .correlationId(sagaId)
          .causationId(causationId)
          .paymentId(payment.getPaymentId())
          .amount(payment.getAmount())
          .transactionId(payment.getTransactionId())
          .paymentMethod(payment.getPaymentMethod())
          .processedAt(payment.getProcessedAt())
          .build());
    } else {
      // Payment failed
      handlePaymentFailed(PaymentFailedEvent.builder()
          .eventId(UUID.randomUUID())
          .sagaId(sagaId)
          .orderId(orderId)
          .eventType("PaymentFailed")
          .timestamp(Instant.now())
          .retryCount(0)
          .correlationId(sagaId)
          .causationId(causationId)
          .amount(order.getTotalAmount())
          .errorCode(payment != null ? "PAYMENT_FAILED" : "PAYMENT_SERVICE_ERROR")
          .errorMessage(payment != null ? payment.getErrorMessage() : "Payment service error")
          .paymentMethod("CREDIT_CARD")
          .failedAt(Instant.now())
          .build());
    }
  }

  /**
   * Handles PaymentProcessed event by approving the order
   */
  @Transactional
  public void handlePaymentProcessed(PaymentProcessedEvent event) {
    UUID sagaId = event.getSagaId();
    UUID orderId = event.getOrderId();

    log.info("Handling PaymentProcessed event for orderId: {}, sagaId: {}", orderId, sagaId);

    try {
      // Update saga state
      ESagaState sagaState = sagaStateRepository.findById(sagaId)
          .orElseThrow(() -> new RuntimeException("Saga not found: " + sagaId));

      sagaState.setStatus(SagaStatus.PAYMENT_PROCESSED);
      sagaState.setCurrentStep("APPROVE_ORDER");
      sagaStateRepository.save(sagaState);

      // Publish PaymentProcessed event to Kafka
      kafkaTemplate.send(paymentProcessedTopic, event);

      // Call Order service to update order status
      orderServiceClient.updateOrderStatus(orderId, "CONFIRMED", "Payment processed successfully");

      // Call Product service to confirm reservation
      // This would convert the reserved inventory to sold inventory
      KafkaOrchestratorResponseDTO updateProductPayload = KafkaOrchestratorResponseDTO.builder()
          .orderId(orderId)
          .status("CONFIRMED")
          .message("Order confirmed, update inventory")
          .build();

      kafkaProducer.sendUpdateProduct(updateProductPayload);

      // Update saga state to completed
      sagaState.setStatus(SagaStatus.COMPLETED);
      sagaState.setCurrentStep("SAGA_COMPLETED");
      sagaState.setCompletedAt(Instant.now());
      sagaStateRepository.save(sagaState);

      // Create and publish OrderApproved event
      OrderApprovedEvent approvedEvent = OrderApprovedEvent.builder()
          .eventId(UUID.randomUUID())
          .sagaId(sagaId)
          .orderId(orderId)
          .eventType("OrderApproved")
          .timestamp(Instant.now())
          .retryCount(0)
          .correlationId(sagaId)
          .causationId(event.getEventId())
          .paymentId(event.getPaymentId())
          .newOrderStatus("CONFIRMED")
          .build();

      kafkaTemplate.send(orderApprovedTopic, approvedEvent);

      log.info("Order saga completed successfully for orderId: {}, sagaId: {}", orderId, sagaId);
    } catch (Exception e) {
      log.error("Error handling PaymentProcessed event for orderId: " + orderId, e);

      // Update saga state to failed
      ESagaState sagaState = sagaStateRepository.findById(sagaId)
          .orElseThrow(() -> new RuntimeException("Saga not found: " + sagaId));

      sagaState.setStatus(SagaStatus.FAILED);
      sagaState.setErrorMessage("Failed to approve order: " + e.getMessage());
      sagaStateRepository.save(sagaState);

      // Start compensation - refund payment
      try {
        RefundRequest refundRequest = RefundRequest.builder()
            .orderId(orderId)
            .amount(event.getAmount())
            .reason("Order approval failed: " + e.getMessage())
            .isFullRefund(true)
            .build();

        paymentServiceClient.issueRefund(event.getPaymentId(), refundRequest);

        // Send cancel messages
        KafkaOrchestratorResponseDTO cancelProductPayload = KafkaOrchestratorResponseDTO.builder()
            .orderId(orderId)
            .status("CANCELLED")
            .message("Order approval failed, release inventory")
            .build();

        kafkaProducer.sendCancelPayment(cancelProductPayload);

        // Create and publish OrderCancelled event
        OrderCancelledEvent cancelEvent = OrderCancelledEvent.builder()
            .eventId(UUID.randomUUID())
            .sagaId(sagaId)
            .orderId(orderId)
            .eventType("OrderCancelled")
            .timestamp(Instant.now())
            .retryCount(0)
            .correlationId(sagaId)
            .causationId(event.getEventId())
            .reason("Failed to approve order: " + e.getMessage())
            .cancelledBy("SYSTEM")
            .shouldRefund(true)
            .shouldReleaseInventory(true)
            .build();

        kafkaTemplate.send(orderCancelledTopic, cancelEvent);
      } catch (Exception refundEx) {
        log.error("Failed to issue refund for orderId: " + orderId, refundEx);
      }
    }
  }

  /**
   * Handles PaymentFailed event
   */
  @Transactional
  public void handlePaymentFailed(PaymentFailedEvent event) {
    UUID sagaId = event.getSagaId();
    UUID orderId = event.getOrderId();

    log.info("Handling PaymentFailed event for orderId: {}, sagaId: {}", orderId, sagaId);

    try {
      // Update saga state
      ESagaState sagaState = sagaStateRepository.findById(sagaId)
          .orElseThrow(() -> new RuntimeException("Saga not found: " + sagaId));

      sagaState.setStatus(SagaStatus.FAILED);
      sagaState.setCurrentStep("HANDLE_PAYMENT_FAILURE");
      sagaState.setErrorMessage(event.getErrorMessage());
      sagaStateRepository.save(sagaState);

      // Publish PaymentFailed event to Kafka
      kafkaTemplate.send(paymentFailedTopic, event);

      // Update order status to payment failed
      orderServiceClient.updateOrderStatus(orderId, "PAYMENT_FAILED", event.getErrorMessage());

      // Send cancel messages
      KafkaOrchestratorResponseDTO cancelProductPayload = KafkaOrchestratorResponseDTO.builder()
          .orderId(orderId)
          .status("CANCELLED")
          .message("Payment failed, release inventory")
          .build();

      kafkaProducer.sendReserveProduct(cancelProductPayload);

      RestResponse<OrderResponse> response = orderServiceClient.getOrderById(orderId).getBody();
      OrderResponse order = response.data();

      // Cancel coupon if one was applied
      if (order != null && order.getCouponCode() != null && !order.getCouponCode().isEmpty()) {
        KafkaOrchestratorResponseDTO cancelCouponPayload = KafkaOrchestratorResponseDTO.builder()
            .orderId(orderId)
            .status("CANCELLED")
            .message("Payment failed, cancel coupon")
            .build();

        kafkaProducer.sendCancelCoupon(cancelCouponPayload);
      }

      // Create and publish OrderCancelled event
      OrderCancelledEvent cancelEvent = OrderCancelledEvent.builder()
          .eventId(UUID.randomUUID())
          .sagaId(sagaId)
          .orderId(orderId)
          .eventType("OrderCancelled")
          .timestamp(Instant.now())
          .retryCount(0)
          .correlationId(sagaId)
          .causationId(event.getEventId())
          .reason("Payment failed: " + event.getErrorMessage())
          .cancelledBy("SYSTEM")
          .shouldRefund(false)
          .shouldReleaseInventory(true)
          .build();

      kafkaTemplate.send(orderCancelledTopic, cancelEvent);

      // Update saga state to compensated
      sagaState.setStatus(SagaStatus.COMPENSATED);
      sagaState.setCurrentStep("SAGA_COMPENSATED");
      sagaState.setCompletedAt(Instant.now());
      sagaStateRepository.save(sagaState);

      log.info("Order saga compensated for payment failure, orderId: {}, sagaId: {}", orderId, sagaId);
    } catch (Exception e) {
      log.error("Error handling PaymentFailed event for orderId: " + orderId, e);
    }
  }

  /**
   * Handles ProductReservationFailed event
   */
  @Transactional
  public void handleProductReservationFailed(ProductReservationFailedEvent event) {
    UUID sagaId = event.getSagaId();
    UUID orderId = event.getOrderId();

    log.info("Handling ProductReservationFailed event for orderId: {}, sagaId: {}", orderId, sagaId);

    try {
      // Update saga state
      ESagaState sagaState = sagaStateRepository.findById(sagaId)
          .orElseThrow(() -> new RuntimeException("Saga not found: " + sagaId));

      sagaState.setStatus(SagaStatus.FAILED);
      sagaState.setCurrentStep("HANDLE_RESERVATION_FAILURE");
      sagaState.setErrorMessage(event.getReason());
      sagaStateRepository.save(sagaState);

      // Publish ProductReservationFailed event to Kafka
      kafkaTemplate.send(productReservationFailedTopic, event);

      // Update order status to inventory not available
      orderServiceClient.updateOrderStatus(orderId, "INVENTORY_ERROR", event.getReason());

      // Create and publish OrderCancelled event
      OrderCancelledEvent cancelEvent = OrderCancelledEvent.builder()
          .eventId(UUID.randomUUID())
          .sagaId(sagaId)
          .orderId(orderId)
          .eventType("OrderCancelled")
          .timestamp(Instant.now())
          .retryCount(0)
          .correlationId(sagaId)
          .causationId(event.getEventId())
          .reason("Inventory reservation failed: " + event.getReason())
          .cancelledBy("SYSTEM")
          .shouldRefund(false)
          .shouldReleaseInventory(false)
          .build();

      kafkaTemplate.send(orderCancelledTopic, cancelEvent);

      // Update saga state to compensated
      sagaState.setStatus(SagaStatus.COMPENSATED);
      sagaState.setCurrentStep("SAGA_COMPENSATED");
      sagaState.setCompletedAt(Instant.now());
      sagaStateRepository.save(sagaState);

      log.info("Order saga compensated for inventory failure, orderId: {}, sagaId: {}", orderId, sagaId);
    } catch (Exception e) {
      log.error("Error handling ProductReservationFailed event for orderId: " + orderId, e);
    }
  }

  // Helper methods for the new order flow

  private void handleInventoryReservationFailure(UUID sagaId, UUID orderId, UUID causationId, String reason) {
    log.error("Inventory reservation failed for orderId: {}, reason: {}", orderId, reason);

    ESagaState sagaState = sagaStateRepository.findById(sagaId)
        .orElseThrow(() -> new RuntimeException("Saga not found: " + sagaId));

    sagaState.setStatus(SagaStatus.FAILED);
    sagaState.setErrorMessage(reason);
    sagaStateRepository.save(sagaState);

    orderServiceClient.updateOrderStatus(orderId, "INVENTORY_ERROR", reason);
  }

  private void handlePricingCalculationFailure(UUID sagaId, UUID orderId, UUID causationId, String reason) {
    log.error("Pricing calculation failed for orderId: {}, reason: {}", orderId, reason);

    ESagaState sagaState = sagaStateRepository.findById(sagaId)
        .orElseThrow(() -> new RuntimeException("Saga not found: " + sagaId));

    sagaState.setStatus(SagaStatus.FAILED);
    sagaState.setErrorMessage(reason);
    sagaStateRepository.save(sagaState);

    orderServiceClient.updateOrderStatus(orderId, "PRICING_ERROR", reason);
  }

  private void handleDiscountApplicationFailure(UUID sagaId, UUID orderId, UUID causationId, String reason) {
    log.error("Discount application failed for orderId: {}, reason: {}", orderId, reason);

    ESagaState sagaState = sagaStateRepository.findById(sagaId)
        .orElseThrow(() -> new RuntimeException("Saga not found: " + sagaId));

    sagaState.setStatus(SagaStatus.FAILED);
    sagaState.setErrorMessage(reason);
    sagaStateRepository.save(sagaState);

    orderServiceClient.updateOrderStatus(orderId, "DISCOUNT_ERROR", reason);
  }

  private void handleShopOrderCreationFailure(UUID sagaId, UUID orderId, UUID causationId, String reason) {
    log.error("Shop order creation failed for orderId: {}, reason: {}", orderId, reason);

    ESagaState sagaState = sagaStateRepository.findById(sagaId)
        .orElseThrow(() -> new RuntimeException("Saga not found: " + sagaId));

    sagaState.setStatus(SagaStatus.FAILED);
    sagaState.setErrorMessage(reason);
    sagaStateRepository.save(sagaState);

    orderServiceClient.updateOrderStatus(orderId, "ORDER_CREATION_ERROR", reason);
  }

  private void handleOrderCreationFailure(UUID sagaId, UUID orderId, UUID causationId, String reason) {
    log.error("Order creation failed for orderId: {}, reason: {}", orderId, reason);

    ESagaState sagaState = sagaStateRepository.findById(sagaId)
        .orElseThrow(() -> new RuntimeException("Saga not found: " + sagaId));

    sagaState.setStatus(SagaStatus.FAILED);
    sagaState.setErrorMessage(reason);
    sagaStateRepository.save(sagaState);

    orderServiceClient.updateOrderStatus(orderId, "FAILED", reason);
  }

  private ComprehensiveDiscountRequest buildComprehensiveDiscountRequest(OrderCreatedEvent event,
      PriceCalculationResponse pricingData) {
    
    // Validate input parameters
    if (event == null) {
      throw new IllegalArgumentException("OrderCreatedEvent cannot be null");
    }
    if (pricingData == null || pricingData.getShopPricings() == null) {
      throw new IllegalArgumentException("PriceCalculationResponse or shopPricings cannot be null");
    }
    
    log.debug("Building comprehensive discount request for sagaId: {}, customerId: {}", 
              event.getSagaId(), event.getCustomerId());
    
    // Build list of DrawOrder items for each shop
    List<ComprehensiveDiscountRequest.DrawOrder> checkoutItems = new ArrayList<>();

    for (PriceCalculationResponse.ShopPricing shop : pricingData.getShopPricings()) {
      if (shop == null || shop.getShopId() == null) {
        log.warn("Skipping null shop or shop with null ID in pricing data");
        continue;
      }
      
      OrderCreatedEvent.CheckoutItem checkoutItem = event.getCheckoutItems().stream()
          .filter(item -> item != null && Objects.equals(item.getShopId(), shop.getShopId()))
          .findFirst()
          .orElse(null);

      if (checkoutItem != null) {
        // Validate and build DrawOrderItem list using unit prices from pricing response
        List<ComprehensiveDiscountRequest.DrawOrderItem> items = new ArrayList<>();
        
        if (shop.getProductPricings() != null) {
          items = shop.getProductPricings()
              .stream()
              .filter(product -> product != null && product.getProductId() != null)
              .map(product -> {
                try {
                  return ComprehensiveDiscountRequest.DrawOrderItem.builder()
                      .productId(product.getProductId())
                      .variantId(product.getVariantId())
                      .quantity(product.getQuantity() != null ? product.getQuantity() : 0)
                      .unitPrice(product.getUnitPrice() != null ? product.getUnitPrice() : 0.0)
                      .productSku(product.getProductSku())
                      .build();
                } catch (Exception e) {
                  log.error("Failed to build DrawOrderItem for product {}: {}", 
                           product.getProductId(), e.getMessage());
                  return null;
                }
              })
              .filter(Objects::nonNull)
              .collect(Collectors.toList());
        }

        if (!items.isEmpty()) {
          checkoutItems.add(ComprehensiveDiscountRequest.DrawOrder.builder()
              .shopId(shop.getShopId())
              .orderId(checkoutItem.getOrderId())
              .items(items)
              .shopProductDiscountId(checkoutItem.getShopProductDiscountId())
              .build());
          
          log.debug("Added shop {} with {} items to comprehensive discount request", 
                   shop.getShopId(), items.size());
        } else {
          log.warn("No valid items found for shop {}, skipping discount application", shop.getShopId());
        }
      } else {
        log.warn("No matching checkout item found for shop {} in event, skipping", shop.getShopId());
      }
    }

    if (checkoutItems.isEmpty()) {
      log.warn("No valid checkout items found for comprehensive discount request");
    }

    ComprehensiveDiscountRequest request = ComprehensiveDiscountRequest.builder()
        .sagaId(event.getSagaId())
        .customerId(event.getCustomerId())
        .eventType("APPLY_COMPREHENSIVE_DISCOUNTS")
        .globalProductDiscountId(event.getGlobalProductDiscountId())
        .globalShippingDiscountId(event.getShippingDiscountId())
        .checkoutItems(checkoutItems)
        .build();
    
    log.info("Built comprehensive discount request for {} shops with sagaId: {}", 
             checkoutItems.size(), event.getSagaId());
    
    return request;
  }

  private String extractRegionFromAddress(String address) {
    // Simple region extraction logic - should be enhanced based on actual address
    // parsing
    if (address == null)
      return "UNKNOWN";

    // Example: Extract region based on address patterns
    if (address.toLowerCase().contains("hanoi") || address.toLowerCase().contains("hà nội")) {
      return "NORTH";
    } else if (address.toLowerCase().contains("ho chi minh") || address.toLowerCase().contains("hồ chí minh")) {
      return "SOUTH";
    } else if (address.toLowerCase().contains("da nang") || address.toLowerCase().contains("đà nẵng")) {
      return "CENTRAL";
    }
    return "OTHER";
  }

  private List<CreateShopOrderRequest> buildShopOrderRequests(OrderCreatedEvent event,
      PriceCalculationResponse pricingData,
      ComprehensiveDiscountResponse discountData, ReservationResponse reservationData) {
    List<CreateShopOrderRequest> requests = new ArrayList<>();

    for (PriceCalculationResponse.ShopPricing shop : pricingData.getShopPricings()) {
      OrderCreatedEvent.CheckoutItem checkoutItem = event.getCheckoutItems().stream()
          .filter(item -> item.getShopId().equals(shop.getShopId()))
          .findFirst()
          .orElse(null);

      if (checkoutItem != null) {
        // Find discount data for this shop from the response
        ComprehensiveDiscountResponse.DrawOrder shopDiscountData = discountData.getCheckoutItems().stream()
            .filter(discountOrder -> discountOrder.getShopId().equals(shop.getShopId()))
            .findFirst()
            .orElse(null);

        List<CreateShopOrderRequest.OrderItem> orderItems = shop.getProductPricings().stream()
            .map(product -> CreateShopOrderRequest.OrderItem.builder()
                .productId(product.getProductId())
                .variantId(product.getVariantId())
                .productSku(product.getProductSku())
                .quantity(product.getQuantity())
                .unitPrice(product.getUnitPrice())
                .lineTotal(product.getLineTotal())
                .reservationId(product.getReservationId())
                .build())
            .collect(Collectors.toList());

        // Calculate shop total using discount data from response
        double shopDiscountAmount = shopDiscountData != null
            ? (shopDiscountData.getTotalShopDiscounts() + shopDiscountData.getTotalGlobalDiscounts())
            : 0.0;
        double shopTotal = shop.getSubtotal() - shopDiscountAmount + shop.getTaxAmount();

        requests.add(CreateShopOrderRequest.builder()
            .sagaId(event.getSagaId())
            .parentOrderId(event.getOrderId())
            .shopId(shop.getShopId())
            .customerId(event.getCustomerId())
            .orderNumber(event.getOrderNumber() + "-" + shop.getShopId().toString().substring(0, 8))
            .currency(event.getCurrency())
            .shippingAddress(event.getShippingAddress())
            .billingAddress(event.getBillingAddress())
            .paymentMethod(event.getPaymentMethod())
            .items(orderItems)
            .shopProductDiscountId(checkoutItem.getShopProductDiscountId())
            .shopSubtotal(shop.getSubtotal())
            .shopDiscountAmount(shopDiscountAmount)
            .shopTaxAmount(shop.getTaxAmount())
            .shopShippingAmount(0.0) // Calculated separately
            .shopTotalAmount(shopTotal)
            .build());
      }
    }

    return requests;
  }

  // TODO: IMPLEMENTATION: PAYMENT API LOGIC HERE , Currently we are not using
  // payment service
  private void processPaymentsForShopOrders(OrderCreatedEvent event, List<OrderResponse> shopOrders,
      ComprehensiveDiscountResponse discountData) {
    // For now, process a single payment for all shop orders
    double totalAmount = shopOrders.stream()
        .mapToDouble(order -> order.getTotalAmount().doubleValue())
        .sum();

    // TODO: pay chung thì thoio còn riêng thì tạo OrderId riêngriêng
    PaymentRequest paymentRequest = PaymentRequest.builder()
        // .orderId(event.getOrderId())
        .customerId(event.getCustomerId()) // Convert from Long to UUID properly based on business logic
        .amount(BigDecimal.valueOf(totalAmount))
        .paymentMethod("CREDIT_CARD")
        .currency(event.getCurrency())
        .description("Payment for multi-shop order " + event.getOrderNumber())
        .build();

    // TODO:
    RestResponse<PaymentResponse> paymentResponse = paymentServiceClient.processPayment(paymentRequest).getBody();

    if (paymentResponse != null && paymentResponse.data() != null
        && "COMPLETED".equals(paymentResponse.data().getStatus())) {
      updateShopOrdersStatus(shopOrders, "PAYMENT_COMPLETED", "Payment processed successfully");
    } else {
      updateShopOrdersStatus(shopOrders, "PAYMENT_FAILED", "Payment processing failed");
    }
  }

  private void updateShopOrdersStatus(List<OrderResponse> shopOrders, String status, String reason) {
    List<OrderServiceClient.OrderStatusUpdate> updates = shopOrders.stream()
        .map(order -> new OrderServiceClient.OrderStatusUpdate(order.getId(), status, reason))
        .collect(Collectors.toList());

    orderServiceClient.batchUpdateOrderStatus(updates);
  }

  private void publishShopOrdersCreatedEvent(UUID sagaId, UUID orderId, List<OrderResponse> shopOrders,
      UUID causationId) {
    // Implementation would publish an event about shop orders being created
    log.info("Publishing ShopOrdersCreated event for sagaId: {}, parentOrderId: {}, shopOrders: {}",
        sagaId, orderId, shopOrders.size());
  }

  private Double calculateTotalWeight(OrderCreatedEvent event) {
    return event.getCheckoutItems().stream()
        .flatMap(checkoutItem -> checkoutItem.getItems().stream())
        .mapToDouble(item -> item.getWeight() != null ? item.getWeight() * item.getQuantity() : 0.0)
        .sum();
  }
}
