package com.winnguyen1905.orchestrator.messaging;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;

import com.winnguyen1905.orchestrator.model.event.CouponAppliedEvent;
import com.winnguyen1905.orchestrator.model.event.OrderCreatedEvent;
import com.winnguyen1905.orchestrator.model.event.PaymentFailedEvent;
import com.winnguyen1905.orchestrator.model.event.PaymentProcessedEvent;
import com.winnguyen1905.orchestrator.model.event.ProductReservationFailedEvent;
import com.winnguyen1905.orchestrator.model.event.ProductReservedEvent;
import com.winnguyen1905.orchestrator.model.event.SplitPaymentEvent;
import com.winnguyen1905.orchestrator.service.DiscountSagaHandler;
import com.winnguyen1905.orchestrator.service.OrderSagaOrchestrator;
import com.winnguyen1905.orchestrator.service.SplitPaymentSagaHandler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Configuration
@EnableKafka
@RequiredArgsConstructor
@Slf4j
public class KafkaConsumerConfig {

  private final OrderSagaOrchestrator orchestrator;
  private final DiscountSagaHandler discountSagaHandler;
  private final SplitPaymentSagaHandler splitPaymentSagaHandler;

  @KafkaListener(topics = "${kafka.topics.order-created}", groupId = "${spring.kafka.consumer.group-id}")
  public void handleOrderCreatedEvent(@Payload OrderCreatedEvent event,
      @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
      @Header(KafkaHeaders.RECEIVED_PARTITION) int partition) {
    log.info("Received OrderCreatedEvent: {}, topic: {}, partition: {}", event, topic, partition);
    orchestrator.handleOrderCreated(event);
  }

  @KafkaListener(topics = "${kafka.topics.product-reserved}", groupId = "${spring.kafka.consumer.group-id}")
  public void handleProductReservedEvent(@Payload ProductReservedEvent event,
      @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
      @Header(KafkaHeaders.RECEIVED_PARTITION) int partition) {
    log.info("Received ProductReservedEvent: {}, topic: {}, partition: {}", event, topic, partition);
    orchestrator.handleProductReserved(event);
  }

  @KafkaListener(topics = "${kafka.topics.product-reservation-failed}", groupId = "${spring.kafka.consumer.group-id}")
  public void handleProductReservationFailedEvent(@Payload ProductReservationFailedEvent event,
      @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
      @Header(KafkaHeaders.RECEIVED_PARTITION) int partition) {
    log.info("Received ProductReservationFailedEvent: {}, topic: {}, partition: {}", event, topic, partition);
    orchestrator.handleProductReservationFailed(event);
  }

  @KafkaListener(topics = "${kafka.topics.payment-processed}", groupId = "${spring.kafka.consumer.group-id}")
  public void handlePaymentProcessedEvent(@Payload PaymentProcessedEvent event,
      @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
      @Header(KafkaHeaders.RECEIVED_PARTITION) int partition) {
    log.info("Received PaymentProcessedEvent: {}, topic: {}, partition: {}", event, topic, partition);
    orchestrator.handlePaymentProcessed(event);
  }

  @KafkaListener(topics = "${kafka.topics.payment-failed}", groupId = "${spring.kafka.consumer.group-id}")
  public void handlePaymentFailedEvent(@Payload PaymentFailedEvent event,
      @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
      @Header(KafkaHeaders.RECEIVED_PARTITION) int partition) {
    log.info("Received PaymentFailedEvent: {}, topic: {}, partition: {}", event, topic, partition);
    orchestrator.handlePaymentFailed(event);
  }

  @KafkaListener(topics = "${kafka.topics.coupon-applied}", groupId = "${spring.kafka.consumer.group-id}")
  public void handleCouponAppliedEvent(@Payload CouponAppliedEvent event,
      @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
      @Header(KafkaHeaders.RECEIVED_PARTITION) int partition) {
    log.info("Received CouponAppliedEvent: {}, topic: {}, partition: {}", event, topic, partition);
    // This would typically be handled in conjunction with an OrderCreatedEvent
    // For now, we're just logging it
  }

  @KafkaListener(topics = "${kafka.topics.split-payment-processed}", groupId = "${spring.kafka.consumer.group-id}")
  public void handleSplitPaymentEvent(@Payload SplitPaymentEvent event,
      @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
      @Header(KafkaHeaders.RECEIVED_PARTITION) int partition) {
    log.info("Received SplitPaymentEvent: {}, topic: {}, partition: {}", event, topic, partition);
    // This would typically be handled in conjunction with an OrderCreatedEvent
    // For now, we're just logging it
  }
}
