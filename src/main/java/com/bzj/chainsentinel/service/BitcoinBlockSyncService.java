package com.bzj.chainsentinel.service;

import com.bzj.chainsentinel.vo.sync.SyncRunVO;
import com.bzj.chainsentinel.vo.sync.SyncStatusVO;

public interface BitcoinBlockSyncService {

    SyncRunVO backfillRecent(int count);

    SyncStatusVO getStatus();
}
