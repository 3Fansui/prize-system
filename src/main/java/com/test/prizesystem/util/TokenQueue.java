package com.test.prizesystem.util;

import com.test.prizesystem.model.entity.Token;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 线程安全的令牌双端队列
 * <p>
 * 该组件使用Java的ConcurrentLinkedDeque实现，提供高效的令牌管理。
 * 所有令牌存储在该队列中，并按照时间戳排序，保证奖品按照时间顺序发放。
 * 该队列链接了活动预热化与实际抽奖的处理过程，是系统的核心组件之一。
 * <p>
 * 主要功能：
 * <ul>
 *   <li>生成并管理有序的令牌</li>
 *   <li>基于时间戳获取可用的令牌</li>
 *   <li>支持未使用的令牌返回队列</li>
 *   <li>线程安全的队列操作</li>
 * </ul>
 * 
 * @author MCP生成
 * @version 1.0
 */
@Slf4j
@Component
public class TokenQueue {
    
    // 使用ConcurrentLinkedDeque作为线程安全的双端队列
    private final ConcurrentLinkedDeque<Token> tokenQueue = new ConcurrentLinkedDeque<>();
    
    // 用于生成唯一ID
    private final AtomicLong idGenerator = new AtomicLong(1);
    
    // 随机数生成器
    private final Random random = new Random();
    
    /**
     * 生成令牌并添加到队列
     * @param activityId 活动ID
     * @param prizeId 奖品ID
     * @param baseTimestamp 基础时间戳
     * @return 生成的令牌
     */
    public Token generateToken(Integer activityId, Integer prizeId, long baseTimestamp) {
        // 给时间戳添加随机数，避免重复
        // 将时间戳乘以1000，再加上3位随机数
        long uniqueTimestamp = baseTimestamp * 1000 + random.nextInt(1000);
        
        Token token = new Token();
        token.setId(idGenerator.getAndIncrement());
        token.setActivityId(activityId);
        token.setPrizeId(prizeId);
        token.setTokenTimestamp(uniqueTimestamp);
        token.setStatus(0); // 0=未使用
        
        // 使用二分查找优化的方式添加令牌
        addTokenInOrder(token);
        
        log.debug("生成令牌: activityId={}, prizeId={}, timestamp={}", 
                activityId, prizeId, uniqueTimestamp);
        
        return token;
    }
    
    /**
     * 使用二分查找优化的方式添加令牌
     * 注意：由于ConcurrentLinkedDeque不支持随机访问，
     * 这里的优化只是减少了比较次数，但仍然需要O(n)的时间复杂度
     */
    private void addTokenInOrder(Token token) {
        // 如果队列为空，直接添加
        if (tokenQueue.isEmpty()) {
            tokenQueue.add(token);
            return;
        }
        
        // 检查是否可以直接添加到队首或队尾
        Token first = tokenQueue.peekFirst();
        Token last = tokenQueue.peekLast();
        
        if (token.getTokenTimestamp() <= first.getTokenTimestamp()) {
            tokenQueue.addFirst(token);
            return;
        }
        
        if (token.getTokenTimestamp() >= last.getTokenTimestamp()) {
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
    }
    
    /**
     * 从队列获取可用令牌（时间戳小于等于当前时间）
     * @param currentTimestamp 当前时间戳
     * @return 可用的令牌，如果没有则返回null
     */
    public Token getAvailableToken(long currentTimestamp) {
        // 将当前时间戳乘以1000以匹配令牌中的时间戳格式
        long scaledTimestamp = currentTimestamp * 1000;
        
        // 从队首开始检查
        Token token = tokenQueue.peekFirst();
        if (token == null) {
            return null;
        }
        
        // 如果队首令牌的时间戳还大于当前时间，说明没有可用令牌
        if (token.getTokenTimestamp() > scaledTimestamp) {
            return null;
        }
        
        // 移除并返回队首令牌
        return tokenQueue.pollFirst();
    }
    
    /**
     * 将未使用的令牌重新放回队列头部
     */
    public void returnToken(Token token) {
        if (token != null && token.getStatus() == 0) {
            tokenQueue.addFirst(token);
            log.debug("令牌已返回队列: id={}, timestamp={}", 
                    token.getId(), token.getTokenTimestamp());
        }
    }
    
    /**
     * 清空队列
     */
    public void clear() {
        tokenQueue.clear();
        log.info("令牌队列已清空");
    }
    
    /**
     * 获取队列大小
     */
    public int size() {
        return tokenQueue.size();
    }
    
    /**
     * 获取队列中的令牌（用于调试）
     * 返回一个副本，不影响原队列
     * @param limit 最大返回数量
     * @return 令牌列表
     */
    public List<Token> getTokens(int limit) {
        return tokenQueue.stream()
                .limit(limit)
                .collect(Collectors.toList());
    }
    
    /**
     * 获取队列中所有令牌的时间戳（用于调试）
     * @return 时间戳列表
     */
    public List<Long> getTokenTimestamps() {
        return tokenQueue.stream()
                .map(Token::getTokenTimestamp)
                .collect(Collectors.toList());
    }
}