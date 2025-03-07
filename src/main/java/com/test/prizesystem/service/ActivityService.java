package com.test.prizesystem.service;


import com.test.prizesystem.model.entity.Activity;
import com.test.prizesystem.model.entity.ActivityRule;

public interface ActivityService {

    /**
     * 获取活动
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
     * 初始化演示数据
     */
    //void initDemoData();
}
