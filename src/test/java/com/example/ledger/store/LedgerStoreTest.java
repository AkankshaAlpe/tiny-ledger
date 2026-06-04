package com.example.ledger.store;

import com.example.ledger.model.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static com.example.ledger.utils.FixtureUtils.deposit;
import static com.example.ledger.utils.FixtureUtils.withdrawal;
import static org.assertj.core.api.Assertions.*;

class LedgerStoreTest {

    private LedgerStore store;

    private static final String ACC = "test-account";
    private static final String ACC_B = "other-account";

    @BeforeEach
    void setUp() {
        store = new LedgerStore();
    }

    @Test
    void emptyAccountHasZeroBalanceAndEmptyHistory() {
        assertThat(store.balance(ACC)).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(store.getTransactions(ACC)).isEmpty();
    }

    @Test
    void saveReturnsTheSameTransaction() {
        Transaction saved = store.save(deposit(ACC, "100.00", "Salary"));
        assertThat(store.getTransactions(ACC)).containsExactly(saved);
    }

    @Test
    void balanceSumsDepositsMinusWithdrawals() {
        store.save(deposit(ACC, "200.00", null));
        store.save(withdrawal(ACC, "75.00", null));

        assertThat(store.balance(ACC)).isEqualByComparingTo("125.00");
    }

    @Test
    void getTransactionsReturnsNewestFirst() {
        store.save(deposit(ACC, "10.00", "First"));
        store.save(deposit(ACC, "20.00", "Second"));

        List<Transaction> history = store.getTransactions(ACC);
        assertThat(history).hasSize(2);
        assertThat(history.get(0).description()).isEqualTo("Second");
        assertThat(history.get(1).description()).isEqualTo("First");
    }

    @Test
    void accountsAreIsolated() {
        store.save(deposit(ACC, "500.00", null));
        store.save(deposit(ACC_B, "100.00", null));

        assertThat(store.balance(ACC)).isEqualByComparingTo("500.00");
        assertThat(store.balance(ACC_B)).isEqualByComparingTo("100.00");
        assertThat(store.getTransactions(ACC)).hasSize(1);
        assertThat(store.getTransactions(ACC_B)).hasSize(1);
    }
}
