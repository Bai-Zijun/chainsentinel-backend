package com.bzj.chainsentinel.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sync_checkpoints")
public class SyncCheckpoint {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String network;
    private Long lastHeight;
    private String lastBlockHash;
    private String status;
    private LocalDateTime lastSyncedAt;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
