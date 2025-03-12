package com.test.prizesystem.model.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * 活动信息视图对象
 * <p>
 * 包含活动的基本信息以及关联的奖品数量信息。
 * 用于前端显示活动详情和参与状态。
 * 
 * @author wu
 * @version 1.0
 */
@Data
@ApiModel(value = "活动信息", description = "活动详细信息及奖品数量")
public class ActivityInfoVO {
    
    @ApiModelProperty(value = "活动ID", example = "1", position = 1)
    private Integer id;
    
    @ApiModelProperty(value = "活动标题", example = "双11抽奖活动", position = 2)
    private String title;
    
    @ApiModelProperty(value = "活动开始时间", example = "2025-01-01 00:00:00", position = 3)
    private Date startTime;
    
    @ApiModelProperty(value = "活动结束时间", example = "2025-01-07 23:59:59", position = 4)
    private Date endTime;
    
    @ApiModelProperty(value = "活动状态", example = "1", notes = "0-未开始，1-进行中，2-已结束", position = 5)
    private Integer status;
    
    @ApiModelProperty(value = "奖品信息列表", position = 6)
    private List<PrizeInfoVO> prizes;
    
    /**
     * 嵌套的奖品信息类
     */
    @Data
    @ApiModel(value = "奖品信息", description = "活动中的奖品名称和数量")
    public static class PrizeInfoVO {
        
        @ApiModelProperty(value = "奖品ID", example = "1", position = 1)
        private Integer id;
        
        @ApiModelProperty(value = "奖品名称", example = "iPhone 14", position = 2)
        private String name;
        
        @ApiModelProperty(value = "奖品数量", example = "10", position = 3)
        private Integer amount;
    }
}