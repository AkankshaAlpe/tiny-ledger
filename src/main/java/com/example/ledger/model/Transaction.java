package com.example.ledger.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record Transaction(
        UUID id,
        String accountId,
        TransactionType type,
        BigDecimal amount,
        BigDecimal balanceAfter,
        String description,
        Instant timestamp
) {
    public static Transaction of(String accountId, TransactionType type, BigDecimal amount,
                                  BigDecimal balanceAfter, String description) {
        return new Transaction(UUID.randomUUID(), accountId, type, amount, balanceAfter, description, Instant.now());
    }
}
