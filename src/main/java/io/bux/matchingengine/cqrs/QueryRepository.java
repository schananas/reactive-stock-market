package io.bux.matchingengine.cqrs;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author Stefan Dragisic
 */
public interface QueryRepository<T> {

    Mono<Void> updateProjection(Event e);
    //Flux<Void> getBookOrders(String book);
    Mono<T> getOrder(long orderId);
}
