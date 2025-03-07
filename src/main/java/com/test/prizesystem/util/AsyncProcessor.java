package com.test.prizesystem.util;


import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

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
