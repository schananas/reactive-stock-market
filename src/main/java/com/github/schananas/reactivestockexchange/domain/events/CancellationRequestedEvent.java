package com.github.schananas.reactivestockexchange.domain.events;

import com.github.schananas.reactivestockexchange.cqrs.SourcingEvent;
import org.springframework.lang.NonNull;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Event that user requested cancellation of order
 * Not used - POC
 *
 * @author Stefan Dragisic
 */
public record CancellationRequestedEvent(String aggregateId, UUID eventId,
                                         long orderId,
                                         Boolean cancelAll, BigDecimal newAmount)
        implements SourcingEvent {

    public CancellationRequestedEvent(
            @NonNull String aggregateId,
            @NonNull UUID eventId,
            @NonNull long orderId,
            @NonNull Boolean cancelAll,
            @NonNull BigDecimal newAmount) {
        this.aggregateId = aggregateId;
        this.eventId = eventId;
        this.orderId = orderId;
        this.cancelAll = cancelAll;
        this.newAmount = newAmount;
    }
}
