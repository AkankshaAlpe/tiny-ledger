package com.example.ledger.store;

import com.example.ledger.model.Transaction;
import com.example.ledger.model.TransactionType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory storage shared by all ledger services.
 *
 * <p>Writers must hold the monitor of this instance ({@code synchronized (store)})
 * around any read-balance-then-save sequence so concurrent deposits/withdrawals
 * across separate service beans stay atomic.
 */
@Component
public class LedgerStore {

    // accountId → transactions in chronological (insertion) order
    private final ConcurrentHashMap<String, List<Transaction>> store = new ConcurrentHashMap<>();

    private List<Transaction> transactionsFor(String accountId) {
        return store.computeIfAbsent(accountId, k -> new ArrayList<>());
    }

    public BigDecimal balance(String accountId) {
        return transactionsFor(accountId).stream()
                .map(t -> t.type() == TransactionType.DEPOSIT ? t.amount() : t.amount().negate())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public Transaction save(Transaction transaction) {
        transactionsFor(transaction.accountId()).add(transaction);
        return transaction;
    }

    /** Returns the account's transactions newest-first (insertion order reversed). */
    public List<Transaction> getTransactions(String accountId) {
        List<Transaction> newestFirst = new ArrayList<>(transactionsFor(accountId));
        Collections.reverse(newestFirst);
        return newestFirst;
    }
}
