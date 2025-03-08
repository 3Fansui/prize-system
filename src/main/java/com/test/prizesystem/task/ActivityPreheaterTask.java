package com.test.prizesystem.task;

import com.test.prizesystem.model.entity.Activity;
import com.test.prizesystem.service.ActivityService;
import com.test.prizesystem.util.RedBlackTreeStorage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * 活动预热任务
 * <p>
 * 该组件负责定时扫描即将开始的活动，并进行预热处理。
 * 预热处理包括生成奖品令牌、将令牌放入双端队列以及准备其他活动资源。
 * 通过预热机制，系统能够提前准备好抽奖所需的资源，减少活动启动时的响应延迟。
 * <p>
 * 主要特点：
 * <ul>
 *   <li>定时扫描即将开始的活动</li>
 *   <li>为活动生成并排序奖品令牌</li>
 *   <li>提前为高并发抽奖做准备</li>
 * </ul>
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
    private RedBlackTreeStorage treeStorage;

    /**
     * 每分钟扫描即将开始的活动并进行预热
     * cron表达式: 0 * * * * ? 表示每分钟的第0秒执行
     */
    @Scheduled(cron = "0 * * * * ?")
    public void scanAndPreheatActivities() {
        log.info("开始扫描需要预热的活动...");

        // 由于我们使用红黑树存储，这里需要遍历所有活动查找需要预热的
        // 实际应用中会有更高效的索引方式，这里简化处理
        for (int i = 1; i <= 10; i++) {  // 假设活动ID从1到10
            Activity activity = treeStorage.find("activities", i, Activity.class);
            if (activity == null) continue;
            
            // 检查是否需要预热：状态为0(未开始)且开始时间在未来1分钟内
            if (activity.getStatus() == 0) {
                Date now = new Date();
                Date startTime = activity.getStartTime();
                
                // 如果开始时间在当前时间和未来1分钟之间
                if (startTime.after(now) && 
                        startTime.getTime() <= now.getTime() + 60 * 1000) {
                    try {
                        log.info("开始预热活动: {}", activity.getTitle());
                        activityService.preheatActivity(activity.getId());
                    } catch (Exception e) {
                        log.error("预热活动{}失败", activity.getId(), e);
                    }
                }
            }
        }

        log.info("活动预热任务完成");
    }
}