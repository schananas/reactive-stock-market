package com.github.schananas.reactivestockmarket.domain.engine;

import com.github.schananas.reactivestockmarket.domain.query.OrderType;

import java.math.BigDecimal;

/**
 * Representation of order used by {@link MatchingEngine}
 *
 * @author Stefan Dragisic
 */
public class Order {
    private final OrderType type;
    private final BigDecimal price;
    private final long term;
    private final long id;

    private BigDecimal remainingAmount;

    public Order(long id, OrderType type, BigDecimal price, BigDecimal amount, long term) {
        this.id = id;
        this.type  = type;
        this.price = price;
        this.term = term;

        this.remainingAmount = amount;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public OrderType type() {
        return type;
    }

    public BigDecimal getRemainingAmount() {
        return remainingAmount;
    }

    public long getTerm() {
        return term;
    }

    public void reduce(BigDecimal amount) {
        remainingAmount = remainingAmount.subtract(amount);
    }

    public long getId() {
        return id;
    }

    public void resize(BigDecimal newAmount) {
        remainingAmount = newAmount;
    }
}
