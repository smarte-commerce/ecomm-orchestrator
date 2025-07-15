package com.winnguyen1905.orchestrator.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShippingQuoteResponse {

  private String quoteId;

  private Instant quotedAt;

  private Instant expiresAt;

  private String requestType; // SINGLE_VENDOR, MULTI_VENDOR

  private List<ShippingOption> shippingOptions;

  private ShippingOption recommendedOption; // Best overall option

  private ShippingOption cheapestOption; // Lowest cost option

  private ShippingOption fastestOption; // Quickest delivery option

  private QuoteMetadata metadata;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ShippingOption {

    private String provider; // DHL, VNPost, Giao Hang Nhanh, etc.

    private String service; // Express, Economy, Standard, etc.

    private BigDecimal cost;

    private String currency;

    private Integer estimatedDays;

    private LocalDate estimatedDeliveryDate;

    private String serviceCode; // Internal service identifier

    private String trackingSupported;

    private Boolean insuranceIncluded;

    private BigDecimal insuranceCost;

    private BigDecimal maxInsuranceValue;

    private String deliveryType; // DOOR_TO_DOOR, PICKUP_POINT, etc.

    private List<String> features; // Signature required, weekend delivery, etc.

    private String providerLogo;

    private String estimatedPickupTime;

    private ProviderRating rating;

    private List<String> restrictions; // Size, weight, content restrictions

    private String vendorId; // For multi-vendor scenarios

    private String vendorName; // For multi-vendor scenarios
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ProviderRating {

    private Double overallRating; // 1.0 to 5.0

    private Double deliveryTimeRating;

    private Double reliabilityRating;

    private Double customerServiceRating;

    private Integer totalReviews;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class QuoteMetadata {

    private String originCountry;

    private String destinationCountry;

    private Boolean isDomestic;

    private Boolean requiresCustoms;

    private Integer totalPackages;

    private BigDecimal totalWeight;

    private BigDecimal totalDeclaredValue;

    private String baseCurrency;

    private List<String> availableProviders;

    private List<String> unavailableProviders;

    private List<ProviderError> providerErrors;

    private String calculationMethod; // EXTERNAL_API, INTERNAL_CALCULATION, HYBRID
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ProviderError {

    private String provider;

    private String errorCode;

    private String errorMessage;

    private String fallbackUsed;
  }

  // Utility methods
  public ShippingOption getCheapestValidOption() {
    return shippingOptions != null ? shippingOptions.stream()
        .filter(option -> option.getCost() != null)
        .min((o1, o2) -> o1.getCost().compareTo(o2.getCost()))
        .orElse(null) : null;
  }

  public ShippingOption getFastestValidOption() {
    return shippingOptions != null ? shippingOptions.stream()
        .filter(option -> option.getEstimatedDays() != null)
        .min((o1, o2) -> o1.getEstimatedDays().compareTo(o2.getEstimatedDays()))
        .orElse(null) : null;
  }

  public List<ShippingOption> getOptionsForVendor(String vendorId) {
    return shippingOptions != null ? shippingOptions.stream()
        .filter(option -> vendorId.equals(option.getVendorId()))
        .toList() : List.of();
  }

  public boolean hasValidOptions() {
    return shippingOptions != null && !shippingOptions.isEmpty();
  }
} 
