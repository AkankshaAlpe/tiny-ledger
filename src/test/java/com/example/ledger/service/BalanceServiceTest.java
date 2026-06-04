package com.example.ledger.service;

import com.example.ledger.store.LedgerStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static com.example.ledger.utils.FixtureUtils.deposit;
import static com.example.ledger.utils.FixtureUtils.withdrawal;
import static org.assertj.core.api.Assertions.*;

class BalanceServiceTest {

    private LedgerStore store;
    private BalanceService balances;

    private static final String ACC = "test-account";
    private static final String ACC_B = "other-account";

    @BeforeEach
    void setUp() {
        store = new LedgerStore();
        balances = new BalanceService(store);
    }

    @Test
    void shouldStartWithZeroBalance() {
        assertThat(balances.getBalance(ACC)).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void shouldReflectDepositsMinusWithdrawals() {
        store.save(deposit(ACC, "200.00", null));
        store.save(deposit(ACC, "50.00", null));
        store.save(withdrawal(ACC, "75.00", null));

        assertThat(balances.getBalance(ACC)).isEqualByComparingTo("175.00");
    }

    @Test
    void accountsHaveIsolatedBalances() {
        store.save(deposit(ACC, "500.00", null));
        store.save(deposit(ACC_B, "100.00", null));

        assertThat(balances.getBalance(ACC)).isEqualByComparingTo("500.00");
        assertThat(balances.getBalance(ACC_B)).isEqualByComparingTo("100.00");
    }
}
