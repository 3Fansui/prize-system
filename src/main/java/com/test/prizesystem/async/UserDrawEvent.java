package com.test.prizesystem.async;

import lombok.Data;

/**
 * 用户抽奖事件
 * <p>
 * 记录用户抽奖相关的信息，用于异步处理
 * 
 * @version 1.0
 */
@Data
public class UserDrawEvent {
    /**
     * 用户ID
     */
    private Integer userId;
    
    /**
     * 活动ID
     */
    private Integer activityId;
    
    /**
     * 抽奖时间戳
     */
    private Long timestamp;
    
    /**
     * 奖品ID（中奖时有值）
     */
    private Integer prizeId;
    
    /**
     * 奖品名称（中奖时有值）
     */
    private String prizeName;
}
