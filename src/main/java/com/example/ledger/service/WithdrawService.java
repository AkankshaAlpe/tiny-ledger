package com.example.ledger.service;

import com.example.ledger.exception.InsufficientFundsException;
import com.example.ledger.model.Transaction;
import com.example.ledger.model.TransactionType;
import com.example.ledger.store.IdempotencyStore;
import com.example.ledger.store.LedgerStore;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class WithdrawService {

    private final LedgerStore store;
    private final IdempotencyStore idempotencyStore;

    public WithdrawService(LedgerStore store, IdempotencyStore idempotencyStore) {
        this.store = store;
        this.idempotencyStore = idempotencyStore;
    }

    public Transaction withdraw(String transactionId, String accountId, BigDecimal amount, String description) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive, got: " + amount);
        }
        synchronized (store) {
            return idempotencyStore.replayOrCompute(transactionId, () -> {
                BigDecimal current = store.balance(accountId);
                if (current.compareTo(amount) < 0) {
                    throw new InsufficientFundsException(amount, current);
                }
                return store.save(Transaction.of(
                        accountId, TransactionType.WITHDRAWAL, amount, current.subtract(amount), description));
            });
        }
    }
}
