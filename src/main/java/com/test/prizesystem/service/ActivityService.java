package com.test.prizesystem.service;


import com.test.prizesystem.model.entity.Activity;
import com.test.prizesystem.model.entity.ActivityRule;
import com.test.prizesystem.model.vo.ActivityInfoVO;

public interface ActivityService {

    /**
     * 获取活动基本信息
     */
    Activity getActivity(Integer activityId);

    /**
     * 获取活动规则
     */
    ActivityRule getActivityRule(Integer activityId, Integer userLevel);

    /**
     * 活动预热
     */
    void preheatActivity(Integer activityId);
    
    /**
     * 获取活动详细信息（包括奖品信息）
     */
    ActivityInfoVO getActivityInfo(Integer activityId);
}