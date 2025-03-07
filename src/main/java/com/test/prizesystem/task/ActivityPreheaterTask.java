package com.test.prizesystem.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.test.prizesystem.mapper.ActivityMapper;
import com.test.prizesystem.model.entity.Activity;
import com.test.prizesystem.service.ActivityService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

@Slf4j
@Component
public class ActivityPreheaterTask {

    @Autowired
    private ActivityService activityService;

    @Autowired
    private ActivityMapper activityMapper;

    /**
     * 每分钟扫描即将开始的活动并进行预热
     * cron表达式: 0 * * * * ? 表示每分钟的第0秒执行
     */
    @Scheduled(cron = "0 * * * * ?")
    public void scanAndPreheatActivities() {
        log.info("开始扫描需要预热的活动...");

        // 查询未来1分钟内要开始的活动（状态为0=未开始的活动）
        LambdaQueryWrapper<Activity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Activity::getStatus, 0)
                .ge(Activity::getStartTime, new Date()) // 开始时间 >= 当前时间
                .le(Activity::getStartTime, new Date(System.currentTimeMillis() + 60 * 1000)); // 开始时间 <= 当前时间+1分钟

        List<Activity> activities = activityMapper.selectList(queryWrapper);

        if (activities.isEmpty()) {
            log.info("没有需要预热的活动");
            return;
        }

        log.info("找到{}个需要预热的活动", activities.size());

        // 预热每个活动
        for (Activity activity : activities) {
            try {
                log.info("开始预热活动: {}", activity.getTitle());
                activityService.preheatActivity(activity.getId());
            } catch (Exception e) {
                log.error("预热活动{}失败", activity.getId(), e);
            }
        }

        log.info("活动预热任务完成");
    }
}
