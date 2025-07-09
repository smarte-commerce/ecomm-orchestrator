package com.winnguyen1905.orchestrator.messaging;

import com.winnguyen1905.orchestrator.model.event.*;
import com.winnguyen1905.orchestrator.service.OrderSagaOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaConsumer {

  private final OrderSagaOrchestrator orderSagaOrchestrator;

  @KafkaListener(topics = "${topic.name.order.created}", groupId = "${spring.kafka.consumer.order-group-id}")
  public void consumeOrderCreated(OrderCreatedEvent event) {
    log.info("Order created event received: {}", event);
    // Use the new enhanced order creation flow
    orderSagaOrchestrator.handleCreateOrder_New(event);
  }

  @KafkaListener(topics = "${topic.name.stock.reserved}", groupId = "${spring.kafka.consumer.stock-group-id}")
  public void consumeProductReserved(ProductReservedEvent event) {
    log.info("Product reserved event received: {}", event);
    orderSagaOrchestrator.processProductReserved(event);
  }

  @KafkaListener(topics = "${topic.name.stock.cancel}", groupId = "${spring.kafka.consumer.stock-group-id}")
  public void consumeProductReservationFailed(ProductReservationFailedEvent event) {
    log.info("Product reservation failed event received: {}", event);
    orderSagaOrchestrator.processProductReservationFailed(event);
  }

  @KafkaListener(topics = "${topic.name.payment.out}", groupId = "${spring.kafka.consumer.payment-group-id}")
  public void consumePaymentProcessed(PaymentProcessedEvent event) {
    log.info("Payment processed event received: {}", event);
    orderSagaOrchestrator.processPaymentProcessed(event);
  }

  // New listener for discount/promotion service
  @KafkaListener(topics = "${topic.name.promotion.out}", groupId = "${spring.kafka.consumer.promotion-group-id}")
  public void consumeCouponApplied(CouponAppliedEvent event) {
    log.info("Coupon applied event received: {}", event);
    orderSagaOrchestrator.processCouponApplied(event);
  }
}
