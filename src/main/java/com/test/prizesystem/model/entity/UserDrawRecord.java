package com.test.prizesystem.model.entity;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import java.util.Date;

/**
 * 用户抽奖记录实体类
 * <p>
 * 记录用户的每次抽奖行为，无论是否中奖。
 * 用于统计用户参与活动的次数、频率等信息。
 * 
 * @author MCP生成
 * @version 1.0
 */
@Data
@ApiModel(value = "用户抽奖记录", description = "记录用户参与抽奖的行为")
public class UserDrawRecord {
    @ApiModelProperty(value = "ID", example = "1", position = 1)
    private Long id;
    
    @ApiModelProperty(value = "用户ID", example = "10086", position = 2)
    private Integer userId;
    
    @ApiModelProperty(value = "活动ID", example = "1", position = 3)
    private Integer activityId;
    
    @ApiModelProperty(value = "抽奖时间", position = 4)
    private Date drawTime;
}