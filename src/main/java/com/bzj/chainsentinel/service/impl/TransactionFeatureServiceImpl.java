package com.bzj.chainsentinel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bzj.chainsentinel.entity.TransactionFeature;
import com.bzj.chainsentinel.mapper.TransactionFeatureMapper;
import com.bzj.chainsentinel.service.AnomalyResultService;
import com.bzj.chainsentinel.service.TransactionFeatureService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TransactionFeatureServiceImpl implements TransactionFeatureService {

    private final TransactionFeatureMapper transactionFeatureMapper;

    @Override
    public TransactionFeature getByTxHash(String txHash) {
        LambdaQueryWrapper<TransactionFeature> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TransactionFeature::getTxHash, txHash);
        return transactionFeatureMapper.selectOne(wrapper);
    }
}
