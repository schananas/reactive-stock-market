package io.bux.matchingengine.domain.command;

import io.bux.matchingengine.cqrs.Command;
import io.bux.matchingengine.domain.query.OrderType;
import org.springframework.lang.NonNull;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Command to place new order
 *
 * @author Stefan Dragisic
 */
public record MakeOrderCommand(String aggregateId, UUID commandId,
                               OrderType type, BigDecimal amount, BigDecimal price)
        implements Command {

    public MakeOrderCommand(
            @NonNull String aggregateId,
            @NonNull UUID commandId,
            @NonNull OrderType type,
            @NonNull BigDecimal amount,
            @NonNull BigDecimal price) {
        this.aggregateId = aggregateId;
        this.commandId = commandId;
        this.type = type;
        this.amount = amount;
        this.price = price;
    }
}
