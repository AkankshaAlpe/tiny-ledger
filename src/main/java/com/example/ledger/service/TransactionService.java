package com.example.ledger.service;

import com.example.ledger.model.Transaction;
import com.example.ledger.store.LedgerStore;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TransactionService {

    private final LedgerStore store;

    public TransactionService(LedgerStore store) {
        this.store = store;
    }

    public List<Transaction> getTransactions(String accountId) {
        return store.getTransactions(accountId);
    }
}
