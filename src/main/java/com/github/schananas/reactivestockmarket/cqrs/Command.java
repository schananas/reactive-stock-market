package com.github.schananas.reactivestockmarket.cqrs;

import java.util.UUID;

/**
 * Interface to define command
 *
 * @author Stefan Dragisic
 */
public interface Command {

    /**
     * Aggregate identifier that is used to uniquely represent asset
     *
     * @return unique aggregate identifier
     */
    String aggregateId();

    /**
     * Uniquely identifies command
     *
     * @return unique command identifier
     */
    UUID commandId();
}
