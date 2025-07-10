package com.winnguyen1905.orchestrator.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.winnguyen1905.orchestrator.core.feign.OrderServiceClient;
import com.winnguyen1905.orchestrator.core.feign.PaymentServiceClient;
import com.winnguyen1905.orchestrator.model.event.OrderCreatedEvent;
import com.winnguyen1905.orchestrator.model.event.SagaEvent;
import com.winnguyen1905.orchestrator.model.event.SplitPaymentEvent;
import com.winnguyen1905.orchestrator.model.request.SplitPaymentRequest;
import com.winnguyen1905.orchestrator.model.request.SplitPaymentRequest.PaymentMethod;
import com.winnguyen1905.orchestrator.model.response.OrderResponse;
import com.winnguyen1905.orchestrator.model.response.RestResponse;
import com.winnguyen1905.orchestrator.model.response.SplitPaymentResponse;
import com.winnguyen1905.orchestrator.persistance.entity.ESagaState;
import com.winnguyen1905.orchestrator.persistance.entity.ESagaState.SagaStatus;
import com.winnguyen1905.orchestrator.persistance.repository.SagaStateRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class SplitPaymentSagaHandler {

  private final SagaStateRepository sagaStateRepository;
  private final KafkaTemplate<String, SagaEvent> kafkaTemplate;
  private final OrderServiceClient orderServiceClient;
  private final PaymentServiceClient paymentServiceClient;

  // Kafka topic for split payment events
  private final String splitPaymentProcessedTopic;

  /**
   * Processes a split payment for an order
   */
  @Transactional
  public UUID processSplitPayment(UUID orderId, List<PaymentMethod> paymentMethods) {
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
          .currentStep("PROCESS_SPLIT_PAYMENT")
          .build();

      sagaStateRepository.save(sagaState);

      // Validate the total amount matches
      BigDecimal totalOrderAmount = order.getTotalAmount();
      BigDecimal totalPaymentAmount = paymentMethods.stream()
          .map(PaymentMethod::getAmount)
          .reduce(BigDecimal.ZERO, BigDecimal::add);

      if (totalOrderAmount.subtract(totalPaymentAmount).abs().compareTo(new BigDecimal("0.01")) > 0) {
        sagaState.setStatus(SagaStatus.FAILED);
        sagaState.setErrorMessage("Payment amount mismatch: Order total is " +
            totalOrderAmount + " but payment total is " + totalPaymentAmount);
        sagaStateRepository.save(sagaState);

        throw new RuntimeException("Payment amount mismatch: Order total is " +
            totalOrderAmount + " but payment total is " + totalPaymentAmount);
      }

      // Create split payment request
      SplitPaymentRequest splitPaymentRequest = SplitPaymentRequest.builder()
          .orderId(orderId)
          .customerId(order.getCustomerId())
          .totalAmount(totalOrderAmount)
          .paymentMethods(paymentMethods)
          .build();

      // Process the split payment
      RestResponse<SplitPaymentResponse> paymentResponse = paymentServiceClient.processSplitPayment(splitPaymentRequest)
          .getBody();
      SplitPaymentResponse splitPaymentResponse = paymentResponse.data();

      if (splitPaymentResponse == null || !splitPaymentResponse.isSuccess()) {
        sagaState.setStatus(SagaStatus.FAILED);
        sagaState.setErrorMessage("Failed to process split payment");
        sagaStateRepository.save(sagaState);

        throw new RuntimeException("Failed to process split payment");
      }

      // Create payment parts for the event
      List<SplitPaymentEvent.PaymentPart> paymentParts = new ArrayList<>();
      for (SplitPaymentResponse.PaymentResult result : splitPaymentResponse.getPaymentResults()) {
        SplitPaymentEvent.PaymentPart part = SplitPaymentEvent.PaymentPart.builder()
            .paymentMethod(result.getType())
            .paymentId(result.getPaymentId())
            .transactionId(result.getTransactionId())
            .amount(result.getAmount())
            .status(result.getStatus())
            .build();
        paymentParts.add(part);
      }

      // Create and publish SplitPayment event
      SplitPaymentEvent event = SplitPaymentEvent.builder()
          .eventId(UUID.randomUUID())
          .sagaId(sagaId)
          .orderId(orderId)
          .eventType("SplitPayment")
          .timestamp(Instant.now())
          .retryCount(0)
          .correlationId(sagaId)
          .payments(paymentParts)
          .totalAmount(totalOrderAmount)
          .completed(true)
          .processedAt(splitPaymentResponse.getProcessedAt())
          .build();

      kafkaTemplate.send(splitPaymentProcessedTopic, event);

      // Update order status to payment completed
      orderServiceClient.updateOrderStatus(orderId, "PAYMENT_COMPLETED",
          "Split payment processed successfully");

      // Update saga state to completed
      sagaState.setStatus(SagaStatus.COMPLETED);
      sagaState.setCurrentStep("SPLIT_PAYMENT_COMPLETED");
      sagaState.setCompletedAt(Instant.now());
      sagaStateRepository.save(sagaState);

      log.info("Split payment processed for order: {}, sagaId: {}, amount: {}",
          orderId, sagaId, totalOrderAmount);

      return sagaId;

    } catch (Exception e) {
      log.error("Failed to process split payment for order: " + orderId, e);
      throw new RuntimeException("Failed to process split payment", e);
    }
  }

  /**
   * Handles a split payment event during order processing
   */
  @Transactional
  public void handleSplitPayment(SplitPaymentEvent event, OrderCreatedEvent orderEvent) {
    UUID sagaId = orderEvent.getSagaId();
    UUID orderId = orderEvent.getOrderId();

    log.info("Handling SplitPayment event for orderId: {}, sagaId: {}", orderId, sagaId);

    try {
      // Get the order saga state
      ESagaState sagaState = sagaStateRepository.findById(sagaId)
          .orElseThrow(() -> new RuntimeException("Saga not found: " + sagaId));

      // Update the saga state
      sagaState.setStatus(SagaStatus.PAYMENT_PROCESSED);
      sagaState.setCurrentStep("APPROVE_ORDER");
      sagaStateRepository.save(sagaState);

      // Continue with the order flow
      // This would typically call the product reservation confirmation
      // Similar to OrderSagaOrchestrator.handlePaymentProcessed

    } catch (Exception e) {
      log.error("Error handling SplitPayment event for orderId: " + orderId, e);
    }
  }
}
