package com.test.prizesystem.model.entity;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

/**
 * 活动规则实体类
 * <p>
 * 定义活动的参与规则和限制条件，如用户等级要求、每日最大抽奖次数等。
 * 不同用户等级可以有不同的规则设置。
 * 
 * @author wu
 * @version 2.0
 */
@Data
@ApiModel(value = "活动规则", description = "活动的参与规则及限制条件")
public class ActivityRule implements Serializable {
    private static final long serialVersionUID = 1L;
    
    @ApiModelProperty(value = "ID", example = "1", position = 1)
    private Integer id;
    
    @ApiModelProperty(value = "活动ID", example = "1", position = 2)
    private Integer activityId;
    
    @ApiModelProperty(value = "用户等级", example = "0", notes = "0=所有用户，1=普通用户，2=VIP用户", position = 3)
    private Integer userLevel;
    
    @ApiModelProperty(value = "每日最大抽奖次数", example = "5", position = 4)
    private Integer maxDrawsDaily;
    
    @ApiModelProperty(value = "每日最大中奖次数", example = "2", position = 5)
    private Integer maxWinsDaily;
    
    @ApiModelProperty(value = "创建时间", example = "2025-01-01 00:00:00", position = 6)
    private Date createTime;
    
    @ApiModelProperty(value = "更新时间", example = "2025-01-01 00:00:00", position = 7)
    private Date updateTime;
}