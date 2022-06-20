package com.github.schananas.reactivestockmarket.domain.engine.events;

import com.github.schananas.reactivestockmarket.cqrs.UpdateEvent;
import com.github.schananas.reactivestockmarket.domain.query.OrderType;

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