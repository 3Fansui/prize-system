package com.test.prizesystem.config;

import com.test.prizesystem.model.entity.Activity;
import com.test.prizesystem.model.entity.ActivityPrize;
import com.test.prizesystem.model.entity.ActivityRule;
import com.test.prizesystem.model.entity.Prize;
import com.test.prizesystem.model.entity.User;
import com.test.prizesystem.service.ActivityService;
import com.test.prizesystem.service.DrawService;
import com.test.prizesystem.service.UserService;
import com.test.prizesystem.util.RedBlackTreeStorage;
import com.test.prizesystem.util.TreeNames;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import com.test.prizesystem.util.PersistenceManager;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * 演示数据初始化器
 * <p>
 * 负责创建系统演示所需的测试数据，包括活动、奖品、活动奖品关联、活动规则和测试用户。
 * 可以通过API手动触发初始化，也可以通过配置在系统启动时自动初始化。
 * 
 * @author wu
 * @version 1.1
 */
@Slf4j
@Component
public class DemoDataInitializer {

    @Autowired
    private ActivityService activityService;
    
    @Autowired
    private RedBlackTreeStorage treeStorage;
    
    @Autowired
    private PersistenceManager persistenceManager;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private DrawService drawService;

    /**
     * 手动初始化演示数据
     * <p>
     * 由用户请求触发，创建系统所需的测试数据，包括活动、奖品、活动奖品关联、
     * 活动规则和测试用户。数据创建后会自动预热。
     * 
     * @return 初始化结果描述
     * @author wu
     */
    public String initializeDemoData() {
        // 检查活动树是否已有数据
        if (treeStorage.size(TreeNames.ACTIVITIES) > 0) {
            log.info("已存在演示数据，清空现有数据并重新初始化");
            // 清空现有数据
            treeStorage.clear(TreeNames.ACTIVITIES);
            treeStorage.clear(TreeNames.PRIZES);
            treeStorage.clear(TreeNames.ACTIVITY_PRIZES);
            treeStorage.clear(TreeNames.ACTIVITY_RULES);
        }

        log.info("开始初始化演示数据");

        // 创建演示活动
        Activity activity = createDemoActivity();
        
        // 创建演示奖品
        List<Prize> prizes = createDemoPrizes();
        
        // 创建活动奖品关联
        List<ActivityPrize> activityPrizes = createActivityPrizes(activity, prizes);
        
        // 创建活动规则
        createDemoRule(activity);

        // 预热活动
        activityService.preheatActivity(activity.getId());
        
        // 创建测试用户
        createDemoUsers();
        
        // 标记数据已变更，需要持久化
        persistenceManager.markDataChanged();
        // 立即同步到磁盘
        persistenceManager.forceSyncNow();

        log.info("演示数据初始化完成");
        return "演示数据初始化完成：1个活动，3个奖品，11个用户";
    }

    /**
     * 创建演示活动
     * <p>
     * 创建一个固定时间型抽奖活动，从当前时间开始，持续1小时。
     * 
     * @return 创建的活动对象
     * @author wu
     */
    private Activity createDemoActivity() {
        // 创建固定时间型抽奖活动（类型2）
        Activity activity = new Activity();
        activity.setId(1);
        activity.setTitle("固定时间抽奖活动");

        // 当前时间
        Date now = new Date();

        // 设置活动开始时间为现在
        activity.setStartTime(now);

        // 设置活动结束时间为开始时间后1小时
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(now);
        calendar.add(Calendar.HOUR_OF_DAY, 1);
        activity.setEndTime(calendar.getTime());

        // 设置活动类型为固定时间型
        activity.setType(2);

        // 设置活动状态为未开始
        activity.setStatus(0);

        // 保存活动到红黑树
        treeStorage.save(TreeNames.ACTIVITIES, activity.getId(), activity);

        log.info("创建演示活动: {}, 开始时间: {}", activity.getTitle(), activity.getStartTime());
        
        return activity;
    }

    /**
     * 创建演示奖品
     * <p>
     * 创建三个不同的演示奖品，包括iPhone 14、AirPods和小米手环。
     * 奖品数量设置较多，便于压力测试。
     * 
     * @return 奖品列表
     * @author wu
     */
    private List<Prize> createDemoPrizes() {
        List<Prize> prizes = new ArrayList<>();
        
        // 创建奖品，增加数量以便于压测
        Prize prize1 = new Prize();
        prize1.setId(1);
        prize1.setName("iPhone 14");
        prize1.setPrice(new BigDecimal("5999.00"));
        prize1.setTotalAmount(50);
        prize1.setRemainingAmount(50);
        prize1.setImageUrl("https://example.com/images/iphone14.jpg");
        prizes.add(prize1);

        Prize prize2 = new Prize();
        prize2.setId(2);
        prize2.setName("AirPods");
        prize2.setPrice(new BigDecimal("1299.00"));
        prize2.setTotalAmount(100);
        prize2.setRemainingAmount(100);
        prize2.setImageUrl("https://example.com/images/airpods.jpg");
        prizes.add(prize2);

        Prize prize3 = new Prize();
        prize3.setId(3);
        prize3.setName("小米手环");
        prize3.setPrice(new BigDecimal("249.00"));
        prize3.setTotalAmount(200);
        prize3.setRemainingAmount(200);
        prize3.setImageUrl("https://example.com/images/miband.jpg");
        prizes.add(prize3);

        // 保存奖品到红黑树
        for (Prize prize : prizes) {
            treeStorage.save(TreeNames.PRIZES, prize.getId(), prize);
        }

        log.info("创建演示奖品: {}, {}, {}", 
                prize1.getName(), prize2.getName(), prize3.getName());
        
        return prizes;
    }
    
    /**
     * 创建活动奖品关联
     * 
     * @param activity 活动对象
     * @param prizes 奖品列表
     * @return 活动奖品关联列表
     * @author wu
     */
    private List<ActivityPrize> createActivityPrizes(Activity activity, List<Prize> prizes) {
        List<ActivityPrize> activityPrizes = new ArrayList<>();
        
        ActivityPrize ap1 = new ActivityPrize();
        ap1.setActivityId(activity.getId());
        ap1.setPrizeId(prizes.get(0).getId());
        ap1.setAmount(50);
        activityPrizes.add(ap1);

        ActivityPrize ap2 = new ActivityPrize();
        ap2.setActivityId(activity.getId());
        ap2.setPrizeId(prizes.get(1).getId());
        ap2.setAmount(100);
        activityPrizes.add(ap2);

        ActivityPrize ap3 = new ActivityPrize();
        ap3.setActivityId(activity.getId());
        ap3.setPrizeId(prizes.get(2).getId());
        ap3.setAmount(200);
        activityPrizes.add(ap3);

        // 保存活动奖品关联到红黑树，使用activityId_prizeId组合作为键
        for (ActivityPrize ap : activityPrizes) {
            String relationKey = ap.getActivityId() + "_" + ap.getPrizeId();
            treeStorage.save(TreeNames.ACTIVITY_PRIZES, relationKey.hashCode(), ap);
        }
        
        return activityPrizes;
    }

    /**
     * 创建测试用户
     * <p>
     * 创建10个测试用户和1个管理员用户。每个普通用户拥有100次抽奖次数和10次中奖上限，
     * 管理员有更高的配额。用户创建后会预热到缓存中，提高抽奖效率。
     *
     * @author wu
     */
    private void createDemoUsers() {
        // 先检查是否已有用户数据
        if (treeStorage.size(TreeNames.USERS) > 0) {
            log.info("已存在用户数据，清除并重新创建");
            treeStorage.clear(TreeNames.USERS);
        }
        
        // 创建测试用户
        userService.register(1001, "test1", "test", 100, 10);
        userService.register(1002, "test2", "test", 100, 10);
        userService.register(1003, "test3", "test", 100, 10);
        userService.register(1004, "test4", "test", 100, 10);
        userService.register(1005, "test5", "test", 100, 10);
        userService.register(1006, "test6", "test", 100, 10);
        userService.register(1007, "test7", "test", 100, 10);
        userService.register(1008, "test8", "test", 100, 10);
        userService.register(1009, "test9", "test", 100, 10);
        userService.register(1010, "test10", "test", 100, 10);
        userService.register(9999, "admin", "admin", 1000, 100);
        
        log.info("创建测试用户完成");
        
        // 获取所有用户并预热到缓存
        try {
            List<User> allUsers = treeStorage.getSampleData(TreeNames.USERS, User.class, Integer.MAX_VALUE);
            if (allUsers != null && !allUsers.isEmpty()) {
                // 预热前过滤检查用户数据
                List<User> validUsers = new ArrayList<>();
                for (User user : allUsers) {
                    if (user != null && user.getId() != null) {
                        validUsers.add(user);
                    }
                }
                
                drawService.preloadUserCache(validUsers);
                log.info("测试用户缓存预热完成，共加载 {} 个用户", validUsers.size());
            } else {
                log.warn("未找到用户数据进行预热");
            }
        } catch (Exception e) {
            log.error("预热用户缓存失败", e);
        }
    }
    
    /**
     * 创建活动规则
     * <p>
     * 为指定活动创建活动规则，规定用户每日抽奖和中奖次数上限。
     * 此处设置的次数较多，主要是为了支持压力测试。
     *
     * @param activity 要创建规则的活动
     * @author wu
     */
    private void createDemoRule(Activity activity) {
        // 创建活动规则
        ActivityRule rule = new ActivityRule();
        rule.setId(1);
        rule.setActivityId(activity.getId());
        rule.setUserLevel(0); // 0表示对所有用户等级适用
        rule.setMaxDrawsDaily(1000); // 增加次数上限以便于压测
        rule.setMaxWinsDaily(100);  // 增加中奖次数上限

        // 保存规则到红黑树
        treeStorage.save(TreeNames.ACTIVITY_RULES, rule.getId(), rule);

        log.info("创建演示活动规则: 每日最多抽奖{}次，最多中奖{}次", 
                rule.getMaxDrawsDaily(), rule.getMaxWinsDaily());
    }
}