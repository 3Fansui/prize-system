package com.test.prizesystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.test.prizesystem.model.entity.UserDrawRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserDrawRecordMapper extends BaseMapper<UserDrawRecord> {

    /**
     * 获取用户当天抽奖次数
     */
    @Select("SELECT COUNT(*) FROM t_user_draw_record WHERE user_id = #{userId} AND activity_id = #{activityId} AND draw_date = CURDATE()")
    int getDrawCountToday(@Param("userId") Integer userId, @Param("activityId") Integer activityId);
}
