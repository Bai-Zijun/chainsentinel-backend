package com.bzj.chainsentinel.service;

import com.bzj.chainsentinel.entity.SyncCheckpoint;
import com.bzj.chainsentinel.exception.ConflictException;
import com.bzj.chainsentinel.mapper.BitcoinBlockMapper;
import com.bzj.chainsentinel.mapper.SyncCheckpointMapper;
import com.bzj.chainsentinel.mapper.SyncRunMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SyncPersistenceServiceTest {

    @Mock
    private BitcoinBlockMapper bitcoinBlockMapper;

    @Mock
    private SyncCheckpointMapper syncCheckpointMapper;

    @Mock
    private SyncRunMapper syncRunMapper;

    @InjectMocks
    private SyncPersistenceService persistenceService;

    @Test
    void rejectsConcurrentSynchronization() {
        SyncCheckpoint checkpoint = new SyncCheckpoint();
        checkpoint.setNetwork("testnet4");
        checkpoint.setStatus("RUNNING");
        when(syncCheckpointMapper.selectForUpdate("testnet4")).thenReturn(checkpoint);

        ConflictException exception = assertThrows(
                ConflictException.class,
                () -> persistenceService.startRun("testnet4", "BACKFILL", 98, 100)
        );

        assertEquals("block synchronization is already running", exception.getMessage());
        verifyNoInteractions(syncRunMapper);
    }
}
