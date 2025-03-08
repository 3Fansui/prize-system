package com.test.prizesystem.util;


import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * 异步任务处理器
 * <p>
 * 该组件使用阻塞队列模拟MQ，提供异步任务处理能力。
 * 它启动一个后台线程不断从队列中获取并执行任务，实现生产者-消费者模式。
 * 主要用于处理那些不需要立即响应给用户的后台任务，如数据库更新、日志记录等。
 * <p>
 * 主要特点：
 * <ul>
 *   <li>使用阻塞队列实现生产者-消费者模式</li>
 *   <li>提供异步任务处理，减轻主线程压力</li>
 *   <li>线程安全的任务队列管理</li>
 * </ul>
 * 
 * @author MCP生成
 * @version 1.0
 */
@Slf4j
@Component
public class AsyncProcessor {

    private BlockingQueue2<Runnable> taskQueue = new BlockingQueue2<>(1000);

    @PostConstruct
    public void init() {
        // 启动工作线程处理任务
        Thread worker = new Thread(() -> {
            log.info("AsyncProcessor工作线程启动");
            while (true) {
                try {
                    log.debug("等待任务...");
                    Runnable task = taskQueue.poll();
                    log.info("从阻塞队列获取到任务，开始执行");
                    task.run();
                    log.info("任务执行完成");
                } catch (InterruptedException e) {
                    log.error("AsyncProcessor被中断", e);
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    log.error("AsyncProcessor执行任务异常", e);
                }
            }
        });
        worker.setDaemon(true);
        worker.start();
        log.info("AsyncProcessor初始化完成");
    }

    /**
     * 提交任务
     */
    public void submitTask(Runnable task) {
        try {
            log.info("提交任务到阻塞队列");
            taskQueue.offer(task);
            log.info("任务已提交到阻塞队列");
        } catch (InterruptedException e) {
            log.error("提交任务被中断", e);
            Thread.currentThread().interrupt();
        }
    }
}
