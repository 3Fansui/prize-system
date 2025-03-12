package com.test.prizesystem.task;

import com.test.prizesystem.model.entity.Activity;
import com.test.prizesystem.service.ActivityService;
import com.test.prizesystem.util.PersistenceManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Calendar;
import java.util.TimeZone;

/**
 * 活动预热任务
 * <p>
 * 该组件负责定时扫描即将开始的活动，并进行预热处理。
 * 预热处理包括生成奖品令牌、将令牌放入双端队列以及准备其他活动资源。
 * 通过预热机制，系统能够提前准备好抽奖所需的资源，减少活动启动时的响应延迟。
 * 
 * @author MCP生成
 * @version 1.0
 */
@Slf4j
@Component
public class ActivityPreheaterTask {

    @Autowired
    private ActivityService activityService;
    
    @Autowired
    private PersistenceManager persistenceManager;

    /**
     * 每分钟扫描即将开始的活动并进行预热
     * cron表达式: 0 * * * * ? 表示每分钟的第0秒执行
     */
    @Scheduled(cron = "0 * * * * ?")
    public void scanAndPreheatActivities() {
        log.info("开始扫描需要预热的活动...");

        // 查找需要预热的活动
        for (Activity activity : activityService.findActivitiesNeedingPreheat()) {
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