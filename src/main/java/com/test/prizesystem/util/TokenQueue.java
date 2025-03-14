package com.test.prizesystem.util;

import com.test.prizesystem.model.entity.Prize;
import com.test.prizesystem.model.entity.Token;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Collectors;

/**
 * 线程安全的活动令牌双端队列管理器
 * <p>
 * 该组件为每个活动维护一个独立的ConcurrentLinkedDeque，提供高效的令牌管理。
 * 令牌按照时间戳排序存储，保证奖品按照时间顺序发放。
 * 每个令牌包含奖品ID和名称，简化抽奖过程中的数据访问。
 * 使用活动ID隔离不同活动的令牌，提高并发性能。
 * 
 * @author wu
 * @version 5.0
 */
@Slf4j
@Component
public class TokenQueue {
    
    // 使用Map存储每个活动ID对应的令牌队列
    private final Map<Integer, ConcurrentLinkedDeque<Token>> activityTokenQueues = new ConcurrentHashMap<>();
    
    /**
     * 生成令牌并添加到对应活动的队列
     * @param activityId 活动ID
     * @param prize 奖品对象
     * @param tokenTimestamp 令牌时间戳(秒级)
     * @return 生成的令牌
     */
    public Token generateToken(Integer activityId, Prize prize, long tokenTimestamp) {
        // 确保活动队列存在
        ConcurrentLinkedDeque<Token> tokenQueue = activityTokenQueues.computeIfAbsent(
                activityId, k -> new ConcurrentLinkedDeque<>());
        
        Token token = new Token();
        token.setActivityId(activityId);
        token.setPrizeId(prize.getId());
        token.setPrizeName(prize.getName());
        token.setTokenTimestamp(tokenTimestamp);
        
        // 使用优化的方式添加令牌到对应活动的队列
        addTokenInOrder(activityId, token);
        
        if (log.isDebugEnabled()) {
            log.debug("生成令牌: activityId={}, prizeId={}, prizeName={}, timestamp={}", 
                    activityId, prize.getId(), prize.getName(), tokenTimestamp);
        }
        
        return token;
    }
    
    /**
     * 使用优化的方式添加令牌到指定活动的队列
     * 注意：由于ConcurrentLinkedDeque不支持随机访问，
     * 这里的优化只是减少了比较次数，但仍然需要O(n)的时间复杂度
     * 
     * @param activityId 活动ID
     * @param token 要添加的令牌
     */
    private void addTokenInOrder(Integer activityId, Token token) {
        ConcurrentLinkedDeque<Token> tokenQueue = activityTokenQueues.get(activityId);
        
        // 如果队列为空，直接添加
        if (tokenQueue.isEmpty()) {
            tokenQueue.add(token);
            return;
        }
        
        // 检查是否可以直接添加到队首或队尾
        Token first = tokenQueue.peekFirst();
        Token last = tokenQueue.peekLast();
        
        if (first != null && token.getTokenTimestamp() <= first.getTokenTimestamp()) {
            tokenQueue.addFirst(token);
            return;
        }
        
        if (last != null && token.getTokenTimestamp() >= last.getTokenTimestamp()) {
            tokenQueue.addLast(token);
            return;
        }
        
        // 需要插入队列中间位置，将队列转为有序列表
        List<Token> sortedTokens = new ArrayList<>(tokenQueue);
        tokenQueue.clear();
        
        // 插入新令牌到适当位置
        boolean inserted = false;
        for (Token t : sortedTokens) {
            if (!inserted && token.getTokenTimestamp() < t.getTokenTimestamp()) {
                tokenQueue.add(token);
                inserted = true;
            }
            tokenQueue.add(t);
        }
        
        // 如果遍历完列表还没插入，说明应该插在最后
        if (!inserted) {
            tokenQueue.add(token);
        }
    }
    
    /**
     * 从指定活动的队列获取可用令牌（时间戳小于等于当前时间）
     * @param activityId 活动ID
     * @param currentTimestamp 当前时间戳（秒级）
     * @return 可用的令牌，如果没有则返回null
     */
    public Token getAvailableToken(Integer activityId, long currentTimestamp) {
        // 获取活动对应的令牌队列
        ConcurrentLinkedDeque<Token> tokenQueue = activityTokenQueues.get(activityId);
        if (tokenQueue == null || tokenQueue.isEmpty()) {
            return null;
        }
        
        // 从队首开始检查
        Token token = tokenQueue.peekFirst();
        if (token == null) {
            return null;
        }
        
        // 如果队首令牌的时间戳还大于当前时间，说明没有可用令牌
        if (token.getTokenTimestamp() > currentTimestamp) {
            // 重要：不符合条件的令牌不应该被消费
            log.debug("当前令牌时间戳({})大于当前时间({})，未到使用时间", 
                    token.getTokenTimestamp(), currentTimestamp);
            return null;
        }
        
        // 移除并返回队首令牌
        return tokenQueue.pollFirst();
    }
    

    
    /**
     * 清空指定活动的令牌队列
     * @param activityId 活动ID
     */
    public void clear(Integer activityId) {
        ConcurrentLinkedDeque<Token> tokenQueue = activityTokenQueues.get(activityId);
        if (tokenQueue != null) {
            tokenQueue.clear();
            log.info("活动{}的令牌队列已清空", activityId);
        }
    }
    
    /**
     * 清空所有活动的令牌队列
     */
    public void clearAll() {
        activityTokenQueues.clear();
        log.info("所有活动的令牌队列已清空");
    }
    
    /**
     * 获取指定活动的令牌队列大小
     * @param activityId 活动ID
     * @return 队列大小
     */
    public int size(Integer activityId) {
        ConcurrentLinkedDeque<Token> tokenQueue = activityTokenQueues.get(activityId);
        return tokenQueue != null ? tokenQueue.size() : 0;
    }
    
    /**
     * 获取所有活动的令牌总数
     * @return 总令牌数
     */
    public int totalSize() {
        return activityTokenQueues.values().stream()
                .mapToInt(ConcurrentLinkedDeque::size)
                .sum();
    }
    
    /**
     * 获取指定活动队列中的令牌（用于调试）
     * 返回一个副本，不影响原队列
     * @param activityId 活动ID
     * @param limit 最大返回数量
     * @return 令牌列表
     */
    public List<Token> getTokens(Integer activityId, int limit) {
        ConcurrentLinkedDeque<Token> tokenQueue = activityTokenQueues.get(activityId);
        if (tokenQueue == null) {
            return new ArrayList<>();
        }
        
        return tokenQueue.stream()
                .limit(limit)
                .collect(Collectors.toList());
    }
}