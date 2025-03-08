package com.test.prizesystem.service.imp;

import com.test.prizesystem.model.entity.UserPrizeRecord;
import com.test.prizesystem.service.UserPrizeRecordService;
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
public class UserPrizeRecordServiceImpl implements UserPrizeRecordService {

    @Autowired
    private RedBlackTreeStorage treeStorage;
    
    // 使用内存缓存记录用户当日中奖次数
    private final Map<String, AtomicInteger> userWinCountMap = new ConcurrentHashMap<>();
    
    // 上次清理缓存的日期
    private volatile long lastClearDate = 0;
    
    // 树名称
    private static final String TREE_NAME = "user_prize_records";

    @Override
    public int getUserWinCountToday(Integer userId, Integer activityId) {
        // 检查是否需要清理缓存（每天零点清理）
        checkAndClearCache();
        
        // 生成缓存key：userId_activityId_date
        String cacheKey = getUserDateKey(userId, activityId);
        
        // 从缓存获取次数
        AtomicInteger count = userWinCountMap.get(cacheKey);
        if (count != null) {
            return count.get();
        }
        
        // 缓存中没有，返回0（因为已经转为内存数据，第一次访问当然是0）
        userWinCountMap.put(cacheKey, new AtomicInteger(0));
        return 0;
    }

    @Override
    public boolean addUserPrizeRecord(UserPrizeRecord record) {
        try {
            // 检查记录是否有效
            if (record == null || record.getUserId() == null || record.getActivityId() == null || record.getPrizeId() == null) {
                return false;
            }
            
            // 设置中奖时间
            if (record.getWinTime() == null) {
                record.setWinTime(new Date());
            }
            
            // 生成key：userId_timestamp
            long key = record.getUserId() * 1000000L + record.getWinTime().getTime() % 1000000L;
            
            // 保存到红黑树
            treeStorage.save(TREE_NAME, key, record);
            
            // 更新缓存中的中奖次数
            String cacheKey = getUserDateKey(record.getUserId(), record.getActivityId());
            userWinCountMap.computeIfAbsent(cacheKey, k -> new AtomicInteger(0)).incrementAndGet();
            
            log.info("用户 {} 在活动 {} 中奖记录已保存，奖品ID: {}", 
                    record.getUserId(), record.getActivityId(), record.getPrizeId());
            
            return true;
        } catch (Exception e) {
            log.error("添加用户中奖记录失败", e);
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
                    userWinCountMap.clear();
                    lastClearDate = currentDate;
                    log.info("已清理用户中奖次数缓存");
                }
            }
        }
    }
}