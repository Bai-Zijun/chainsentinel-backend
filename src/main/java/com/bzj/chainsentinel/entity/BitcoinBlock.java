package com.bzj.chainsentinel.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("bitcoin_blocks")
public class BitcoinBlock {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String network;
    private Long height;
    private String blockHash;
    private String previousBlockHash;
    private String merkleRoot;
    private LocalDateTime blockTime;
    private Integer transactionCount;
    private Integer size;
    private Integer weight;
    private Boolean isCanonical;
    private LocalDateTime syncedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
