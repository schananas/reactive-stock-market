package io.bux.matchingengine.domain.query;

import java.math.BigDecimal;

public record OrderTradeEntry(
        long orderId,
        BigDecimal amount,
        BigDecimal price
) {

    public OrderTradeEntry(
            long orderId,
            BigDecimal amount,
            BigDecimal price) {
        this.orderId = orderId;
        this.price = price;
        this.amount = amount;
    }
}
