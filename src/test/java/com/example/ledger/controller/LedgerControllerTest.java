package com.example.ledger.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class LedgerControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    private static final String ACCOUNT_A = "acc-001";
    private static final String ACCOUNT_B = "acc-002";

    private String json(Object o) throws Exception {
        return objectMapper.writeValueAsString(o);
    }

    private String txId() { return UUID.randomUUID().toString(); }

    // ── balance URL helpers ─────────────────────────────────────────────
    private String balanceUrl(String accountId) {
        return "/api/v1/accounts/" + accountId + "/balance";
    }

    private String historyUrl(String accountId) {
        return "/api/v1/accounts/" + accountId + "/transactions";
    }

    private String depositUrl(String accountId) {
        return "/api/v1/accounts/" + accountId + "/transactions/deposit";
    }

    private String withdrawUrl(String accountId) {
        return "/api/v1/accounts/" + accountId + "/transactions/withdraw";
    }

    // ── initial state ───────────────────────────────────────────────────

    @Test
    void shouldReturnZeroBalanceInitially() throws Exception {
        mockMvc.perform(get(balanceUrl(ACCOUNT_A)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(0))
                .andExpect(jsonPath("$.currency").value("USD"));
    }

    @Test
    void shouldReturnEmptyHistoryInitially() throws Exception {
        mockMvc.perform(get(historyUrl(ACCOUNT_A)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    // ── deposit ─────────────────────────────────────────────────────────

    @Test
    void shouldDepositAndReturn201() throws Exception {
        mockMvc.perform(post(depositUrl(ACCOUNT_A))
                        .header("Transaction-Id", txId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("amount", 100.00, "description", "Salary"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.type").value("DEPOSIT"))
                .andExpect(jsonPath("$.amount").value(100.00))
                .andExpect(jsonPath("$.balanceAfter").value(100.00))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void shouldUpdateBalanceAfterDeposit() throws Exception {
        mockMvc.perform(post(depositUrl(ACCOUNT_A))
                .header("Transaction-Id", txId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("amount", 250.00)))).andReturn();

        mockMvc.perform(get(balanceUrl(ACCOUNT_A)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(250.00));
    }

    @Test
    void shouldRejectDepositWithNegativeAmount() throws Exception {
        mockMvc.perform(post(depositUrl(ACCOUNT_A))
                        .header("Transaction-Id", txId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("amount", -50.00))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectDepositWithZeroAmount() throws Exception {
        mockMvc.perform(post(depositUrl(ACCOUNT_A))
                        .header("Transaction-Id", txId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("amount", 0))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectDepositWithMissingAmount() throws Exception {
        mockMvc.perform(post(depositUrl(ACCOUNT_A))
                        .header("Transaction-Id", txId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectDepositWithoutTransactionId() throws Exception {
        mockMvc.perform(post(depositUrl(ACCOUNT_A))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("amount", 100.00))))
                .andExpect(status().isBadRequest());
    }

    // ── withdrawal ──────────────────────────────────────────────────────

    @Test
    void shouldWithdrawAndReturn201() throws Exception {
        mockMvc.perform(post(depositUrl(ACCOUNT_A))
                .header("Transaction-Id", txId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("amount", 200.00)))).andReturn();

        mockMvc.perform(post(withdrawUrl(ACCOUNT_A))
                        .header("Transaction-Id", txId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("amount", 75.00, "description", "Rent"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("WITHDRAWAL"))
                .andExpect(jsonPath("$.amount").value(75.00))
                .andExpect(jsonPath("$.balanceAfter").value(125.00));
    }

    @Test
    void shouldRejectWithdrawalExceedingBalance() throws Exception {
        mockMvc.perform(post(depositUrl(ACCOUNT_A))
                .header("Transaction-Id", txId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("amount", 50.00)))).andReturn();

        mockMvc.perform(post(withdrawUrl(ACCOUNT_A))
                        .header("Transaction-Id", txId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("amount", 100.00))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Insufficient Funds"));
    }

    @Test
    void shouldRejectWithdrawalOnZeroBalance() throws Exception {
        mockMvc.perform(post(withdrawUrl(ACCOUNT_A))
                        .header("Transaction-Id", txId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("amount", 1.00))))
                .andExpect(status().isBadRequest());
    }

    // ── history ─────────────────────────────────────────────────────────

    @Test
    void shouldReturnHistoryWithAllTransactions() throws Exception {
        mockMvc.perform(post(depositUrl(ACCOUNT_A))
                .header("Transaction-Id", txId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("amount", 300.00, "description", "Deposit")))).andReturn();

        mockMvc.perform(post(withdrawUrl(ACCOUNT_A))
                .header("Transaction-Id", txId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("amount", 100.00, "description", "Withdrawal")))).andReturn();

        mockMvc.perform(get(historyUrl(ACCOUNT_A)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].type").value("WITHDRAWAL"))
                .andExpect(jsonPath("$[1].type").value("DEPOSIT"));
    }

    // ── multi-account isolation ──────────────────────────────────────────

    @Test
    void accountsShouldHaveIsolatedBalances() throws Exception {
        // Deposit 500 into account A, 200 into account B
        mockMvc.perform(post(depositUrl(ACCOUNT_A))
                .header("Transaction-Id", txId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("amount", 500.00)))).andReturn();

        mockMvc.perform(post(depositUrl(ACCOUNT_B))
                .header("Transaction-Id", txId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("amount", 200.00)))).andReturn();

        mockMvc.perform(get(balanceUrl(ACCOUNT_A)))
                .andExpect(jsonPath("$.balance").value(500.00));

        mockMvc.perform(get(balanceUrl(ACCOUNT_B)))
                .andExpect(jsonPath("$.balance").value(200.00));
    }

    @Test
    void accountsShouldHaveIsolatedHistories() throws Exception {
        mockMvc.perform(post(depositUrl(ACCOUNT_A))
                .header("Transaction-Id", txId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("amount", 100.00, "description", "A-deposit")))).andReturn();

        mockMvc.perform(post(depositUrl(ACCOUNT_B))
                .header("Transaction-Id", txId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("amount", 50.00, "description", "B-deposit")))).andReturn();

        // A sees only its own transaction
        mockMvc.perform(get(historyUrl(ACCOUNT_A)))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].description").value("A-deposit"));

        // B sees only its own transaction
        mockMvc.perform(get(historyUrl(ACCOUNT_B)))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].description").value("B-deposit"));
    }

    @Test
    void withdrawalOnOneAccountDoesNotAffectAnother() throws Exception {
        mockMvc.perform(post(depositUrl(ACCOUNT_A))
                .header("Transaction-Id", txId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("amount", 300.00)))).andReturn();

        mockMvc.perform(post(depositUrl(ACCOUNT_B))
                .header("Transaction-Id", txId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("amount", 300.00)))).andReturn();

        mockMvc.perform(post(withdrawUrl(ACCOUNT_A))
                .header("Transaction-Id", txId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("amount", 100.00)))).andReturn();

        // A reduced, B untouched
        mockMvc.perform(get(balanceUrl(ACCOUNT_A)))
                .andExpect(jsonPath("$.balance").value(200.00));
        mockMvc.perform(get(balanceUrl(ACCOUNT_B)))
                .andExpect(jsonPath("$.balance").value(300.00));
    }
}
