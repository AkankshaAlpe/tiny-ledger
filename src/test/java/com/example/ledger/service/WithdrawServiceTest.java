package com.example.ledger.service;

import com.example.ledger.exception.InsufficientFundsException;
import com.example.ledger.model.Transaction;
import com.example.ledger.model.TransactionType;
import com.example.ledger.store.IdempotencyStore;
import com.example.ledger.store.LedgerStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static com.example.ledger.utils.FixtureUtils.deposit;
import static com.example.ledger.utils.FixtureUtils.txId;
import static org.assertj.core.api.Assertions.*;

class WithdrawServiceTest {

    private LedgerStore store;
    private WithdrawService withdrawals;

    private static final String ACC = "test-account";

    @BeforeEach
    void setUp() {
        store = new LedgerStore();
        withdrawals = new WithdrawService(store, new IdempotencyStore());
    }

    /** Seeds funds directly into the store so the test depends only on WithdrawService. */
    private void fund(String accountId, String amount) {
        store.save(deposit(accountId, amount, "seed"));
    }

    @Test
    void shouldWithdrawAndUpdateBalance() {
        fund(ACC, "200.00");
        Transaction tx = withdrawals.withdraw(txId(), ACC, new BigDecimal("50.00"), "Rent");

        assertThat(tx.type()).isEqualTo(TransactionType.WITHDRAWAL);
        assertThat(tx.amount()).isEqualByComparingTo("50.00");
        assertThat(tx.balanceAfter()).isEqualByComparingTo("150.00");
        assertThat(store.balance(ACC)).isEqualByComparingTo("150.00");
    }

    @Test
    void shouldAllowWithdrawalOfExactBalance() {
        fund(ACC, "100.00");
        Transaction tx = withdrawals.withdraw(txId(), ACC, new BigDecimal("100.00"), null);

        assertThat(tx.balanceAfter()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(store.balance(ACC)).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void shouldRejectWithdrawalExceedingBalance() {
        fund(ACC, "50.00");

        assertThatThrownBy(() -> withdrawals.withdraw(txId(), ACC, new BigDecimal("100.00"), null))
                .isInstanceOf(InsufficientFundsException.class)
                .hasMessageContaining("100.00")
                .hasMessageContaining("50.00");
    }

    @Test
    void shouldRejectWithdrawalOnZeroBalance() {
        assertThatThrownBy(() -> withdrawals.withdraw(txId(), ACC, new BigDecimal("1.00"), null))
                .isInstanceOf(InsufficientFundsException.class);
    }

    @Test
    void shouldRejectNegativeAmount() {
        assertThatThrownBy(() -> withdrawals.withdraw(txId(), ACC, new BigDecimal("-1.00"), null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectZeroAmount() {
        assertThatThrownBy(() -> withdrawals.withdraw(txId(), ACC, BigDecimal.ZERO, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void sameTransactionIdReplaysWithoutDoubleCharging() {
        fund(ACC, "200.00");

        String id = "tx-withdraw-1";
        Transaction first = withdrawals.withdraw(id, ACC, new BigDecimal("50.00"), "Rent");
        Transaction replay = withdrawals.withdraw(id, ACC, new BigDecimal("50.00"), "Rent");

        assertThat(replay.id()).isEqualTo(first.id());
        assertThat(store.balance(ACC)).isEqualByComparingTo("150.00"); // withdrawn once
    }

    @Test
    void failedWithdrawalDoesNotRecordTransactionId() {
        String id = "tx-retry-1";
        // First attempt fails (no funds) → id must not be recorded
        assertThatThrownBy(() -> withdrawals.withdraw(id, ACC, new BigDecimal("100.00"), null))
                .isInstanceOf(InsufficientFundsException.class);

        // Fund the account, then a genuine retry with the same id should now succeed
        fund(ACC, "100.00");
        Transaction tx = withdrawals.withdraw(id, ACC, new BigDecimal("100.00"), null);

        assertThat(tx.balanceAfter()).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
