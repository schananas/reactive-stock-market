package com.github.schananas.reactivestockexchange.cqrs;

import reactor.core.publisher.Mono;

/**
 * Interface to represent query repository
 *
 * @author Stefan Dragisic
 */
public interface QueryRepository<T> {

    /**
     * Updates projection with event. Repository uses this event to create/maintain projections.
     *
     * @param e - materialized event
     */
    Mono<Void> updateProjection(Event e);

    /**
     * Returns current order projection
     *
     * @param orderId - order identifier
     * @return - materialized projection
     */
    Mono<T> getOrder(long orderId);
}
