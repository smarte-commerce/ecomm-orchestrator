package com.winnguyen1905.orchestrator.core.feign;

import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.winnguyen1905.orchestrator.model.request.CouponValidationRequest;
import com.winnguyen1905.orchestrator.model.response.CouponValidationResponse;
import com.winnguyen1905.orchestrator.model.response.RestResponse;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;

@FeignClient(name = "promotion-service", url = "${microservices.promotion-service.url:http://promotion-service:8093}")
@CircuitBreaker(name = "promotionService")
@Retry(name = "promotionService")
public interface PromotionServiceClient {

    /**
     * Validate a coupon code for an order
     */
    @PostMapping("/api/promotions/coupons/validate")
    ResponseEntity<RestResponse<CouponValidationResponse>> validateCoupon(@RequestBody CouponValidationRequest request);
    
    /**
     * Cancel a coupon application
     */
    @PostMapping("/api/promotions/coupons/{couponId}/cancel")
    ResponseEntity<RestResponse<Void>> cancelCoupon(@PathVariable("couponId") UUID couponId, @RequestBody UUID orderId);
}
