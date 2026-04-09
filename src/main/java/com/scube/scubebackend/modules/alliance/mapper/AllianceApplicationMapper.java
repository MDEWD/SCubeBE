package com.scube.scubebackend.modules.alliance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scube.scubebackend.modules.alliance.model.entity.AllianceApplication;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AllianceApplicationMapper extends BaseMapper<AllianceApplication> {
    AllianceApplication selectLatestByUserId(@Param("userId") Long userId);
}

