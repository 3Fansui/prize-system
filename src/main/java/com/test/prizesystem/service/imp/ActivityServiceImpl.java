package com.test.prizesystem.service.imp;

import com.test.prizesystem.model.entity.Activity;
import com.test.prizesystem.model.entity.ActivityPrize;
import com.test.prizesystem.model.entity.ActivityRule;
import com.test.prizesystem.model.entity.Prize;
import com.test.prizesystem.model.entity.User;
import com.test.prizesystem.model.vo.ActivityInfoVO;
import com.test.prizesystem.service.ActivityService;
import com.test.prizesystem.service.DrawService;
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
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 活动服务实现类
 * <p>
 * 该服务类实现活动的全生命周期管理，包括活动的创建、修改、查询、预热等功能。
 * 主要功能包括：
 * <ul>
 *   <li>活动的CRUD操作，维护活动基本信息</li>
 *   <li>奖品管理，包括奖品的创建和查询</li>
 *   <li>活动与奖品的关联管理</li>
 *   <li>活动预热，包括生成令牌和用户缓存预热</li>
 *   <li>活动规则管理，实现基于用户等级的规则管理</li>
 * </ul>
 * 类使用红黑树作为数据存储，实现高效的数据访问和管理。
 * 
 * @author wu
 * @version 2.0
 */
@Slf4j
@Service
public class ActivityServiceImpl implements ActivityService {

    @Autowired
    private RedBlackTreeStorage treeStorage;

    @Autowired
    private TokenService tokenService;
    
    @Autowired
    private PersistenceManager persistenceManager;

    
    @Autowired
    private DrawService drawService;
    
    // 用于缓存活动规则信息
    private final ConcurrentHashMap<String, ActivityRule> ruleCache = new ConcurrentHashMap<>();
    
    // 自增ID计数器
    private final AtomicInteger activityIdCounter = new AtomicInteger(1);
    private final AtomicInteger prizeIdCounter = new AtomicInteger(1);
    
    // 红黑树存储的名称
    // 使用枚举而非字符串常量，确保持久化时可以找到所有树
    
    /**
     * 根据活动ID获取活动信息
     * <p>
     * 直接从红黑树存储中读取活动信息，不使用缓存。
     * 这确保了数据的实时性，但在高并发情况下可能有性能影响。
     *
     * @param activityId 活动ID
     * @return 活动对象，如果不存在则返回numl
     * @author wu
     */
    @Override
    public Activity getActivity(Integer activityId) {
        // 直接从红黑树中获取活动信息，不使用缓存
        return treeStorage.find(TreeNames.ACTIVITIES, activityId, Activity.class);
    }

    /**
     * 获取活动规则
     * <p>
     * 根据活动ID和用户等级获取相应的活动规则。方法使用两级查询策略：
     * <ol>
     *   <li>首先从内存缓存中获取，如果存在则直接返回</li>
     *   <li>如果缓存中不存在，则从红黑树中查找特定用户等级的规则</li>
     *   <li>如果没有找到特定等级的规则，则查找默认规则（用户等级为0）</li>
     * </ol>
     * 找到的规则会被缓存起来，以提高后续访问效率。
     *
     * @param activityId 活动ID
     * @param userLevel 用户等级
     * @return 活动规则对象，如果不存在则返回null
     * @author wu
     */
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
            ActivityRule r = treeStorage.find(TreeNames.ACTIVITY_RULES, i, ActivityRule.class);
            if (r != null && r.getActivityId().equals(activityId) && r.getUserLevel().equals(userLevel)) {
                result = r;
                break;
            }
        }
        
        // 如果没找到指定用户等级的规则，查找默认规则
        if (result == null) {
            for (int i = 1; i <= 10; i++) {
                ActivityRule r = treeStorage.find(TreeNames.ACTIVITY_RULES, i, ActivityRule.class);
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

    /**
     * 活动预热处理
     * <p>
     * 对指定活动进行预热处理，主要包括以下步骤：
     * <ol>
     *   <li>根据活动关联的奖品信息，生成奖品令牌并放入令牌队列</li>
     *   <li>获取所有用户信息，预加载到用户缓存中，提高抽奖效率</li>
     *   <li>更新活动状态为已预热（状态值为1）</li>
     * </ol>
     * 预热过程是活动开始前的必要准备工作，可以提高活动进行中的性能和用户体验。
     * 如果活动没有关联奖品，预热会被标记为不完整。
     *
     * @param activityId 要预热的活动ID
     * @author wu
     */
    @Override
    public void preheatActivity(Integer activityId) {
        // 获取活动信息
        Activity activity = getActivity(activityId);
        if (activity == null) {
            log.error("活动{}ID不存在，无法预热", activityId);
            return;
        }

        log.info("开始预热活动: {}, ID={}", activity.getTitle(), activityId);

        // 1. 根据奖品数量生成令牌放入双端队列
        List<ActivityPrize> activityPrizes = findActivityPrizes(activityId);
        if (activityPrizes.isEmpty()) {
            log.warn("活动{}没有关联奖品，预热将不完整", activityId);
        } else {
            log.info("活动{}共有{}(个)奖品配置", activityId, activityPrizes.size());
        }

        // 生成并放入令牌队列
        tokenService.generateTokens(activity, activityPrizes);
        log.info("已生成令牌并放入队列，第一步预热完成");

        // 2. 查询用户信息放入缓存map
        try {
            List<User> allUsers = treeStorage.getSampleData(TreeNames.USERS, User.class, Integer.MAX_VALUE);
            if (allUsers == null || allUsers.isEmpty()) {
                log.warn("未找到用户数据，无法预热用户缓存");
            } else {
                // 直接加载有效用户到缓存
                int validUserCount = 0;
                for (User user : allUsers) {
                    if (user != null && user.getId() != null) {
                        validUserCount++;
                    }
                }
                
                // 如果有效用户数量大于0，进行缓存预热
                if (validUserCount > 0) {
                    drawService.preloadUserCache(allUsers);
                    log.info("用户缓存预热完成，已加载 {} 个有效用户", validUserCount);
                } else {
                    log.warn("找到的用户数据全部无效，无法预热用户缓存");
                }
            }
        } catch (Exception e) {
            log.error("预热用户缓存失败", e);
        }

        // 3. 更新活动状态为已预热
        activity.setStatus(1);
        treeStorage.save(TreeNames.ACTIVITIES, activity.getId(), activity);

        log.info("活动{}预热完成，已准备好令牌和用户缓存", activityId);
        
        // 标记数据变更，需要持久化
        persistenceManager.markDataChanged();
    }
    
    /**
     * 获取活动详细信息
     * <p>
     * 根据活动ID获取活动的完整信息，包括基本信息和关联的奖品信息。
     * 处理过程包括：
     * <ol>
     *   <li>获取活动基本信息，如标题、开始时间、结束时间等</li>
     *   <li>查询活动关联的所有奖品信息</li>
     *   <li>将活动基本信息和奖品信息组装成VO对象返回</li>
     * </ol>
     * 这个方法主要用于前端展示活动详情。
     *
     * @param activityId 活动ID
     * @return 活动信息VO对象，包含完整的活动和奖品信息，如果活动不存在则返回null
     * @author wu
     */
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
        List<ActivityPrize> activityPrizes = findActivityPrizes(activityId);
        
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
    
    /**
     * 查找活动关联的奖品
     * <p>
     * 根据活动ID查询该活动关联的所有奖品信息。方法流程：
     * <ol>
     *   <li>从红黑树中获取活动奖品关联表中的所有数据</li>
     *   <li>通过activityId过滤出当前活动相关的关联数据</li>
     * </ol>
     * 这是一个内部辅助方法，用于支持活动信息展示、活动预热等功能。
     * 
     * @param activityId 活动ID
     * @return 奖品关联列表，如果没有则返回空列表
     * @author wu
     */
    private List<ActivityPrize> findActivityPrizes(Integer activityId) {
        // 从红黑树中查询活动奖品关联
        List<ActivityPrize> result = new ArrayList<>();
        
        // 从PRIZE_RELATION_TREE中查找与活动ID相关的所有奖品关联
        // 注意：这里需要根据实际红黑树的存储方式调整查询逻辑
        // 下面是一个示例实现，需要根据实际存储结构优化
        List<ActivityPrize> allRelations = treeStorage.getSampleData(TreeNames.ACTIVITY_PRIZES, ActivityPrize.class, 100);
        for (ActivityPrize relation : allRelations) {
            if (relation.getActivityId().equals(activityId)) {
                result.add(relation);
            }
        }
        
        return result;
    }
    
    /**
     * 创建新活动
     * <p>
     * 创建并保存新的活动对象。处理流程：
     * <ol>
     *   <li>如果活动ID为null，自动生成新的ID</li>
     *   <li>设置创建时间和更新时间为当前时间</li>
     *   <li>如果状态为null，设置默认状态为未开始(0)</li>
     *   <li>将活动数据保存到红黑树存储中</li>
     *   <li>标记数据变更，触发持久化机制</li>
     * </ol>
     * 所有新创建的活动都为“未开始”状态，需要通过预热过程改变状态。
     *
     * @param activity 要创建的活动对象
     * @return 新创建的活动ID
     * @author wu
     */
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
        
        // 保存到红黑树
        treeStorage.save(TreeNames.ACTIVITIES, activity.getId(), activity);
        
        // 标记数据变更
        persistenceManager.markDataChanged();
        
        log.info("创建活动成功: ID={}, 标题={}", activity.getId(), activity.getTitle());
        return activity.getId();
    }
    
    /**
     * 更新活动信息
     * <p>
     * 更新现有活动的信息。处理流程：
     * <ol>
     *   <li>检查活动ID是否为null，如果为null则无法更新</li>
     *   <li>查询现有活动，如果不存在则无法更新</li>
     *   <li>更新活动的更新时间为当前时间</li>
     *   <li>保持原有的创建时间不变</li>
     *   <li>将更新后的活动保存到红黑树存储中</li>
     *   <li>标记数据变更，触发持久化机制</li>
     * </ol>
     * 注意：此方法仅更新活动基本信息，不涉及活动与奖品的关联关系。
     *
     * @param activity 要更新的活动对象
     * @return 更新是否成功
     * @author wu
     */
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
        
        // 更新到红黑树
        treeStorage.save(TreeNames.ACTIVITIES, activity.getId(), activity);
        
        // 标记数据变更
        persistenceManager.markDataChanged();
        
        log.info("更新活动成功: ID={}", activity.getId());
        return true;
    }
    
    /**
     * 创建新奖品
     * <p>
     * 创建并保存新的奖品对象。处理流程：
     * <ol>
     *   <li>如果奖品ID为null，自动生成新的ID</li>
     *   <li>设置创建时间和更新时间为当前时间</li>
     *   <li>如果剩余数量为null，则默认设置为总数量</li>
     *   <li>将奖品数据保存到红黑树存储中</li>
     *   <li>标记数据变更，触发持久化机制</li>
     * </ol>
     * 创建奖品后，需要通过associateActivityPrize方法将奖品关联到活动中才能使用。
     *
     * @param prize 要创建的奖品对象
     * @return 新创建的奖品ID
     * @author wu
     */
    @Override
    public Integer createPrize(Prize prize) {
        // 生成ID
        if (prize.getId() == null) {
            prize.setId(generatePrizeId());
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
        treeStorage.save(TreeNames.PRIZES, prize.getId(), prize);
        
        // 标记数据变更
        persistenceManager.markDataChanged();
        
        log.info("创建奖品成功: ID={}, 名称={}", prize.getId(), prize.getName());
        return prize.getId();
    }
    
    /**
     * 关联活动和奖品
     * <p>
     * 将奖品关联到指定活动中，并指定奖品数量。处理流程：
     * <ol>
     *   <li>验证活动和奖品是否存在</li>
     *   <li>生成关联键，基于活动ID和奖品ID的组合</li>
     *   <li>将关联信息保存到红黑树存储中</li>
     *   <li>标记数据变更，触发持久化机制</li>
     * </ol>
     * 应在活动预热前完成活动和奖品的关联，以确保预热时能正确生成令牌。
     * 如果活动或奖品不存在，则关联操作失败。
     *
     * @param activityPrize 包含活动ID、奖品ID和数量的关联对象
     * @return 关联是否成功
     * @author wu
     */
    @Override
    public boolean associateActivityPrize(ActivityPrize activityPrize) {
        // 检查活动和奖品是否存在
        Activity activity = getActivity(activityPrize.getActivityId());
        Prize prize = treeStorage.find(TreeNames.PRIZES, activityPrize.getPrizeId(), Prize.class);
        
        if (activity == null || prize == null) {
            log.error("关联活动和奖品失败: 活动或奖品不存在");
            return false;
        }
        
        // 生成关联ID
        String relationKey = activityPrize.getActivityId() + "_" + activityPrize.getPrizeId();
        
        // 保存关联到红黑树
        treeStorage.save(TreeNames.ACTIVITY_PRIZES, relationKey.hashCode(), activityPrize);
        
        // 标记数据变更
        persistenceManager.markDataChanged();
        
        log.info("关联活动和奖品成功: 活动ID={}, 奖品ID={}, 数量={}",
                activityPrize.getActivityId(), activityPrize.getPrizeId(), activityPrize.getAmount());
        return true;
    }
    
    /**
     * 获取所有活动
     * <p>
     * 从红黑树存储中检索并返回所有已创建的活动。
     * 注意：当活动数量过多时，可能会影响查询性能，目前限制为最多返回100个活动。
     *
     * @return 活动对象列表
     * @author wu
     */
    @Override
    public List<Activity> getAllActivities() {
        List<Activity> activities = treeStorage.getSampleData(TreeNames.ACTIVITIES, Activity.class, 100);
        return activities;
    }
    
    /**
     * 获取所有奖品
     * <p>
     * 从红黑树存储中检索并返回所有已创建的奖品。
     * 注意：当奖品数量过多时，可能会影响查询性能，目前限制为最多返回100个奖品。
     *
     * @return 奖品对象列表
     * @author wu
     */
    @Override
    public List<Prize> getAllPrizes() {
        List<Prize> prizes = treeStorage.getSampleData(TreeNames.PRIZES, Prize.class, 100);
        return prizes;
    }
    
    /**
     * 查找需要预热的活动
     * <p>
     * 搜索并返回所有不超过1分钟就要开始且尚未预热的活动。处理流程：
     * <ol>
     *   <li>获取所有活动</li>
     *   <li>获取当前时间和1分钟后的时间点</li>
     *   <li>筛选状态为未开始(0)的活动</li>
     *   <li>检查活动开始时间是否在当前时间和1分钟后之间</li>
     * </ol>
     * 该方法用于定时任务，自动发现需要预热的活动并触发预热过程。
     *
     * @return 需要预热的活动列表
     * @author wu
     */
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
    
    /**
     * 生成活动ID（自增实现）
     * <p>
     * 使用原子性整数计数器生成递增的活动ID。
     * 这种实现方式保证了在并发环境中ID的唯一性。
     *
     * @return 新生成的活动ID
     * @author wu
     */
    private Integer generateId() {
        return activityIdCounter.getAndIncrement();
    }
    
    /**
     * 生成奖品ID（自增实现）
     * <p>
     * 使用原子性整数计数器生成递增的奖品ID。
     * 这种实现方式保证了在并发环境中ID的唯一性。
     * 与活动ID生成器分开，确保两种实体各自维护独立的ID空间。
     *
     * @return 新生成的奖品ID
     * @author wu
     */
    private Integer generatePrizeId() {
        return prizeIdCounter.getAndIncrement();
    }
}