package com.winnguyen1905.orchestrator.core.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import com.winnguyen1905.orchestrator.model.request.ShippingQuoteRequest;
import com.winnguyen1905.orchestrator.model.response.RestResponse;
import com.winnguyen1905.orchestrator.model.response.ShippingQuoteResponse;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import jakarta.validation.Valid;

import java.util.List;
import java.util.Map;

@FeignClient(name = "shipping-service", url = "${microservices.shipping-service.url:http://shipping-service:8094}")
@CircuitBreaker(name = "shippingService")
@Retry(name = "shippingService")
public interface ShippingServiceClient {

  /**
   * Calculate shipping quotes for all available providers
   */
  @PostMapping("/api/v1/shipping/quotes/calculate")
  ResponseEntity<RestResponse<ShippingQuoteResponse>> calculateShippingQuotes(
      @Valid @RequestBody ShippingQuoteRequest request);

  /**
   * Calculate shipping costs for checkout - always fresh calculation
   */
  @PostMapping("/api/v1/shipping/quotes/checkout/calculate")
  ResponseEntity<RestResponse<ShippingQuoteResponse>> calculateCheckoutShipping(
      @Valid @RequestBody ShippingQuoteRequest request,
      @RequestParam(defaultValue = "true") boolean forceFresh);

  /**
   * Get quick shipping estimate with limited providers (faster response)
   */
  @PostMapping("/api/v1/shipping/quotes/quick-estimate")
  ResponseEntity<RestResponse<ShippingQuoteResponse>> getQuickEstimate(
      @Valid @RequestBody ShippingQuoteRequest request,
      @RequestParam(defaultValue = "3") int maxProviders);

  /**
   * Review shipping costs for cart
   */
  @PostMapping("/api/v1/shipping/quotes/cart/review")
  ResponseEntity<RestResponse<ShippingQuoteResponse>> reviewCartShipping(
      @Valid @RequestBody ShippingQuoteRequest request,
      @RequestParam(defaultValue = "true") boolean allowCached);

  /**
   * Get quotes from specific provider
   */
  @PostMapping("/api/v1/shipping/quotes/providers/{providerName}")
  ResponseEntity<RestResponse<List<ShippingQuoteResponse.ShippingOption>>> getQuotesFromProvider(
      @RequestParam String providerName,
      @Valid @RequestBody ShippingQuoteRequest request);

  /**
   * Get available providers for a route
   */
  @GetMapping("/api/v1/shipping/quotes/providers")
  ResponseEntity<RestResponse<List<String>>> getAvailableProviders(
      @RequestParam String originCountry,
      @RequestParam String destinationCountry);

  /**
   * Validate shipping quote request
   */
  @PostMapping("/api/v1/shipping/quotes/validate")
  ResponseEntity<RestResponse<ShippingValidationResult>> validateShippingQuoteRequest(
      @Valid @RequestBody ShippingQuoteRequest request);

  /**
   * Get provider status
   */
  @GetMapping("/api/v1/shipping/quotes/providers/status")
  ResponseEntity<RestResponse<Map<String, Object>>> getProviderStatus();

  /**
   * Test provider connectivity
   */
  @PostMapping("/api/v1/shipping/quotes/providers/test-connectivity")
  ResponseEntity<RestResponse<Map<String, Boolean>>> testProviderConnectivity();

  /**
   * DTO for shipping validation result
   */
  public static class ShippingValidationResult {
    private boolean valid;
    private List<String> errors;
    private List<String> warnings;

    public ShippingValidationResult() {
    }

    public ShippingValidationResult(boolean valid, List<String> errors, List<String> warnings) {
      this.valid = valid;
      this.errors = errors;
      this.warnings = warnings;
    }

    public boolean isValid() {
      return valid;
    }

    public void setValid(boolean valid) {
      this.valid = valid;
    }

    public List<String> getErrors() {
      return errors;
    }

    public void setErrors(List<String> errors) {
      this.errors = errors;
    }

    public List<String> getWarnings() {
      return warnings;
    }

    public void setWarnings(List<String> warnings) {
      this.warnings = warnings;
    }
  }
}
