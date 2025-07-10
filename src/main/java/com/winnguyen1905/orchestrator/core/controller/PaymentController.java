package com.winnguyen1905.orchestrator.core.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.winnguyen1905.orchestrator.model.request.SplitPaymentRequest;
import com.winnguyen1905.orchestrator.model.response.RestResponse;
import com.winnguyen1905.orchestrator.service.DiscountSagaHandler;
import com.winnguyen1905.orchestrator.service.SplitPaymentSagaHandler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final DiscountSagaHandler discountSagaHandler;
    private final SplitPaymentSagaHandler splitPaymentSagaHandler;
    
    /**
     * Apply coupon to an order
     */
    @PostMapping("/orders/{orderId}/coupon/{couponCode}")
    public ResponseEntity<RestResponse<Map<String, UUID>>> applyCoupon(
            @PathVariable UUID orderId,
            @PathVariable String couponCode) {
        try {
            UUID sagaId = discountSagaHandler.applyCouponToOrder(orderId, couponCode);
            
            Map<String, UUID> response = new HashMap<>();
            response.put("sagaId", sagaId);
            response.put("orderId", orderId);
            
            return ResponseEntity.ok(
                    RestResponse.<Map<String, UUID>>builder()
                            .statusCode(HttpStatus.OK.value())
                            .message("Coupon applied successfully")
                            .data(response)
                            .build()
            );
        } catch (Exception e) {
            log.error("Failed to apply coupon to order: " + orderId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(RestResponse.<Map<String, UUID>>builder()
                            .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                            .error("Failed to apply coupon")
                            .message(e.getMessage())
                            .build()
                    );
        }
    }
    
    /**
     * Process split payment for an order
     */
    @PostMapping("/orders/{orderId}/split")
    public ResponseEntity<RestResponse<Map<String, UUID>>> processSplitPayment(
            @PathVariable UUID orderId,
            @RequestBody SplitPaymentRequest request) {
        try {
            if (!orderId.equals(request.getOrderId())) {
                return ResponseEntity.badRequest()
                        .body(RestResponse.<Map<String, UUID>>builder()
                                .statusCode(HttpStatus.BAD_REQUEST.value())
                                .error("Bad Request")
                                .message("Order ID in path must match order ID in request body")
                                .build()
                        );
            }
            
            UUID sagaId = splitPaymentSagaHandler.processSplitPayment(orderId, request.getPaymentMethods());
            
            Map<String, UUID> response = new HashMap<>();
            response.put("sagaId", sagaId);
            response.put("orderId", orderId);
            
            return ResponseEntity.ok(
                    RestResponse.<Map<String, UUID>>builder()
                            .statusCode(HttpStatus.OK.value())
                            .message("Split payment processed successfully")
                            .data(response)
                            .build()
            );
        } catch (Exception e) {
            log.error("Failed to process split payment for order: " + orderId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(RestResponse.<Map<String, UUID>>builder()
                            .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                            .error("Failed to process split payment")
                            .message(e.getMessage())
                            .build()
                    );
        }
    }
} 
