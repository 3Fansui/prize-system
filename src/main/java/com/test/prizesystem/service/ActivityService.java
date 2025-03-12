package com.test.prizesystem.service;


import com.test.prizesystem.model.entity.Activity;
import com.test.prizesystem.model.entity.ActivityPrize;
import com.test.prizesystem.model.entity.ActivityRule;
import com.test.prizesystem.model.entity.Prize;
import com.test.prizesystem.model.vo.ActivityInfoVO;

import java.util.Date;
import java.util.List;

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
    
    /**
     * 创建新活动
     * @param activity 活动信息
     * @return 创建的活动ID
     */
    Integer createActivity(Activity activity);
    
    /**
     * 更新活动信息
     * @param activity 活动信息
     * @return 是否更新成功
     */
    boolean updateActivity(Activity activity);
    
    /**
     * 创建新奖品
     * @param prize 奖品信息
     * @return 创建的奖品ID
     */
    Integer createPrize(Prize prize);
    
    /**
     * 关联活动和奖品
     * @param activityPrize 活动奖品关联信息
     * @return 是否关联成功
     */
    boolean associateActivityPrize(ActivityPrize activityPrize);
    
    /**
     * 获取所有活动列表
     * @return 活动列表
     */
    List<Activity> getAllActivities();
    
    /**
     * 获取所有奖品列表
     * @return 奖品列表
     */
    List<Prize> getAllPrizes();
    
    /**
     * 查找需要预热的活动
     * 即将在1分钟内开始且未预热的活动
     * @return 需要预热的活动列表
     */
    List<Activity> findActivitiesNeedingPreheat();
}