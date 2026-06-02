package com.example.ledger.service;

import com.example.ledger.store.LedgerStore;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class BalanceService {

    private final LedgerStore store;

    public BalanceService(LedgerStore store) {
        this.store = store;
    }

    public BigDecimal getBalance(String accountId) {
        return store.balance(accountId);
    }
}
