package com.test.prizesystem.config;

import com.test.prizesystem.model.entity.Activity;
import com.test.prizesystem.model.entity.ActivityPrize;
import com.test.prizesystem.model.entity.ActivityRule;
import com.test.prizesystem.model.entity.Prize;
import com.test.prizesystem.service.ActivityService;
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

@Slf4j
@Component
public class DemoDataInitializer {
    private static final String INITIALIZED_FLAG = "demo_data_initialized";

    @Value("${prize.init-demo-data:false}")
    private boolean autoInitDemoData; // 更名为autoInitDemoData，表示是否启动时自动初始化

    @Autowired
    private ActivityService activityService;
    
    @Autowired
    private RedBlackTreeStorage treeStorage;
    
    @Autowired
    private PersistenceManager persistenceManager;
    
    @Autowired
    private UserService userService;

    /**
     * 手动初始化演示数据
     * 由用户请求触发，而不是自动执行
     * 
     * @return 初始化结果描述
     */
    public String initializeDemoData() {
        // 用户手动触发时，不检查initDemoData配置

        // 检查活动树是否已有数据
        if (treeStorage.size("activities") > 0) {
            log.info("已存在演示数据，清空现有数据并重新初始化");
            // 清空现有数据
            treeStorage.clear("activities");
            treeStorage.clear("prizes");
            treeStorage.clear("activity_prizes");
            treeStorage.clear("activity_rules");
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
        treeStorage.save("activities", activity.getId(), activity);

        log.info("创建演示活动: {}, 开始时间: {}", activity.getTitle(), activity.getStartTime());
        
        return activity;
    }

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
            treeStorage.save("prizes", prize.getId(), prize);
        }

        log.info("创建演示奖品: {}, {}, {}", 
                prize1.getName(), prize2.getName(), prize3.getName());
        
        return prizes;
    }
    
    private List<ActivityPrize> createActivityPrizes(Activity activity, List<Prize> prizes) {
        List<ActivityPrize> activityPrizes = new ArrayList<>();
        
        ActivityPrize ap1 = new ActivityPrize();
        ap1.setId(1);
        ap1.setActivityId(activity.getId());
        ap1.setPrizeId(prizes.get(0).getId());
        ap1.setAmount(50);
        activityPrizes.add(ap1);

        ActivityPrize ap2 = new ActivityPrize();
        ap2.setId(2);
        ap2.setActivityId(activity.getId());
        ap2.setPrizeId(prizes.get(1).getId());
        ap2.setAmount(100);
        activityPrizes.add(ap2);

        ActivityPrize ap3 = new ActivityPrize();
        ap3.setId(3);
        ap3.setActivityId(activity.getId());
        ap3.setPrizeId(prizes.get(2).getId());
        ap3.setAmount(200);
        activityPrizes.add(ap3);

        // 保存活动奖品关联到红黑树
        for (ActivityPrize ap : activityPrizes) {
            treeStorage.save("activity_prizes", ap.getId(), ap);
        }
        
        return activityPrizes;
    }

    /**
     * 创建测试用户
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
    }
    
    private void createDemoRule(Activity activity) {
        // 创建活动规则
        ActivityRule rule = new ActivityRule();
        rule.setId(1);
        rule.setActivityId(activity.getId());
        rule.setUserLevel(0); // 0表示对所有用户等级适用
        rule.setMaxDrawsDaily(1000); // 增加次数上限以便于压测
        rule.setMaxWinsDaily(100);  // 增加中奖次数上限

        // 保存规则到红黑树
        treeStorage.save("activity_rules", rule.getId(), rule);

        log.info("创建演示活动规则: 每日最多抽奖{}次，最多中奖{}次", 
                rule.getMaxDrawsDaily(), rule.getMaxWinsDaily());
    }
}