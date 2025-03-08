package com.test.prizesystem.model.entity;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import java.util.Date;

/**
 * 活动实体类
 * <p>
 * 表示抽奖系统中的活动信息，包含活动的名称、时间范围、状态等。
 * 活动是抽奖系统的核心元素，用户及奖品都会关联到特定活动。
 * 
 * @author MCP生成
 * @version 1.0
 */
@Data
@ApiModel(value = "活动信息", description = "抽奖活动的详细信息")
public class Activity {
    @ApiModelProperty(value = "活动ID", example = "1", position = 1)
    private Integer id;
    
    @ApiModelProperty(value = "活动标题", example = "双11抽奖活动", position = 2)
    private String title;
    
    @ApiModelProperty(value = "开始时间", position = 3)
    private Date startTime;
    
    @ApiModelProperty(value = "结束时间", position = 4)
    private Date endTime;
    
    @ApiModelProperty(value = "活动类型", example = "1", notes = "1=固定时间型，2=概率型", position = 5)
    private Integer type;
    
    @ApiModelProperty(value = "活动状态", example = "1", notes = "0=未开始，1=进行中，2=已结束", position = 6)
    private Integer status;
    
    @ApiModelProperty(value = "中奖概率基数", example = "10000", notes = "只在概率型活动中使用", position = 7)
    private Integer probability;
    
    @ApiModelProperty(value = "创建时间", position = 8)
    private Date createTime;
    
    @ApiModelProperty(value = "更新时间", position = 9)
    private Date updateTime;
}