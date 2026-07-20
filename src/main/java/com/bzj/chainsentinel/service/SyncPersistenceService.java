package com.bzj.chainsentinel.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.bzj.chainsentinel.entity.BitcoinBlock;
import com.bzj.chainsentinel.entity.SyncCheckpoint;
import com.bzj.chainsentinel.entity.SyncRun;
import com.bzj.chainsentinel.exception.ConflictException;
import com.bzj.chainsentinel.mapper.BitcoinBlockMapper;
import com.bzj.chainsentinel.mapper.SyncCheckpointMapper;
import com.bzj.chainsentinel.mapper.SyncRunMapper;
import com.bzj.chainsentinel.sync.BitcoinBlockData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SyncPersistenceService {

    private final BitcoinBlockMapper bitcoinBlockMapper;
    private final SyncCheckpointMapper syncCheckpointMapper;
    private final SyncRunMapper syncRunMapper;

    @Transactional
    public SyncRun startRun(String network, String runType, long startHeight, long targetHeight) {
        syncCheckpointMapper.insertIfAbsent(network);
        SyncCheckpoint checkpoint = syncCheckpointMapper.selectForUpdate(network);
        if (checkpoint == null) {
            throw new IllegalStateException("sync checkpoint could not be created");
        }
        if ("RUNNING".equals(checkpoint.getStatus())) {
            throw new ConflictException("block synchronization is already running");
        }

        checkpoint.setStatus("RUNNING");
        checkpoint.setErrorMessage(null);
        syncCheckpointMapper.updateById(checkpoint);

        SyncRun run = new SyncRun();
        run.setNetwork(network);
        run.setRunType(runType);
        run.setStartHeight(startHeight);
        run.setTargetHeight(targetHeight);
        run.setBlocksProcessed(0);
        run.setStatus("RUNNING");
        run.setStartedAt(LocalDateTime.now());
        syncRunMapper.insert(run);
        return run;
    }

    @Transactional
    public void saveBlockProgress(String network, Long runId, BitcoinBlockData data) {
        LocalDateTime now = LocalDateTime.now();
        LambdaUpdateWrapper<BitcoinBlock> demoteWrapper = new LambdaUpdateWrapper<>();
        demoteWrapper.eq(BitcoinBlock::getNetwork, network)
                .eq(BitcoinBlock::getHeight, data.height())
                .eq(BitcoinBlock::getIsCanonical, true)
                .ne(BitcoinBlock::getBlockHash, data.blockHash())
                .set(BitcoinBlock::getIsCanonical, false);
        bitcoinBlockMapper.update(null, demoteWrapper);

        LambdaQueryWrapper<BitcoinBlock> blockQuery = new LambdaQueryWrapper<>();
        blockQuery.eq(BitcoinBlock::getNetwork, network)
                .eq(BitcoinBlock::getBlockHash, data.blockHash());
        BitcoinBlock block = bitcoinBlockMapper.selectOne(blockQuery);
        if (block == null) {
            block = new BitcoinBlock();
            block.setNetwork(network);
            block.setBlockHash(data.blockHash());
        }
        block.setHeight(data.height());
        block.setPreviousBlockHash(data.previousBlockHash());
        block.setMerkleRoot(data.merkleRoot());
        block.setBlockTime(data.blockTime());
        block.setTransactionCount(data.transactionCount());
        block.setSize(data.size());
        block.setWeight(data.weight());
        block.setIsCanonical(true);
        block.setSyncedAt(now);
        if (block.getId() == null) {
            bitcoinBlockMapper.insert(block);
        } else {
            bitcoinBlockMapper.updateById(block);
        }

        SyncCheckpoint checkpoint = requireCheckpointForUpdate(network);
        if (checkpoint.getLastHeight() == null || data.height() >= checkpoint.getLastHeight()) {
            checkpoint.setLastHeight(data.height());
            checkpoint.setLastBlockHash(data.blockHash());
            checkpoint.setLastSyncedAt(now);
        }
        checkpoint.setStatus("RUNNING");
        checkpoint.setErrorMessage(null);
        syncCheckpointMapper.updateById(checkpoint);

        SyncRun run = requireRun(runId);
        run.setLastSuccessHeight(data.height());
        run.setBlocksProcessed(run.getBlocksProcessed() + 1);
        syncRunMapper.updateById(run);
    }

    @Transactional
    public SyncRun completeRun(String network, Long runId) {
        SyncRun run = requireRun(runId);
        run.setStatus("SUCCESS");
        run.setFinishedAt(LocalDateTime.now());
        syncRunMapper.updateById(run);

        SyncCheckpoint checkpoint = requireCheckpointForUpdate(network);
        checkpoint.setStatus("IDLE");
        checkpoint.setErrorMessage(null);
        syncCheckpointMapper.updateById(checkpoint);
        return syncRunMapper.selectById(runId);
    }

    @Transactional
    public void failRun(String network, Long runId, String errorMessage) {
        LocalDateTime now = LocalDateTime.now();
        SyncRun run = syncRunMapper.selectById(runId);
        if (run != null) {
            run.setStatus("FAILED");
            run.setErrorMessage(truncate(errorMessage, 2000));
            run.setFinishedAt(now);
            syncRunMapper.updateById(run);
        }

        syncCheckpointMapper.insertIfAbsent(network);
        SyncCheckpoint checkpoint = requireCheckpointForUpdate(network);
        checkpoint.setStatus("FAILED");
        checkpoint.setErrorMessage(truncate(errorMessage, 1000));
        syncCheckpointMapper.updateById(checkpoint);
    }

    public SyncCheckpoint getCheckpoint(String network) {
        LambdaQueryWrapper<SyncCheckpoint> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SyncCheckpoint::getNetwork, network);
        return syncCheckpointMapper.selectOne(wrapper);
    }

    public SyncRun getLatestRun(String network) {
        LambdaQueryWrapper<SyncRun> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SyncRun::getNetwork, network)
                .orderByDesc(SyncRun::getId)
                .last("LIMIT 1");
        return syncRunMapper.selectOne(wrapper);
    }

    public long countCanonicalBlocks(String network) {
        LambdaQueryWrapper<BitcoinBlock> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BitcoinBlock::getNetwork, network)
                .eq(BitcoinBlock::getIsCanonical, true);
        return bitcoinBlockMapper.selectCount(wrapper);
    }

    public BitcoinBlock getCanonicalBlock(String network, long height) {
        LambdaQueryWrapper<BitcoinBlock> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BitcoinBlock::getNetwork, network)
                .eq(BitcoinBlock::getHeight, height)
                .eq(BitcoinBlock::getIsCanonical, true);
        return bitcoinBlockMapper.selectOne(wrapper);
    }

    private SyncCheckpoint requireCheckpointForUpdate(String network) {
        SyncCheckpoint checkpoint = syncCheckpointMapper.selectForUpdate(network);
        if (checkpoint == null) {
            throw new IllegalStateException("sync checkpoint not found");
        }
        return checkpoint;
    }

    private SyncRun requireRun(Long runId) {
        SyncRun run = syncRunMapper.selectById(runId);
        if (run == null) {
            throw new IllegalStateException("sync run not found: " + runId);
        }
        return run;
    }

    private String truncate(String value, int maxLength) {
        String message = value == null || value.isBlank() ? "unknown synchronization error" : value;
        return message.length() <= maxLength ? message : message.substring(0, maxLength);
    }
}
