package com.github.schananas.reactivestockmarket.domain;

import com.github.schananas.reactivestockmarket.domain.engine.events.OrderMatchedEvent;
import com.github.schananas.reactivestockmarket.domain.engine.events.OrderPlacedEvent;
import com.github.schananas.reactivestockmarket.domain.query.BookQueryRepository;
import com.github.schananas.reactivestockmarket.domain.query.OrderType;
import org.junit.jupiter.api.*;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * @author Stefan Dragisic
 */
class BookQueryRepositoryTest {

    private BookQueryRepository testSubject;

    @BeforeEach
    void setUp() {
        testSubject = new BookQueryRepository();
    }

    @Test
    void testOrderPlacedProjection() {
        StepVerifier.create(testSubject.updateProjection(new OrderPlacedEvent(1L,
                                                                              "BTC",
                                                                              Instant.MIN,
                                                                              OrderType.SELL,
                                                                              BigDecimal.valueOf(10),
                                                                              BigDecimal.valueOf(100)))
                                       .then(testSubject.getProjection(1L)))
                    .expectNextMatches(orderEntry -> orderEntry.orderId() == 1L
                            && orderEntry.entryTimestamp().equals(Instant.MIN)
                            && orderEntry.direction() == OrderType.SELL
                            && orderEntry.price().compareTo(BigDecimal.valueOf(10)) == 0
                            && orderEntry.amount().compareTo(BigDecimal.valueOf(100)) == 0
                    )
                    .verifyComplete();
    }


    @Test
    void stockTest() {
        StepVerifier.create(testSubject.updateProjection(new OrderPlacedEvent(0L,
                                                                              "BTC",
                                                                              Instant.MIN,
                                                                              OrderType.SELL,
                                                                              BigDecimal.valueOf(
                                                                                      43251.00),
                                                                              BigDecimal.valueOf(
                                                                                      1.0)))
                                       .then(testSubject.getProjection(0L)))
                    .expectNextMatches(orderEntry -> orderEntry.orderId() == 0L
                            && orderEntry.entryTimestamp().equals(Instant.MIN)
                            && orderEntry.direction() == OrderType.SELL
                            && orderEntry.price().compareTo(BigDecimal.valueOf(43251.00))
                            == 0
                            && orderEntry.amount().compareTo(BigDecimal.valueOf(1.0)) == 0
                            && orderEntry.pendingAmount().compareTo(BigDecimal.valueOf(1.0))
                            == 0
                            && orderEntry.trades().isEmpty()
                    )
                    .expectComplete()
                    .verify();

        StepVerifier.create(testSubject.updateProjection(new OrderPlacedEvent(1L,
                                                                              "BTC",
                                                                              Instant.MIN,
                                                                              OrderType.BUY,
                                                                              BigDecimal.valueOf(
                                                                                      43250.00),
                                                                              BigDecimal.valueOf(
                                                                                      0.25)
                                       ))
                                       .then(testSubject.getProjection(1L)))
                    .expectNextMatches(orderEntry -> orderEntry.orderId() == 1L
                            && orderEntry.entryTimestamp().equals(Instant.MIN)
                            && orderEntry.direction() == OrderType.BUY
                            && orderEntry.price().compareTo(BigDecimal.valueOf(43250.00)) == 0
                            && orderEntry.amount().compareTo(BigDecimal.valueOf(0.25)) == 0
                            && orderEntry.pendingAmount().compareTo(BigDecimal.valueOf(0.25))
                            == 0
                            && orderEntry.trades().isEmpty()
                    )
                    .expectComplete()
                    .verify();

        StepVerifier.create(testSubject.updateProjection(new OrderMatchedEvent(0L,
                                                                               "BTC",
                                                                               Instant.MAX,
                                                                               2L,
                                                                               OrderType.BUY,
                                                                               BigDecimal.valueOf(
                                                                                       43250.00),
                                                                               BigDecimal.valueOf(
                                                                                       43251.00),
                                                                               BigDecimal.valueOf(
                                                                                       0.35),
                                                                               BigDecimal.valueOf(
                                                                                       1.0),
                                                                               BigDecimal.valueOf(
                                                                                       0.65)
                                       ))
                                       .then(testSubject.getProjection(0L)))
                    .expectNextMatches(orderEntry -> orderEntry.orderId() == 0L
                            && orderEntry.entryTimestamp().equals(Instant.MIN)
                            && orderEntry.direction() == OrderType.SELL
                            && orderEntry.price().compareTo(BigDecimal.valueOf(43251.00)) == 0
                            && orderEntry.amount().compareTo(BigDecimal.valueOf(1.0)) == 0
                            && orderEntry.pendingAmount().compareTo(BigDecimal.valueOf(0.65))
                            == 0
                            && orderEntry.trades().stream().allMatch(t -> t.orderId() == 2L
                            && t.amount().compareTo(BigDecimal.valueOf(0.35)) == 0
                            && t.price().compareTo(BigDecimal.valueOf(
                            43251.00)) == 0)
                    )
                    .expectComplete()
                    .verify();


        StepVerifier.create(testSubject.updateProjection(new OrderMatchedEvent(0L,
                                                                               "BTC",
                                                                               Instant.MAX,
                                                                               3L,
                                                                               OrderType.BUY,
                                                                               BigDecimal.valueOf(
                                                                                       43250.00),
                                                                               BigDecimal.valueOf(
                                                                                       43251.00),
                                                                               BigDecimal.valueOf(
                                                                                       0.65),
                                                                               BigDecimal.valueOf(
                                                                                       0.65),
                                                                               BigDecimal.valueOf(
                                                                                       0.0)
                                       ))
                                       .then(testSubject.getProjection(0L)))
                    .expectNextMatches(orderEntry -> orderEntry.orderId() == 0L
                            && orderEntry.entryTimestamp().equals(Instant.MIN)
                            && orderEntry.direction() == OrderType.SELL
                            && orderEntry.price().compareTo(BigDecimal.valueOf(43251.00)) == 0
                            && orderEntry.amount().compareTo(BigDecimal.valueOf(1)) == 0
                            && orderEntry.pendingAmount().compareTo(BigDecimal.valueOf(0.0))
                            == 0
                            && orderEntry.trades().stream().anyMatch(t -> t.orderId() == 2L
                            && t.amount().compareTo(BigDecimal.valueOf(0.35)) == 0
                            && t.price().compareTo(BigDecimal.valueOf(
                            43251.00)) == 0)
                            && orderEntry.trades().stream().anyMatch(t -> t.orderId() == 3L
                            && t.amount().compareTo(BigDecimal.valueOf(0.65)) == 0
                            && t.price().compareTo(BigDecimal.valueOf(
                            43251.00)) == 0)
                    )
                    .expectComplete()
                    .verify();
    }
}
