package io.bux.matchingengine.domain.engine.events;

import io.bux.matchingengine.cqrs.UpdateEvent;
import io.bux.matchingengine.domain.query.OrderType;

import java.math.BigDecimal;

/**
 * Update event that signals that order has been canceled.
 *
 * @author Stefan Dragisic
 */
public record OrderCanceledEvent(
        long orderId,
        String aggregateId,
        OrderType orderType,
        BigDecimal canceledAmount,
        BigDecimal remainingAmount) implements UpdateEvent {

}