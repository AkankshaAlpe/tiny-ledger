package com.example.ledger.utils;

import com.example.ledger.model.Transaction;
import com.example.ledger.model.TransactionType;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Shared helpers for tests: id generation, JSON serialization, URL building, and
 * domain {@link Transaction} fixtures for seeding the store directly.
 */
public final class FixtureUtils {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private FixtureUtils() {
    }

    /** A fresh, spec-valid (8–64 char) Transaction-Id. */
    public static String txId() {
        return UUID.randomUUID().toString();
    }

    public static String toJson(Object value) {
        try {
            return MAPPER.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize test fixture to JSON", e);
        }
    }

    public static String balanceUrl(String accountId) {
        return "/api/v1/accounts/" + accountId + "/balance";
    }

    public static String historyUrl(String accountId) {
        return "/api/v1/accounts/" + accountId + "/transactions";
    }

    /** Deposit/withdraw now carry the accountId in the request body, not the path. */
    public static String depositUrl() {
        return "/api/v1/transactions/deposit";
    }

    public static String withdrawUrl() {
        return "/api/v1/transactions/withdraw";
    }


    public static Transaction deposit(String accountId, String amount, String description) {
        BigDecimal a = new BigDecimal(amount);
        return Transaction.of(accountId, TransactionType.DEPOSIT, a, a, description);
    }

    public static Transaction withdrawal(String accountId, String amount, String description) {
        BigDecimal a = new BigDecimal(amount);
        return Transaction.of(accountId, TransactionType.WITHDRAWAL, a, a, description);
    }
}
