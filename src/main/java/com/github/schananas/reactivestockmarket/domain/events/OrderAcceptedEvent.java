package com.github.schananas.reactivestockmarket.domain.events;

import com.github.schananas.reactivestockmarket.cqrs.SourcingEvent;
import com.github.schananas.reactivestockmarket.domain.query.OrderType;
import org.springframework.lang.NonNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Event that marks that order has passed validation phase, and order id is generated to be used for tracker.
 *
 * @author Stefan Dragisic
 */
public record OrderAcceptedEvent(String aggregateId, UUID eventId, long orderId,
                                 OrderType type, BigDecimal amount, BigDecimal price, Instant entryTimestamp)
        implements SourcingEvent {

    public OrderAcceptedEvent(
            @NonNull String aggregateId,
            @NonNull UUID eventId,
            @NonNull long orderId,
            @NonNull OrderType type,
            @NonNull BigDecimal amount,
            @NonNull BigDecimal price,
            @NonNull Instant entryTimestamp) {
        this.aggregateId = aggregateId;
        this.orderId = orderId;
        this.eventId = eventId;
        this.type = type;
        this.amount = amount;
        this.price = price;
        this.entryTimestamp = entryTimestamp;
    }
}
