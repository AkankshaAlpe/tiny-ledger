package com.example.ledger.controller;

import com.example.ledger.api.dto.TransactionResponse;
import com.example.ledger.model.Transaction;

import java.time.ZoneOffset;

/** Maps domain {@link Transaction} records to API DTOs. */
final class TransactionMapper {

    private TransactionMapper() {}

    static TransactionResponse toDto(Transaction t) {
        TransactionResponse dto = new TransactionResponse();
        dto.setId(t.id());
        dto.setType(TransactionResponse.TypeEnum.valueOf(t.type().name()));
        dto.setAmount(t.amount());
        dto.setBalanceAfter(t.balanceAfter());
        dto.setDescription(t.description());
        dto.setTimestamp(t.timestamp().atOffset(ZoneOffset.UTC));
        return dto;
    }
}
