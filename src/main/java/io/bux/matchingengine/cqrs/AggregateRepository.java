package io.bux.matchingengine.cqrs;

import reactor.core.publisher.Mono;

/**
 * Represents repository that stores all aggregates
 *
 * @author Stefan Dragisic
 */
public interface AggregateRepository<T extends Aggregate> {

    Mono<T> load(String aggregateId);
}
