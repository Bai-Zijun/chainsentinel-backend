package com.bzj.chainsentinel.controller;

import com.bzj.chainsentinel.common.ApiResponse;
import com.bzj.chainsentinel.entity.Transaction;
import com.bzj.chainsentinel.exception.ResourceNotFoundException;
import com.bzj.chainsentinel.service.TransactionService;
import com.bzj.chainsentinel.vo.TransactionAnalysisVO;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@Validated
public class TransactionController {

    private static final String TX_HASH_PATTERN = "^[0-9a-fA-F]{64}$";
    private static final String TX_HASH_MESSAGE = "txHash must be a 64-character hexadecimal string";

    private final TransactionService transactionService;

    @GetMapping("/{txHash}")
    public ApiResponse<Transaction> getByTxHash(
            @PathVariable
            @Pattern(regexp = TX_HASH_PATTERN, message = TX_HASH_MESSAGE) String txHash
    ) {
        Transaction transaction = transactionService.getByTxHash(txHash);

        if (transaction == null) {
            throw new ResourceNotFoundException("transaction not found");
        }

        return ApiResponse.success(transaction);
    }

    @GetMapping("/{txHash}/analysis")
    public ApiResponse<TransactionAnalysisVO> getAnalysisByTxHash(
            @PathVariable
            @Pattern(regexp = TX_HASH_PATTERN, message = TX_HASH_MESSAGE) String txHash
    ) {
        TransactionAnalysisVO analysis = transactionService.getAnalysisByTxHash(txHash);

        if (analysis == null) {
            throw new ResourceNotFoundException("transaction not found");
        }

        return ApiResponse.success(analysis);
    }
}
