package com.winnguyen1905.orchestrator.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.winnguyen1905.orchestrator.core.feign.OrderServiceClient;
import com.winnguyen1905.orchestrator.core.feign.PaymentServiceClient;
import com.winnguyen1905.orchestrator.core.feign.ProductServiceClient;
import com.winnguyen1905.orchestrator.core.feign.PromotionServiceClient;
import com.winnguyen1905.orchestrator.messaging.KafkaProducer;
import com.winnguyen1905.orchestrator.model.event.SagaEvent;
import com.winnguyen1905.orchestrator.persistance.repository.SagaStateRepository;
import com.winnguyen1905.orchestrator.service.DiscountSagaHandler;
import com.winnguyen1905.orchestrator.service.OrderSagaOrchestrator;
import com.winnguyen1905.orchestrator.service.SplitPaymentSagaHandler;

@Configuration
public class KafkaConfig {

  @Bean
  public KafkaTemplate<String, SagaEvent> kafkaTemplate(ProducerFactory<String, SagaEvent> producerFactory) {
    return new KafkaTemplate<>(producerFactory);
  }

  @Bean
  public OrderSagaOrchestrator orderSagaOrchestrator(
      SagaStateRepository sagaStateRepository,
      KafkaTemplate<String, SagaEvent> kafkaTemplate,
      ObjectMapper objectMapper,
      OrderServiceClient orderServiceClient,
      ProductServiceClient productServiceClient,
      PaymentServiceClient paymentServiceClient,
      PromotionServiceClient promotionServiceClient,
      KafkaProducer kafkaProducer) {

    return new OrderSagaOrchestrator(
        sagaStateRepository,
        kafkaTemplate,
        objectMapper,
        orderServiceClient,
        productServiceClient,
        paymentServiceClient,
        promotionServiceClient,
        kafkaProducer);
  }

  @Bean
  public DiscountSagaHandler discountSagaHandler(
      SagaStateRepository sagaStateRepository,
      KafkaTemplate<String, SagaEvent> kafkaTemplate,
      OrderServiceClient orderServiceClient,
      PromotionServiceClient promotionServiceClient) {

    return new DiscountSagaHandler(
        sagaStateRepository,
        kafkaTemplate,
        orderServiceClient,
        promotionServiceClient,
        "coupon-applied-events");
  }

  @Bean
  public SplitPaymentSagaHandler splitPaymentSagaHandler(
      SagaStateRepository sagaStateRepository,
      KafkaTemplate<String, SagaEvent> kafkaTemplate,
      OrderServiceClient orderServiceClient,
      PaymentServiceClient paymentServiceClient) {

    return new SplitPaymentSagaHandler(
        sagaStateRepository,
        kafkaTemplate,
        orderServiceClient,
        paymentServiceClient,
        "split-payment-processed-events");
  }
}
