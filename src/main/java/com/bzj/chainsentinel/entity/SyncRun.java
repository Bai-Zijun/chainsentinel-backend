package com.bzj.chainsentinel.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sync_runs")
public class SyncRun {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String network;
    private String runType;
    private Long startHeight;
    private Long targetHeight;
    private Long lastSuccessHeight;
    private Integer blocksProcessed;
    private String status;
    private String errorMessage;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
}
