# Shipping Service Integration Summary

## Overview
Successfully integrated the shipping service into the order orchestrator to calculate shipping costs during the order checkout process.

## Components Added

### 1. Feign Client
- **File**: `orchestrator/src/main/java/com/winnguyen1905/orchestrator/core/feign/ShippingServiceClient.java`
- **Purpose**: Provides interface to communicate with shipping service
- **Key Methods**:
  - `calculateCheckoutShipping()` - Calculate shipping for checkout (fresh quotes)
  - `calculateShippingQuotes()` - Get shipping quotes from all providers
  - `getQuickEstimate()` - Fast shipping estimate with limited providers
  - `reviewCartShipping()` - Cart review with cached options

### 2. Request/Response Models
- **ShippingQuoteRequest**: Comprehensive request model with customer, vendor, and package information
- **ShippingQuoteResponse**: Response with shipping options, pricing, and provider details

### 2.1. Enhanced OrderCreatedEvent Structure
Added real address support to the event structure:

**Customer Information:**
- `customerName`: Customer's full name
- `customerEmail`: Customer's email address  
- `customerPhone`: Customer's phone number
- `shippingAddress`: Customer's shipping address (structured string)

**Shop Address Information:**
- `CheckoutItem.shopAddress`: Complete shop address structure
  - `shopName`: Shop/vendor name
  - `contactEmail`: Shop contact email
  - `contactPhone`: Shop contact phone
  - `street`, `city`, `state`, `zipCode`, `country`: Address components
  - `addressLine2`: Additional address information

### 3. Event Handling
- **ShippingCalculatedEvent**: New saga event to track shipping calculations
- Updated `SagaEvent.java` to include shipping events in JsonSubTypes
- **Enhanced OrderCreatedEvent**: Added customer contact info and shop address structure

### 4. Enhanced Order Flow
Updated `OrderSagaOrchestrator.handleCreateOrder_New()` method:

```
STEP 1: Reserve inventory for each shop's orders
STEP 2: Calculate subtotal for each shop's orders using pricing service  
STEP 3-5: Apply comprehensive discounts (shop, shipping, global)
STEP 6: Calculate shipping costs for each shop's orders  ⭐ NEW
STEP 7: Create shop orders with calculated shipping costs    ⭐ UPDATED
STEP 8: Process payment based on payment method            ⭐ UPDATED
```

## Key Features

### Multi-Vendor Shipping Support
- Calculates shipping costs separately for each shop/vendor
- Supports different shipping providers per shop
- Handles shipping cost aggregation in final order total

### Intelligent Provider Selection
- Uses recommended shipping option when available
- Falls back to cheapest option if no recommendation
- Provides default shipping cost on service failures

### Error Handling
- Graceful fallback to default shipping costs on API failures
- Comprehensive error logging and saga state management
- Shipping calculation failure handling with proper saga compensation

### Address & Package Handling
- **Real Customer Addresses**: Uses actual customer name, email, phone, and shipping address from OrderCreatedEvent
- **Real Shop Addresses**: Uses actual shop address, contact info from CheckoutItem.ShopAddress
- **Enhanced Address Parsing**: Parses structured address strings (street, city, state, zip, country)
- **Smart Fallbacks**: Graceful fallback to defaults when address information is missing
- Calculates package dimensions and weight from order items

## Configuration Requirements

### 1. Application Properties
Add to `application.yaml`:
```yaml
microservices:
  shipping-service:
    url: http://shipping-service:8094
```

### 2. Circuit Breaker Configuration
The shipping client includes:
- `@CircuitBreaker(name = "shippingService")`
- `@Retry(name = "shippingService")`

### 3. Default Values
- Default shipping cost: $5.00 (fallback)
- Default package dimensions: 30cm x 20cm x 15cm
- Default vendor address: Ho Chi Minh City, Vietnam (used when shop address not provided)
- Default customer address: Ho Chi Minh City, Vietnam (used when address parsing fails)

## Usage in Order Flow

### 1. Shipping Calculation
For each shop in the order:
1. **Extract Real Customer Data**: Get customer name, email, phone from OrderCreatedEvent
2. **Parse Customer Address**: Enhanced parsing of shipping address string into components
3. **Extract Shop Data**: Get shop name, contact info, and address from CheckoutItem.shopAddress
4. **Build Shipping Request**: Create comprehensive shipping quote request with real addresses
5. **Calculate Package Details**: Determine weight and dimensions from order items
6. **Call Shipping Service**: Get quotes from shipping providers
7. **Select Best Option**: Choose recommended > cheapest > default fallback
8. **Add to Order Total**: Include shipping cost in shop order total

### 2. Order Creation
- Shop orders now include `shopShippingAmount` field
- Total order amount includes shipping costs
- Shipping costs are tracked per shop for multi-vendor scenarios

## New Methods Added

### Address Handling Methods
- `buildVendorInfoFromCheckoutItem()`: Creates vendor info from real shop address data
- `parseAddressFromString()`: Enhanced address parsing with component extraction  
- `getDefaultCustomerAddress()`: Fallback customer address when parsing fails
- `getDefaultVendorAddress()`: Fallback vendor address when shop data missing

## Future Enhancements

### 1. Advanced Address Services
- Integration with address validation services (Google Maps, etc.)
- Address geocoding for distance-based shipping calculations
- International address format support

### 2. Shop/Vendor Service Integration  
- Dynamic shop address lookup from vendor management service
- Real-time shop availability and shipping preferences
- Shop-specific shipping rate negotiations

### 3. Advanced Address Parsing
- Implement sophisticated address parsing from order strings
- Support multiple address formats and validation

### 4. Shipping Provider Selection
- Allow customers to choose specific shipping providers
- Support provider preferences per shop
- Implement shipping service level agreements

### 5. Shipping Event Publishing
- Publish shipping events to Kafka for downstream services
- Enable shipping cost optimization and analytics
- Support shipping notification services

## Sample OrderCreatedEvent Usage

```java
// Example of how to populate OrderCreatedEvent with real addresses
OrderCreatedEvent event = OrderCreatedEvent.builder()
    .customerId(customerId)
    .customerName("John Doe")
    .customerEmail("john.doe@example.com") 
    .customerPhone("+84901234567")
    .shippingAddress("123 Main St, District 1, Ho Chi Minh City, 70000, VN")
    .checkoutItems(List.of(
        OrderCreatedEvent.CheckoutItem.builder()
            .shopId(shopId)
            .shopAddress(OrderCreatedEvent.ShopAddress.builder()
                .shopName("TechStore Vietnam")
                .contactEmail("contact@techstore.vn")
                .contactPhone("+84287654321")
                .street("456 Business Ave")
                .city("Ho Chi Minh City")
                .state("Ho Chi Minh")
                .zipCode("70000")
                .country("VN")
                .build())
            .items(orderItems)
            .build()))
    .build();
```

## Testing Considerations

### 1. Unit Tests
- Test shipping cost calculation logic with real addresses
- Test address parsing and fallback scenarios
- Mock shipping service responses
- Validate fallback behavior

### 2. Integration Tests
- Test end-to-end order flow with shipping
- Validate multi-vendor shipping scenarios
- Test shipping service failure scenarios

### 3. Performance Tests
- Test shipping calculation latency impact
- Validate circuit breaker behavior
- Test shipping service capacity limits

## Monitoring & Observability

### 1. Logging
- Shipping calculation start/completion logs
- Shipping provider selection reasoning
- Error logs with fallback behavior

### 2. Metrics
- Shipping calculation duration
- Shipping service failure rates
- Default shipping cost usage frequency

### 3. Alerts
- High shipping service error rates
- Shipping calculation timeouts
- Unusual shipping cost patterns 
