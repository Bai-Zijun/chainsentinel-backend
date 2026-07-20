package com.bzj.chainsentinel.service.impl;

import com.bzj.chainsentinel.client.BitcoinCoreRpcClient;
import com.bzj.chainsentinel.config.BitcoinProperties;
import com.bzj.chainsentinel.entity.BitcoinBlock;
import com.bzj.chainsentinel.entity.SyncCheckpoint;
import com.bzj.chainsentinel.entity.SyncRun;
import com.bzj.chainsentinel.exception.BitcoinRpcException;
import com.bzj.chainsentinel.exception.ConflictException;
import com.bzj.chainsentinel.service.BitcoinBlockSyncService;
import com.bzj.chainsentinel.service.BitcoinNodeService;
import com.bzj.chainsentinel.service.SyncPersistenceService;
import com.bzj.chainsentinel.sync.BitcoinBlockData;
import com.bzj.chainsentinel.vo.sync.SyncCheckpointVO;
import com.bzj.chainsentinel.vo.sync.SyncRunVO;
import com.bzj.chainsentinel.vo.sync.SyncStatusVO;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BitcoinBlockSyncServiceImpl implements BitcoinBlockSyncService {

    private final BitcoinCoreRpcClient rpcClient;
    private final BitcoinNodeService bitcoinNodeService;
    private final SyncPersistenceService persistenceService;
    private final BitcoinProperties properties;

    @Override
    public SyncRunVO backfillRecent(int count) {
        long tipHeight = bitcoinNodeService.getBlockchainInfo().blocks();
        long startHeight = Math.max(0, tipHeight - count + 1);
        String network = properties.getNetwork();
        long startNanos = System.nanoTime();

        SyncRun run = persistenceService.startRun(network, "BACKFILL", startHeight, tipHeight);
        log.info(
                "block_sync_started runId={} type=BACKFILL network={} startHeight={} targetHeight={}",
                run.getId(), network, startHeight, tipHeight
        );

        try {
            BitcoinBlock previous = startHeight == 0
                    ? null
                    : persistenceService.getCanonicalBlock(network, startHeight - 1);
            String expectedPreviousHash = previous == null ? null : previous.getBlockHash();

            for (long height = startHeight; height <= tipHeight; height++) {
                BitcoinBlockData block = fetchBlock(height);
                if (expectedPreviousHash != null
                        && !expectedPreviousHash.equals(block.previousBlockHash())) {
                    throw new ConflictException("blockchain discontinuity detected at height " + height);
                }

                persistenceService.saveBlockProgress(network, run.getId(), block);
                expectedPreviousHash = block.blockHash();
            }

            SyncRun completed = persistenceService.completeRun(network, run.getId());
            log.info(
                    "block_sync_completed runId={} blocksProcessed={} durationMs={}",
                    completed.getId(),
                    completed.getBlocksProcessed(),
                    elapsedMillis(startNanos)
            );
            return toRunVO(completed);
        } catch (RuntimeException exception) {
            markFailed(network, run.getId(), exception);
            log.warn(
                    "block_sync_failed runId={} durationMs={} exception={} message={}",
                    run.getId(),
                    elapsedMillis(startNanos),
                    exception.getClass().getSimpleName(),
                    exception.getMessage()
            );
            throw exception;
        }
    }

    @Override
    public SyncStatusVO getStatus() {
        String network = properties.getNetwork();
        return new SyncStatusVO(
                network,
                persistenceService.countCanonicalBlocks(network),
                toCheckpointVO(persistenceService.getCheckpoint(network)),
                toRunVO(persistenceService.getLatestRun(network))
        );
    }

    private BitcoinBlockData fetchBlock(long height) {
        JsonNode hashResult = rpcClient.call("getblockhash", List.of(height));
        if (hashResult == null || !hashResult.isTextual()) {
            throw invalidBlock(height);
        }

        String blockHash = hashResult.textValue();
        JsonNode block = rpcClient.call("getblock", List.of(blockHash, 1));
        if (block == null || !block.isObject()) {
            throw invalidBlock(height);
        }

        long returnedHeight = requireLong(block, "height", height);
        String returnedHash = requireText(block, "hash", height);
        JsonNode transactions = requireField(block, "tx", height);
        int transactionCount = requireInt(block, "nTx", height);
        if (returnedHeight != height
                || !blockHash.equals(returnedHash)
                || !transactions.isArray()
                || transactions.size() != transactionCount) {
            throw invalidBlock(height);
        }

        long epochSeconds = requireLong(block, "time", height);
        LocalDateTime blockTime;
        try {
            blockTime = LocalDateTime.ofInstant(Instant.ofEpochSecond(epochSeconds), ZoneOffset.UTC);
        } catch (RuntimeException exception) {
            throw invalidBlock(height);
        }

        return new BitcoinBlockData(
                height,
                blockHash,
                optionalText(block, "previousblockhash", height),
                requireText(block, "merkleroot", height),
                blockTime,
                transactionCount,
                requireInt(block, "size", height),
                requireInt(block, "weight", height)
        );
    }

    private JsonNode requireField(JsonNode object, String field, long height) {
        JsonNode value = object.get(field);
        if (value == null || value.isNull()) {
            throw invalidBlock(height);
        }
        return value;
    }

    private String requireText(JsonNode object, String field, long height) {
        JsonNode value = requireField(object, field, height);
        if (!value.isTextual()) {
            throw invalidBlock(height);
        }
        return value.textValue();
    }

    private String optionalText(JsonNode object, String field, long height) {
        JsonNode value = object.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        if (!value.isTextual()) {
            throw invalidBlock(height);
        }
        return value.textValue();
    }

    private int requireInt(JsonNode object, String field, long height) {
        JsonNode value = requireField(object, field, height);
        if (!value.canConvertToInt()) {
            throw invalidBlock(height);
        }
        return value.intValue();
    }

    private long requireLong(JsonNode object, String field, long height) {
        JsonNode value = requireField(object, field, height);
        if (!value.canConvertToLong()) {
            throw invalidBlock(height);
        }
        return value.longValue();
    }

    private BitcoinRpcException invalidBlock(long height) {
        return new BitcoinRpcException(
                HttpStatus.BAD_GATEWAY,
                "bitcoin rpc returned inconsistent block " + height
        );
    }

    private void markFailed(String network, Long runId, RuntimeException originalException) {
        try {
            persistenceService.failRun(network, runId, originalException.getMessage());
        } catch (RuntimeException persistenceException) {
            originalException.addSuppressed(persistenceException);
            log.error("Failed to persist synchronization failure runId={}", runId, persistenceException);
        }
    }

    private long elapsedMillis(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }

    private SyncRunVO toRunVO(SyncRun run) {
        if (run == null) {
            return null;
        }
        return new SyncRunVO(
                run.getId(),
                run.getNetwork(),
                run.getRunType(),
                run.getStartHeight(),
                run.getTargetHeight(),
                run.getLastSuccessHeight(),
                run.getBlocksProcessed(),
                run.getStatus(),
                run.getErrorMessage(),
                run.getStartedAt(),
                run.getFinishedAt()
        );
    }

    private SyncCheckpointVO toCheckpointVO(SyncCheckpoint checkpoint) {
        if (checkpoint == null) {
            return null;
        }
        return new SyncCheckpointVO(
                checkpoint.getNetwork(),
                checkpoint.getLastHeight(),
                checkpoint.getLastBlockHash(),
                checkpoint.getStatus(),
                checkpoint.getLastSyncedAt(),
                checkpoint.getErrorMessage(),
                checkpoint.getUpdatedAt()
        );
    }
}
