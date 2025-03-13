package com.test.prizesystem.async;

import com.test.prizesystem.model.entity.UserDrawRecord;
import com.test.prizesystem.util.RedBlackTreeStorage;
import com.test.prizesystem.util.TreeNames;
import com.test.prizesystem.util.PersistenceManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import javax.annotation.PostConstruct;

import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 事件队列管理类
 * <p>
 * 使用BlockingQueue2实现单消费者模式，
 * 保证只有一个线程修改红黑树，类似Redis的单线程模型。
 * 将队列与处理逻辑整合，避免多余的组件。
 * 
 * @version 5.0
 */
@Slf4j
@Component
public class EventQueue implements DisposableBean {
    // 使用自定义的双锁阻塞队列
    private final BlockingQueue2<UserDrawEvent> queue;
    private volatile boolean running = true;
    private Thread consumerThread;
    
    // 用于生成记录ID
    private static final AtomicLong idGenerator = new AtomicLong(1);
    
    @Autowired
    private RedBlackTreeStorage treeStorage;
    
    @Autowired
    private PersistenceManager persistenceManager;

    /**
     * 构造函数
     */
    public EventQueue() {
        // 创建足够大的队列容量，避免阻塞生产者
        int capacity = 10000;
        this.queue = new BlockingQueue2<>(capacity);
        log.info("创建双锁阻塞事件队列，容量: {}", capacity);
    }
    
    /**
     * 初始化并启动事件处理线程
     */
    @PostConstruct
    public void init() {
        log.info("启动单一事件消费者线程");
        
        consumerThread = new Thread(() -> {
            log.info("事件消费者线程已启动");
            
            while (running) {
                try {
                    // 使用poll()方法阻塞式获取事件
                    UserDrawEvent event = queue.poll();
                    // 检查运行状态，避免在关闭过程中处理事件
                    if (running && event != null) {
                        processEvent(event);
                    }
                } catch (InterruptedException e) {
                    // 中断通常是关闭信号，不需要输出警告日志
                    if (running) {
                        log.warn("事件消费者线程被意外中断", e);
                    } else {
                        log.info("事件消费者线程收到关闭信号");
                    }
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    log.error("事件消费者处理异常", e);
                }
            }
            log.info("事件消费者线程已停止");
        });
        
        consumerThread.setName("event-consumer");
        consumerThread.setDaemon(true);
        consumerThread.start();
    }
    
    /**
     * 处理抽奖事件
     * @param event 抽奖事件
     */
    private void processEvent(UserDrawEvent event) {
        // 只记录中奖信息
        if (event != null && event.getPrizeId() != null) {
            try {
                // 创建用户中奖记录
                UserDrawRecord record = new UserDrawRecord();
                record.setId(idGenerator.getAndIncrement());
                record.setUserId(event.getUserId());
                record.setActivityId(event.getActivityId());
                record.setPrizeId(event.getPrizeId());
                record.setPrizeName(event.getPrizeName());
                record.setWinTime(new Date(event.getTimestamp()));
                
                log.info("开始保存用户中奖记录: 用户ID={}, 奖品={}, 记录ID={}",
                        event.getUserId(), event.getPrizeName(), record.getId());
                
                // 使用红黑树存储中奖记录
                treeStorage.save(TreeNames.USER_DRAW_RECORDS, record.getId(), record);
                
                // 移除标记数据变更的调用，改为依靠定时和系统关闭时持久化
                // 不再调用: persistenceManager.markDataChanged();
                
                // 验证记录是否成功保存
                UserDrawRecord savedRecord = treeStorage.find(TreeNames.USER_DRAW_RECORDS, record.getId(), UserDrawRecord.class);
                if (savedRecord != null) {
                    log.info("验证成功 - 已保存中奖记录: 用户ID={}, 奖品={}, 记录ID={}",
                            event.getUserId(), event.getPrizeName(), record.getId());
                } else {
                    log.error("验证失败 - 无法查询到已保存的记录: 用户ID={}, 奖品={}, 记录ID={}",
                            event.getUserId(), event.getPrizeName(), record.getId());
                }
            } catch (Exception e) {
                log.error("保存用户中奖记录失败", e);
            }
        }
    }

    /**
     * 提交抽奖事件
     * @param event 抽奖事件
     * @return 是否成功提交
     */
    public boolean offer(UserDrawEvent event) {
        if (event == null) {
            return false;
        }
        
        try {
            // 只处理中奖信息
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
    
    /**
     * Spring容器销毁时调用，实现DisposableBean接口
     */
    @Override
    public void destroy() throws Exception {
        log.info("应用程序关闭，正在处理队列中剩余事件...");
        
        // 检查队列中是否有未处理的事件
        int remainingEvents = queue.size();
        if (remainingEvents > 0) {
            log.info("队列中还有 {} 个事件未处理，等待处理完成...", remainingEvents);
            
            // 在关闭前等待一段时间，处理一些可能的事件
            try {
                Thread.sleep(Math.min(remainingEvents * 10, 500)); // 最多等待500毫秒
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        // 标记关闭状态
        this.running = false;
        
        // 在关闭前标记数据变更，确保持久化
        persistenceManager.markDataChanged();
        persistenceManager.forceSyncNow();
        
        // 中断消费者线程
        if (consumerThread != null) {
            consumerThread.interrupt();
            
            // 等待消费者线程完全结束，最多等待两秒
            try {
                consumerThread.join(2000);
                log.info("事件消费者线程成功关闭");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("等待事件消费者线程结束时被中断");
            }
        }
        
        log.info("事件队列已完全关闭");
    }
}