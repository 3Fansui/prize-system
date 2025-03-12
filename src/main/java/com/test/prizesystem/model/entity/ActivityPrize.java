package com.test.prizesystem.model.entity;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import java.util.Date;

/**
 * 活动奖品关联实体类
 * <p>
 * 表示活动与奖品的关系，定义了活动中特定奖品的数量。
 * 一个活动可以有多个奖品，一个奖品也可以参与多个活动。
 * 
 * @author wu
 * @version 1.0
 */
@Data
@ApiModel(value = "活动奖品关联", description = "活动与奖品的关联信息")
public class ActivityPrize {
    @ApiModelProperty(value = "ID", example = "1", position = 1)
    private Integer id;
    
    @ApiModelProperty(value = "活动ID", example = "1", position = 2)
    private Integer activityId;
    
    @ApiModelProperty(value = "奖品ID", example = "2", position = 3)
    private Integer prizeId;
    
    @ApiModelProperty(value = "奖品数量", example = "10", position = 4)
    private Integer amount;
    
    @ApiModelProperty(value = "创建时间", example = "2025-01-01 00:00:00", position = 5)
    private Date createTime;
}