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
class TransactionApiTest {

    @Autowired MockMvc mockMvc;

    private static final String ACCOUNT_A = "acc-001";
    private static final String ACCOUNT_B = "acc-002";

    // ── deposit ─────────────────────────────────────────────────────────

    @Test
    void shouldDepositAndReturn201() throws Exception {
        mockMvc.perform(post(depositUrl())
                        .header("Transaction-Id", txId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("accountId", ACCOUNT_A, "amount", 100.00, "description", "Salary"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.type").value("DEPOSIT"))
                .andExpect(jsonPath("$.amount").value(100.00))
                .andExpect(jsonPath("$.balanceAfter").value(100.00))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void shouldRejectDepositWithNegativeAmount() throws Exception {
        mockMvc.perform(post(depositUrl())
                        .header("Transaction-Id", txId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("accountId", ACCOUNT_A, "amount", -50.00))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectDepositWithZeroAmount() throws Exception {
        mockMvc.perform(post(depositUrl())
                        .header("Transaction-Id", txId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("accountId", ACCOUNT_A, "amount", 0))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectDepositWithMissingAmount() throws Exception {
        mockMvc.perform(post(depositUrl())
                        .header("Transaction-Id", txId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("accountId", ACCOUNT_A))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectDepositWithMissingAccountId() throws Exception {
        mockMvc.perform(post(depositUrl())
                        .header("Transaction-Id", txId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("amount", 100.00))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectDepositWithoutTransactionId() throws Exception {
        mockMvc.perform(post(depositUrl())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("accountId", ACCOUNT_A, "amount", 100.00))))
                .andExpect(status().isBadRequest());
    }

    // ── withdrawal ──────────────────────────────────────────────────────

    @Test
    void shouldWithdrawAndReturn201() throws Exception {
        mockMvc.perform(post(depositUrl())
                .header("Transaction-Id", txId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(Map.of("accountId", ACCOUNT_A, "amount", 200.00)))).andReturn();

        mockMvc.perform(post(withdrawUrl())
                        .header("Transaction-Id", txId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("accountId", ACCOUNT_A, "amount", 75.00, "description", "Rent"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("WITHDRAWAL"))
                .andExpect(jsonPath("$.amount").value(75.00))
                .andExpect(jsonPath("$.balanceAfter").value(125.00));
    }

    @Test
    void shouldRejectWithdrawalExceedingBalance() throws Exception {
        mockMvc.perform(post(depositUrl())
                .header("Transaction-Id", txId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(Map.of("accountId", ACCOUNT_A, "amount", 50.00)))).andReturn();

        mockMvc.perform(post(withdrawUrl())
                        .header("Transaction-Id", txId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("accountId", ACCOUNT_A, "amount", 100.00))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Insufficient Funds"));
    }

    @Test
    void shouldRejectWithdrawalOnZeroBalance() throws Exception {
        mockMvc.perform(post(withdrawUrl())
                        .header("Transaction-Id", txId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("accountId", ACCOUNT_A, "amount", 1.00))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void withdrawalOnOneAccountDoesNotAffectAnother() throws Exception {
        mockMvc.perform(post(depositUrl())
                .header("Transaction-Id", txId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(Map.of("accountId", ACCOUNT_A, "amount", 300.00)))).andReturn();
        mockMvc.perform(post(depositUrl())
                .header("Transaction-Id", txId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(Map.of("accountId", ACCOUNT_B, "amount", 300.00)))).andReturn();

        mockMvc.perform(post(withdrawUrl())
                .header("Transaction-Id", txId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(Map.of("accountId", ACCOUNT_A, "amount", 100.00)))).andReturn();

        mockMvc.perform(get(balanceUrl(ACCOUNT_A)))
                .andExpect(jsonPath("$.balance").value(200.00));
        mockMvc.perform(get(balanceUrl(ACCOUNT_B)))
                .andExpect(jsonPath("$.balance").value(300.00));
    }

    // ── idempotency ─────────────────────────────────────────────────────

    @Test
    void retryWithSameTransactionIdDoesNotDoubleDeposit() throws Exception {
        String id = txId();
        String body = toJson(Map.of("accountId", ACCOUNT_A, "amount", 100.00, "description", "Salary"));

        mockMvc.perform(post(depositUrl())
                        .header("Transaction-Id", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        // Same Transaction-Id replayed — must not create a second transaction
        mockMvc.perform(post(depositUrl())
                        .header("Transaction-Id", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(get(balanceUrl(ACCOUNT_A)))
                .andExpect(jsonPath("$.balance").value(100.00));
        mockMvc.perform(get(historyUrl(ACCOUNT_A)))
                .andExpect(jsonPath("$", hasSize(1)));
    }
}
