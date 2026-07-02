package com.bzj.chainsentinel.controller;

import com.bzj.chainsentinel.common.ApiResponse;
import com.bzj.chainsentinel.entity.AnomalyResult;
import com.bzj.chainsentinel.service.AnomalyResultService;
import com.bzj.chainsentinel.vo.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class AnomalyResultController {

    private final AnomalyResultService anomalyResultService;

    @GetMapping("/{txHash}/risk")
    public ApiResponse<AnomalyResult> getRiskByTxHash(@PathVariable String txHash) {
        AnomalyResult result = anomalyResultService.getByTxHash(txHash);

        if (result == null) {
            return ApiResponse.fail(404, "anomaly result not found");
        }

        return ApiResponse.success(result);
    }

    @GetMapping("/risk/high")
    public ApiResponse<PageResult<AnomalyResult>> pageHighRisk(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size
    ) {
        if (page <= 0) {
            return ApiResponse.fail(400, "page must be greater than 0");
        }

        if (size <= 0 || size > 100) {
            return ApiResponse.fail(400, "size must be between 1 and 100");
        }

        return ApiResponse.success(anomalyResultService.pageHighRisk(page, size));
    }
}