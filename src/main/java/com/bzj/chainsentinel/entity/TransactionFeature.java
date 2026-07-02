package com.bzj.chainsentinel.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("transaction_features")
public class TransactionFeature {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String txHash;

    private BigDecimal inputOutputRatio;

    private BigDecimal amountEntropy;

    private BigDecimal roundAmountRatio;

    private BigDecimal dustOutputRatio;

    private String featureVersion;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
