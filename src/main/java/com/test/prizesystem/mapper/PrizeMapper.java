package com.test.prizesystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.test.prizesystem.model.entity.Prize;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface PrizeMapper extends BaseMapper<Prize> {

    /**
     * 减少奖品数量
     */
    @Update("UPDATE t_prize SET remaining_amount = remaining_amount - 1 WHERE id = #{prizeId} AND remaining_amount > 0")
    int decreaseRemainingAmount(@Param("prizeId") Integer prizeId);
}