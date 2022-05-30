package io.bux.matchingengine.cqrs;

import io.bux.matchingengine.domain.Book;
import io.bux.matchingengine.domain.BookAggregateRepository;
import io.bux.matchingengine.domain.query.OrderType;
import io.bux.matchingengine.domain.command.CancelOrderCommand;
import io.bux.matchingengine.domain.command.MakeOrderCommand;
import io.bux.matchingengine.domain.bus.CommandBus;
import io.bux.matchingengine.domain.engine.MatchingEngine;
import io.bux.matchingengine.domain.events.CancellationRequestedEvent;
import io.bux.matchingengine.domain.events.OrderAcceptedEvent;
import org.junit.jupiter.api.*;
import org.mockito.*;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * @author Stefan Dragisic
 */
class CommandBusTest {

    private CommandBus commandBus;
    private MatchingEngine matchingEngineMock;

    @BeforeEach
    public void setUp() {
        BookAggregateRepository aggregateRepositoryMock = mock(BookAggregateRepository.class);
        matchingEngineMock = mock(MatchingEngine.class);
        Book book = new Book("instrumentId", matchingEngineMock);
        when(aggregateRepositoryMock.load("instrumentId")).thenReturn(Mono.just(book));
        commandBus = new CommandBus(aggregateRepositoryMock);
    }


    @Test
    public void testMakeOrderCommand() {
        StepVerifier.create(commandBus.sendCommand(new MakeOrderCommand("instrumentId",
                                                                        UUID.randomUUID(),
                                                                        OrderType.BUY,
                                                                        BigDecimal.ONE,
                                                                        BigDecimal.valueOf(1))))
                    .expectNextMatches(result -> result instanceof OrderAcceptedEvent &&
                            result.aggregateId().equals("instrumentId")
                            && ((OrderAcceptedEvent) result).type() == OrderType.BUY
                            && ((OrderAcceptedEvent) result).amount().compareTo(BigDecimal.ONE) == 0
                            && ((OrderAcceptedEvent) result).price().compareTo(BigDecimal.ONE) == 0)
                    .verifyComplete();
        verify(matchingEngineMock).placeOrder(anyLong(),
                                              eq("instrumentId"),
                                              any(Instant.class),
                                              eq(OrderType.BUY),
                                              eq(BigDecimal.ONE),
                                              eq(BigDecimal.ONE));
    }

    @Test
    public void testMakeOrderCommandMany() {
        Mono<Void> sendCommands = commandBus.sendCommand(new MakeOrderCommand("instrumentId",
                                                                              UUID.randomUUID(),
                                                                              OrderType.BUY,
                                                                              BigDecimal.ONE,
                                                                              BigDecimal.ONE))
                                            .then(commandBus.sendCommand(new MakeOrderCommand("instrumentId",
                                                                                              UUID.randomUUID(),
                                                                                              OrderType.SELL,
                                                                                              BigDecimal.ONE,
                                                                                              BigDecimal.valueOf(2))))
                                            .then(commandBus.sendCommand(new MakeOrderCommand("instrumentId",
                                                                                              UUID.randomUUID(),
                                                                                              OrderType.BUY,
                                                                                              BigDecimal.valueOf(2),
                                                                                              BigDecimal.valueOf(2))))
                                            .then();

        StepVerifier.create(sendCommands)
                    .verifyComplete();

        ArgumentCaptor<Long> orderIdCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<OrderType> orderTypeCaptor = ArgumentCaptor.forClass(OrderType.class);
        ArgumentCaptor<BigDecimal> amountCaptor = ArgumentCaptor.forClass(BigDecimal.class);
        ArgumentCaptor<BigDecimal> priceCaptor = ArgumentCaptor.forClass(BigDecimal.class);
        verify(matchingEngineMock, times(3)).placeOrder(orderIdCaptor.capture(),
                                                        anyString(),
                                                        any(Instant.class),
                                                        orderTypeCaptor.capture(),
                                                        priceCaptor.capture(),
                                                        amountCaptor.capture());

        assertEquals(orderIdCaptor.getAllValues(), List.of(1L, 2L, 3L));
        assertEquals(orderTypeCaptor.getAllValues(), List.of(OrderType.BUY, OrderType.SELL, OrderType.BUY));
        assertEquals(amountCaptor.getAllValues(), List.of(BigDecimal.ONE, BigDecimal.ONE, BigDecimal.valueOf(2)));
        assertEquals(priceCaptor.getAllValues(), List.of(BigDecimal.ONE, BigDecimal.valueOf(2), BigDecimal.valueOf(2)));
    }

    @Test
    public void testMakeInvalidOrderCommand() {
        StepVerifier.create(commandBus.sendCommand(new MakeOrderCommand("instrumentId",
                                                                        UUID.randomUUID(),
                                                                        OrderType.BUY,
                                                                        BigDecimal.valueOf(-1),
                                                                        BigDecimal.valueOf(-1))))
                    .expectErrorMatches(err -> err instanceof IllegalStateException
                            && err.getMessage().startsWith("Amount/Price needs to be larger then zero!"))
                    .verify();
        verify(matchingEngineMock, times(0)).placeOrder(anyLong(),
                                                        anyString(),
                                                        any(Instant.class),
                                                        any(), any(BigDecimal.class), any(BigDecimal.class));
    }

    @Test
    public void testCancelAllOrderCommand() {
        StepVerifier.create(commandBus.sendCommand(new CancelOrderCommand("instrumentId",
                                                                          UUID.randomUUID(),
                                                                          1,
                                                                          true,
                                                                          BigDecimal.ZERO)))
                    .expectNextMatches(result -> result instanceof CancellationRequestedEvent &&
                            result.aggregateId().equals("instrumentId")
                            && ((CancellationRequestedEvent) result).orderId() == 1
                            && ((CancellationRequestedEvent) result).cancelAll())
                    .verifyComplete();
        verify(matchingEngineMock).cancelAll(1, "instrumentId");
    }

    @Test
    public void testCancelPartialOrderCommand() {
        StepVerifier.create(commandBus.sendCommand(new CancelOrderCommand("instrumentId",
                                                                          UUID.randomUUID(),
                                                                          1,
                                                                          false,
                                                                          BigDecimal.TEN)))
                    .expectNextMatches(result -> result instanceof CancellationRequestedEvent &&
                            result.aggregateId().equals("instrumentId")
                            && ((CancellationRequestedEvent) result).orderId() == 1
                            && !((CancellationRequestedEvent) result).cancelAll()
                            && ((CancellationRequestedEvent) result).newAmount().compareTo(BigDecimal.TEN) == 0)
                    .verifyComplete();
        verify(matchingEngineMock).cancel(1,"instrumentId", BigDecimal.TEN);
    }

    @Test
    public void testCancelPartialInvalidOrderCommand() {
        StepVerifier.create(commandBus.sendCommand(new CancelOrderCommand("instrumentId",
                                                                          UUID.randomUUID(),
                                                                          1,
                                                                          false,
                                                                          BigDecimal.valueOf(-10))))
                    .expectErrorMatches(err -> err instanceof IllegalStateException
                            && err.getMessage().startsWith("Cancellation: new amount can't be <= 0!"))
                    .verify();
        verify(matchingEngineMock, times(0)).cancel(anyLong(), anyString(), any(BigDecimal.class));
    }
}