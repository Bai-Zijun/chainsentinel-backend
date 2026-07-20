package com.bzj.chainsentinel.vo.sync;

public record SyncStatusVO(
        String network,
        long canonicalBlockCount,
        SyncCheckpointVO checkpoint,
        SyncRunVO latestRun
) {
}
