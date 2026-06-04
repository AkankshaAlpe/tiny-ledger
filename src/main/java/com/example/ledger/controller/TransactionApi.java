package com.example.ledger.controller;

import com.example.ledger.api.TransactionsApi;
import com.example.ledger.api.dto.TransactionRequest;
import com.example.ledger.api.dto.TransactionResponse;
import com.example.ledger.model.Transaction;
import com.example.ledger.service.DepositService;
import com.example.ledger.service.WithdrawService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TransactionApi implements TransactionsApi {

    private final DepositService depositService;
    private final WithdrawService withdrawService;

    public TransactionApi(DepositService depositService, WithdrawService withdrawService) {
        this.depositService = depositService;
        this.withdrawService = withdrawService;
    }

    @Override
    public ResponseEntity<TransactionResponse> deposit(String transactionId, TransactionRequest req) {
        Transaction tx = depositService.deposit(transactionId, req.getAccountId(), req.getAmount(), req.getDescription());
        return ResponseEntity.status(HttpStatus.CREATED).body(TransactionMapper.toDto(tx));
    }

    @Override
    public ResponseEntity<TransactionResponse> withdraw(String transactionId, TransactionRequest req) {
        Transaction tx = withdrawService.withdraw(transactionId, req.getAccountId(), req.getAmount(), req.getDescription());
        return ResponseEntity.status(HttpStatus.CREATED).body(TransactionMapper.toDto(tx));
    }
}
