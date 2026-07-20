package com.bzj.chainsentinel.controller;

import com.bzj.chainsentinel.exception.GlobalExceptionHandler;
import com.bzj.chainsentinel.service.BitcoinBlockSyncService;
import com.bzj.chainsentinel.vo.sync.SyncCheckpointVO;
import com.bzj.chainsentinel.vo.sync.SyncRunVO;
import com.bzj.chainsentinel.vo.sync.SyncStatusVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BitcoinBlockSyncController.class)
@Import(GlobalExceptionHandler.class)
class BitcoinBlockSyncControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BitcoinBlockSyncService bitcoinBlockSyncService;

    @Test
    void startsBackfillAndReturnsRun() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        SyncRunVO run = new SyncRunVO(
                1L, "testnet4", "BACKFILL", 98, 100,
                100L, 3, "SUCCESS", null, now, now.plusSeconds(1)
        );
        when(bitcoinBlockSyncService.backfillRecent(3)).thenReturn(run);

        mockMvc.perform(post("/api/sync/blocks/backfill").param("count", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.runType").value("BACKFILL"))
                .andExpect(jsonPath("$.data.startHeight").value(98))
                .andExpect(jsonPath("$.data.targetHeight").value(100))
                .andExpect(jsonPath("$.data.blocksProcessed").value(3))
                .andExpect(jsonPath("$.data.status").value("SUCCESS"));
    }

    @Test
    void validatesBackfillCount() throws Exception {
        mockMvc.perform(post("/api/sync/blocks/backfill").param("count", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));

        verifyNoInteractions(bitcoinBlockSyncService);
    }

    @Test
    void returnsSyncStatus() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        SyncCheckpointVO checkpoint = new SyncCheckpointVO(
                "testnet4", 100L, "1".repeat(64), "IDLE", now, null, now
        );
        SyncStatusVO statusVO = new SyncStatusVO("testnet4", 3, checkpoint, null);
        when(bitcoinBlockSyncService.getStatus()).thenReturn(statusVO);

        mockMvc.perform(get("/api/sync/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.network").value("testnet4"))
                .andExpect(jsonPath("$.data.canonicalBlockCount").value(3))
                .andExpect(jsonPath("$.data.checkpoint.lastHeight").value(100))
                .andExpect(jsonPath("$.data.checkpoint.status").value("IDLE"));
    }
}
