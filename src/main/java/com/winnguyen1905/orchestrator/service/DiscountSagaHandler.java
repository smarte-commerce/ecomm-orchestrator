package com.winnguyen1905.orchestrator.service;

import java.util.UUID;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.winnguyen1905.orchestrator.core.feign.OrderServiceClient;
import com.winnguyen1905.orchestrator.core.feign.PromotionServiceClient;
import com.winnguyen1905.orchestrator.model.event.SagaEvent;
import com.winnguyen1905.orchestrator.persistance.repository.SagaStateRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class DiscountSagaHandler {

  private final SagaStateRepository sagaStateRepository;
  private final KafkaTemplate<String, SagaEvent> kafkaTemplate;
  private final OrderServiceClient orderServiceClient;
  private final PromotionServiceClient promotionServiceClient;
  private final String topicName;

  public DiscountSagaHandler(
      SagaStateRepository sagaStateRepository,
      KafkaTemplate<String, SagaEvent> kafkaTemplate,
      OrderServiceClient orderServiceClient,
      PromotionServiceClient promotionServiceClient,
      String topicName) {
    this.sagaStateRepository = sagaStateRepository;
    this.kafkaTemplate = kafkaTemplate;
    this.orderServiceClient = orderServiceClient;
    this.promotionServiceClient = promotionServiceClient;
    this.topicName = topicName;
  }

  /**
   * Apply coupon to order - creates a discount-specific saga
   */
  public UUID applyCouponToOrder(UUID orderId, String couponCode) {
    log.info("Applying coupon {} to order {} via discount saga", couponCode, orderId);

    // Generate saga ID for this discount operation
    UUID sagaId = UUID.randomUUID();

    // For now, return the saga ID
    // TODO: Implement actual discount saga logic here
    log.warn("DiscountSagaHandler.applyCouponToOrder is not fully implemented yet");

    return sagaId;
  }
}
