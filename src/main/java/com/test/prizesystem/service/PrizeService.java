package com.test.prizesystem.service;


import com.test.prizesystem.model.entity.Prize;
import com.test.prizesystem.model.vo.PrizeVO;

import java.util.List;

public interface PrizeService {

    /**
     * 获取活动奖品列表
     */
    List<Prize> getActivityPrizes(Integer activityId);

    /**
     * 获取奖品信息
     */
    Prize getPrize(Integer prizeId);

    /**
     * 随机获取一个奖品
     */
    Prize getRandomPrize(Integer activityId);

    /**
     * 减少奖品数量
     */
    boolean decreasePrizeAmount(Integer prizeId);

    /**
     * 将Prize转换为PrizeVO
     */
    PrizeVO toPrizeVO(Prize prize);
}
