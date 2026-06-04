package com.example.ledger.store;

import com.example.ledger.model.Transaction;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Remembers which {@code Transaction-Id} values have already been processed so a
 * retried request replays the original result instead of creating a second transaction.
 *
 * <p>Callers must invoke {@link #replayOrCompute} while holding the {@link LedgerStore}
 * monitor so the look-up-then-compute sequence stays atomic with the write itself.
 */
@Component
public class IdempotencyStore {

    private final ConcurrentHashMap<String, Transaction> seen = new ConcurrentHashMap<>();

    /**
     * Returns the transaction previously recorded for {@code transactionId}, or runs
     * {@code work}, records its result, and returns it. If {@code work} throws, nothing
     * is recorded so a corrected retry can still succeed.
     */
    public Transaction replayOrCompute(String transactionId, Supplier<Transaction> work) {
        Transaction existing = seen.get(transactionId);
        if (existing != null) {
            return existing;
        }
        Transaction tx = work.get();
        seen.put(transactionId, tx);
        return tx;
    }
}
