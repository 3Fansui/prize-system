package com.test.prizesystem.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.test.prizesystem.model.entity.Activity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ActivityMapper extends BaseMapper<Activity> {
}