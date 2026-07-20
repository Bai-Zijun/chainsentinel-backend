package com.bzj.chainsentinel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bzj.chainsentinel.entity.SyncCheckpoint;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SyncCheckpointMapper extends BaseMapper<SyncCheckpoint> {

    @Insert("""
            INSERT IGNORE INTO sync_checkpoints (network, status)
            VALUES (#{network}, 'IDLE')
            """)
    int insertIfAbsent(@Param("network") String network);

    @Select("""
            SELECT *
            FROM sync_checkpoints
            WHERE network = #{network}
            FOR UPDATE
            """)
    SyncCheckpoint selectForUpdate(@Param("network") String network);
}
