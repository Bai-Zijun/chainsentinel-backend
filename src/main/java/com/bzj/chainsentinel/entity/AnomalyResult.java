package com.bzj.chainsentinel.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("anomaly_results")
public class AnomalyResult {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String txHash;

    private BigDecimal anomalyScore;

    private String riskLevel;

    private String modelName;

    private String modelVersion;

    private String reason;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
