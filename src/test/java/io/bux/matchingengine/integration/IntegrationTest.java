package io.bux.matchingengine.integration;

import io.bux.matchingengine.MatchingEngineApplication;
import io.bux.matchingengine.api.protobuf.OrderStatusResponse;
import io.bux.matchingengine.api.protobuf.OrderType;
import org.junit.*;
import org.junit.runner.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Stefan Dragisic
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = MatchingEngineApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class IntegrationTest {

    private final Logger logger = LoggerFactory.getLogger(IntegrationTest.class);

    private final String SIMPLE_SELL_ORDER = """
            {
                "asset": "BTC",
                "price": 43251.00,
                "amount": 1.0,
                "direction": "SELL"
            }
                        """.stripIndent();

    private final String SIMPLE_BUY_ORDER = """
            {
                "asset": "BTC",
                    "price": 43252.00,
                    "amount": 0.25,
                    "direction": "BUY"
            }
                                """.stripIndent();
    private final String BTC_SELL_ORDER = """
            {
                "asset": "BTC",
                "price": 40000.00,
                "amount": 1.0,
                "direction": "SELL"
            }
                        """.stripIndent();
    private final String BTC_BUY_ORDER = """
            {
                "asset": "BTC",
                    "price": 40000.00,
                    "amount": 1,
                    "direction": "BUY"
            }
                                """.stripIndent();
    private final String SOL_SELL_ORDER = """
            {
                "asset": "BTC",
                "price": 40000.00,
                "amount": 1.0,
                "direction": "SELL"
            }
                        """.stripIndent();
    private final String SOL_BUY_ORDER = """
            {
                "asset": "BTC",
                    "price": 40000.00,
                    "amount": 1,
                    "direction": "BUY"
            }
                                """.stripIndent();
    @LocalServerPort
    private int port;
    private WebClient client;

    @Before
    public void setUp() {
        client = WebClient.builder()
                          .baseUrl("http://localhost:" + port)
                          .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                          .build();
    }

    @Test
    public void simplePlaceAndGetOrder() {
        AtomicLong id = new AtomicLong();
        StepVerifier.create((client.post()
                                   .uri("/orders"))
                                    .body(Mono.just(SIMPLE_SELL_ORDER), String.class).header(HttpHeaders.CONTENT_TYPE,
                                                                                             MediaType.APPLICATION_JSON_VALUE)
                                    .retrieve().bodyToMono(OrderStatusResponse.class)
                                    .doOnNext(or -> id.set(or.getId())))
                    .expectNextMatches(response ->
                                               response.getAsset().equals("BTC")
                                                       && response.getPrice() == 43251.00
                                                       && response.getAmount() == 1.0
                                                       && response.getDirection().equals(OrderType.SELL)
                                                       && response.getPendingAmount() == 1.0)
                    .verifyComplete();

        StepVerifier.create((client.get()
                                   .uri("/orders/" + id + "/"))
                                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                    .retrieve().bodyToMono(OrderStatusResponse.class))
                    .expectNextMatches(response ->
                                               response.getAsset().equals("BTC")
                                                       && response.getPrice() == 43251.00
                                                       && response.getAmount() == 1.0
                                                       && response.getDirection().equals(OrderType.SELL)
                                                       && response.getPendingAmount() == 1.0)
                    .verifyComplete();
    }

    @Test
    public void simpleCancelOrder() {
        AtomicLong id = new AtomicLong();
        StepVerifier.create((client.post()
                                   .uri("/orders"))
                                    .body(Mono.just(SIMPLE_SELL_ORDER), String.class).header(HttpHeaders.CONTENT_TYPE,
                                                                                             MediaType.APPLICATION_JSON_VALUE)
                                    .retrieve().bodyToMono(OrderStatusResponse.class)
                                    .doOnNext(or -> id.set(or.getId())))
                    .expectNextMatches(response ->
                                               response.getAsset().equals("BTC")
                                                       && response.getPrice() == 43251.00
                                                       && response.getAmount() == 1.0
                                                       && response.getDirection().equals(OrderType.SELL)
                                                       && response.getPendingAmount() == 1.0)
                    .verifyComplete();

        StepVerifier.create((client.post()
                                   .uri("/orders/" + id + "/cancel"))
                                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                    .retrieve().bodyToMono(String.class))
                    .expectNextMatches(response -> response.equals("OK"))
                    .verifyComplete();
    }

    @Test
    public void simpleTradeTest() {
        AtomicLong id = new AtomicLong();
        AtomicLong id2 = new AtomicLong();
        StepVerifier.create((client.post()
                                   .uri("/orders"))
                                    .body(Mono.just(SIMPLE_SELL_ORDER), String.class)
                                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                    .retrieve().bodyToMono(OrderStatusResponse.class)
                                    .doOnNext(or -> id.set(or.getId())))
                    .expectNextMatches(response ->
                                               response.getAsset().equals("BTC")
                                                       && response.getPrice() == 43251.00
                                                       && response.getAmount() == 1.0
                                                       && response.getDirection().equals(OrderType.SELL)
                                                       && response.getPendingAmount() == 1.0)
                    .verifyComplete();

        StepVerifier.create((client.post()
                                   .uri("/orders"))
                                    .body(Mono.just(SIMPLE_BUY_ORDER), String.class)
                                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                    .retrieve().bodyToMono(OrderStatusResponse.class)
                                    .doOnNext(or -> id2.set(or.getId())))
                    .expectNextMatches(response ->
                                               response.getAsset().equals("BTC")
                                                       && response.getPrice() == 43252.00
                                                       && response.getAmount() == 0.25
                                                       && response.getDirection().equals(OrderType.BUY))
                    .verifyComplete();

        StepVerifier.create((client.get()
                                   .uri("/orders/" + id + "/"))
                                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                    .retrieve().bodyToMono(OrderStatusResponse.class))
                    .expectNextMatches(response ->
                                               response.getAsset().equals("BTC")
                                                       && response.getPrice() == 43251.00
                                                       && response.getAmount() == 1.0
                                                       && response.getDirection().equals(OrderType.SELL)
                                                       && response.getPendingAmount() == 0.75
                                                       && response.getTradesList().stream()
                                                                  .anyMatch(t -> t.getOrderId() == id2.get()
                                                                          && t.getAmount() == 0.25
                                                                          && t.getPrice() == 43251.0
                                                                  )
                    )
                    .verifyComplete();
    }

    @Test
    public void stressTest() {
        Mono<OrderStatusResponse> btcSellOrderMono = WebClient.builder()
                                                              .baseUrl("http://localhost:" + port)
                                                              .defaultHeader(HttpHeaders.CONTENT_TYPE,
                                                                             MediaType.APPLICATION_JSON_VALUE)
                                                              .build().post()
                                                              .uri("/orders")
                                                              .body(Mono.just(BTC_SELL_ORDER), String.class)
                                                              .header(HttpHeaders.CONTENT_TYPE,
                                                                      MediaType.APPLICATION_JSON_VALUE)
                                                              .retrieve().bodyToMono(OrderStatusResponse.class).timeout(
                        Duration.ofSeconds(1));

        Mono<OrderStatusResponse> btcBuyOrderMono = WebClient.builder()
                                                             .baseUrl("http://localhost:" + port)
                                                             .defaultHeader(HttpHeaders.CONTENT_TYPE,
                                                                            MediaType.APPLICATION_JSON_VALUE)
                                                             .build().post()
                                                             .uri("/orders")
                                                             .body(Mono.just(BTC_BUY_ORDER), String.class)
                                                             .header(HttpHeaders.CONTENT_TYPE,
                                                                     MediaType.APPLICATION_JSON_VALUE)
                                                             .retrieve().bodyToMono(OrderStatusResponse.class)
                                                             .subscribeOn(Schedulers.boundedElastic()).timeout(
                        Duration.ofSeconds(1));

        Mono<OrderStatusResponse> solSellOrderMono = WebClient.builder()
                                                              .baseUrl("http://localhost:" + port)
                                                              .defaultHeader(HttpHeaders.CONTENT_TYPE,
                                                                             MediaType.APPLICATION_JSON_VALUE)
                                                              .build().post()
                                                              .uri("/orders")
                                                              .body(Mono.just(SOL_SELL_ORDER), String.class)
                                                              .header(HttpHeaders.CONTENT_TYPE,
                                                                      MediaType.APPLICATION_JSON_VALUE)
                                                              .retrieve().bodyToMono(OrderStatusResponse.class)
                                                              .subscribeOn(Schedulers.boundedElastic()).timeout(
                        Duration.ofSeconds(1));

        Mono<OrderStatusResponse> solBuyOrderMono = WebClient.builder()
                                                             .baseUrl("http://localhost:" + port)
                                                             .defaultHeader(HttpHeaders.CONTENT_TYPE,
                                                                            MediaType.APPLICATION_JSON_VALUE)
                                                             .build().post()
                                                             .uri("/orders")
                                                             .body(Mono.just(SOL_BUY_ORDER), String.class)
                                                             .header(HttpHeaders.CONTENT_TYPE,
                                                                     MediaType.APPLICATION_JSON_VALUE)
                                                             .retrieve().bodyToMono(OrderStatusResponse.class)
                                                             .subscribeOn(Schedulers.boundedElastic()).timeout(
                        Duration.ofSeconds(1));

        Flux<OrderStatusResponse> buySellAll = Flux.merge(btcBuyOrderMono,
                                                          solSellOrderMono,
                                                          btcSellOrderMono,
                                                          solBuyOrderMono);

        //16 threads
        //2 district asset
        //2 operations per asset (BUY/SELL)
        //executed 1000 times
        //total 4000 operations
        Duration duration = StepVerifier.create(Flux.range(0, 1000).flatMap(unused -> buySellAll, 16))
                                        .expectNextCount(4000)
                                        .verifyComplete();

        logger.info("Stress test took: {} ms", duration.toMillis());
        logger.info("Average: {} ms / end to end rest call",  duration.toMillis() / 4000);
    }
}