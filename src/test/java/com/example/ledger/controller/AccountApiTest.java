package com.example.ledger.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static com.example.ledger.utils.FixtureUtils.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class AccountApiTest {

    @Autowired MockMvc mockMvc;

    private static final String ACCOUNT_A = "acc-001";
    private static final String ACCOUNT_B = "acc-002";

    private void deposit(String accountId, double amount, String description) throws Exception {
        mockMvc.perform(post(depositUrl())
                .header("Transaction-Id", txId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(Map.of("accountId", accountId, "amount", amount, "description", description)))).andReturn();
    }

    @Test
    void shouldReturnZeroBalanceInitially() throws Exception {
        mockMvc.perform(get(balanceUrl(ACCOUNT_A)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(0))
                .andExpect(jsonPath("$.currency").value("GBP"));
    }

    @Test
    void shouldReturnEmptyHistoryInitially() throws Exception {
        mockMvc.perform(get(historyUrl(ACCOUNT_A)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void shouldReturnBalanceAfterDeposit() throws Exception {
        deposit(ACCOUNT_A, 250.00, "Salary");

        mockMvc.perform(get(balanceUrl(ACCOUNT_A)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(250.00));
    }

    @Test
    void shouldReturnHistoryWithAllTransactions() throws Exception {
        deposit(ACCOUNT_A, 300.00, "Deposit");
        mockMvc.perform(post(withdrawUrl())
                .header("Transaction-Id", txId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(Map.of("accountId", ACCOUNT_A, "amount", 100.00, "description", "Withdrawal")))).andReturn();

        mockMvc.perform(get(historyUrl(ACCOUNT_A)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].type").value("WITHDRAWAL"))
                .andExpect(jsonPath("$[1].type").value("DEPOSIT"));
    }

    @Test
    void accountsShouldHaveIsolatedBalances() throws Exception {
        deposit(ACCOUNT_A, 500.00, "A");
        deposit(ACCOUNT_B, 200.00, "B");

        mockMvc.perform(get(balanceUrl(ACCOUNT_A)))
                .andExpect(jsonPath("$.balance").value(500.00));
        mockMvc.perform(get(balanceUrl(ACCOUNT_B)))
                .andExpect(jsonPath("$.balance").value(200.00));
    }

    @Test
    void accountsShouldHaveIsolatedHistories() throws Exception {
        deposit(ACCOUNT_A, 100.00, "A-deposit");
        deposit(ACCOUNT_B, 50.00, "B-deposit");

        mockMvc.perform(get(historyUrl(ACCOUNT_A)))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].description").value("A-deposit"));
        mockMvc.perform(get(historyUrl(ACCOUNT_B)))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].description").value("B-deposit"));
    }
}
