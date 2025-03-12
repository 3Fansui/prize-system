package com.test.prizesystem.config;

import com.test.prizesystem.model.entity.Activity;
import com.test.prizesystem.service.ActivityService;
import com.test.prizesystem.util.PersistenceManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 系统启动处理器
 * <p>
 * 负责在系统启动完成后执行一些初始化操作，比如重新预热活动。
 * 主要处理从持久化文件恢复数据后的状态初始化工作。
 * </p>
 *
 * @author MCP
 */
@Slf4j
@Component
public class SystemStartupHandler {
    
    @Autowired
    private ActivityService activityService;
    
    @Autowired
    private PersistenceManager persistenceManager;
    
    /**
     * 在应用程序启动完成后执行
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("系统启动完成，开始检查需要预热的活动...");
        
        // 必须确保持久化数据已加载
        if (!persistenceManager.isInitialized()) {
            log.warn("持久化数据尚未初始化，跳过活动预热检查");
            return;
        }
        
        // 查找需要预热的活动
        List<Activity> activitiesToPreheat = activityService.findActivitiesNeedingPreheat();
        if (activitiesToPreheat.isEmpty()) {
            log.info("没有找到需要预热的活动");
            return;
        }
        
        log.info("找到{}个需要预热的活动", activitiesToPreheat.size());
        for (Activity activity : activitiesToPreheat) {
            try {
                log.info("系统启动后预热活动: {}", activity.getTitle());
                activityService.preheatActivity(activity.getId());
            } catch (Exception e) {
                log.error("系统启动后预热活动{}失败", activity.getId(), e);
            }
        }
    }
}