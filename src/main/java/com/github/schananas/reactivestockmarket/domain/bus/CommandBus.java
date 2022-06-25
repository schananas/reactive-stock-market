package com.github.schananas.reactivestockmarket.domain.bus;

import com.github.schananas.reactivestockmarket.cqrs.Command;
import com.github.schananas.reactivestockmarket.cqrs.SourcingEvent;
import com.github.schananas.reactivestockmarket.domain.BookAggregateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

import java.util.function.Consumer;
import javax.annotation.PreDestroy;

import static com.github.schananas.reactivestockmarket.Config.DEFAULT_CONCURRENCY_LEVEL;

/**
 * Routes commands to corresponding aggregate.
 * <p>
 * Routes command for district assets/aggregates in parallel, but routes commands withing one aggregate sequentially.
 * <p>
 * Thread synchronization is done by Reactor {@see <a href="https://github.com/reactor/reactor-core/blob/178e0c7cf799122afdd6bf89be32975d2b433609/reactor-core/src/main/java/reactor/core/publisher/SinksSpecs.java#L46">tryAcquire</a>}
 * <p>
 * Is fire and forget, canceling subscription will not change execution flow, but subscriber has option to "stay" and
 * get signaled once corresponding event has been materialized or if execution has failed.
 *
 * @author Stefan Dragisic
 */
@Component
public class CommandBus {

    private final Logger logger = LoggerFactory.getLogger(CommandBus.class);

    private final Sinks.Many<CommandWrapper> commandExecutor = Sinks.many()
                                                                    .unicast()
                                                                    .onBackpressureBuffer();

    private final Disposable commandExecutorDisposable;

    /**
     * Instantiate command bus by subscribing to hot stream on which commands are published
     *
     * @param aggregateRepository
     */
    public CommandBus(BookAggregateRepository aggregateRepository) {

        commandExecutorDisposable = commandExecutor.asFlux()
                                                   .doOnNext(n -> logger.debug("{} being executed....",
                                                                               n.getCommand().getClass()
                                                                                .getSimpleName()))
                                                   .groupBy(cw -> cw.getCommand().aggregateId()) //multiplex
                                                   .flatMap(aggregateCommands -> aggregateCommands //and execute distinct assets in parallel
                                                           .concatMap(cmd -> aggregateRepository
                                                                   .load(cmd.getCommand().aggregateId())
                                                                   .flatMap(aggregate -> aggregate.routeCommand(cmd.getCommand())
                                                                                                  .flatMap(event -> aggregate.routeEvent(
                                                                                                                                     event)
                                                                                                                             .then(cmd.signalMaterialized(
                                                                                                                                     event)))
                                                                                                  .doOnError(cmd::signalError)


                                                                   )), DEFAULT_CONCURRENCY_LEVEL)
                                                   .subscribe();
    }

    /**
     * Sends a command that is then routed to command handler at corresponding aggregate. Routes command for district
     * assets/aggregates in parallel, but routes commands withing one aggregate sequentially.
     * <p>
     * Is fire and forget - canceling execution does not change execution flow.
     *
     * @param command to send
     * @return sourcing event once it has been materialized
     */
    public Mono<SourcingEvent> sendCommand(Command command) {
        return Mono.defer(() -> {
            Sinks.One<SourcingEvent> actionResult = Sinks.one();
            //de-multiplexes multiple subscriptions by publishing commands to a single flow
            return Mono.<Void>fromRunnable(() -> commandExecutor.emitNext(new CommandWrapper(command,
                                                                                             actionResult::tryEmitValue,
                                                                                             actionResult::tryEmitError),
                                                                          (signalType, emitResult) -> emitResult
                                                                                  .equals(Sinks.EmitResult.FAIL_NON_SERIALIZED)))
                       .subscribeOn(Schedulers.parallel())
                       .then(actionResult.asMono());
        });
    }

    /**
     * Shutdown command bus on bean destruction
     */
    @PreDestroy
    public void destroy() {
        commandExecutor.tryEmitComplete();
        commandExecutorDisposable.dispose();
    }

    private static class CommandWrapper {

        private final Command command;
        private final Consumer<SourcingEvent> signalDone;
        private final Consumer<Throwable> signalError;

        public CommandWrapper(Command command,
                              Consumer<SourcingEvent> action,
                              Consumer<Throwable> signalError) {
            this.command = command;
            this.signalDone = action;
            this.signalError = signalError;
        }

        public Command getCommand() {
            return command;
        }

        public Mono<Void> signalMaterialized(SourcingEvent event) {
            return Mono.fromRunnable(() -> signalDone.accept(event));
        }

        public void signalError(Throwable t) {
            signalError.accept(t);
        }
    }
}
