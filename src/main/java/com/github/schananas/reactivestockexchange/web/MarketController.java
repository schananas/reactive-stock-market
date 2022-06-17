package com.github.schananas.reactivestockexchange.web;

import com.github.schananas.reactivestockexchange.api.protobuf.OrderStatusResponse;
import com.github.schananas.reactivestockexchange.api.protobuf.PlaceOrderRequest;
import com.github.schananas.reactivestockexchange.api.protobuf.Trade;
import com.github.schananas.reactivestockexchange.cqrs.Event;
import com.github.schananas.reactivestockexchange.cqrs.SourcingEvent;
import com.github.schananas.reactivestockexchange.domain.bus.CommandBus;
import com.github.schananas.reactivestockexchange.domain.command.CancelOrderCommand;
import com.github.schananas.reactivestockexchange.domain.command.MakeOrderCommand;
import com.github.schananas.reactivestockexchange.domain.events.OrderAcceptedEvent;
import com.github.schananas.reactivestockexchange.domain.query.BookQueryRepository;
import com.github.schananas.reactivestockexchange.domain.query.OrderEntry;
import com.github.schananas.reactivestockexchange.domain.query.OrderType;
import com.github.schananas.reactivestockexchange.domain.Book;
import com.github.schananas.reactivestockexchange.domain.BookAggregateRepository;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implements REST Endpoints to place, get or cancel order.
 *
 * @author Stefan Dragisic
 */
@RestController
public class MarketController {

    private final CommandBus commandBus;
    private final BookAggregateRepository bookAggregateRepository;
    private final BookQueryRepository bookQueryRepository;

    public MarketController(CommandBus commandBus,
                            BookAggregateRepository bookAggregateRepository,
                            BookQueryRepository bookQueryRepository) {
        this.commandBus = commandBus;
        this.bookAggregateRepository = bookAggregateRepository;
        this.bookQueryRepository = bookQueryRepository;
    }

    /**
     * Places order into trading system
     *
     * @param request user request to place order
     * @return order status
     */
    @PostMapping("/orders")
    public Mono<OrderStatusResponse> placeOrder(@RequestBody PlaceOrderRequest request) {
        return commandBus.sendCommand(toMakeOrderCommand(request))
                         .cast(OrderAcceptedEvent.class)
                         .flatMap(this::getOrderProjection)
                         .map(this::toOrderStatus);
    }

    /**
     * Not used - POC
     * Intended to UI or client applications to maintain their own projection
     *
     * @param asset
     * @return streams all events from aggregate
     */
    @GetMapping(value = "/book/{asset}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<Event> bookEvents(@PathVariable String asset) {
        return bookAggregateRepository.load(asset)
                                      .flatMapMany(Book::aggregateEvents);
    }

    /**
     * Retrieves order from projection
     *
     * @param orderId - order identifier
     * @return order status
     */
    @GetMapping("/orders/{orderId}")
    public Mono<OrderStatusResponse> getOrder(@PathVariable Long orderId) {
        return bookQueryRepository.getProjection(orderId)
                                  .map(this::toOrderStatus);
    }

    /**
     * POC
     * Cancels pending order from book.
     * @param orderId - order identifier
     * @return response OK or error with error message
     */
    @PostMapping("/orders/{orderId}/cancel")
    public Mono<ResponseEntity<String>> cancelOrder(@PathVariable Long orderId) {
        return bookQueryRepository.getProjection(orderId)
                                  .flatMap(MarketController::validateOrderAmount)
                                  .flatMap(this::sendCancelCommand)
                                  .switchIfEmpty(Mono.error(new IllegalStateException(
                                          "You can't cancel non-existing order.")))
                                  .map(order -> ResponseEntity.accepted().body("OK"))
                                  .onErrorResume(e -> Mono.just(ResponseEntity.badRequest().body(e.getMessage())));
    }

    private Mono<SourcingEvent> sendCancelCommand(OrderEntry order) {
        return commandBus.sendCommand(new CancelOrderCommand(order.asset(),
                                                             UUID.randomUUID(),
                                                             order.orderId(),
                                                             true,
                                                             BigDecimal.ZERO));
    }

    private Mono<? extends OrderEntry> getOrderProjection(OrderAcceptedEvent ev) {
        return bookQueryRepository.getProjection(ev.orderId())
                                  .repeatWhenEmpty(5, o -> o.delayElements(
                                          Duration.ofMillis(50)));
    }

    private MakeOrderCommand toMakeOrderCommand(PlaceOrderRequest request) {
        return new MakeOrderCommand(request.getAsset(),
                                    UUID.randomUUID(),
                                    OrderType.valueOf(request.getDirection().name()),
                                    BigDecimal.valueOf(request.getAmount()),
                                    BigDecimal.valueOf(request.getPrice()));
    }

    private OrderStatusResponse toOrderStatus(OrderEntry order) {
        return OrderStatusResponse.newBuilder()
                                  .setId(order.orderId())
                                  .setTimestamp(order.entryTimestamp().toString())
                                  .setAsset(order.asset())
                                  .setAmount(order.amount().doubleValue())
                                  .setPrice(order.price().doubleValue())
                                  .setDirection(com.github.schananas.reactivestockexchange.api.protobuf.OrderType.valueOf(
                                          order.direction().name()))
                                  .addAllTrades(order.trades().stream()
                                                     .map(t -> Trade.newBuilder()
                                                                    .setOrderId(t.orderId())
                                                                    .setPrice(t.price()
                                                                               .doubleValue())
                                                                    .setAmount(t.amount()
                                                                                .doubleValue())
                                                                    .build())
                                                     .collect(Collectors.toList()))
                                  .setPendingAmount(order.pendingAmount().doubleValue())
                                  .build();
    }

    private static Mono<? extends OrderEntry> validateOrderAmount(OrderEntry order) {
        if (order.price().compareTo(BigDecimal.ZERO) > 0) {
            return Mono.just(order);
        } else {
            return Mono.error(new IllegalStateException("Order already executed."));
        }
    }

}
