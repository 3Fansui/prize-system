package com.test.prizesystem.async;

import com.test.prizesystem.util.RedBlackTreeStorage;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 事件队列管理类
 * <p>
 * 负责事件入队和消费线程管理。
 * 提供异步处理用户抽奖事件的机制，避免阻塞主业务流程。
 * 
 * @version 1.0
 */
@Slf4j
public class EventQueue {
    private final BlockingQueue2<UserDrawEvent> queue;
    private boolean running = true;

    /**
     * 构造函数
     * @param capacity 队列容量
     */
    public EventQueue(int capacity) {
        this.queue = new BlockingQueue2<>(capacity);
        log.info("创建事件队列，容量: {}", capacity);
    }

    /**
     * 启动消费者线程池
     * @param executor 线程池
     * @param treeStorage 红黑树存储
     */
    public void startConsumers(ExecutorService executor, RedBlackTreeStorage treeStorage) {
        int consumerCount = Runtime.getRuntime().availableProcessors();
        log.info("启动 {} 个事件消费者线程", consumerCount);
        
        for (int i = 0; i < consumerCount; i++) {
            final int consumerId = i + 1;
            executor.submit(() -> {
                EventConsumer consumer = new EventConsumer(treeStorage);
                log.info("事件消费者 #{} 已启动", consumerId);
                
                while (running) {
                    try {
                        UserDrawEvent event = queue.poll();
                        if (event != null) {
                            consumer.processEvent(event);
                        }
                    } catch (InterruptedException e) {
                        log.warn("事件消费者 #{} 被中断", consumerId, e);
                        Thread.currentThread().interrupt();
                        break;
                    } catch (Exception e) {
                        log.error("事件消费者 #{} 处理异常", consumerId, e);
                    }
                }
                log.info("事件消费者 #{} 已停止", consumerId);
            });
        }
    }

    /**
     * 关闭队列
     */
    public void shutdown() {
        log.info("关闭事件队列");
        this.running = false;
    }

    /**
     * 提交抽奖事件
     * @param event 抽奖事件
     * @return 是否成功提交
     */
    public boolean offer(UserDrawEvent event) {
        try {
            // 只记录中奖信息
            if (event.getPrizeId() != null) {
                log.debug("提交中奖事件: 用户ID={}, 活动ID={}, 奖品={}",
                        event.getUserId(), event.getActivityId(), event.getPrizeName());
                queue.offer(event);
                return true;
            }
            return false;
        } catch (InterruptedException e) {
            log.warn("提交中奖事件被中断", e);
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * 获取队列大小
     * @return 队列大小
     */
    public int size() {
        return queue.size();
    }
}
