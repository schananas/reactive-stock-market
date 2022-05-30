package io.bux.matchingengine.domain.engine;

import io.bux.matchingengine.cqrs.UpdateEvent;
import io.bux.matchingengine.domain.query.OrderType;
import io.bux.matchingengine.domain.engine.events.OrderCanceledEvent;
import io.bux.matchingengine.domain.engine.events.OrderMatchedEvent;
import io.bux.matchingengine.domain.engine.events.OrderPlacedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Single threaded matching engine - any thread synchronization should be done external.
 * <p>
 * Uses Max-Heap and Min-Heap {@link TreeSet}
 * <p>
 * Time complexity for critical operations are as.
 * - Add – O(log N)
 * - Cancel – O(1)
 * <p>
 * Asynchronous - no immediate return values, all events are publishes to local event bus {@link
 * MatchingEngine#engineEventSink}
 *
 * @author Stefan Dragisic
 */
public class MatchingEngine {

    private final Logger logger = LoggerFactory.getLogger(MatchingEngine.class);
    private final TreeSet<Order> bids;
    private final TreeSet<Order> asks;
    private final Map<Long, Order> orders;
    private final AtomicLong term;
    Sinks.Many<UpdateEvent> engineEventSink = Sinks.many().multicast().onBackpressureBuffer();
    Flux<UpdateEvent> engineEventFlux = engineEventSink.asFlux()
                                                       .doOnNext(n -> logger.info(n.toString()))
                                                       .publish()
                                                       .autoConnect();

    public MatchingEngine() {
        this.bids = new TreeSet<>(MatchingEngine::compareBids);
        this.asks = new TreeSet<>(MatchingEngine::compareAsks);

        this.orders = new HashMap<>();
        this.term = new AtomicLong(0);
    }

    private static int compareBids(Order a, Order b) {
        int result = b.getPrice().compareTo(a.getPrice());
        if (result != 0) {
            return result;
        }

        return Long.compare(a.getTerm(), b.getTerm());
    }

    private static int compareAsks(Order a, Order b) {
        int result = a.getPrice().compareTo(b.getPrice());
        if (result != 0) {
            return result;
        }

        return Long.compare(a.getTerm(), b.getTerm());
    }

    /**
     * All engine execution events are published to this bus
     *
     * @return local engine event bus - hot stream
     */
    public Flux<UpdateEvent> engineEvents() {
        return engineEventFlux;
    }

    /**
     * Places order into matching engine
     *
     * @param orderId - order identifier
     * @param aggregateId - asset name / aggregate identifier
     * @param entryTimestamp - time when the system registered order
     * @param type - direction - can be either "BUY" or "SELL"
     * @param price - a price for limit order
     * @param amount - amount of asset to fill by order
     */
    public void placeOrder(long orderId, String aggregateId, Instant entryTimestamp, OrderType type, BigDecimal price,
                           BigDecimal amount) {
        if (orders.containsKey(orderId)) {
            return;
        }
        if (type == OrderType.BUY) {
            buy(orderId, aggregateId, entryTimestamp, price, amount);
        } else {
            sell(orderId, aggregateId, entryTimestamp, price, amount);
        }
    }

    private void buy(long incomingId, String aggregateId, Instant entryTimestamp, BigDecimal incomingPrice,
                     BigDecimal incomingAmount) {
        while (!asks.isEmpty()) {
            Order resting = asks.first();

            BigDecimal restingPrice = resting.getPrice();
            if (restingPrice.compareTo(incomingPrice) > 0) {
                break;
            }

            long restingId = resting.getId();

            BigDecimal restingAmount = resting.getRemainingAmount();

            if (restingAmount.compareTo(incomingAmount) > 0) {
                resting.reduce(incomingAmount);

                engineEventSink.tryEmitNext(new OrderMatchedEvent(restingId,
                                                                  aggregateId,
                                                                  entryTimestamp,
                                                                  incomingId,
                                                                  OrderType.BUY,
                                                                  incomingPrice,
                                                                  restingPrice,
                                                                  incomingAmount,
                                                                  restingAmount,
                                                                  resting.getRemainingAmount()));

                return;
            }

            asks.remove(resting);
            orders.remove(restingId);

            engineEventSink.tryEmitNext(new OrderMatchedEvent(restingId,
                                                              aggregateId,
                                                              entryTimestamp,
                                                              incomingId,
                                                              OrderType.BUY,
                                                              incomingPrice,
                                                              restingPrice,
                                                              incomingAmount,
                                                              restingAmount,
                                                              BigDecimal.ZERO));


            incomingAmount = incomingAmount.subtract(restingAmount);

            if (incomingAmount.compareTo(BigDecimal.ZERO) == 0) {
                return;
            }
        }

        add(incomingId, aggregateId, entryTimestamp, OrderType.BUY, incomingPrice, incomingAmount, bids);
    }

    private void sell(long incomingId, String aggregateId, Instant entryTimestamp, BigDecimal incomingPrice,
                      BigDecimal incomingAmount) {
        while (!bids.isEmpty()) {
            Order resting = bids.first();

            BigDecimal restingPrice = resting.getPrice();
            if (restingPrice.compareTo(incomingPrice) < 0) {
                break;
            }

            long restingId = resting.getId();

            BigDecimal restingAmount = resting.getRemainingAmount();
            if (restingAmount.compareTo(incomingAmount) > 0) {
                resting.reduce(incomingAmount);

                engineEventSink.tryEmitNext(new OrderMatchedEvent(restingId,
                                                                  aggregateId,
                                                                  entryTimestamp,
                                                                  incomingId,
                                                                  OrderType.SELL,
                                                                  incomingPrice,
                                                                  restingPrice,
                                                                  incomingAmount,
                                                                  restingAmount,
                                                                  resting.getRemainingAmount()));

                return;
            }

            bids.remove(resting);
            orders.remove(restingId);

            engineEventSink.tryEmitNext(new OrderMatchedEvent(restingId,
                                                              aggregateId,
                                                              entryTimestamp,
                                                              incomingId,
                                                              OrderType.SELL,
                                                              incomingPrice,
                                                              restingPrice,
                                                              incomingAmount,
                                                              restingAmount,
                                                              BigDecimal.ZERO));

            incomingAmount = incomingAmount.subtract(restingAmount);
            if (incomingAmount.compareTo(BigDecimal.ZERO) == 0) {
                return;
            }
        }

        add(incomingId, aggregateId, entryTimestamp, OrderType.SELL, incomingPrice, incomingAmount, asks);
    }

    private void add(long orderId, String aggregateId, Instant entryTimestamp, OrderType type, BigDecimal price,
                     BigDecimal amount, TreeSet<Order> queue) {
        Order order = new Order(orderId, type, price, amount, term.incrementAndGet());

        queue.add(order);
        orders.put(orderId, order);

        engineEventSink.tryEmitNext(new OrderPlacedEvent(orderId,
                                                         aggregateId,
                                                         entryTimestamp,
                                                         type,
                                                         price,
                                                         amount));
    }

    /**
     * Cancels full amount of order.
     *
     * @param orderId - order identifier
     * @param aggregateId - asset name / aggregate identifier
     */
    public void cancelAll(long orderId, String aggregateId) {
        cancel(orderId, aggregateId, BigDecimal.ZERO);
    }

    /**
     * Partially cancels order and sets new amount to be filled
     *
     * @param orderId - order identifier
     * @param aggregateId - asset name / aggregate identifier
     * @param newAmount - new amount to replace previous amount
     */
    public void cancel(long orderId, String aggregateId, BigDecimal newAmount) {
        Order order = orders.get(orderId);
        if (order == null) {
            return;
        }

        BigDecimal remainingAmount = order.getRemainingAmount();

        if (newAmount.compareTo(remainingAmount) >= 0) {
            return;
        }

        if (newAmount.compareTo(BigDecimal.ZERO) > 0) {
            order.resize(newAmount);
        } else {
            TreeSet<Order> queue = order.type() == OrderType.BUY ? bids : asks;

            queue.remove(order);
            orders.remove(orderId);
        }

        engineEventSink.tryEmitNext(new OrderCanceledEvent(orderId,
                                                           aggregateId,
                                                           order.type(),
                                                           remainingAmount.subtract(newAmount),
                                                           newAmount));
    }

    ;
}
