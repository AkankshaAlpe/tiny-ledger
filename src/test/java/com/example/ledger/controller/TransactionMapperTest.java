package com.example.ledger.controller;

import com.example.ledger.api.dto.TransactionResponse;
import com.example.ledger.model.Transaction;
import com.example.ledger.model.TransactionType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class TransactionMapperTest {

    @Test
    void mapsAllFieldsFromDomainToDto() {
        UUID id = UUID.randomUUID();
        Instant now = Instant.parse("2026-01-01T10:15:30Z");
        Transaction tx = new Transaction(
                id, "acc-1", TransactionType.WITHDRAWAL,
                new BigDecimal("75.00"), new BigDecimal("125.00"), "Rent", now);

        TransactionResponse dto = TransactionMapper.toDto(tx);

        assertThat(dto.getId()).isEqualTo(id);
        assertThat(dto.getType()).isEqualTo(TransactionResponse.TypeEnum.WITHDRAWAL);
        assertThat(dto.getAmount()).isEqualByComparingTo("75.00");
        assertThat(dto.getBalanceAfter()).isEqualByComparingTo("125.00");
        assertThat(dto.getDescription()).isEqualTo("Rent");
        assertThat(dto.getTimestamp()).isEqualTo(now.atOffset(ZoneOffset.UTC));
    }

    @Test
    void mapsDepositTypeAndNullDescription() {
        Transaction tx = Transaction.of(
                "acc-2", TransactionType.DEPOSIT, new BigDecimal("10.00"), new BigDecimal("10.00"), null);

        TransactionResponse dto = TransactionMapper.toDto(tx);

        assertThat(dto.getType()).isEqualTo(TransactionResponse.TypeEnum.DEPOSIT);
        assertThat(dto.getDescription()).isNull();
    }
}
