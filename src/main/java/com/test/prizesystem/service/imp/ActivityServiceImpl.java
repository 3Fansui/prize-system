package com.test.prizesystem.service.imp;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import com.test.prizesystem.mapper.ActivityMapper;
import com.test.prizesystem.mapper.ActivityPrizeMapper;
import com.test.prizesystem.mapper.ActivityRuleMapper;
import com.test.prizesystem.mapper.PrizeMapper;
import com.test.prizesystem.model.entity.Activity;
import com.test.prizesystem.model.entity.ActivityPrize;
import com.test.prizesystem.model.entity.ActivityRule;
import com.test.prizesystem.model.entity.Prize;
import com.test.prizesystem.service.ActivityService;
import com.test.prizesystem.service.TokenService;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Slf4j
@Service
public class ActivityServiceImpl implements ActivityService {

    @Autowired
    private ActivityMapper activityMapper;

    @Autowired
    private ActivityRuleMapper activityRuleMapper;

    @Autowired
    private ActivityPrizeMapper activityPrizeMapper;

    @Autowired
    private TokenService tokenService;

    @Autowired
    PrizeMapper prizeMapper;

    @Override
    public Activity getActivity(Integer activityId) {
        return activityMapper.selectById(activityId);
    }

    @Override
    public ActivityRule getActivityRule(Integer activityId, Integer userLevel) {
        LambdaQueryWrapper<ActivityRule> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ActivityRule::getActivityId, activityId)
                .eq(ActivityRule::getUserLevel, userLevel);
        ActivityRule rule = activityRuleMapper.selectOne(queryWrapper);

        if (rule == null) {
            // 如果没有找到对应级别的规则，查找默认规则（userLevel = 0）
            queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(ActivityRule::getActivityId, activityId)
                    .eq(ActivityRule::getUserLevel, 0);
            rule = activityRuleMapper.selectOne(queryWrapper);
        }

        return rule;
    }

    @Override
    public void preheatActivity(Integer activityId) {
        // 获取活动信息
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            log.error("活动{}不存在，无法预热", activityId);
            return;
        }

        // 获取活动关联的奖品
        LambdaQueryWrapper<ActivityPrize> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ActivityPrize::getActivityId, activityId);
        List<ActivityPrize> activityPrizes = activityPrizeMapper.selectList(queryWrapper);

        // 根据活动类型生成令牌
        if (activity.getType() == 2 || activity.getType() == 3) {
            tokenService.generateTokens(activity, activityPrizes);
        }

        // 更新活动状态为已预热
        activity.setStatus(1);
        activityMapper.updateById(activity);

        log.info("活动{}预热完成", activityId);
    }



    private void createDemoActivities() {
        // 创建概率型抽奖活动（类型1）
        Activity activity1 = new Activity();
        activity1.setId(1);
        activity1.setTitle("概率抽奖活动");

        // 当前时间
        Date now = new Date();

        // 设置活动开始时间为现在
        activity1.setStartTime(now);

        // 设置活动结束时间为7天后
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(now);
        calendar.add(Calendar.DAY_OF_MONTH, 7);
        activity1.setEndTime(calendar.getTime());

        // 设置活动类型为概率型
        activity1.setType(1);

        // 设置活动状态为未开始
        activity1.setStatus(0);

        // 设置中奖概率为30%
        activity1.setProbability(30);

        // 保存活动到数据库
        activityMapper.insert(activity1);

        log.info("创建演示活动: {}", activity1.getTitle());
    }

    private void createDemoPrizes() {
        // 创建奖品
        Prize prize1 = new Prize();
        prize1.setId(1);
        prize1.setName("iPhone 14");
        prize1.setPrice(new BigDecimal("5999.00"));
        prize1.setTotalAmount(5);
        prize1.setRemainingAmount(5);
        prize1.setImageUrl("https://example.com/images/iphone14.jpg");

        Prize prize2 = new Prize();
        prize2.setId(2);
        prize2.setName("AirPods");
        prize2.setPrice(new BigDecimal("1299.00"));
        prize2.setTotalAmount(10);
        prize2.setRemainingAmount(10);
        prize2.setImageUrl("https://example.com/images/airpods.jpg");

        Prize prize3 = new Prize();
        prize3.setId(3);
        prize3.setName("小米手环");
        prize3.setPrice(new BigDecimal("249.00"));
        prize3.setTotalAmount(20);
        prize3.setRemainingAmount(20);
        prize3.setImageUrl("https://example.com/images/miband.jpg");

        // 保存奖品到数据库
        prizeMapper.insert(prize1);
        prizeMapper.insert(prize2);
        prizeMapper.insert(prize3);

        // 创建活动奖品关联
        ActivityPrize activityPrize1 = new ActivityPrize();
        activityPrize1.setActivityId(1);
        activityPrize1.setPrizeId(1);
        activityPrize1.setAmount(5);

        ActivityPrize activityPrize2 = new ActivityPrize();
        activityPrize2.setActivityId(1);
        activityPrize2.setPrizeId(2);
        activityPrize2.setAmount(10);

        ActivityPrize activityPrize3 = new ActivityPrize();
        activityPrize3.setActivityId(1);
        activityPrize3.setPrizeId(3);
        activityPrize3.setAmount(20);

        // 保存活动奖品关联到数据库
        activityPrizeMapper.insert(activityPrize1);
        activityPrizeMapper.insert(activityPrize2);
        activityPrizeMapper.insert(activityPrize3);

        log.info("创建演示奖品: {}, {}, {}", prize1.getName(), prize2.getName(), prize3.getName());
    }

    private void createDemoRules() {
        // 创建活动规则（对所有用户等级适用）
        ActivityRule rule = new ActivityRule();
        rule.setActivityId(1);
        rule.setUserLevel(0); // 0表示对所有用户等级适用
        rule.setMaxDrawsDaily(10); // 每天最多抽奖10次
        rule.setMaxWinsDaily(3);  // 每天最多中奖3次

        // 保存规则到数据库
        activityRuleMapper.insert(rule);

        log.info("创建演示活动规则: 每日最多抽奖{}次，最多中奖{}次", rule.getMaxDrawsDaily(), rule.getMaxWinsDaily());
    }
}
