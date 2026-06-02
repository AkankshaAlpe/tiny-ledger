package com.example.ledger.service;

import com.example.ledger.model.Transaction;
import com.example.ledger.model.TransactionType;
import com.example.ledger.store.LedgerStore;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class DepositService {

    private final LedgerStore store;

    public DepositService(LedgerStore store) {
        this.store = store;
    }

    public Transaction deposit(String accountId, BigDecimal amount, String description) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive, got: " + amount);
        }
        synchronized (store) {
            BigDecimal balanceAfter = store.balance(accountId).add(amount);
            return store.save(Transaction.of(accountId, TransactionType.DEPOSIT, amount, balanceAfter, description));
        }
    }
}
