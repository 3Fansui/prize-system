package com.test.prizesystem.model.entity;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 令牌实体类
 * <p>
 * 令牌用于抽奖活动中控制奖品发放的时间和顺序。
 * 每个令牌代表一个实际的奖品，并直接包含基本奖品信息。
 * 简化版本：移除了不必要的id和创建时间字段。
 * 
 * @author MCP生成
 * @version 4.0
 */
@Data
@ApiModel(value = "令牌信息", description = "控制奖品发放的令牌")
public class Token {
    @ApiModelProperty(value = "活动ID", example = "1", position = 1)
    private Integer activityId;
    
    @ApiModelProperty(value = "奖品ID", example = "2", position = 2)
    private Integer prizeId;
    
    @ApiModelProperty(value = "奖品名称", example = "iPhone 14", position = 3)
    private String prizeName;
    
    @ApiModelProperty(value = "令牌时间戳", example = "1615456800", notes = "指定令牌使用的时间（秒级时间戳）", position = 4)
    private Long tokenTimestamp;
}