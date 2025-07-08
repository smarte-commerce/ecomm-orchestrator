package com.winnguyen1905.orchestrator.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
import com.winnguyen1905.orchestrator.model.request.CouponValidationRequest;
import com.winnguyen1905.orchestrator.model.request.PaymentRequest;
import com.winnguyen1905.orchestrator.model.request.RefundRequest;
import com.winnguyen1905.orchestrator.model.request.ReservationRequest;
import com.winnguyen1905.orchestrator.model.response.CouponValidationResponse;
import com.winnguyen1905.orchestrator.model.response.KafkaOrchestratorResponseDTO;
import com.winnguyen1905.orchestrator.model.response.OrderResponse;
import com.winnguyen1905.orchestrator.model.response.PaymentResponse;
import com.winnguyen1905.orchestrator.model.response.ReservationResponse;
import com.winnguyen1905.orchestrator.model.response.RestResponse;
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
                    .orderDetails(order)
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
    
    /**
     * Handles OrderCreated event by trying to reserve inventory
     */
    @Transactional
    public void handleOrderCreated(OrderCreatedEvent event) {
        UUID sagaId = event.getSagaId();
        UUID orderId = event.getOrderId();
        
        log.info("Handling OrderCreated event for orderId: {}, sagaId: {}", orderId, sagaId);
        
        try {
            // Update saga state
            ESagaState sagaState = sagaStateRepository.findById(sagaId)
                    .orElseThrow(() -> new RuntimeException("Saga not found: " + sagaId));
            
            sagaState.setStatus(SagaStatus.PRODUCT_RESERVING);
            sagaState.setCurrentStep("RESERVE_INVENTORY");
            sagaStateRepository.save(sagaState);
            
            // Prepare reservation request
            List<ReservationRequest.ReservationItem> items = new ArrayList<>();
            OrderResponse orderDetails = event.getOrderDetails();
            
            for (OrderResponse.OrderItemResponse item : orderDetails.getOrderItems()) {
                items.add(ReservationRequest.ReservationItem.builder()
                        .sku(item.getProductSku())
                        .productId(item.getProductId())
                        .quantity(item.getQuantity())
                        .build());
            }
            
            ReservationRequest reservationRequest = ReservationRequest.builder()
                    .orderId(orderId)
                    .items(items)
                    .build();
            
            // Call Product service to reserve inventory
            RestResponse<ReservationResponse> response = productServiceClient.reserveInventory(reservationRequest).getBody();
            ReservationResponse reservationResponse = response.data();
            
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
                RestResponse<CouponValidationResponse> couponResponse = promotionServiceClient.validateCoupon(couponRequest).getBody();
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
} 
