package com.test.prizesystem.service.imp;

import com.test.prizesystem.model.entity.Activity;
import com.test.prizesystem.model.entity.ActivityPrize;
import com.test.prizesystem.model.entity.ActivityRule;
import com.test.prizesystem.model.entity.Prize;
import com.test.prizesystem.model.vo.ActivityInfoVO;
import com.test.prizesystem.service.ActivityService;
import com.test.prizesystem.service.TokenService;
import com.test.prizesystem.util.PersistenceManager;
import com.test.prizesystem.util.RedBlackTreeStorage;
import com.test.prizesystem.util.TreeNames;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Calendar;
import java.util.Date;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class ActivityServiceImpl implements ActivityService {

    @Autowired
    private RedBlackTreeStorage treeStorage;

    @Autowired
    private TokenService tokenService;
    
    @Autowired
    private PersistenceManager persistenceManager;
    
    // 用于缓存活动信息
    private final ConcurrentHashMap<Integer, Activity> activityCache = new ConcurrentHashMap<>();
    
    // 用于缓存活动规则信息
    private final ConcurrentHashMap<String, ActivityRule> ruleCache = new ConcurrentHashMap<>();
    
    // 红黑树存储的名称
    private static final String ACTIVITY_TREE = "activities";
    private static final String RULE_TREE = "activity_rules";
    private static final String PRIZE_RELATION_TREE = "activity_prizes";
    private static final String PRIZE_TREE = "prizes";
    
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
        
        // 标记数据变更，需要持久化
        persistenceManager.markDataChanged();
    }
    
    @Override
    public ActivityInfoVO getActivityInfo(Integer activityId) {
        // 获取活动基本信息
        Activity activity = getActivity(activityId);
        if (activity == null) {
            return null;
        }
        
        // 创建返回对象
        ActivityInfoVO result = new ActivityInfoVO();
        result.setId(activity.getId());
        result.setTitle(activity.getTitle());
        result.setStartTime(activity.getStartTime());
        result.setEndTime(activity.getEndTime());
        result.setStatus(activity.getStatus());
        
        // 查询活动关联的奖品信息
        List<ActivityInfoVO.PrizeInfoVO> prizeInfos = new ArrayList<>();
        
        // 从红黑树中查找活动奖品关联
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
        
        // 获取奖品详细信息并组装
        for (ActivityPrize ap : activityPrizes) {
            Prize prize = treeStorage.find(TreeNames.PRIZES, ap.getPrizeId(), Prize.class);
            if (prize != null) {
                ActivityInfoVO.PrizeInfoVO prizeInfo = new ActivityInfoVO.PrizeInfoVO();
                prizeInfo.setId(prize.getId());
                prizeInfo.setName(prize.getName());
                prizeInfo.setAmount(ap.getAmount());
                prizeInfos.add(prizeInfo);
            }
        }
        
        result.setPrizes(prizeInfos);
        return result;
    }
    
    @Override
    public Integer createActivity(Activity activity) {
        // 生成ID（实际应用中应该有更好的ID生成策略）
        if (activity.getId() == null) {
            activity.setId(generateId());
        }
        
        // 设置创建时间和更新时间
        Date now = new Date();
        if (activity.getCreateTime() == null) {
            activity.setCreateTime(now);
        }
        activity.setUpdateTime(now);
        
        // 默认状态为未开始
        if (activity.getStatus() == null) {
            activity.setStatus(0); // 0=未开始, 1=进行中, 2=已结束
        }
        
        // 保存到红黑树和缓存
        treeStorage.save(ACTIVITY_TREE, activity.getId(), activity);
        activityCache.put(activity.getId(), activity);
        
        // 标记数据变更
        persistenceManager.markDataChanged();
        
        log.info("创建活动成功: ID={}, 标题={}", activity.getId(), activity.getTitle());
        return activity.getId();
    }
    
    @Override
    public boolean updateActivity(Activity activity) {
        if (activity.getId() == null) {
            return false;
        }
        
        // 获取现有活动
        Activity existingActivity = getActivity(activity.getId());
        if (existingActivity == null) {
            return false;
        }
        
        // 更新时间
        activity.setUpdateTime(new Date());
        activity.setCreateTime(existingActivity.getCreateTime()); // 保持创建时间不变
        
        // 更新到红黑树和缓存
        treeStorage.save(ACTIVITY_TREE, activity.getId(), activity);
        activityCache.put(activity.getId(), activity);
        
        // 标记数据变更
        persistenceManager.markDataChanged();
        
        log.info("更新活动成功: ID={}", activity.getId());
        return true;
    }
    
    @Override
    public Integer createPrize(Prize prize) {
        // 生成ID
        if (prize.getId() == null) {
            prize.setId(generateId());
        }
        
        // 设置创建时间和更新时间
        Date now = new Date();
        if (prize.getCreateTime() == null) {
            prize.setCreateTime(now);
        }
        prize.setUpdateTime(now);
        
        // 默认设置库存
        if (prize.getRemainingAmount() == null) {
            prize.setRemainingAmount(prize.getTotalAmount());
        }
        
        // 保存到红黑树
        treeStorage.save(PRIZE_TREE, prize.getId(), prize);
        
        // 标记数据变更
        persistenceManager.markDataChanged();
        
        log.info("创建奖品成功: ID={}, 名称={}", prize.getId(), prize.getName());
        return prize.getId();
    }
    
    @Override
    public boolean associateActivityPrize(ActivityPrize activityPrize) {
        // 检查活动和奖品是否存在
        Activity activity = getActivity(activityPrize.getActivityId());
        Prize prize = treeStorage.find(PRIZE_TREE, activityPrize.getPrizeId(), Prize.class);
        
        if (activity == null || prize == null) {
            log.error("关联活动和奖品失败: 活动或奖品不存在");
            return false;
        }
        
        // 生成关联ID
        String relationKey = activityPrize.getActivityId() + "_" + activityPrize.getPrizeId();
        
        // 保存关联到红黑树
        treeStorage.save(PRIZE_RELATION_TREE, relationKey.hashCode(), activityPrize);
        
        // 标记数据变更
        persistenceManager.markDataChanged();
        
        log.info("关联活动和奖品成功: 活动ID={}, 奖品ID={}, 数量={}",
                activityPrize.getActivityId(), activityPrize.getPrizeId(), activityPrize.getAmount());
        return true;
    }
    
    @Override
    public List<Activity> getAllActivities() {
        List<Activity> activities = treeStorage.getSampleData(ACTIVITY_TREE, Activity.class, 100);
        return activities;
    }
    
    @Override
    public List<Prize> getAllPrizes() {
        List<Prize> prizes = treeStorage.getSampleData(PRIZE_TREE, Prize.class, 100);
        return prizes;
    }
    
    @Override
    public List<Activity> findActivitiesNeedingPreheat() {
        List<Activity> result = new ArrayList<>();
        List<Activity> allActivities = getAllActivities();
        
        // 获取当前时间
        Calendar now = Calendar.getInstance();
        
        // 获取1分钟后的时间
        Calendar oneMinuteLater = Calendar.getInstance();
        oneMinuteLater.add(Calendar.MINUTE, 1);
        
        for (Activity activity : allActivities) {
            // 检查活动是否即将开始且未预热
            if (activity.getStatus() == 0 && activity.getStartTime() != null) {
                Calendar startTime = Calendar.getInstance();
                startTime.setTime(activity.getStartTime());
                
                // 如果活动开始时间在当前时间和1分钟后之间，且未预热，则加入列表
                if (startTime.after(now) && startTime.before(oneMinuteLater)) {
                    result.add(activity);
                    log.debug("找到需要预热的活动: ID={}, 开始时间={}", activity.getId(), activity.getStartTime());
                }
            }
        }
        
        return result;
    }
    
    // 生成ID（简单实现，实际应用中应该使用更可靠的算法）
    private Integer generateId() {
        return Math.abs((int) System.currentTimeMillis() % 1000000);
    }
}