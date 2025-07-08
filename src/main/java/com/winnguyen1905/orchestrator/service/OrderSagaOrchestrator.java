package com.winnguyen1905.orchestrator.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.winnguyen1905.orchestrator.core.feign.OrderServiceClient;
import com.winnguyen1905.orchestrator.core.feign.PaymentServiceClient;
import com.winnguyen1905.orchestrator.core.feign.ProductServiceClient;
import com.winnguyen1905.orchestrator.model.event.OrderApprovedEvent;
import com.winnguyen1905.orchestrator.model.event.OrderCancelledEvent;
import com.winnguyen1905.orchestrator.model.event.OrderCreatedEvent;
import com.winnguyen1905.orchestrator.model.event.PaymentFailedEvent;
import com.winnguyen1905.orchestrator.model.event.PaymentProcessedEvent;
import com.winnguyen1905.orchestrator.model.event.ProductReservationFailedEvent;
import com.winnguyen1905.orchestrator.model.event.ProductReservedEvent;
import com.winnguyen1905.orchestrator.model.event.SagaEvent;
import com.winnguyen1905.orchestrator.model.request.PaymentRequest;
import com.winnguyen1905.orchestrator.model.request.RefundRequest;
import com.winnguyen1905.orchestrator.model.request.ReservationRequest;
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
    
    // Kafka topics
    private final String orderCreatedTopic;
    private final String productReservedTopic;
    private final String productReservationFailedTopic;
    private final String paymentProcessedTopic;
    private final String paymentFailedTopic;
    private final String orderApprovedTopic;
    private final String orderCancelledTopic;
    
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
     * Handles ProductReserved event by processing payment
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
            sagaState.setCurrentStep("PROCESS_PAYMENT");
            sagaStateRepository.save(sagaState);
            
            // Publish ProductReserved event to Kafka
            kafkaTemplate.send(productReservedTopic, event);
            
            // Get order details
            RestResponse<OrderResponse> response = orderServiceClient.getOrderById(orderId).getBody();
            OrderResponse order = response.data();
            
            if (order == null) {
                throw new RuntimeException("Order not found: " + orderId);
            }
            
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
                        .causationId(event.getEventId())
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
                        .causationId(event.getEventId())
                        .amount(order.getTotalAmount())
                        .errorCode(payment != null ? "PAYMENT_FAILED" : "PAYMENT_SERVICE_ERROR")
                        .errorMessage(payment != null ? payment.getErrorMessage() : "Payment service error")
                        .paymentMethod("CREDIT_CARD")
                        .failedAt(Instant.now())
                        .build());
            }
        } catch (Exception e) {
            log.error("Error handling ProductReserved event for orderId: " + orderId, e);
            
            // Update saga state to failed
            ESagaState sagaState = sagaStateRepository.findById(sagaId)
                    .orElseThrow(() -> new RuntimeException("Saga not found: " + sagaId));
            
            sagaState.setStatus(SagaStatus.FAILED);
            sagaState.setErrorMessage("Failed to process payment: " + e.getMessage());
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
                    .reason("Failed to process payment: " + e.getMessage())
                    .cancelledBy("SYSTEM")
                    .shouldRefund(false)
                    .shouldReleaseInventory(true)
                    .build();
            
            kafkaTemplate.send(orderCancelledTopic, cancelEvent);
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
            // In a real implementation, we would iterate through the order items
            
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
            
            // Start compensation - release inventory
            // In a real implementation, we would call the Product service to release the inventory
            
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