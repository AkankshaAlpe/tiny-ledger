package com.example.ledger.service;

import com.example.ledger.model.Transaction;
import com.example.ledger.store.LedgerStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.example.ledger.utils.FixtureUtils.deposit;
import static org.assertj.core.api.Assertions.*;

class TransactionServiceTest {

    private LedgerStore store;
    private TransactionService transactions;

    private static final String ACC = "test-account";
    private static final String ACC_B = "other-account";

    @BeforeEach
    void setUp() {
        store = new LedgerStore();
        transactions = new TransactionService(store);
    }

    private void record(String accountId, String description) {
        store.save(deposit(accountId, "10.00", description));
    }

    @Test
    void shouldReturnEmptyHistoryInitially() {
        assertThat(transactions.getTransactions(ACC)).isEmpty();
    }

    @Test
    void shouldReturnHistoryNewestFirst() {
        record(ACC, "First");
        record(ACC, "Second");

        List<Transaction> history = transactions.getTransactions(ACC);
        assertThat(history).hasSize(2);
        assertThat(history.get(0).description()).isEqualTo("Second");
        assertThat(history.get(1).description()).isEqualTo("First");
    }

    @Test
    void accountsHaveIsolatedHistories() {
        record(ACC, "A-deposit");
        record(ACC_B, "B-deposit");

        assertThat(transactions.getTransactions(ACC)).hasSize(1);
        assertThat(transactions.getTransactions(ACC).get(0).description()).isEqualTo("A-deposit");
        assertThat(transactions.getTransactions(ACC_B)).hasSize(1);
        assertThat(transactions.getTransactions(ACC_B).get(0).description()).isEqualTo("B-deposit");
    }
}
