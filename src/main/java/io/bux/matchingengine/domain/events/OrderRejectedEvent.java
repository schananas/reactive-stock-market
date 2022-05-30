package io.bux.matchingengine.domain.events;

import io.bux.matchingengine.cqrs.SourcingEvent;
import io.bux.matchingengine.domain.query.OrderType;
import org.springframework.lang.NonNull;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Event that marks that order didn't pass validation.
 * Not used - POC
 *
 * @author Stefan Dragisic
 */
public record OrderRejectedEvent(String aggregateId, UUID eventId,
                                 OrderType type, BigDecimal amount, BigDecimal price, String cause)
        implements SourcingEvent {

    public OrderRejectedEvent(
            @NonNull String aggregateId,
            @NonNull UUID eventId,
            @NonNull OrderType type,
            @NonNull BigDecimal amount,
            @NonNull BigDecimal price,
            @NonNull String cause) {
        this.aggregateId = aggregateId;
        this.eventId = eventId;
        this.type = type;
        this.amount = amount;
        this.price = price;
        this.cause = cause;
    }
}
