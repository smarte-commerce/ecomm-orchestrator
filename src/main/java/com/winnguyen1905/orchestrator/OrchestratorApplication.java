package com.winnguyen1905.orchestrator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableKafka
@EnableRetry
@EnableAsync
@EnableFeignClients
@SpringBootApplication
@EnableDiscoveryClient
public class OrchestratorApplication {
  public static void main(String[] args) {
    SpringApplication.run(OrchestratorApplication.class, args);
  }
}
