package com.example.ledger.store;

import com.example.ledger.model.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static com.example.ledger.utils.FixtureUtils.deposit;
import static org.assertj.core.api.Assertions.*;

class IdempotencyStoreTest {

    private IdempotencyStore store;

    @BeforeEach
    void setUp() {
        store = new IdempotencyStore();
    }

    private Transaction newTransaction() {
        return deposit("acc", "100.00", null);
    }

    @Test
    void firstCallRunsWorkAndReturnsResult() {
        Transaction tx = newTransaction();
        Transaction result = store.replayOrCompute("key-1", () -> tx);

        assertThat(result).isSameAs(tx);
    }

    @Test
    void repeatedKeyReplaysWithoutRunningWorkAgain() {
        Transaction first = newTransaction();
        AtomicInteger workRuns = new AtomicInteger();

        Transaction r1 = store.replayOrCompute("key-2", () -> { workRuns.incrementAndGet(); return first; });
        Transaction r2 = store.replayOrCompute("key-2", () -> { workRuns.incrementAndGet(); return newTransaction(); });

        assertThat(r2).isSameAs(r1);                 // original result replayed
        assertThat(workRuns).hasValue(1);            // work ran only once
    }

    @Test
    void differentKeysRunIndependently() {
        Transaction a = newTransaction();
        Transaction b = newTransaction();

        assertThat(store.replayOrCompute("key-a", () -> a)).isSameAs(a);
        assertThat(store.replayOrCompute("key-b", () -> b)).isSameAs(b);
    }

    @Test
    void failingWorkIsNotRecordedSoKeyCanBeRetried() {
        assertThatThrownBy(() -> store.replayOrCompute("key-3", () -> {
            throw new IllegalStateException("boom");
        })).isInstanceOf(IllegalStateException.class);

        // Same key now succeeds because the failed attempt was not recorded
        Transaction tx = newTransaction();
        assertThat(store.replayOrCompute("key-3", () -> tx)).isSameAs(tx);
    }
}
