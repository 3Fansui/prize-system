package com.test.prizesystem.config;


import com.test.prizesystem.mapper.ActivityMapper;
import com.test.prizesystem.mapper.ActivityPrizeMapper;
import com.test.prizesystem.mapper.ActivityRuleMapper;
import com.test.prizesystem.mapper.PrizeMapper;
import com.test.prizesystem.model.entity.Activity;
import com.test.prizesystem.model.entity.ActivityPrize;
import com.test.prizesystem.model.entity.ActivityRule;
import com.test.prizesystem.model.entity.Prize;
import com.test.prizesystem.service.ActivityService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;

@Slf4j
@Component
public class DemoDataInitializer implements CommandLineRunner {

    @Value("${prize.init-demo-data:true}")
    private boolean initDemoData;

    @Value("${prize.clear-database:false}")
    private boolean clearDatabase;

    @Autowired
    private ActivityMapper activityMapper;

    @Autowired
    private ActivityRuleMapper activityRuleMapper;

    @Autowired
    private ActivityPrizeMapper activityPrizeMapper;

    @Autowired
    private PrizeMapper prizeMapper;

    @Autowired
    private ActivityService activityService;


    @Override
    @Transactional
    public void run(String... args) {
        if (!initDemoData) {
            log.info("演示数据初始化已禁用，跳过初始化");
            return;
        }



        // 检查活动表是否有数据
        if (activityMapper.selectCount(null) > 0) {
            log.info("已存在演示数据，跳过初始化");
            return;
        }

        log.info("开始初始化演示数据");

        // 创建演示活动
        createDemoActivities();

        // 创建演示奖品和活动关联
        createDemoPrizes();

        // 创建活动规则
        createDemoRules();

        // 预热活动
        //activityService.preheatActivity(1);

        log.info("演示数据初始化完成");
    }


    private void createDemoActivities() {
        // 创建概率型抽奖活动（类型1）
        Activity activity1 = new Activity();
        activity1.setId(1);
        activity1.setTitle("概率抽奖活动");

        // 当前时间
        Date now = new Date();

        // 设置活动开始时间为现在后3分钟
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(now);
        calendar.add(Calendar.MINUTE, 3); // 3分钟后开始
        activity1.setStartTime(calendar.getTime());

        // 设置活动结束时间为开始时间后7天
        calendar.add(Calendar.DAY_OF_MONTH, 7);
        activity1.setEndTime(calendar.getTime());

        // 设置活动类型为概率型
        activity1.setType(1);

        // 设置活动状态为未开始
        activity1.setStatus(0); // 设为0表示未开始，等待预热

        // 设置中奖概率为50%(提高概率以便测试)
        activity1.setProbability(50);

        // 保存活动到数据库
        activityMapper.insert(activity1);

        log.info("创建演示活动: {}, 开始时间: {}", activity1.getTitle(), activity1.getStartTime());
    }

    private void createDemoPrizes() {
        // 创建奖品，增加数量以便于压测
        Prize prize1 = new Prize();
        prize1.setId(1);
        prize1.setName("iPhone 14");
        prize1.setPrice(new BigDecimal("5999.00"));
        prize1.setTotalAmount(50);  // 增加数量
        prize1.setRemainingAmount(50);
        prize1.setImageUrl("https://example.com/images/iphone14.jpg");

        Prize prize2 = new Prize();
        prize2.setId(2);
        prize2.setName("AirPods");
        prize2.setPrice(new BigDecimal("1299.00"));
        prize2.setTotalAmount(100); // 增加数量
        prize2.setRemainingAmount(100);
        prize2.setImageUrl("https://example.com/images/airpods.jpg");

        Prize prize3 = new Prize();
        prize3.setId(3);
        prize3.setName("小米手环");
        prize3.setPrice(new BigDecimal("249.00"));
        prize3.setTotalAmount(200); // 增加数量
        prize3.setRemainingAmount(200);
        prize3.setImageUrl("https://example.com/images/miband.jpg");

        // 保存奖品到数据库
        prizeMapper.insert(prize1);
        prizeMapper.insert(prize2);
        prizeMapper.insert(prize3);

        // 创建活动奖品关联
        ActivityPrize activityPrize1 = new ActivityPrize();
        activityPrize1.setActivityId(1);
        activityPrize1.setPrizeId(1);
        activityPrize1.setAmount(50);

        ActivityPrize activityPrize2 = new ActivityPrize();
        activityPrize2.setActivityId(1);
        activityPrize2.setPrizeId(2);
        activityPrize2.setAmount(100);

        ActivityPrize activityPrize3 = new ActivityPrize();
        activityPrize3.setActivityId(1);
        activityPrize3.setPrizeId(3);
        activityPrize3.setAmount(200);

        // 保存活动奖品关联到数据库
        activityPrizeMapper.insert(activityPrize1);
        activityPrizeMapper.insert(activityPrize2);
        activityPrizeMapper.insert(activityPrize3);

        log.info("创建演示奖品: {}, {}, {}", prize1.getName(), prize2.getName(), prize3.getName());
    }

    private void createDemoRules() {
        // 创建活动规则
        ActivityRule rule = new ActivityRule();
        rule.setActivityId(1);
        rule.setUserLevel(0); // 0表示对所有用户等级适用
        rule.setMaxDrawsDaily(1000); // 增加次数上限以便于压测
        rule.setMaxWinsDaily(100);  // 增加中奖次数上限

        // 保存规则到数据库
        activityRuleMapper.insert(rule);

        log.info("创建演示活动规则: 每日最多抽奖{}次，最多中奖{}次", rule.getMaxDrawsDaily(), rule.getMaxWinsDaily());
    }
}