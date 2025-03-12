package com.test.prizesystem.model.entity;

import io.swagger.annotations.ApiModel;
import lombok.Data;
import java.util.Date;

/**
 * 活动实体类
 * <p>
 * 表示抽奖系统中的活动信息，包含活动的名称、时间范围、状态等。
 * 活动是抽奖系统的核心元素，用户及奖品都会关联到特定活动。
 * 
 * @author wu
 * @version 1.0
 */
@Data
@ApiModel(value = "活动信息", description = "抽奖活动的详细信息")
public class Activity {
    private Integer id;
    
    private String title;
    
    private Date startTime;
    
    private Date endTime;
    
    private Integer type;
    
    private Integer status;
    
    private Integer probability;
    
    private Date createTime;
    
    private Date updateTime;
}