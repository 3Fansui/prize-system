package com.test.prizesystem.util;

import com.test.prizesystem.model.entity.UserPrizeRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 中奖记录处理器
 * <p>
 * 该组件使用阻塞队列模拟MQ，异步将中奖记录写入红黑树存储引擎。
 * 它是系统中将消息队列与存储引擎连接的关键桥梁，避免了在抽奖过程中的同步存储操作。
 * <p>
 * 主要特点：
 * <ul>
 *   <li>使用标准Java BlockingQueue实现生产者-消费者模式</li>
 *   <li>异步处理中奖记录，不阻塞抽奖主流程</li>
 *   <li>将中奖记录保存到红黑树存储引擎，模拟MySQL存储</li>
 * </ul>
 * 
 * @author MCP生成
 * @version 1.0
 */
@Slf4j
@Component
public class WinRecordProcessor {
    
    @Autowired
    private RedBlackTreeStorage treeStorage;
    
    // 使用阻塞队列存储待处理的中奖记录
    private final BlockingQueue<UserPrizeRecord> recordQueue = new LinkedBlockingQueue<>();
    
    // 记录存储的红黑树名称
    private static final String TREE_NAME = "user_prize_records";
    
    @PostConstruct
    public void init() {
        // 启动工作线程处理中奖记录
        Thread worker = new Thread(() -> {
            log.info("中奖记录处理线程启动");
            while (true) {
                try {
                    // 从队列中获取中奖记录
                    UserPrizeRecord record = recordQueue.take();
                    processRecord(record);
                } catch (InterruptedException e) {
                    log.error("中奖记录处理线程被中断", e);
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    log.error("处理中奖记录时出错", e);
                }
            }
        });
        worker.setDaemon(true);
        worker.start();
    }
    
    /**
     * 提交中奖记录到处理队列
     */
    public void submitRecord(UserPrizeRecord record) {
        try {
            recordQueue.put(record);
            log.debug("中奖记录已提交到处理队列: userId={}, prizeId={}",
                    record.getUserId(), record.getPrizeId());
        } catch (InterruptedException e) {
            log.error("提交中奖记录被中断", e);
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * 处理中奖记录
     */
    private void processRecord(UserPrizeRecord record) {
        try {
            // 使用用户ID和时间戳组合作为键
            long key = record.getUserId() * 1000000L + record.getWinTime().getTime() % 1000000L;
            
            // 将记录保存到红黑树
            treeStorage.save(TREE_NAME, key, record);
            
            log.info("中奖记录已保存到红黑树: userId={}, prizeId={}, key={}",
                    record.getUserId(), record.getPrizeId(), key);
        } catch (Exception e) {
            log.error("保存中奖记录到红黑树失败", e);
        }
    }
    
    /**
     * 获取队列中待处理记录数量
     */
    public int getPendingCount() {
        return recordQueue.size();
    }
}