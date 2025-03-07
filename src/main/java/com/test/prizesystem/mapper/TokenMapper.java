package com.test.prizesystem.mapper;



import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.test.prizesystem.model.entity.Token;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

@Mapper
public interface TokenMapper extends BaseMapper<Token> {

    /**
     * 获取当前时间之前的有效令牌
     */
    @Select("SELECT * FROM t_token WHERE activity_id = #{activityId} AND token_timestamp <= #{timestamp} AND status = 0 ORDER BY token_timestamp DESC LIMIT 1")
    Token getAvailableToken(@Param("activityId") Integer activityId, @Param("timestamp") Long timestamp);

    /**
     * 获取先到先得的令牌
     */
    @Select("SELECT * FROM t_token WHERE activity_id = #{activityId} AND status = 0 ORDER BY token_timestamp ASC LIMIT 1")
    Token getNextToken(@Param("activityId") Integer activityId);

    /**
     * 更新令牌状态为已使用
     */
    @Update("UPDATE t_token SET status = 1 WHERE id = #{id} AND status = 0")
    int updateTokenStatus(@Param("id") Long id);

    /**
     * 统计每个奖品的令牌数量
     */
    @Select("SELECT prize_id as prizeId, COUNT(*) as tokenCount " +
            "FROM t_token " +
            "WHERE activity_id = #{activityId} AND status = #{status} " +
            "GROUP BY prize_id")
    List<Map<String, Object>> selectPrizeTokenStats(@Param("activityId") Integer activityId,
                                                    @Param("status") Integer status);
}