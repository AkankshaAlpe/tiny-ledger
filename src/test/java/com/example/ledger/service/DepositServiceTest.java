package com.example.ledger.service;

import com.example.ledger.model.Transaction;
import com.example.ledger.model.TransactionType;
import com.example.ledger.store.IdempotencyStore;
import com.example.ledger.store.LedgerStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static com.example.ledger.utils.FixtureUtils.txId;
import static org.assertj.core.api.Assertions.*;

class DepositServiceTest {

    private LedgerStore store;
    private DepositService deposits;

    private static final String ACC = "test-account";
    private static final String ACC_B = "other-account";

    @BeforeEach
    void setUp() {
        store = new LedgerStore();
        deposits = new DepositService(store, new IdempotencyStore());
    }

    @Test
    void shouldDepositAndUpdateBalance() {
        Transaction tx = deposits.deposit(txId(), ACC, new BigDecimal("100.00"), "Salary");

        assertThat(tx.accountId()).isEqualTo(ACC);
        assertThat(tx.type()).isEqualTo(TransactionType.DEPOSIT);
        assertThat(tx.amount()).isEqualByComparingTo("100.00");
        assertThat(tx.balanceAfter()).isEqualByComparingTo("100.00");
        assertThat(store.balance(ACC)).isEqualByComparingTo("100.00");
    }

    @Test
    void shouldAccumulateMultipleDeposits() {
        deposits.deposit(txId(), ACC, new BigDecimal("100.00"), null);
        deposits.deposit(txId(), ACC, new BigDecimal("200.00"), null);
        deposits.deposit(txId(), ACC, new BigDecimal("50.00"), null);

        assertThat(store.balance(ACC)).isEqualByComparingTo("350.00");
    }

    @Test
    void shouldRejectNegativeAmount() {
        assertThatThrownBy(() -> deposits.deposit(txId(), ACC, new BigDecimal("-1.00"), null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectZeroAmount() {
        assertThatThrownBy(() -> deposits.deposit(txId(), ACC, BigDecimal.ZERO, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectNullAmount() {
        assertThatThrownBy(() -> deposits.deposit(txId(), ACC, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldAssignUniqueIdsToTransactions() {
        Transaction t1 = deposits.deposit(txId(), ACC, new BigDecimal("10.00"), null);
        Transaction t2 = deposits.deposit(txId(), ACC, new BigDecimal("10.00"), null);

        assertThat(t1.id()).isNotEqualTo(t2.id());
    }

    @Test
    void shouldSetTimestampOnTransaction() {
        Transaction tx = deposits.deposit(txId(), ACC, new BigDecimal("10.00"), null);
        assertThat(tx.timestamp()).isNotNull();
    }

    @Test
    void sameTransactionIdReplaysWithoutDoubleCharging() {
        String id = "tx-deposit-1";
        Transaction first = deposits.deposit(id, ACC, new BigDecimal("100.00"), "Salary");
        Transaction replay = deposits.deposit(id, ACC, new BigDecimal("100.00"), "Salary");

        assertThat(replay.id()).isEqualTo(first.id());
        assertThat(store.balance(ACC)).isEqualByComparingTo("100.00"); // charged once, not 200
        assertThat(store.getTransactions(ACC)).hasSize(1);
    }

    @Test
    void depositsAreIsolatedAcrossAccounts() {
        deposits.deposit(txId(), ACC, new BigDecimal("500.00"), null);
        deposits.deposit(txId(), ACC_B, new BigDecimal("100.00"), null);

        assertThat(store.balance(ACC)).isEqualByComparingTo("500.00");
        assertThat(store.balance(ACC_B)).isEqualByComparingTo("100.00");
    }
}
