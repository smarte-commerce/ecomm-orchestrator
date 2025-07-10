package com.winnguyen1905.orchestrator.model.event;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class OrderApprovedEvent extends SagaEvent {
    private UUID paymentId;
    private UUID reservationId;
    private String newOrderStatus;
} 