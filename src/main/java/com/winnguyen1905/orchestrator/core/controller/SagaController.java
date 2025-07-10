package com.winnguyen1905.orchestrator.core.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.winnguyen1905.orchestrator.model.response.RestResponse;
import com.winnguyen1905.orchestrator.persistance.entity.ESagaState;
import com.winnguyen1905.orchestrator.persistance.repository.SagaStateRepository;
import com.winnguyen1905.orchestrator.service.OrderSagaOrchestrator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/sagas")
@RequiredArgsConstructor
@Slf4j
public class SagaController {

    private final OrderSagaOrchestrator orderSagaOrchestrator;
    private final SagaStateRepository sagaStateRepository;

    /**
     * Start a new saga for an order
     */
    @PostMapping("/orders/{orderId}")
    public ResponseEntity<RestResponse<Map<String, UUID>>> startOrderSaga(@PathVariable UUID orderId) {
        try {
            UUID sagaId = orderSagaOrchestrator.startOrderSaga(orderId);
            
            Map<String, UUID> response = new HashMap<>();
            response.put("sagaId", sagaId);
            response.put("orderId", orderId);
            
            return ResponseEntity.ok(
                    RestResponse.<Map<String, UUID>>builder()
                            .statusCode(HttpStatus.OK.value())
                            .message("Saga started successfully")
                            .data(response)
                            .build()
            );
        } catch (Exception e) {
            log.error("Failed to start saga for order: " + orderId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(RestResponse.<Map<String, UUID>>builder()
                            .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                            .error("Failed to start saga")
                            .message(e.getMessage())
                            .build()
                    );
        }
    }
    
    /**
     * Get saga state by ID
     */
    @GetMapping("/{sagaId}")
    public ResponseEntity<RestResponse<ESagaState>> getSagaById(@PathVariable UUID sagaId) {
        try {
            Optional<ESagaState> sagaState = sagaStateRepository.findById(sagaId);
            
            if (sagaState.isPresent()) {
                return ResponseEntity.ok(
                        RestResponse.<ESagaState>builder()
                                .statusCode(HttpStatus.OK.value())
                                .message("Saga found")
                                .data(sagaState.get())
                                .build()
                );
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(RestResponse.<ESagaState>builder()
                                .statusCode(HttpStatus.NOT_FOUND.value())
                                .error("Not Found")
                                .message("Saga not found with ID: " + sagaId)
                                .build()
                        );
            }
        } catch (Exception e) {
            log.error("Failed to get saga: " + sagaId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(RestResponse.<ESagaState>builder()
                            .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                            .error("Internal Server Error")
                            .message(e.getMessage())
                            .build()
                    );
        }
    }
    
    /**
     * Get saga state by order ID
     */
    @GetMapping("/orders/{orderId}")
    public ResponseEntity<RestResponse<ESagaState>> getSagaByOrderId(@PathVariable UUID orderId) {
        try {
            Optional<ESagaState> sagaState = sagaStateRepository.findByOrderId(orderId);
            
            if (sagaState.isPresent()) {
                return ResponseEntity.ok(
                        RestResponse.<ESagaState>builder()
                                .statusCode(HttpStatus.OK.value())
                                .message("Saga found")
                                .data(sagaState.get())
                                .build()
                );
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(RestResponse.<ESagaState>builder()
                                .statusCode(HttpStatus.NOT_FOUND.value())
                                .error("Not Found")
                                .message("No saga found for order ID: " + orderId)
                                .build()
                        );
            }
        } catch (Exception e) {
            log.error("Failed to get saga for order: " + orderId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(RestResponse.<ESagaState>builder()
                            .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                            .error("Internal Server Error")
                            .message(e.getMessage())
                            .build()
                    );
        }
    }
    
    /**
     * Get all sagas by status
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<RestResponse<List<ESagaState>>> getSagasByStatus(
            @PathVariable ESagaState.SagaStatus status) {
        try {
            List<ESagaState> sagas = sagaStateRepository.findByStatus(status);
            
            return ResponseEntity.ok(
                    RestResponse.<List<ESagaState>>builder()
                            .statusCode(HttpStatus.OK.value())
                            .message("Sagas found with status: " + status)
                            .data(sagas)
                            .build()
            );
        } catch (Exception e) {
            log.error("Failed to get sagas by status: " + status, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(RestResponse.<List<ESagaState>>builder()
                            .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                            .error("Internal Server Error")
                            .message(e.getMessage())
                            .build()
                    );
        }
    }
} 