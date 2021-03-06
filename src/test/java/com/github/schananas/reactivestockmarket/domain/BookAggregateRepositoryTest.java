package com.github.schananas.reactivestockmarket.domain;

import com.github.schananas.reactivestockmarket.domain.query.BookQueryRepository;
import org.junit.jupiter.api.*;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.*;

/**
 * @author Stefan Dragisic
 */
class BookAggregateRepositoryTest {

    private final BookAggregateRepository testSubject = new BookAggregateRepository(mock(BookQueryRepository.class));

    @Test
    public void loadOrCreate() {
        StepVerifier.create(testSubject.load("instrumentId"))
                .expectNextCount(1)
                .verifyComplete();
    }

}