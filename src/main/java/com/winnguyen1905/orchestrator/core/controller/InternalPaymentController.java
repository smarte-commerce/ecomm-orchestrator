package com.winnguyen1905.orchestrator.core.controller;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.winnguyen1905.orchestrator.model.event.PaymentFailedEvent;
import com.winnguyen1905.orchestrator.model.event.PaymentProcessedEvent;
import com.winnguyen1905.orchestrator.model.request.PaymentStatusUpdateRequest;
import com.winnguyen1905.orchestrator.model.response.RestResponse;
import com.winnguyen1905.orchestrator.service.OrderSagaOrchestrator;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
public class InternalPaymentController {

  private final OrderSagaOrchestrator orderSagaOrchestrator;

  /**
   * Internal endpoint for payment service to update payment status
   * This is called by the payment service when payment status changes
   */
  @PostMapping("/payment-status")
  public ResponseEntity<RestResponse<String>> updatePaymentStatus(
      @Valid @RequestBody PaymentStatusUpdateRequest request) {
    try {
      log.info("Received payment status update for orderId: {}, paymentId: {}, status: {}",
          request.getOrderId(), request.getPaymentId(), request.getPaymentStatus());

      // Process based on payment status
      switch (request.getPaymentStatus().toUpperCase()) {
        case "COMPLETED":
        case "PAID":
          handlePaymentSuccess(request);
          break;
        case "FAILED":
        case "DECLINED":
        case "CANCELLED":
          handlePaymentFailure(request);
          break;
        case "PENDING":
        case "PROCESSING":
          // For pending status, we might just log or update tracking
          log.info("Payment is still processing for orderId: {}", request.getOrderId());
          break;
        default:
          log.warn("Unknown payment status: {} for orderId: {}",
              request.getPaymentStatus(), request.getOrderId());
      }

      return ResponseEntity.ok(
          RestResponse.<String>builder()
              .statusCode(HttpStatus.OK.value())
              .message("Payment status updated successfully")
              .data("Status: " + request.getPaymentStatus())
              .build());

    } catch (Exception e) {
      log.error("Failed to process payment status update for orderId: " + request.getOrderId(), e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(RestResponse.<String>builder()
              .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
              .error("Failed to process payment status update")
              .message(e.getMessage())
              .build());
    }
  }

  private void handlePaymentSuccess(PaymentStatusUpdateRequest request) {
    // Create PaymentProcessedEvent
    PaymentProcessedEvent event = PaymentProcessedEvent.builder()
        .eventId(UUID.randomUUID())
        .sagaId(UUID.randomUUID()) // This should be retrieved based on orderId
        .orderId(request.getOrderId())
        .eventType("PaymentProcessed")
        .timestamp(Instant.now())
        .retryCount(0)
        .correlationId(UUID.randomUUID())
        .causationId(UUID.randomUUID())
        .paymentId(request.getPaymentId())
        .amount(request.getAmount() != null ? request.getAmount().doubleValue() : 0.0)
        .transactionId(request.getTransactionId())
        .paymentMethod(request.getPaymentMethod())
        .processedAt(request.getProcessedAt() != null ? request.getProcessedAt() : Instant.now())
        .build();

    // Handle the payment processed event
    orderSagaOrchestrator.handlePaymentProcessed(event);
  }

  private void handlePaymentFailure(PaymentStatusUpdateRequest request) {
    // Create PaymentFailedEvent
    PaymentFailedEvent event = PaymentFailedEvent.builder()
        .eventId(UUID.randomUUID())
        .sagaId(UUID.randomUUID()) // This should be retrieved based on orderId
        .orderId(request.getOrderId())
        .eventType("PaymentFailed")
        .timestamp(Instant.now())
        .retryCount(0)
        .correlationId(UUID.randomUUID())
        .causationId(UUID.randomUUID())
        .paymentId(request.getPaymentId())
        .amount(request.getAmount() != null ? request.getAmount() : BigDecimal.ZERO)
        .paymentMethod(request.getPaymentMethod())
        .errorMessage(request.getFailureReason() != null ? request.getFailureReason() : "Payment failed")
        .failedAt(request.getProcessedAt() != null ? request.getProcessedAt() : Instant.now())
        .build();

    // Handle the payment failed event
    orderSagaOrchestrator.handlePaymentFailed(event);
  }
}
