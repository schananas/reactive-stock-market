package io.bux.matchingengine.cqrs;

/**
 * Interface to define event
 * @author Stefan Dragisic
 */
public interface Event {

    /**
     * Aggregate identifier that is used to uniquely represent asset
     *
     * @return unique aggregate identifier
     */
    String aggregateId();

}
