package com.test.prizesystem.service.imp;

import com.test.prizesystem.model.entity.Activity;
import com.test.prizesystem.model.entity.ActivityPrize;
import com.test.prizesystem.model.entity.ActivityRule;
import com.test.prizesystem.service.ActivityService;
import com.test.prizesystem.service.TokenService;
import com.test.prizesystem.util.RedBlackTreeStorage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class ActivityServiceImpl implements ActivityService {

    @Autowired
    private RedBlackTreeStorage treeStorage;

    @Autowired
    private TokenService tokenService;
    
    // 用于缓存活动信息
    private final ConcurrentHashMap<Integer, Activity> activityCache = new ConcurrentHashMap<>();
    
    // 用于缓存活动规则信息
    private final ConcurrentHashMap<String, ActivityRule> ruleCache = new ConcurrentHashMap<>();
    
    // 红黑树存储的名称
    private static final String ACTIVITY_TREE = "activities";
    private static final String RULE_TREE = "activity_rules";
    private static final String PRIZE_RELATION_TREE = "activity_prizes";
    
    @PostConstruct
    public void init() {
        // 初始化默认活动数据
        if (treeStorage.size(ACTIVITY_TREE) == 0) {
            initDefaultActivityData();
        }
    }
    
    private void initDefaultActivityData() {
        // 创建默认活动
        Activity activity = new Activity();
        activity.setId(1);
        activity.setTitle("固定时间抽奖活动");
        
        // 设置活动时间
        Date now = new Date();
        activity.setStartTime(now);
        
        // 设置活动结束时间为7天后
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(now);
        calendar.add(Calendar.DAY_OF_MONTH, 7);
        activity.setEndTime(calendar.getTime());
        
        // 设置活动类型为时间型
        activity.setType(2);
        
        // 设置活动状态为已预热
        activity.setStatus(1);
        
        // 保存到红黑树
        treeStorage.save(ACTIVITY_TREE, activity.getId(), activity);
        activityCache.put(activity.getId(), activity);
        
        // 创建活动规则
        ActivityRule rule = new ActivityRule();
        rule.setId(1);
        rule.setActivityId(1);
        rule.setUserLevel(0);
        rule.setMaxDrawsDaily(1000);
        rule.setMaxWinsDaily(100);
        
        // 保存活动规则
        treeStorage.save(RULE_TREE, rule.getId(), rule);
        ruleCache.put(rule.getActivityId() + "_" + rule.getUserLevel(), rule);
        
        // 创建活动奖品关联
        List<ActivityPrize> activityPrizes = new ArrayList<>();
        
        ActivityPrize ap1 = new ActivityPrize();
        ap1.setId(1);
        ap1.setActivityId(1);
        ap1.setPrizeId(1);
        ap1.setAmount(50);
        activityPrizes.add(ap1);
        
        ActivityPrize ap2 = new ActivityPrize();
        ap2.setId(2);
        ap2.setActivityId(1);
        ap2.setPrizeId(2);
        ap2.setAmount(100);
        activityPrizes.add(ap2);
        
        ActivityPrize ap3 = new ActivityPrize();
        ap3.setId(3);
        ap3.setActivityId(1);
        ap3.setPrizeId(3);
        ap3.setAmount(200);
        activityPrizes.add(ap3);
        
        // 保存活动奖品关联
        for (ActivityPrize ap : activityPrizes) {
            treeStorage.save(PRIZE_RELATION_TREE, ap.getId(), ap);
        }
        
        log.info("默认活动数据初始化完成");
        
        // 生成令牌
        tokenService.generateTokens(activity, activityPrizes);
    }

    @Override
    public Activity getActivity(Integer activityId) {
        // 先从缓存获取
        Activity activity = activityCache.get(activityId);
        if (activity != null) {
            return activity;
        }
        
        // 缓存中没有，从红黑树中获取
        activity = treeStorage.find(ACTIVITY_TREE, activityId, Activity.class);
        
        // 如果存在，加入缓存
        if (activity != null) {
            activityCache.put(activityId, activity);
        }
        
        return activity;
    }

    @Override
    public ActivityRule getActivityRule(Integer activityId, Integer userLevel) {
        // 生成缓存key
        String cacheKey = activityId + "_" + userLevel;
        
        // 先从缓存获取
        ActivityRule rule = ruleCache.get(cacheKey);
        if (rule != null) {
            return rule;
        }
        
        // 缓存中没有，遍历红黑树寻找对应规则
        // 在实际应用中应该有更高效的查询方式，这里简化处理
        ActivityRule result = null;
        
        // 先查找指定用户等级的规则
        // 实际中应该通过索引查询，这里简化为线性查找
        for (int i = 1; i <= 10; i++) {
            ActivityRule r = treeStorage.find(RULE_TREE, i, ActivityRule.class);
            if (r != null && r.getActivityId().equals(activityId) && r.getUserLevel().equals(userLevel)) {
                result = r;
                break;
            }
        }
        
        // 如果没找到指定用户等级的规则，查找默认规则
        if (result == null) {
            for (int i = 1; i <= 10; i++) {
                ActivityRule r = treeStorage.find(RULE_TREE, i, ActivityRule.class);
                if (r != null && r.getActivityId().equals(activityId) && r.getUserLevel().equals(0)) {
                    result = r;
                    break;
                }
            }
        }
        
        // 如果找到了规则，加入缓存
        if (result != null) {
            ruleCache.put(cacheKey, result);
        }
        
        return result;
    }

    @Override
    public void preheatActivity(Integer activityId) {
        // 获取活动信息
        Activity activity = getActivity(activityId);
        if (activity == null) {
            log.error("活动{}不存在，无法预热", activityId);
            return;
        }

        // 获取活动关联的奖品
        List<ActivityPrize> activityPrizes = new ArrayList<>();
        
        // 简化处理，直接使用硬编码的演示数据
        if (activityId == 1) {
            ActivityPrize ap1 = new ActivityPrize();
            ap1.setActivityId(1);
            ap1.setPrizeId(1);
            ap1.setAmount(50);
            activityPrizes.add(ap1);
            
            ActivityPrize ap2 = new ActivityPrize();
            ap2.setActivityId(1);
            ap2.setPrizeId(2);
            ap2.setAmount(100);
            activityPrizes.add(ap2);
            
            ActivityPrize ap3 = new ActivityPrize();
            ap3.setActivityId(1);
            ap3.setPrizeId(3);
            ap3.setAmount(200);
            activityPrizes.add(ap3);
        }

        // 根据活动类型生成令牌
        tokenService.generateTokens(activity, activityPrizes);

        // 更新活动状态为已预热
        activity.setStatus(1);
        treeStorage.save(ACTIVITY_TREE, activity.getId(), activity);
        activityCache.put(activity.getId(), activity);

        log.info("活动{}预热完成", activityId);
    }
}