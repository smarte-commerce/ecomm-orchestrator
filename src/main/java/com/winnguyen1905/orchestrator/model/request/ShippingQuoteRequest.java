package com.winnguyen1905.orchestrator.model.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShippingQuoteRequest {
  @NotNull(message = "Customer information is required")
  @Valid
  private CustomerInfo customer;

  @Valid
  private VendorInfo vendor; // For single vendor scenario

  @Valid
  private List<VendorPackageInfo> vendorPackages; // For multi-vendor scenario

  @Valid
  private PackageInfo packageInfo; // For single vendor scenario

  private Boolean requireInsurance;

  private String preferredCurrency;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class CustomerInfo {

    @NotBlank(message = "Customer name is required")
    @Size(max = 255, message = "Customer name must not exceed 255 characters")
    private String name;

    @NotNull(message = "Customer address is required")
    @Valid
    private AddressInfo address;

    @Email(message = "Valid email is required")
    private String email;

    @Pattern(regexp = "^[+]?[0-9]{10,15}$", message = "Valid phone number is required")
    private String phone;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class VendorInfo {

    @NotBlank(message = "Vendor name is required")
    @Size(max = 255, message = "Vendor name must not exceed 255 characters")
    private String name;

    @NotNull(message = "Vendor address is required")
    @Valid
    private AddressInfo address;

    private String contactEmail;

    private String contactPhone;

    private String vendorId; // Internal vendor identifier
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class VendorPackageInfo {

    @NotNull(message = "Vendor information is required")
    @Valid
    private VendorInfo vendor;

    @NotNull(message = "Package information is required")
    @Valid
    private PackageInfo packageInfo;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class AddressInfo {

    @NotBlank(message = "Country is required")
    @Size(min = 2, max = 3, message = "Country code must be 2-3 characters")
    private String country;

    @NotBlank(message = "City is required")
    @Size(max = 100, message = "City must not exceed 100 characters")
    private String city;

    @NotBlank(message = "ZIP/Postal code is required")
    @Size(max = 20, message = "ZIP code must not exceed 20 characters")
    private String zip;

    @NotBlank(message = "Street address is required")
    @Size(max = 255, message = "Street address must not exceed 255 characters")
    private String street;

    @Size(max = 100, message = "State must not exceed 100 characters")
    private String state;

    @Size(max = 255, message = "Address line 2 must not exceed 255 characters")
    private String addressLine2;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class PackageInfo {

    @NotNull(message = "Package weight is required")
    @DecimalMin(value = "0.01", message = "Weight must be greater than 0")
    @DecimalMax(value = "1000.0", message = "Weight must not exceed 1000 kg")
    private BigDecimal weight; // in kg

    @NotNull(message = "Package dimensions are required")
    @Valid
    private DimensionsInfo dimensions;

    @NotBlank(message = "Package type is required")
    @Size(max = 50, message = "Package type must not exceed 50 characters")
    private String type; // box, envelope, tube, etc.

    @NotNull(message = "Declared value is required")
    @DecimalMin(value = "0.01", message = "Declared value must be greater than 0")
    private BigDecimal declaredValue;

    @NotBlank(message = "Currency is required")
    @Size(min = 3, max = 3, message = "Currency must be 3 characters (ISO code)")
    private String currency;

    @Builder.Default
    private Boolean isFragile = false;

    @Builder.Default
    private Boolean isLiquid = false;

    @Builder.Default
    private Boolean isHazardous = false;

    @Size(max = 500, message = "Special instructions must not exceed 500 characters")
    private String specialInstructions;

    private String contentDescription;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class DimensionsInfo {

    @NotNull(message = "Length is required")
    @DecimalMin(value = "0.1", message = "Length must be greater than 0")
    @DecimalMax(value = "300.0", message = "Length must not exceed 300 cm")
    private BigDecimal length; // in cm

    @NotNull(message = "Width is required")
    @DecimalMin(value = "0.1", message = "Width must be greater than 0")
    @DecimalMax(value = "300.0", message = "Width must not exceed 300 cm")
    private BigDecimal width; // in cm

    @NotNull(message = "Height is required")
    @DecimalMin(value = "0.1", message = "Height must be greater than 0")
    @DecimalMax(value = "300.0", message = "Height must not exceed 300 cm")
    private BigDecimal height; // in cm

    @Builder.Default
    private String unit = "cm";
  }

  // Helper methods for validation
  public boolean isSingleVendorRequest() {
    return vendor != null && packageInfo != null && (vendorPackages == null || vendorPackages.isEmpty());
  }

  public boolean isMultiVendorRequest() {
    return vendorPackages != null && !vendorPackages.isEmpty() && vendor == null && packageInfo == null;
  }

  public boolean isValidRequest() {
    return isSingleVendorRequest() || isMultiVendorRequest();
  }
} 
