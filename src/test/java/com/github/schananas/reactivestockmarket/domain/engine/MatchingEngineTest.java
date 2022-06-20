package com.github.schananas.reactivestockmarket.domain.engine;


import com.github.schananas.reactivestockmarket.domain.engine.events.OrderCanceledEvent;
import com.github.schananas.reactivestockmarket.domain.engine.events.OrderMatchedEvent;
import com.github.schananas.reactivestockmarket.domain.engine.events.OrderPlacedEvent;
import com.github.schananas.reactivestockmarket.domain.query.OrderType;
import org.junit.jupiter.api.*;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * @author Stefan Dragisic
 */
class MatchingEngineTest {

    private final MatchingEngine testSubject = new MatchingEngine();

    @Test
    public void stockTest() {
        StepVerifier.create(testSubject.engineEvents().take(9))
                    .expectSubscription()
                    .then(() -> testSubject.placeOrder(1,
                                                       "BTC",
                                                       Instant.MIN,
                                                       OrderType.SELL,
                                                       BigDecimal.valueOf(10.05),
                                                       BigDecimal.valueOf(20)))
                    .then(() -> testSubject.placeOrder(2,
                                                       "BTC",
                                                       Instant.MIN,
                                                       OrderType.SELL,
                                                       BigDecimal.valueOf(10.04),
                                                       BigDecimal.valueOf(20)))
                    .then(() -> testSubject.placeOrder(3,
                                                       "BTC",
                                                       Instant.MIN,
                                                       OrderType.SELL,
                                                       BigDecimal.valueOf(10.05),
                                                       BigDecimal.valueOf(40)))
                    .then(() -> testSubject.placeOrder(5,
                                                       "BTC",
                                                       Instant.MIN,
                                                       OrderType.BUY,
                                                       BigDecimal.valueOf(10.02),
                                                       BigDecimal.valueOf(40)))
                    .then(() -> testSubject.placeOrder(4,
                                                       "BTC",
                                                       Instant.MIN,
                                                       OrderType.BUY,
                                                       BigDecimal.valueOf(10.00),
                                                       BigDecimal.valueOf(20)))
                    .then(() -> testSubject.placeOrder(6,
                                                       "BTC",
                                                       Instant.MIN,
                                                       OrderType.BUY,
                                                       BigDecimal.valueOf(10.00),
                                                       BigDecimal.valueOf(40)))
                    .expectNextCount(6)
                    .then(() -> testSubject.placeOrder(7,
                                                       "BTC",
                                                       Instant.MIN,
                                                       OrderType.BUY,
                                                       BigDecimal.valueOf(10.06),
                                                       BigDecimal.valueOf(55)))
                    .expectNextMatches(orderEvent -> orderEvent instanceof OrderMatchedEvent
                            && ((OrderMatchedEvent) orderEvent).restingId() == 2
                            && ((OrderMatchedEvent) orderEvent).previousRestingAmount().compareTo(BigDecimal.valueOf(20)) == 0
                    )
                    .expectNextMatches(orderEvent -> orderEvent instanceof OrderMatchedEvent
                            && ((OrderMatchedEvent) orderEvent).restingId() == 1)
                    .expectNextMatches(orderEvent -> orderEvent instanceof OrderMatchedEvent
                            && ((OrderMatchedEvent) orderEvent).restingId() == 3)
                    .expectComplete()
                    .verify();
    }

    @Test
    public void stockTest2() {
        StepVerifier.create(testSubject.engineEvents().take(4))
                    .expectSubscription()
                    .then(() -> testSubject.placeOrder(0L,
                                                       "BTC",
                                                       Instant.MIN,
                                                       OrderType.SELL,
                                                       BigDecimal.valueOf(43251.00),
                                                       BigDecimal.valueOf(1.0)))
                    .then(() -> testSubject.placeOrder(1L,
                                                       "BTC",
                                                       Instant.MIN,
                                                       OrderType.BUY,
                                                       BigDecimal.valueOf(43250.00),
                                                       BigDecimal.valueOf(0.25)))
                    .then(() -> testSubject.placeOrder(2L,
                                                       "BTC",
                                                       Instant.MIN,
                                                       OrderType.BUY,
                                                       BigDecimal.valueOf(43253.00),
                                                       BigDecimal.valueOf(0.35)))
                    .then(() -> testSubject.placeOrder(4L,
                                                       "BTC",
                                                       Instant.MIN,
                                                       OrderType.BUY,
                                                       BigDecimal.valueOf(43251.00),
                                                       BigDecimal.valueOf(0.65)))
                    .expectNextMatches(orderEvent -> orderEvent instanceof OrderPlacedEvent
                            && ((OrderPlacedEvent) orderEvent).orderId() == 0L
                            && ((OrderPlacedEvent) orderEvent).amount().compareTo(BigDecimal.valueOf(1.0)) == 0
                            && ((OrderPlacedEvent) orderEvent).price().compareTo(BigDecimal.valueOf(43251.00)) == 0)
                    .expectNextMatches(orderEvent -> orderEvent instanceof OrderPlacedEvent
                            && ((OrderPlacedEvent) orderEvent).orderId() == 1L
                            && ((OrderPlacedEvent) orderEvent).amount().compareTo(BigDecimal.valueOf(0.25)) == 0
                            && ((OrderPlacedEvent) orderEvent).price().compareTo(BigDecimal.valueOf(43250.00)) == 0)
                    .expectNextMatches(orderEvent -> orderEvent instanceof OrderMatchedEvent
                            && ((OrderMatchedEvent) orderEvent).restingId() == 0L
                            && ((OrderMatchedEvent) orderEvent).incomingId() == 2L
                            && ((OrderMatchedEvent) orderEvent).orderType() == OrderType.BUY
                            && ((OrderMatchedEvent) orderEvent).restingPrice().compareTo(BigDecimal.valueOf(43251.00)) == 0
                            && ((OrderMatchedEvent) orderEvent).incomingAmount().compareTo(BigDecimal.valueOf(0.35)) == 0
                            && ((OrderMatchedEvent) orderEvent).previousRestingAmount().compareTo(BigDecimal.valueOf(1.0)) == 0
                            && ((OrderMatchedEvent) orderEvent).restingRemainingAmount().compareTo(BigDecimal.valueOf(0.65)) == 0
                    )
                    .expectNextMatches(orderEvent -> orderEvent instanceof OrderMatchedEvent
                            && ((OrderMatchedEvent) orderEvent).restingId() == 0L
                            && ((OrderMatchedEvent) orderEvent).incomingId() == 4L
                            && ((OrderMatchedEvent) orderEvent).orderType() == OrderType.BUY
                            && ((OrderMatchedEvent) orderEvent).restingPrice().compareTo(BigDecimal.valueOf(43251.00)) == 0
                            && ((OrderMatchedEvent) orderEvent).incomingAmount().compareTo(BigDecimal.valueOf(0.65)) == 0
                            && ((OrderMatchedEvent) orderEvent).previousRestingAmount().compareTo(BigDecimal.valueOf(0.65)) == 0
                            && ((OrderMatchedEvent) orderEvent).restingRemainingAmount().compareTo(BigDecimal.valueOf(0.0)) == 0
                    )
                    .expectComplete()
                    .verify();
    }

    @Test
    public void filledBuyTest() {
        StepVerifier.create(testSubject.engineEvents().take(2))
                    .expectSubscription()
                    .then(() -> testSubject.placeOrder(1,
                                                       "BTC",
                                                       Instant.MIN,
                                                       OrderType.SELL,
                                                       BigDecimal.valueOf(100.0),
                                                       BigDecimal.valueOf(1.0)))
                    .then(() -> testSubject.placeOrder(2,
                                                       "BTC",
                                                       Instant.MIN,
                                                       OrderType.BUY,
                                                       BigDecimal.valueOf(100.0),
                                                       BigDecimal.valueOf(1.0)))
                    .expectNextMatches(orderEvent -> orderEvent instanceof OrderPlacedEvent)
                    .expectNextMatches(orderEvent -> orderEvent instanceof OrderMatchedEvent
                            && ((OrderMatchedEvent) orderEvent).orderType() == OrderType.BUY)
                    .expectComplete()
                    .verify();
    }

    @Test
    public void filledSellTest() {
        StepVerifier.create(testSubject.engineEvents().take(2))
                    .expectSubscription()
                    .then(() -> testSubject.placeOrder(1,
                                                       "BTC",
                                                       Instant.MIN,
                                                       OrderType.BUY,
                                                       BigDecimal.valueOf(100.10),
                                                       BigDecimal.valueOf(10.1)))
                    .then(() -> testSubject.placeOrder(2,
                                                       "BTC",
                                                       Instant.MIN,
                                                       OrderType.SELL,
                                                       BigDecimal.valueOf(100.10),
                                                       BigDecimal.valueOf(10.1)))
                    .expectNextMatches(orderEvent -> orderEvent instanceof OrderPlacedEvent)
                    .expectNextMatches(orderEvent -> orderEvent instanceof OrderMatchedEvent
                            && ((OrderMatchedEvent) orderEvent).orderType() == OrderType.SELL)
                    .expectComplete()
                    .verify();
    }

    @Test
    public void multiBuy() {
        StepVerifier.create(testSubject.engineEvents().take(5))
                    .expectSubscription()
                    .then(() -> testSubject.placeOrder(1,
                                                       "BTC",
                                                       Instant.MIN,
                                                       OrderType.SELL,
                                                       BigDecimal.valueOf(1000.0),
                                                       BigDecimal.valueOf(1.00)))
                    .then(() -> testSubject.placeOrder(2,
                                                       "BTC",
                                                       Instant.MIN,
                                                       OrderType.SELL,
                                                       BigDecimal.valueOf(1001.0),
                                                       BigDecimal.valueOf(1.00)))
                    .then(() -> testSubject.placeOrder(3,
                                                       "BTC",
                                                       Instant.MIN,
                                                       OrderType.SELL,
                                                       BigDecimal.valueOf(999.0),
                                                       BigDecimal.valueOf(0.50)))
                    .then(() -> testSubject.placeOrder(4,
                                                       "BTC",
                                                       Instant.MIN,
                                                       OrderType.BUY,
                                                       BigDecimal.valueOf(1000.0),
                                                       BigDecimal.valueOf(1.00)))
                    .expectNextMatches(orderEvent -> orderEvent instanceof OrderPlacedEvent)
                    .expectNextMatches(orderEvent -> orderEvent instanceof OrderPlacedEvent)
                    .expectNextMatches(orderEvent -> orderEvent instanceof OrderPlacedEvent)
                    .expectNextMatches(orderEvent -> orderEvent instanceof OrderMatchedEvent
                            && ((OrderMatchedEvent) orderEvent).restingId() == 3
                            && ((OrderMatchedEvent) orderEvent).restingRemainingAmount().compareTo(BigDecimal.ZERO)
                            == 0
                            && ((OrderMatchedEvent) orderEvent).restingPrice().compareTo(BigDecimal.valueOf(999.0))
                            == 0
                            && ((OrderMatchedEvent) orderEvent).entryTimestamp().equals(Instant.MIN)
                    )
                    .expectNextMatches(orderEvent -> orderEvent instanceof OrderMatchedEvent
                            && ((OrderMatchedEvent) orderEvent).restingId() == 1
                            && ((OrderMatchedEvent) orderEvent).restingPrice().compareTo(BigDecimal.valueOf(1000.0))
                            == 0
                            && ((OrderMatchedEvent) orderEvent).restingRemainingAmount()
                                                               .compareTo(BigDecimal.valueOf(0.50)) == 0)
                    .expectComplete()
                    .verify();
    }

    @Test
    public void multiSell() {
        StepVerifier.create(testSubject.engineEvents().take(5))
                    .expectSubscription()
                    .then(() -> testSubject.placeOrder(1,
                                                       "BTC",
                                                       Instant.MIN,
                                                       OrderType.BUY,
                                                       BigDecimal.valueOf(1.000),
                                                       BigDecimal.valueOf(1.0)))
                    .then(() -> testSubject.placeOrder(2,
                                                       "BTC",
                                                       Instant.MIN,
                                                       OrderType.BUY,
                                                       BigDecimal.valueOf(0.999),
                                                       BigDecimal.valueOf(1.0)))
                    .then(() -> testSubject.placeOrder(3,
                                                       "BTC",
                                                       Instant.MIN,
                                                       OrderType.BUY,
                                                       BigDecimal.valueOf(1.001),
                                                       BigDecimal.valueOf(0.5)))
                    .then(() -> testSubject.placeOrder(4,
                                                       "BTC",
                                                       Instant.MIN,
                                                       OrderType.SELL,
                                                       BigDecimal.valueOf(1),
                                                       BigDecimal.valueOf(1.0)))
                    .expectNextMatches(orderEvent -> orderEvent instanceof OrderPlacedEvent)
                    .expectNextMatches(orderEvent -> orderEvent instanceof OrderPlacedEvent)
                    .expectNextMatches(orderEvent -> orderEvent instanceof OrderPlacedEvent)
                    .expectNextMatches(orderEvent -> orderEvent instanceof OrderMatchedEvent
                            && ((OrderMatchedEvent) orderEvent).restingId() == 3
                            && ((OrderMatchedEvent) orderEvent).restingPrice().compareTo(BigDecimal.valueOf(1.001))
                            == 0
                            && ((OrderMatchedEvent) orderEvent).restingRemainingAmount().compareTo(BigDecimal.ZERO)
                            == 0)
                    .expectNextMatches(orderEvent -> orderEvent instanceof OrderMatchedEvent
                            && ((OrderMatchedEvent) orderEvent).restingId() == 1
                            && ((OrderMatchedEvent) orderEvent).restingPrice().compareTo(BigDecimal.valueOf(1)) == 0
                            && ((OrderMatchedEvent) orderEvent).restingRemainingAmount()
                                                               .compareTo(BigDecimal.valueOf(0.5)) == 0)
                    .expectComplete()
                    .verify();
    }

    @Test
    public void partialBuy() {
        StepVerifier.create(testSubject.engineEvents().take(3))
                    .expectSubscription()
                    .then(() -> testSubject.placeOrder(1,
                                                       "BTC",
                                                       Instant.MIN,
                                                       OrderType.SELL,
                                                       BigDecimal.valueOf(1000),
                                                       BigDecimal.valueOf(50)))
                    .then(() -> testSubject.placeOrder(2,
                                                       "BTC",
                                                       Instant.MIN,
                                                       OrderType.BUY,
                                                       BigDecimal.valueOf(1000),
                                                       BigDecimal.valueOf(100)))
                    .expectNextMatches(orderEvent -> orderEvent instanceof OrderPlacedEvent)
                    .expectNextMatches(orderEvent -> orderEvent instanceof OrderMatchedEvent
                            && ((OrderMatchedEvent) orderEvent).restingId() == 1
                            && ((OrderMatchedEvent) orderEvent).restingRemainingAmount().compareTo(BigDecimal.ZERO)
                            == 0)
                    .expectNextMatches(orderEvent -> orderEvent instanceof OrderPlacedEvent
                            && ((OrderPlacedEvent) orderEvent).orderId() == 2
                            && ((OrderPlacedEvent) orderEvent).price().compareTo(BigDecimal.valueOf(1000)) == 0
                            && ((OrderPlacedEvent) orderEvent).amount().compareTo(BigDecimal.valueOf(50)) == 0)
                    .expectComplete()
                    .verify();
    }

    @Test
    public void partialSell() {
        StepVerifier.create(testSubject.engineEvents().take(3))
                    .expectSubscription()
                    .then(() -> testSubject.placeOrder(1,
                                                       "BTC",
                                                       Instant.MIN,
                                                       OrderType.BUY,
                                                       BigDecimal.valueOf(1000),
                                                       BigDecimal.valueOf(50)))
                    .then(() -> testSubject.placeOrder(2,
                                                       "BTC",
                                                       Instant.MIN,
                                                       OrderType.SELL,
                                                       BigDecimal.valueOf(1000),
                                                       BigDecimal.valueOf(100)))
                    .expectNextMatches(orderEvent -> orderEvent instanceof OrderPlacedEvent)
                    .expectNextMatches(orderEvent -> orderEvent instanceof OrderMatchedEvent
                            && ((OrderMatchedEvent) orderEvent).restingId() == 1
                            && ((OrderMatchedEvent) orderEvent).restingRemainingAmount().compareTo(BigDecimal.ZERO)
                            == 0)
                    .expectNextMatches(orderEvent -> orderEvent instanceof OrderPlacedEvent
                            && ((OrderPlacedEvent) orderEvent).orderId() == 2
                            && ((OrderPlacedEvent) orderEvent).price().compareTo(BigDecimal.valueOf(1000)) == 0
                            && ((OrderPlacedEvent) orderEvent).amount().compareTo(BigDecimal.valueOf(50)) == 0)
                    .expectComplete()
                    .verify();
    }

    @Test
    public void partialBidFill() {
        StepVerifier.create(testSubject.engineEvents().take(5))
                    .expectSubscription()
                    .then(() -> testSubject.placeOrder(1,
                                                       "BTC",
                                                       Instant.MIN,
                                                       OrderType.BUY,
                                                       BigDecimal.valueOf(1000),
                                                       BigDecimal.valueOf(100)))
                    .then(() -> testSubject.placeOrder(2,
                                                       "BTC",
                                                       Instant.MIN,
                                                       OrderType.SELL,
                                                       BigDecimal.valueOf(1000),
                                                       BigDecimal.valueOf(50)))
                    .then(() -> testSubject.placeOrder(3,
                                                       "BTC",
                                                       Instant.MIN,
                                                       OrderType.SELL,
                                                       BigDecimal.valueOf(1000),
                                                       BigDecimal.valueOf(50)))
                    .then(() -> testSubject.placeOrder(4,
                                                       "BTC",
                                                       Instant.MIN,
                                                       OrderType.SELL,
                                                       BigDecimal.valueOf(1000),
                                                       BigDecimal.valueOf(50)))
                    .then(() -> testSubject.placeOrder(5,
                                                       "BTC",
                                                       Instant.MIN,
                                                       OrderType.SELL,
                                                       BigDecimal.valueOf(1000),
                                                       BigDecimal.valueOf(50)))
                    .expectNextMatches(orderEvent -> orderEvent instanceof OrderPlacedEvent)
                    .expectNextMatches(orderEvent -> orderEvent instanceof OrderMatchedEvent
                            && ((OrderMatchedEvent) orderEvent).restingId() == 1
                            &&
                            ((OrderMatchedEvent) orderEvent).restingRemainingAmount().compareTo(BigDecimal.valueOf(50))
                                    == 0)
                    .expectNextMatches(orderEvent -> orderEvent instanceof OrderMatchedEvent
                            && ((OrderMatchedEvent) orderEvent).restingId() == 1
                            && ((OrderMatchedEvent) orderEvent).restingRemainingAmount().compareTo(BigDecimal.ZERO)
                            == 0)
                    .expectNextMatches(orderEvent -> orderEvent instanceof OrderPlacedEvent)
                    .expectNextMatches(orderEvent -> orderEvent instanceof OrderPlacedEvent)
                    .expectComplete()
                    .verify();
    }

    @Test
    public void partialAskFill() {
        StepVerifier.create(testSubject.engineEvents().take(4))
                    .expectSubscription()
                    .then(() -> testSubject.placeOrder(1,
                                                       "BTC",
                                                       Instant.MIN,
                                                       OrderType.SELL,
                                                       BigDecimal.valueOf(1000),
                                                       BigDecimal.valueOf(100)))
                    .then(() -> testSubject.placeOrder(2,
                                                       "BTC",
                                                       Instant.MIN,
                                                       OrderType.BUY,
                                                       BigDecimal.valueOf(1000),
                                                       BigDecimal.valueOf(50)))
                    .then(() -> testSubject.placeOrder(3,
                                                       "BTC",
                                                       Instant.MIN,
                                                       OrderType.BUY,
                                                       BigDecimal.valueOf(1000),
                                                       BigDecimal.valueOf(50)))
                    .then(() -> testSubject.placeOrder(4,
                                                       "BTC",
                                                       Instant.MIN,
                                                       OrderType.BUY,
                                                       BigDecimal.valueOf(1000),
                                                       BigDecimal.valueOf(50)))
                    .expectNextMatches(orderEvent -> orderEvent instanceof OrderPlacedEvent)
                    .expectNextMatches(orderEvent -> orderEvent instanceof OrderMatchedEvent
                            && ((OrderMatchedEvent) orderEvent).restingId() == 1
                            &&
                            ((OrderMatchedEvent) orderEvent).restingRemainingAmount().compareTo(BigDecimal.valueOf(50))
                                    == 0)
                    .expectNextMatches(orderEvent -> orderEvent instanceof OrderMatchedEvent
                            && ((OrderMatchedEvent) orderEvent).restingId() == 1
                            && ((OrderMatchedEvent) orderEvent).restingRemainingAmount().compareTo(BigDecimal.ZERO)
                            == 0)
                    .expectNextMatches(orderEvent -> orderEvent instanceof OrderPlacedEvent)
                    .expectComplete()
                    .verify();
    }


    @Test
    public void cancel() {
        StepVerifier.create(testSubject.engineEvents().take(3))
                    .expectSubscription()
                    .then(() -> testSubject.placeOrder(1,
                                                       "BTC",
                                                       Instant.MIN,
                                                       OrderType.BUY,
                                                       BigDecimal.valueOf(1000),
                                                       BigDecimal.valueOf(100)))
                    .then(() -> testSubject.cancelAll(1, "BTC"))
                    .then(() -> testSubject.placeOrder(3,
                                                       "BTC",
                                                       Instant.MIN,
                                                       OrderType.SELL,
                                                       BigDecimal.valueOf(1000),
                                                       BigDecimal.valueOf(100)))
                    .expectNextMatches(orderEvent -> orderEvent instanceof OrderPlacedEvent)
                    .expectNextMatches(orderEvent -> orderEvent instanceof OrderCanceledEvent
                            && ((OrderCanceledEvent) orderEvent).canceledAmount().compareTo(BigDecimal.valueOf(100))
                            == 0)
                    .expectNextMatches(orderEvent -> orderEvent instanceof OrderPlacedEvent)
                    .expectComplete()
                    .verify();
    }

    @Test
    public void partialCancel() {
        StepVerifier.create(testSubject.engineEvents().take(4))
                    .expectSubscription()
                    .then(() -> testSubject.placeOrder(1,
                                                       "BTC",
                                                       Instant.MIN,
                                                       OrderType.BUY,
                                                       BigDecimal.valueOf(1000),
                                                       BigDecimal.valueOf(100)))
                    .then(() -> testSubject.cancel(1,"BTC", BigDecimal.valueOf(75)))
                    .then(() -> testSubject.placeOrder(2,
                                                       "BTC",
                                                       Instant.MIN,
                                                       OrderType.SELL,
                                                       BigDecimal.valueOf(1000),
                                                       BigDecimal.valueOf(100)))
                    .expectNextMatches(orderEvent -> orderEvent instanceof OrderPlacedEvent)
                    .expectNextMatches(orderEvent -> orderEvent instanceof OrderCanceledEvent
                            && ((OrderCanceledEvent) orderEvent).canceledAmount().compareTo(BigDecimal.valueOf(25))
                            == 0)
                    .expectNextMatches(orderEvent -> orderEvent instanceof OrderMatchedEvent)
                    .expectNextMatches(orderEvent -> orderEvent instanceof OrderPlacedEvent)
                    .expectComplete()
                    .verify();
    }

    @Test
    public void ineffectiveCancel() {
        StepVerifier.create(testSubject.engineEvents().take(2))
                    .expectSubscription()
                    .then(() -> testSubject.placeOrder(1,
                                                       "BTC",
                                                       Instant.MIN,
                                                       OrderType.BUY,
                                                       BigDecimal.valueOf(1000),
                                                       BigDecimal.valueOf(100)))
                    .then(() -> testSubject.cancel(1,"BTC", BigDecimal.valueOf(100)))
                    .then(() -> testSubject.cancel(1, "BTC",BigDecimal.valueOf(100)))
                    .then(() -> testSubject.cancel(1, "BTC",BigDecimal.valueOf(100)))
                    .then(() -> testSubject.placeOrder(2,
                                                       "BTC",
                                                       Instant.MIN,
                                                       OrderType.SELL,
                                                       BigDecimal.valueOf(1000),
                                                       BigDecimal.valueOf(100)))
                    .expectNextMatches(orderEvent -> orderEvent instanceof OrderPlacedEvent)
                    .expectNextMatches(orderEvent -> orderEvent instanceof OrderMatchedEvent)
                    .expectComplete()
                    .verify();
    }

    @Test
    public void cancelNonExisting() {
        StepVerifier.create(testSubject.engineEvents().take(2))
                    .expectSubscription()
                    .then(() -> testSubject.placeOrder(1,
                                                       "BTC",
                                                       Instant.MIN,
                                                       OrderType.BUY,
                                                       BigDecimal.valueOf(1000),
                                                       BigDecimal.valueOf(100)))
                    .then(() -> testSubject.cancel(3,"BTC", BigDecimal.valueOf(50)))
                    .then(() -> testSubject.placeOrder(2,
                                                       "BTC",
                                                       Instant.MIN,
                                                       OrderType.SELL,
                                                       BigDecimal.valueOf(1000),
                                                       BigDecimal.valueOf(100)))
                    .expectNextMatches(orderEvent -> orderEvent instanceof OrderPlacedEvent)
                    .expectNextMatches(orderEvent -> orderEvent instanceof OrderMatchedEvent)
                    .expectComplete()
                    .verify();
    }

    @Test
    public void sameId() {
        StepVerifier.create(testSubject.engineEvents().take(3))
                    .expectSubscription()
                    .then(() -> testSubject.placeOrder(1,
                                                       "BTC",
                                                       Instant.MIN,
                                                       OrderType.BUY,
                                                       BigDecimal.valueOf(1000),
                                                       BigDecimal.valueOf(100)))
                    .then(() -> testSubject.placeOrder(2,
                                                       "BTC",
                                                       Instant.MIN,
                                                       OrderType.SELL,
                                                       BigDecimal.valueOf(1000),
                                                       BigDecimal.valueOf(100)))
                    .then(() -> testSubject.placeOrder(1,
                                                       "BTC",
                                                       Instant.MIN,
                                                       OrderType.BUY,
                                                       BigDecimal.valueOf(1000),
                                                       BigDecimal.valueOf(100)))
                    .expectNextMatches(orderEvent -> orderEvent instanceof OrderPlacedEvent)
                    .expectNextMatches(orderEvent -> orderEvent instanceof OrderMatchedEvent)
                    .expectNextMatches(orderEvent -> orderEvent instanceof OrderPlacedEvent)
                    .expectComplete()
                    .verify();
    }
}
