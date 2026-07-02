package com.bzj.chainsentinel.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("transactions")
public class Transaction {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String txHash;

    private Long blockId;

    private LocalDateTime txTime;

    private Integer size;

    private Integer weight;

    private Integer inputCount;

    private Integer outputCount;

    private BigDecimal inputTotal;

    private BigDecimal outputTotal;

    private BigDecimal fee;

    private BigDecimal feeRate;

    private Boolean isCoinbase;

    private Boolean hasWitness;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
