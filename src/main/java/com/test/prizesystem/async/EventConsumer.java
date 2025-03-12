package com.test.prizesystem.async;

import com.test.prizesystem.model.entity.UserDrawRecord;
import com.test.prizesystem.util.RedBlackTreeStorage;
import com.test.prizesystem.util.TreeNames;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 事件消费者
 * <p>
 * 负责从队列中取出事件并更新到红黑树
 * 
 * @version 2.0
 */
@Slf4j
public class EventConsumer {
    private final RedBlackTreeStorage treeStorage;
    private static final AtomicLong idGenerator = new AtomicLong(1);

    /**
     * 构造函数
     * @param treeStorage 红黑树存储
     */
    public EventConsumer(RedBlackTreeStorage treeStorage) {
        this.treeStorage = treeStorage;
    }

    /**
     * 处理抽奖事件
     * @param event 抽奖事件
     */
    public void processEvent(UserDrawEvent event) {
        // 只记录中奖信息，非中奖记录已在EventQueue的offer方法过滤
        if (event.getPrizeId() != null) {
            try {
                // 创建用户中奖记录
                UserDrawRecord record = new UserDrawRecord();
                record.setId(idGenerator.getAndIncrement());
                record.setUserId(event.getUserId());
                record.setActivityId(event.getActivityId());
                record.setPrizeId(event.getPrizeId());
                record.setPrizeName(event.getPrizeName());
                record.setWinTime(new Date(event.getTimestamp()));
                
                // 保存到用户中奖记录树（标准树）
                treeStorage.save(TreeNames.USER_DRAW_RECORDS, record.getId(), record);
                
                log.debug("记录用户中奖: 用户ID={}, 活动ID={}, 奖品={}, 记录ID={}",
                        event.getUserId(), event.getActivityId(), event.getPrizeName(), record.getId());
            } catch (Exception e) {
                log.error("保存用户中奖记录失败", e);
            }
        }
    }
}
