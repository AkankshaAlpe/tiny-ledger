package com.example.ledger.controller;

import com.example.ledger.api.dto.BalanceResponse;
import com.example.ledger.api.dto.TransactionResponse;
import com.example.ledger.service.BalanceService;
import com.example.ledger.service.TransactionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/accounts/{accountId}")
public class AccountApi {

    private final BalanceService balanceService;
    private final TransactionService transactionService;

    public AccountApi(BalanceService balanceService, TransactionService transactionService) {
        this.balanceService = balanceService;
        this.transactionService = transactionService;
    }

    @GetMapping("/balance")
    public ResponseEntity<BalanceResponse> getBalance(@PathVariable String accountId) {
        BalanceResponse response = new BalanceResponse();
        response.setBalance(balanceService.getBalance(accountId));
        response.setCurrency("USD");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/transactions")
    public ResponseEntity<List<TransactionResponse>> getTransactionHistory(@PathVariable String accountId) {
        List<TransactionResponse> history = transactionService.getTransactions(accountId).stream()
                .map(TransactionMapper::toDto)
                .toList();
        return ResponseEntity.ok(history);
    }
}
