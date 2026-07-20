package com.bzj.chainsentinel.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bzj.chainsentinel.entity.AnomalyResult;
import com.bzj.chainsentinel.mapper.AnomalyResultMapper;
import com.bzj.chainsentinel.vo.PageResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnomalyResultServiceImplTest {

    @Mock
    private AnomalyResultMapper anomalyResultMapper;

    @InjectMocks
    private AnomalyResultServiceImpl anomalyResultService;

    @Test
    void pageHighRiskMapsMyBatisPageResult() {
        AnomalyResult anomalyResult = new AnomalyResult();
        anomalyResult.setId(7L);
        anomalyResult.setRiskLevel("HIGH");
        anomalyResult.setAnomalyScore(new BigDecimal("0.95000000"));

        Page<AnomalyResult> mapperResult = new Page<>(2, 10, 21);
        mapperResult.setRecords(List.of(anomalyResult));
        when(anomalyResultMapper.selectPage(any(), any())).thenReturn(mapperResult);

        PageResult<AnomalyResult> result = anomalyResultService.pageHighRisk(2, 10);

        assertEquals(21L, result.getTotal());
        assertEquals(2L, result.getPage());
        assertEquals(10L, result.getSize());
        assertEquals(List.of(anomalyResult), result.getRecords());
    }
}
