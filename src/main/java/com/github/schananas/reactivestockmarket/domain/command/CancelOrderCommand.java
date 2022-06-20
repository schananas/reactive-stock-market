package com.github.schananas.reactivestockmarket.domain.command;

import com.github.schananas.reactivestockmarket.cqrs.Command;
import org.springframework.lang.NonNull;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Command to request order cancellation
 * @author Stefan Dragisic
 */
public record CancelOrderCommand(String aggregateId, UUID commandId, long orderId, Boolean cancelAll, BigDecimal newAmount)
        implements Command {

    public CancelOrderCommand(
            @NonNull String aggregateId,
            @NonNull UUID commandId,
            @NonNull  long orderId,
            @NonNull Boolean cancelAll,
            @NonNull BigDecimal newAmount) {
        this.aggregateId = aggregateId;
        this.commandId = commandId;
        this.orderId = orderId;
        this.cancelAll = cancelAll;
        this.newAmount = newAmount;
    }
}
