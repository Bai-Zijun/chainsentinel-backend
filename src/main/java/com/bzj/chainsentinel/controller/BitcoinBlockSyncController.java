package com.bzj.chainsentinel.controller;

import com.bzj.chainsentinel.common.ApiResponse;
import com.bzj.chainsentinel.service.BitcoinBlockSyncService;
import com.bzj.chainsentinel.vo.sync.SyncRunVO;
import com.bzj.chainsentinel.vo.sync.SyncStatusVO;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sync")
@RequiredArgsConstructor
@Validated
public class BitcoinBlockSyncController {

    private final BitcoinBlockSyncService bitcoinBlockSyncService;

    @PostMapping("/blocks/backfill")
    public ApiResponse<SyncRunVO> backfillRecentBlocks(
            @RequestParam(defaultValue = "10")
            @Min(value = 1, message = "count must be between 1 and 100")
            @Max(value = 100, message = "count must be between 1 and 100") int count
    ) {
        return ApiResponse.success(bitcoinBlockSyncService.backfillRecent(count));
    }

    @GetMapping("/status")
    public ApiResponse<SyncStatusVO> getStatus() {
        return ApiResponse.success(bitcoinBlockSyncService.getStatus());
    }
}
