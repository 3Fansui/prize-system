package com.test.prizesystem.model.entity;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import java.util.Date;

/**
 * 令牌实体类
 * <p>
 * 令牌用于抽奖活动中控制奖品发放的时间和顺序。
 * 每个令牌代表一次奖品的获取机会，并包含了与特定活动和奖品的关联。
 * 令牌使用时间戳来控制其可用性，确保奖品按照预定时间分发。
 * 
 * @author MCP生成
 * @version 1.0
 */
@Data
@ApiModel(value = "令牌信息", description = "控制奖品发放的令牌")
public class Token {
    @ApiModelProperty(value = "令牌ID", example = "1", position = 1)
    private Long id;
    
    @ApiModelProperty(value = "活动ID", example = "1", position = 2)
    private Integer activityId;
    
    @ApiModelProperty(value = "奖品ID", example = "2", position = 3)
    private Integer prizeId;
    
    @ApiModelProperty(value = "令牌时间戳", example = "1615456800000", notes = "指定令牌使用的时间", position = 4)
    private Long tokenTimestamp;
    
    @ApiModelProperty(value = "状态", example = "0", notes = "0=未使用，1=已使用", position = 5)
    private Integer status;
    
    @ApiModelProperty(value = "创建时间", position = 6)
    private Date createTime;
    
    @ApiModelProperty(value = "更新时间", position = 7)
    private Date updateTime;
}