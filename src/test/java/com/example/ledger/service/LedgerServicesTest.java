package com.example.ledger.service;

import com.example.ledger.exception.InsufficientFundsException;
import com.example.ledger.model.Transaction;
import com.example.ledger.model.TransactionType;
import com.example.ledger.store.LedgerStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for the per-endpoint services, all sharing a single {@link LedgerStore}
 * so writes from one service are visible to the readers.
 */
class LedgerServicesTest {

    private DepositService deposits;
    private WithdrawService withdrawals;
    private BalanceService balances;
    private TransactionService history;

    private static final String ACC = "test-account";
    private static final String ACC_B = "other-account";

    @BeforeEach
    void setUp() {
        LedgerStore store = new LedgerStore();
        deposits = new DepositService(store);
        withdrawals = new WithdrawService(store);
        balances = new BalanceService(store);
        history = new TransactionService(store);
    }

    @Test
    void shouldStartWithZeroBalance() {
        assertThat(balances.getBalance(ACC)).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void shouldReturnEmptyHistoryInitially() {
        assertThat(history.getTransactions(ACC)).isEmpty();
    }

    @Test
    void shouldDepositAndUpdateBalance() {
        Transaction tx = deposits.deposit(ACC, new BigDecimal("100.00"), "Salary");

        assertThat(tx.accountId()).isEqualTo(ACC);
        assertThat(tx.type()).isEqualTo(TransactionType.DEPOSIT);
        assertThat(tx.amount()).isEqualByComparingTo("100.00");
        assertThat(tx.balanceAfter()).isEqualByComparingTo("100.00");
        assertThat(balances.getBalance(ACC)).isEqualByComparingTo("100.00");
    }

    @Test
    void shouldWithdrawAndUpdateBalance() {
        deposits.deposit(ACC, new BigDecimal("200.00"), null);
        Transaction tx = withdrawals.withdraw(ACC, new BigDecimal("50.00"), "Rent");

        assertThat(tx.type()).isEqualTo(TransactionType.WITHDRAWAL);
        assertThat(tx.amount()).isEqualByComparingTo("50.00");
        assertThat(tx.balanceAfter()).isEqualByComparingTo("150.00");
        assertThat(balances.getBalance(ACC)).isEqualByComparingTo("150.00");
    }

    @Test
    void shouldRejectNegativeDeposit() {
        assertThatThrownBy(() -> deposits.deposit(ACC, new BigDecimal("-1.00"), null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectWithdrawalExceedingBalance() {
        deposits.deposit(ACC, new BigDecimal("50.00"), null);

        assertThatThrownBy(() -> withdrawals.withdraw(ACC, new BigDecimal("100.00"), null))
                .isInstanceOf(InsufficientFundsException.class)
                .hasMessageContaining("100.00")
                .hasMessageContaining("50.00");
    }

    @Test
    void shouldRejectWithdrawalOnZeroBalance() {
        assertThatThrownBy(() -> withdrawals.withdraw(ACC, new BigDecimal("1.00"), null))
                .isInstanceOf(InsufficientFundsException.class);
    }

    @Test
    void shouldReturnHistoryNewestFirst() {
        deposits.deposit(ACC, new BigDecimal("100.00"), "First");
        deposits.deposit(ACC, new BigDecimal("50.00"), "Second");

        List<Transaction> txns = history.getTransactions(ACC);
        assertThat(txns).hasSize(2);
        assertThat(txns.get(0).description()).isEqualTo("Second");
        assertThat(txns.get(1).description()).isEqualTo("First");
    }

    @Test
    void shouldAccumulateMultipleDeposits() {
        deposits.deposit(ACC, new BigDecimal("100.00"), null);
        deposits.deposit(ACC, new BigDecimal("200.00"), null);
        deposits.deposit(ACC, new BigDecimal("50.00"), null);

        assertThat(balances.getBalance(ACC)).isEqualByComparingTo("350.00");
    }

    @Test
    void shouldAllowWithdrawalOfExactBalance() {
        deposits.deposit(ACC, new BigDecimal("100.00"), null);
        Transaction tx = withdrawals.withdraw(ACC, new BigDecimal("100.00"), null);

        assertThat(tx.balanceAfter()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(balances.getBalance(ACC)).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void shouldAssignUniqueIdsToTransactions() {
        Transaction t1 = deposits.deposit(ACC, new BigDecimal("10.00"), null);
        Transaction t2 = deposits.deposit(ACC, new BigDecimal("10.00"), null);

        assertThat(t1.id()).isNotEqualTo(t2.id());
    }

    @Test
    void shouldSetTimestampOnTransaction() {
        Transaction tx = deposits.deposit(ACC, new BigDecimal("10.00"), null);
        assertThat(tx.timestamp()).isNotNull();
    }

    @Test
    void accountsShouldBeIsolated() {
        deposits.deposit(ACC, new BigDecimal("500.00"), null);
        deposits.deposit(ACC_B, new BigDecimal("100.00"), null);

        assertThat(balances.getBalance(ACC)).isEqualByComparingTo("500.00");
        assertThat(balances.getBalance(ACC_B)).isEqualByComparingTo("100.00");
        assertThat(history.getTransactions(ACC)).hasSize(1);
        assertThat(history.getTransactions(ACC_B)).hasSize(1);
    }
}
