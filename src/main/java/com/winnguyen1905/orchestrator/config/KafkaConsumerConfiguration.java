package com.winnguyen1905.orchestrator.config;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import com.winnguyen1905.orchestrator.model.event.SagaEvent;

@Configuration
@EnableKafka
public class KafkaConsumerConfiguration {

  @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
  private String bootstrapServers;

  private Map<String, Object> getCommonConsumerProps() {
    Map<String, Object> props = new HashMap<>();
    // Force localhost instead of kafka hostname
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:29092");
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
    props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

    // Add these properties to ensure proper hostname resolution
    props.put(ConsumerConfig.CLIENT_DNS_LOOKUP_CONFIG, "use_all_dns_ips");
    props.put(ConsumerConfig.METADATA_MAX_AGE_CONFIG, 300000);
    props.put(ConsumerConfig.RECONNECT_BACKOFF_MS_CONFIG, 1000);
    props.put(ConsumerConfig.RETRY_BACKOFF_MS_CONFIG, 100);

    // Connection timeout settings
    props.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 3000);
    props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 10000);
    props.put(ConsumerConfig.REQUEST_TIMEOUT_MS_CONFIG, 30000);

    // JSON configuration
    props.put(JsonDeserializer.TYPE_MAPPINGS,
        "event:com.winnguyen1905.orchestrator.model.event.SagaEvent");
    props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.winnguyen1905.*");

    return props;
  }

  @Bean
  public ConsumerFactory<String, SagaEvent> consumerFactory() {
    Map<String, Object> props = getCommonConsumerProps();
    props.put(ConsumerConfig.GROUP_ID_CONFIG, "orchestrator-group");
    return new DefaultKafkaConsumerFactory<>(props);
  }

  @Bean
  public ConsumerFactory<String, SagaEvent> orderConsumerFactory() {
    Map<String, Object> props = getCommonConsumerProps();
    props.put(ConsumerConfig.GROUP_ID_CONFIG, "order");
    return new DefaultKafkaConsumerFactory<>(props);
  }

  @Bean
  public ConsumerFactory<String, SagaEvent> stockConsumerFactory() {
    Map<String, Object> props = getCommonConsumerProps();
    props.put(ConsumerConfig.GROUP_ID_CONFIG, "stock");
    return new DefaultKafkaConsumerFactory<>(props);
  }

  @Bean
  public ConsumerFactory<String, SagaEvent> paymentConsumerFactory() {
    Map<String, Object> props = getCommonConsumerProps();
    props.put(ConsumerConfig.GROUP_ID_CONFIG, "payment");
    return new DefaultKafkaConsumerFactory<>(props);
  }

  @Bean
  public ConsumerFactory<String, SagaEvent> promotionConsumerFactory() {
    Map<String, Object> props = getCommonConsumerProps();
    props.put(ConsumerConfig.GROUP_ID_CONFIG, "promotion");
    return new DefaultKafkaConsumerFactory<>(props);
  }

  @Bean
  public ConcurrentKafkaListenerContainerFactory<String, SagaEvent> kafkaListenerContainerFactory() {
    ConcurrentKafkaListenerContainerFactory<String, SagaEvent> factory = new ConcurrentKafkaListenerContainerFactory<>();
    factory.setConsumerFactory(consumerFactory());
    return factory;
  }

  @Bean
  public ConcurrentKafkaListenerContainerFactory<String, SagaEvent> orderKafkaListenerContainerFactory() {
    ConcurrentKafkaListenerContainerFactory<String, SagaEvent> factory = new ConcurrentKafkaListenerContainerFactory<>();
    factory.setConsumerFactory(orderConsumerFactory());
    return factory;
  }

  @Bean
  public ConcurrentKafkaListenerContainerFactory<String, SagaEvent> stockKafkaListenerContainerFactory() {
    ConcurrentKafkaListenerContainerFactory<String, SagaEvent> factory = new ConcurrentKafkaListenerContainerFactory<>();
    factory.setConsumerFactory(stockConsumerFactory());
    return factory;
  }

  @Bean
  public ConcurrentKafkaListenerContainerFactory<String, SagaEvent> paymentKafkaListenerContainerFactory() {
    ConcurrentKafkaListenerContainerFactory<String, SagaEvent> factory = new ConcurrentKafkaListenerContainerFactory<>();
    factory.setConsumerFactory(paymentConsumerFactory());
    return factory;
  }

  @Bean
  public ConcurrentKafkaListenerContainerFactory<String, SagaEvent> promotionKafkaListenerContainerFactory() {
    ConcurrentKafkaListenerContainerFactory<String, SagaEvent> factory = new ConcurrentKafkaListenerContainerFactory<>();
    factory.setConsumerFactory(promotionConsumerFactory());
    return factory;
  }
}
