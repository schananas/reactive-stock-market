package io.bux.matchingengine.domain.query;

import io.bux.matchingengine.cqrs.Event;
import io.bux.matchingengine.cqrs.QueryRepository;
import io.bux.matchingengine.domain.engine.events.OrderCanceledEvent;
import io.bux.matchingengine.domain.engine.events.OrderMatchedEvent;
import io.bux.matchingengine.domain.engine.events.OrderPlacedEvent;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Thread-safe implementation of {@link QueryRepository} used to store order & book projections.
 *
 * @author Stefan Dragisic
 */
@Component
public class BookQueryRepository implements QueryRepository<OrderEntry> {

    private final ConcurrentHashMap<Long, OrderEntry> projection = new ConcurrentHashMap<>();

    /**
     * Returns current order projection
     *
     * @param orderId - order identifier
     * @return - materialized projection
     */
    @Override
    public Mono<OrderEntry> getOrder(long orderId) {
        return Mono.fromCallable(() -> projection.get(orderId));
    }

    /**
     * Updates projection with event. Repository uses this event to create/maintain projections.
     *
     * @param event - materialized event
     */
    @Override
    public Mono<Void> updateProjection(Event event) {
        return (switch (event) {
            case OrderPlacedEvent evt -> handleOrderPlacedEvent(evt);
            case OrderMatchedEvent evt -> handleOrderMatchedEvent(evt);
            case OrderCanceledEvent evt -> handleOrderCanceledEvent(evt);
            default -> Mono.empty();
        }).subscribeOn(Schedulers.parallel())
          .then();
    }

    private Mono<OrderEntry> handleOrderPlacedEvent(OrderPlacedEvent evt) {
        return Mono.fromCallable(() -> projection.computeIfAbsent(evt.orderId(), orderId ->
                new OrderEntry(orderId,
                               evt.timestamp(),
                               evt.aggregateId(),
                               evt.price(),
                               evt.amount(),
                               evt.orderType(),
                               new CopyOnWriteArrayList<>(),
                               evt.amount())));
    }

    private Mono<OrderEntry> handleOrderMatchedEvent(OrderMatchedEvent evt) {
        return Mono.fromCallable(() -> {
            //update previous
            projection.computeIfPresent(evt.restingId(), (key, order) -> {
                order.setPendingAmount(evt.restingRemainingAmount());
                order.trades().add(
                        new OrderTradeEntry(evt.incomingId(),
                                            evt.incomingAmount(),
                                            evt.restingPrice()));
                return order;
            });
            //enter new
            return projection.computeIfAbsent(evt.incomingId(), incomingId ->
                    new OrderEntry(incomingId,
                                   evt.entryTimestamp(),
                                   evt.aggregateId(),
                                   evt.incomingPrice(),
                                   evt.incomingAmount(),
                                   evt.orderType(),
                                   new CopyOnWriteArrayList<>(List.of(new OrderTradeEntry(
                                           evt.restingId(),
                                           evt.previousRestingAmount().subtract(evt.restingRemainingAmount()),
                                           evt.restingPrice()
                                   ))),
                                   evt.incomingAmount()
                                      .subtract(evt.previousRestingAmount().subtract(evt.restingRemainingAmount()))));
        });
    }

    private Mono<OrderEntry> handleOrderCanceledEvent(OrderCanceledEvent evt) {
        return Mono.fromCallable(() -> projection.computeIfPresent(evt.orderId(), (key, order) -> {
            order.setPendingAmount(evt.remainingAmount());
            return order;
        }));
    }
}