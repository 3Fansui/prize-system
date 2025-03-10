package com.test.prizesystem.model.entity;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import java.util.Date;

/**
 * 令牌实体类
 * <p>
 * 令牌用于抽奖活动中控制奖品发放的时间和顺序。
 * 每个令牌代表一个实际的奖品，并直接包含基本奖品信息。
 * 
 * @author MCP生成
 * @version 3.0
 */
@Data
@ApiModel(value = "令牌信息", description = "控制奖品发放的令牌")
public class Token {
    @ApiModelProperty(value = "令牌ID", example = "1000", position = 1)
    private Long id;
    
    @ApiModelProperty(value = "活动ID", example = "1", position = 2)
    private Integer activityId;
    
    @ApiModelProperty(value = "奖品ID", example = "2", position = 3)
    private Integer prizeId;
    
    @ApiModelProperty(value = "奖品名称", example = "iPhone 14", position = 4)
    private String prizeName;
    
    @ApiModelProperty(value = "令牌时间戳", example = "1615456800", notes = "指定令牌使用的时间（秒级时间戳）", position = 5)
    private Long tokenTimestamp;
    
    @ApiModelProperty(value = "创建时间", example = "2025-01-01 00:00:00", position = 6)
    private Date createTime;
}