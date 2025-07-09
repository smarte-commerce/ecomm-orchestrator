package com.winnguyen1905.orchestrator.core.feign;

import java.util.List;
import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.winnguyen1905.orchestrator.model.request.ComprehensiveDiscountRequest;
import com.winnguyen1905.orchestrator.model.request.CouponValidationRequest;
import com.winnguyen1905.orchestrator.model.response.ComprehensiveDiscountResponse;
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

  /**
   * Apply comprehensive discounts including shop-level, shipping, and global
   * discounts
   */
  @PostMapping("/api/promotions/discounts/apply-comprehensive")
  ResponseEntity<RestResponse<ComprehensiveDiscountResponse>> applyComprehensiveDiscounts(
      @RequestBody ComprehensiveDiscountRequest request);

  /**
   * Validate shop product discount eligibility
   */
  @PostMapping("/api/promotions/discounts/validate-shop/{shopId}")
  ResponseEntity<RestResponse<Boolean>> validateShopDiscountEligibility(
      @PathVariable("shopId") UUID shopId,
      @RequestBody CouponValidationRequest request);

  /**
   * Validate global discount applicability for shops
   */
  @PostMapping("/api/promotions/discounts/validate-global-eligibility")
  ResponseEntity<RestResponse<List<UUID>>> validateGlobalDiscountEligibility(
      @RequestBody ComprehensiveDiscountRequest request);

  /**
   * Cancel multiple discounts in batch
   */
  @PostMapping("/api/promotions/discounts/cancel-batch")
  ResponseEntity<RestResponse<Void>> cancelDiscountsBatch(@RequestBody List<UUID> discountIds);
}
