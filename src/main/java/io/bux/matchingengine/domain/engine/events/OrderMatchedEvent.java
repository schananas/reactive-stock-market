package io.bux.matchingengine.domain.engine.events;

import io.bux.matchingengine.cqrs.UpdateEvent;
import io.bux.matchingengine.domain.query.OrderType;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Update event that signals that order has been matched.
 *
 * @author Stefan Dragisic
 */
public record OrderMatchedEvent(
        long restingId,
        String aggregateId,
        Instant entryTimestamp,
        long incomingId,
        OrderType orderType,
        BigDecimal incomingPrice,
        BigDecimal restingPrice,
        BigDecimal incomingAmount,
        BigDecimal previousRestingAmount,
        BigDecimal restingRemainingAmount) implements UpdateEvent {

}
