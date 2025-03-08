package com.test.prizesystem.service.imp;

import com.test.prizesystem.model.entity.UserDrawRecord;
import com.test.prizesystem.service.UserDrawRecordService;
import com.test.prizesystem.util.RedBlackTreeStorage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
public class UserDrawRecordServiceImpl implements UserDrawRecordService {

    @Autowired
    private RedBlackTreeStorage treeStorage;
    
    // 使用内存缓存记录用户当日抽奖次数
    private final Map<String, AtomicInteger> userDrawCountMap = new ConcurrentHashMap<>();
    
    // 上次清理缓存的日期
    private volatile long lastClearDate = 0;
    
    // 树名称
    private static final String TREE_NAME = "user_draw_records";

    @Override
    public int getUserDrawCountToday(Integer userId, Integer activityId) {
        // 检查是否需要清理缓存（每天零点清理）
        checkAndClearCache();
        
        // 生成缓存key：userId_activityId_date
        String cacheKey = getUserDateKey(userId, activityId);
        
        // 从缓存获取次数
        AtomicInteger count = userDrawCountMap.get(cacheKey);
        if (count != null) {
            return count.get();
        }
        
        // 缓存中没有，返回0（因为已经转为内存数据，第一次访问当然是0）
        userDrawCountMap.put(cacheKey, new AtomicInteger(0));
        return 0;
    }

    @Override
    public boolean addUserDrawRecord(UserDrawRecord record) {
        try {
            // 检查记录是否有效
            if (record == null || record.getUserId() == null || record.getActivityId() == null) {
                return false;
            }
            
            // 设置抽奖时间
            if (record.getDrawTime() == null) {
                record.setDrawTime(new Date());
            }
            
            // 生成key：userId_timestamp
            long key = record.getUserId() * 1000000L + record.getDrawTime().getTime() % 1000000L;
            
            // 保存到红黑树
            treeStorage.save(TREE_NAME, key, record);
            
            // 更新缓存中的抽奖次数
            String cacheKey = getUserDateKey(record.getUserId(), record.getActivityId());
            userDrawCountMap.computeIfAbsent(cacheKey, k -> new AtomicInteger(0)).incrementAndGet();
            
            return true;
        } catch (Exception e) {
            log.error("添加用户抽奖记录失败", e);
            return false;
        }
    }
    
    /**
     * 生成用户日期缓存key
     */
    private String getUserDateKey(Integer userId, Integer activityId) {
        // 获取当前日期（不包含时间）
        long currentDate = System.currentTimeMillis() / (24 * 60 * 60 * 1000);
        return userId + "_" + activityId + "_" + currentDate;
    }
    
    /**
     * 检查是否需要清理缓存
     */
    private void checkAndClearCache() {
        // 获取当前日期
        long currentDate = System.currentTimeMillis() / (24 * 60 * 60 * 1000);
        
        // 如果日期变化，清理缓存
        if (currentDate > lastClearDate) {
            synchronized (this) {
                if (currentDate > lastClearDate) {
                    userDrawCountMap.clear();
                    lastClearDate = currentDate;
                    log.info("已清理用户抽奖次数缓存");
                }
            }
        }
    }
}