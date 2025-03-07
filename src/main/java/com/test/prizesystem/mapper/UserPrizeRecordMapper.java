package com.test.prizesystem.mapper;



import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.test.prizesystem.model.entity.UserPrizeRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserPrizeRecordMapper extends BaseMapper<UserPrizeRecord> {

    /**
     * 获取用户当天中奖次数
     */
    @Select("SELECT COUNT(*) FROM t_user_prize_record WHERE user_id = #{userId} AND activity_id = #{activityId} AND win_date = CURDATE()")
    int getWinCountToday(@Param("userId") Integer userId, @Param("activityId") Integer activityId);
}
