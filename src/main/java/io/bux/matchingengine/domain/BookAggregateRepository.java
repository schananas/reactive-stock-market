package io.bux.matchingengine.domain;

import io.bux.matchingengine.cqrs.AggregateRepository;
import io.bux.matchingengine.domain.Book;
import io.bux.matchingengine.domain.query.BookQueryRepository;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe implementation of {@link AggregateRepository} used to store Book aggregates
 *
 * @author Stefan Dragisic
 */
@Component("aggregateRepository")
public class BookAggregateRepository implements AggregateRepository<Book> {

    private final ConcurrentHashMap<String, Book> aggregates = new ConcurrentHashMap<>();
    private final BookQueryRepository bookQueryRepository;

    public BookAggregateRepository(BookQueryRepository bookQueryRepository) {
        this.bookQueryRepository = bookQueryRepository;
    }

    /**
     * Loads aggregate from repository.
     * For convenience of demo if aggregate is not found it will be automatically created and stored in repository.
     * Once aggregate is created, query repository subscribes to its events.
     *
     * @param aggregateId / asset name to load or create from repository
     * @return book aggregate
     */
    @Override
    public Mono<Book> load(String aggregateId) {
        return Mono.fromCallable(() -> aggregates.computeIfAbsent(aggregateId, (k) -> {
            Book book = new Book(aggregateId);
            //subscribe query projection for book events
            book.aggregateEvents().flatMap(bookQueryRepository::updateProjection).subscribe();
            return book;
        }));
    }
}
