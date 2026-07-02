package com.bzj.chainsentinel.service;

import com.bzj.chainsentinel.entity.AnomalyResult;
import com.bzj.chainsentinel.vo.PageResult;

import java.util.List;

public interface AnomalyResultService {

    AnomalyResult getByTxHash(String txHash);

    List<AnomalyResult> listHighRisk(int limit);

    PageResult<AnomalyResult> pageHighRisk(long page, long size);
}
