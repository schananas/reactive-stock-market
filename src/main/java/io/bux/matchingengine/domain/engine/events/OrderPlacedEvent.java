package io.bux.matchingengine.domain.engine.events;

import io.bux.matchingengine.cqrs.UpdateEvent;
import io.bux.matchingengine.domain.query.OrderType;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Update event that signals that order has been placed but not yet matched.
 *
 * @author Stefan Dragisic
 */
public record OrderPlacedEvent(
        long orderId,
        String aggregateId,
        Instant timestamp,
        OrderType orderType,
        BigDecimal price,
        BigDecimal amount) implements UpdateEvent {

}
