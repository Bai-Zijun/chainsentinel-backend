package com.bzj.chainsentinel.controller;

import com.bzj.chainsentinel.common.ApiResponse;
import com.bzj.chainsentinel.entity.TransactionFeature;
import com.bzj.chainsentinel.service.TransactionFeatureService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionFeatureController {

    private final TransactionFeatureService transactionFeatureService;

    @GetMapping("/{txHash}/features")
    public ApiResponse<TransactionFeature> getByTxHash(@PathVariable String txHash) {
        TransactionFeature feature = transactionFeatureService.getByTxHash(txHash);

        if (feature == null) {
            return ApiResponse.fail(404, "transaction feature not found");
        }

        return ApiResponse.success(feature);
    }
}
