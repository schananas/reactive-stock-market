package com.github.schananas.reactivestockexchange.cqrs;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Interface to represent aggregate
 *
 * @author Stefan Dragisic
 */
public interface Aggregate {

    /**
     * Aggregate identifier that is used to uniquely represent asset
     *
     * @return unique aggregate identifier
     */
    String aggregateId();

    /**
     * Routes commands to corresponding handler
     *
     * @param command to handle
     * @return event that has been materialized
     */
    Mono<SourcingEvent> routeCommand(Command command);

    /**
     * Routes event  to corresponding handler
     *
     * @param event to handle
     */
    Mono<Void> routeEvent(SourcingEvent event);

    /**
     * Hot stream that emits all aggregate events
     *
     * @return infinitive stream of events
     */
    Flux<Event> aggregateEvents();
}
