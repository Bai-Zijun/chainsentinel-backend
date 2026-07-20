package com.bzj.chainsentinel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bzj.chainsentinel.entity.BitcoinBlock;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BitcoinBlockMapper extends BaseMapper<BitcoinBlock> {
}
