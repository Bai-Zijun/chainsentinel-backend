package com.bzj.chainsentinel.controller;

import com.bzj.chainsentinel.common.ApiResponse;
import com.bzj.chainsentinel.entity.AnomalyResult;
import com.bzj.chainsentinel.exception.ResourceNotFoundException;
import com.bzj.chainsentinel.service.AnomalyResultService;
import com.bzj.chainsentinel.vo.PageResult;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@Validated
public class AnomalyResultController {

    private static final String TX_HASH_PATTERN = "^[0-9a-fA-F]{64}$";
    private static final String TX_HASH_MESSAGE = "txHash must be a 64-character hexadecimal string";

    private final AnomalyResultService anomalyResultService;

    @GetMapping("/{txHash}/risk")
    public ApiResponse<AnomalyResult> getRiskByTxHash(
            @PathVariable
            @Pattern(regexp = TX_HASH_PATTERN, message = TX_HASH_MESSAGE) String txHash
    ) {
        AnomalyResult result = anomalyResultService.getByTxHash(txHash);

        if (result == null) {
            throw new ResourceNotFoundException("anomaly result not found");
        }

        return ApiResponse.success(result);
    }

    @GetMapping("/risk/high")
    public ApiResponse<PageResult<AnomalyResult>> pageHighRisk(
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "page must be greater than 0") long page,
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "size must be between 1 and 100")
            @Max(value = 100, message = "size must be between 1 and 100") long size
    ) {
        return ApiResponse.success(anomalyResultService.pageHighRisk(page, size));
    }
}
