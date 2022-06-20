package com.github.schananas.reactivestockmarket.cqrs;

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
     * Returns materialized projection
     *
     * @param projectionId - order identifier
     * @return - materialized projection
     */
    Mono<T> getProjection(long projectionId);
}
