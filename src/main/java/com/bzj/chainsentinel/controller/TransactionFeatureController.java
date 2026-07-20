package com.bzj.chainsentinel.controller;

import com.bzj.chainsentinel.common.ApiResponse;
import com.bzj.chainsentinel.entity.TransactionFeature;
import com.bzj.chainsentinel.exception.ResourceNotFoundException;
import com.bzj.chainsentinel.service.TransactionFeatureService;
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
public class TransactionFeatureController {

    private static final String TX_HASH_PATTERN = "^[0-9a-fA-F]{64}$";
    private static final String TX_HASH_MESSAGE = "txHash must be a 64-character hexadecimal string";

    private final TransactionFeatureService transactionFeatureService;

    @GetMapping("/{txHash}/features")
    public ApiResponse<TransactionFeature> getByTxHash(
            @PathVariable
            @Pattern(regexp = TX_HASH_PATTERN, message = TX_HASH_MESSAGE) String txHash
    ) {
        TransactionFeature feature = transactionFeatureService.getByTxHash(txHash);

        if (feature == null) {
            throw new ResourceNotFoundException("transaction feature not found");
        }

        return ApiResponse.success(feature);
    }
}
