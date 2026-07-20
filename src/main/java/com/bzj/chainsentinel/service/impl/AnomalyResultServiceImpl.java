package com.bzj.chainsentinel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bzj.chainsentinel.entity.AnomalyResult;
import com.bzj.chainsentinel.mapper.AnomalyResultMapper;
import com.bzj.chainsentinel.service.AnomalyResultService;
import com.bzj.chainsentinel.vo.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AnomalyResultServiceImpl implements AnomalyResultService {

    private final AnomalyResultMapper anomalyResultMapper;

    @Override
    public AnomalyResult getByTxHash(String txHash) {
        LambdaQueryWrapper<AnomalyResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AnomalyResult::getTxHash, txHash);
        return anomalyResultMapper.selectOne(wrapper);
    }

    @Override
    public List<AnomalyResult> listHighRisk(int limit) {
        LambdaQueryWrapper<AnomalyResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AnomalyResult::getRiskLevel, "HIGH")
                .orderByDesc(AnomalyResult::getAnomalyScore)
                .orderByAsc(AnomalyResult::getId);

        Page<AnomalyResult> page = new Page<>(1, limit);
        return anomalyResultMapper.selectPage(page, wrapper).getRecords();
    }

    @Override
    public PageResult<AnomalyResult> pageHighRisk(long page, long size) {
        LambdaQueryWrapper<AnomalyResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AnomalyResult::getRiskLevel, "HIGH")
                .orderByDesc(AnomalyResult::getAnomalyScore)
                .orderByAsc(AnomalyResult::getId);

        Page<AnomalyResult> pageParam = new Page<>(page, size);
        Page<AnomalyResult> result = anomalyResultMapper.selectPage(pageParam, wrapper);

        return new PageResult<>(
                result.getRecords(),
                result.getTotal(),
                result.getCurrent(),
                result.getSize()
        );
    }
}
