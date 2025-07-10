package com.winnguyen1905.orchestrator.core.feign;

import java.util.List;
import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.winnguyen1905.orchestrator.model.request.PriceCalculationRequest;
import com.winnguyen1905.orchestrator.model.request.ReservationRequest;
import com.winnguyen1905.orchestrator.model.request.ReserveInventoryRequest;
import com.winnguyen1905.orchestrator.model.response.PriceCalculationResponse;
import com.winnguyen1905.orchestrator.model.response.ReservationResponse;
import com.winnguyen1905.orchestrator.model.response.ReserveInventoryResponse;
import com.winnguyen1905.orchestrator.model.response.RestResponse;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;

@FeignClient(name = "PRODUCT-SERVICE", url = "${microservices.inventory-service.url}")
@CircuitBreaker(name = "productService")
@Retry(name = "productService")
public interface ProductServiceClient {

    /**
     * Reserve inventory for multiple shops
     */
    @PostMapping("/api/v1/inventories/reserve-batch")
    ResponseEntity<RestResponse<ReserveInventoryResponse>> reserveInventoryBatch(@RequestBody ReserveInventoryRequest request);

    /**
     * Reserve inventory
     */
    @PostMapping("/api/v1/inventories/reserve")
    ResponseEntity<RestResponse<ReserveInventoryResponse>> reserveInventory(@RequestBody ReserveInventoryRequest request);

    /**
     * Release inventory reservation
     */
    @PutMapping("/api/v1/inventories/release/{sku}")
    ResponseEntity<RestResponse<Boolean>> releaseReservation(
            @PathVariable("sku") String sku,
            @RequestBody int quantity);

    /**
     * Confirm inventory reservation (convert reserved to sold)
     */
    @PutMapping("/api/v1/inventories/confirm/{sku}")
    ResponseEntity<RestResponse<Boolean>> confirmReservation(
            @PathVariable("sku") String sku,
            @RequestBody int quantity);

    /**
     * Calculate pricing for order items including subtotals per shop
     */
    @PostMapping("/api/v1/products/calculate-pricing")
    ResponseEntity<RestResponse<PriceCalculationResponse>> calculatePricing(@RequestBody PriceCalculationRequest request);

    /**
     * Batch confirm multiple reservations
     */
    @PostMapping("/api/v1/inventories/confirm-batch")
    ResponseEntity<RestResponse<Boolean>> confirmReservationBatch(@RequestBody List<String> reservationIds);

    /**
     * Batch release multiple reservations
     */
    @PostMapping("/api/v1/inventories/release-batch")
    ResponseEntity<RestResponse<Boolean>> releaseReservationBatch(@RequestBody List<String> reservationIds);
}
