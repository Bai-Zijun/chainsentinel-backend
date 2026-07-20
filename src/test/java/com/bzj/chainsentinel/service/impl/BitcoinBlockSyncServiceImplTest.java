package com.bzj.chainsentinel.service.impl;

import com.bzj.chainsentinel.client.BitcoinCoreRpcClient;
import com.bzj.chainsentinel.config.BitcoinProperties;
import com.bzj.chainsentinel.entity.BitcoinBlock;
import com.bzj.chainsentinel.entity.SyncRun;
import com.bzj.chainsentinel.exception.ConflictException;
import com.bzj.chainsentinel.service.BitcoinNodeService;
import com.bzj.chainsentinel.service.SyncPersistenceService;
import com.bzj.chainsentinel.sync.BitcoinBlockData;
import com.bzj.chainsentinel.vo.node.BlockchainInfoVO;
import com.bzj.chainsentinel.vo.sync.SyncRunVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BitcoinBlockSyncServiceImplTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private BitcoinCoreRpcClient rpcClient;

    @Mock
    private BitcoinNodeService bitcoinNodeService;

    @Mock
    private SyncPersistenceService persistenceService;

    private BitcoinBlockSyncServiceImpl service;

    @BeforeEach
    void setUp() {
        BitcoinProperties properties = new BitcoinProperties();
        properties.setNetwork("testnet4");
        service = new BitcoinBlockSyncServiceImpl(
                rpcClient,
                bitcoinNodeService,
                persistenceService,
                properties
        );
    }

    @Test
    void backfillsRecentRangeAndUpdatesProgress() throws JsonProcessingException {
        when(bitcoinNodeService.getBlockchainInfo()).thenReturn(blockchainInfo(100));
        SyncRun running = run(1L, "RUNNING", 98, 100, 0);
        SyncRun completed = run(1L, "SUCCESS", 98, 100, 3);
        completed.setLastSuccessHeight(100L);
        completed.setFinishedAt(LocalDateTime.now());
        when(persistenceService.startRun("testnet4", "BACKFILL", 98, 100)).thenReturn(running);
        when(persistenceService.getCanonicalBlock("testnet4", 97)).thenReturn(null);
        when(persistenceService.completeRun("testnet4", 1L)).thenReturn(completed);

        for (long height = 98; height <= 100; height++) {
            stubBlock(height);
        }

        SyncRunVO result = service.backfillRecent(3);

        assertEquals(98, result.startHeight());
        assertEquals(100, result.targetHeight());
        assertEquals(3, result.blocksProcessed());
        assertEquals("SUCCESS", result.status());

        ArgumentCaptor<BitcoinBlockData> blockCaptor = ArgumentCaptor.forClass(BitcoinBlockData.class);
        verify(persistenceService, times(3))
                .saveBlockProgress(org.mockito.ArgumentMatchers.eq("testnet4"),
                        org.mockito.ArgumentMatchers.eq(1L), blockCaptor.capture());
        assertEquals(List.of(98L, 99L, 100L), blockCaptor.getAllValues().stream()
                .map(BitcoinBlockData::height)
                .toList());
    }

    @Test
    void marksRunFailedWhenChainIsDiscontinuous() throws JsonProcessingException {
        when(bitcoinNodeService.getBlockchainInfo()).thenReturn(blockchainInfo(100));
        SyncRun running = run(2L, "RUNNING", 100, 100, 0);
        when(persistenceService.startRun("testnet4", "BACKFILL", 100, 100)).thenReturn(running);

        BitcoinBlock previous = new BitcoinBlock();
        previous.setBlockHash("f".repeat(64));
        when(persistenceService.getCanonicalBlock("testnet4", 99)).thenReturn(previous);
        stubBlock(100);

        ConflictException exception = assertThrows(
                ConflictException.class,
                () -> service.backfillRecent(1)
        );

        assertEquals("blockchain discontinuity detected at height 100", exception.getMessage());
        verify(persistenceService).failRun("testnet4", 2L, exception.getMessage());
    }

    private void stubBlock(long height) throws JsonProcessingException {
        String hash = blockHash(height);
        when(rpcClient.call("getblockhash", List.of(height))).thenReturn(json('"' + hash + '"'));
        when(rpcClient.call("getblock", List.of(hash, 1))).thenReturn(json("""
                {
                  "hash":"%s",
                  "height":%d,
                  "previousblockhash":"%s",
                  "merkleroot":"%s",
                  "time":1783860000,
                  "nTx":1,
                  "size":1000,
                  "weight":4000,
                  "tx":["%s"]
                }
                """.formatted(
                hash,
                height,
                blockHash(height - 1),
                blockHash(height + 1000),
                blockHash(height + 2000)
        )));
    }

    private BlockchainInfoVO blockchainInfo(long blocks) {
        return new BlockchainInfoVO(
                "testnet4", blocks, blocks, blockHash(blocks),
                BigDecimal.ONE, BigDecimal.ONE, false, 1, false, List.of()
        );
    }

    private SyncRun run(Long id, String status, long start, long target, int processed) {
        SyncRun run = new SyncRun();
        run.setId(id);
        run.setNetwork("testnet4");
        run.setRunType("BACKFILL");
        run.setStartHeight(start);
        run.setTargetHeight(target);
        run.setBlocksProcessed(processed);
        run.setStatus(status);
        run.setStartedAt(LocalDateTime.now());
        return run;
    }

    private String blockHash(long height) {
        return String.format("%064x", height);
    }

    private JsonNode json(String value) throws JsonProcessingException {
        return objectMapper.readTree(value);
    }
}
