package com.test.prizesystem.model.entity;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

/**
 * 活动奖品关联实体类
 * <p>
 * 表示活动与奖品的关系，定义了活动中特定奖品的数量。
 * 一个活动可以有多个奖品，一个奖品也可以参与多个活动。
 * 系统使用 activityId 和 prizeId 的组合作为唯一标识。
 * 
 * @author wu
 * @version 2.1
 */
@Data
@ApiModel(value = "活动奖品关联", description = "活动与奖品的关联信息")
public class ActivityPrize implements Serializable {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "活动ID", example = "1", required = true, position = 1)
    private Integer activityId;
    
    @ApiModelProperty(value = "奖品ID", example = "2", required = true, position = 2)
    private Integer prizeId;
    
    @ApiModelProperty(value = "奖品数量", example = "10", required = true, position = 3)
    private Integer amount;
    
    @ApiModelProperty(hidden = true)
    private Date createTime;
}