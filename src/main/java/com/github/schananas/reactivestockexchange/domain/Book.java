package com.github.schananas.reactivestockexchange.domain;

import com.github.schananas.reactivestockexchange.cqrs.Aggregate;
import com.github.schananas.reactivestockexchange.cqrs.Command;
import com.github.schananas.reactivestockexchange.cqrs.Event;
import com.github.schananas.reactivestockexchange.cqrs.SourcingEvent;
import com.github.schananas.reactivestockexchange.domain.command.CancelOrderCommand;
import com.github.schananas.reactivestockexchange.domain.command.MakeOrderCommand;
import com.github.schananas.reactivestockexchange.domain.engine.MatchingEngine;
import com.github.schananas.reactivestockexchange.domain.events.CancellationRequestedEvent;
import com.github.schananas.reactivestockexchange.domain.events.OrderAcceptedEvent;
import com.github.schananas.reactivestockexchange.domain.events.OrderRejectedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;


/**
 * Book aggregate to model business domain.
 *
 * @author Stefan Dragisic
 */
public class Book implements Aggregate {

    private final Logger logger = LoggerFactory.getLogger(Book.class);

    private final String aggregateId;

    private final MatchingEngine matchingEngine;
    private static final AtomicLong orderIdGenerator = new AtomicLong();
    Sinks.Many<Event> aggregateEventSink = Sinks.many().multicast().onBackpressureBuffer();
    Flux<Event> aggregateEventFlux = aggregateEventSink.asFlux()
                                                             .doOnNext(n -> logger.debug(n.toString()))
                                                             .publish()
                                                             .autoConnect();

    public Book(String aggregateId, MatchingEngine matchingEngine) {
        this.aggregateId = aggregateId;
        this.matchingEngine = matchingEngine;
    }

    public Book(String aggregateId) {
        this.aggregateId = aggregateId;
        this.matchingEngine = new MatchingEngine();
    }

    @Override
    public String aggregateId() {
        return aggregateId;
    }

    @Override
    public Flux<Event> aggregateEvents() {
        return aggregateEventFlux
                .mergeWith(matchingEngine.engineEvents())
                .cast(Event.class);
    }

    //---------------------------COMMAND HANDLING---------------------------------

    @Override
    public Mono<SourcingEvent> routeCommand(Command command) {
        return switch (command) {
            case MakeOrderCommand cmd -> handleMakeOrderCommand(cmd);
            case CancelOrderCommand cmd -> handleCancelOrderCommand(cmd);
            default -> Mono.error(new RuntimeException(
                    command.getClass().getSimpleName() + ": event not implemented!"));
        };
    }

    public Mono<SourcingEvent> handleMakeOrderCommand(MakeOrderCommand cmd) {
        return Mono.defer(() -> {

            if (cmd.amount().compareTo(BigDecimal.ZERO) <= 0 || cmd.price().compareTo(BigDecimal.ZERO) <= 0) {
                aggregateEventSink.tryEmitNext(new OrderRejectedEvent(cmd.aggregateId(),
                                                                      UUID.randomUUID(),
                                                                      cmd.type(),
                                                                      cmd.amount(),
                                                                      cmd.price(),
                                                                      "Amount/Price needs to be larger then zero!"));
                return Mono.error(new IllegalStateException("Amount/Price needs to be larger then zero!"));
            } else {
                OrderAcceptedEvent orderAcceptedEvent = new OrderAcceptedEvent(cmd.aggregateId(),
                                                                               UUID.randomUUID(),
                                                                               orderIdGenerator.incrementAndGet(),
                                                                               cmd.type(),
                                                                               cmd.amount(),
                                                                               cmd.price(),
                                                                               Instant.now());
                aggregateEventSink.tryEmitNext(orderAcceptedEvent);
                return Mono.just(orderAcceptedEvent);
            }
        });
    }

    public Mono<SourcingEvent> handleCancelOrderCommand(CancelOrderCommand cmd) {
        return Mono.defer(() -> {
            if (!cmd.cancelAll() && cmd.newAmount().compareTo(BigDecimal.ZERO) <= 0) {
                return Mono.error(new IllegalStateException("Cancellation: new amount can't be <= 0!"));
            } else {
                CancellationRequestedEvent event = new CancellationRequestedEvent(cmd.aggregateId(),
                                                                                  UUID.randomUUID(),
                                                                                  cmd.orderId(),
                                                                                  cmd.cancelAll(),
                                                                                  cmd.newAmount());
                aggregateEventSink.tryEmitNext(event);
                return Mono.just(event);
            }
        });
    }


    //---------------------------EVENT HANDLING---------------------------------

    @Override
    public Mono<Void> routeEvent(SourcingEvent event) {
        return switch (event) {
            case OrderAcceptedEvent evt -> handleOrderAcceptedEvent(evt);
            case CancellationRequestedEvent evt -> handleOrderCancellationRequestedEvent(evt);
            default -> Mono.error(new RuntimeException(event.getClass().getSimpleName() + ": event not implemented!"));
        };
    }

    private Mono<Void> handleOrderAcceptedEvent(OrderAcceptedEvent evt) {
        return Mono.fromRunnable(() -> matchingEngine.placeOrder(evt.orderId(),
                                                                 evt.aggregateId(),
                                                                 evt.entryTimestamp(),
                                                                 evt.type(),
                                                                 evt.price(),
                                                                 evt.amount()));
    }

    private Mono<Void> handleOrderCancellationRequestedEvent(CancellationRequestedEvent evt) {
        return Mono.fromRunnable(() -> {
            if (evt.cancelAll()) {
                matchingEngine.cancelAll(evt.orderId(), evt.aggregateId());
            } else {
                matchingEngine.cancel(evt.orderId(),evt.aggregateId(), evt.newAmount());
            }
        });
    }
}