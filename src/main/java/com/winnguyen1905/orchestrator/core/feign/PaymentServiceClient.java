package com.winnguyen1905.orchestrator.core.feign;

import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.winnguyen1905.orchestrator.model.request.PaymentRequest;
import com.winnguyen1905.orchestrator.model.request.RefundRequest;
import com.winnguyen1905.orchestrator.model.request.SplitPaymentRequest;
import com.winnguyen1905.orchestrator.model.response.PaymentResponse;
import com.winnguyen1905.orchestrator.model.response.RefundResponse;
import com.winnguyen1905.orchestrator.model.response.RestResponse;
import com.winnguyen1905.orchestrator.model.response.SplitPaymentResponse;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;

@FeignClient(name = "PAYMENT-SERVICE", url = "${microservices.payment-service.url}")
@CircuitBreaker(name = "paymentService")
@Retry(name = "paymentService")
public interface PaymentServiceClient {

    /**
     * Process payment
     */
    @PostMapping("/api/payments/process")
    ResponseEntity<RestResponse<PaymentResponse>> processPayment(@RequestBody PaymentRequest request);

    /**
     * Issue refund
     */
    @PostMapping("/api/payments/refund/{paymentId}")
    ResponseEntity<RestResponse<RefundResponse>> issueRefund(
            @PathVariable("paymentId") UUID paymentId,
            @RequestBody RefundRequest request);
            
    /**
     * Process split payment
     */
    @PostMapping("/api/payments/split")
    ResponseEntity<RestResponse<SplitPaymentResponse>> processSplitPayment(
            @RequestBody SplitPaymentRequest request);
} 
