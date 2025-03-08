package com.test.prizesystem.model.dto;


import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 抽奖请求DTO
 * <p>
 * 用于接收客户端发起的抽奖请求参数
 * 
 * @author MCP生成
 * @version 1.0
 */
@Data
@ApiModel(value = "抽奖请求", description = "用户发起抽奖的请求参数")
public class DrawRequest {
    @ApiModelProperty(value = "用户ID", example = "1001", required = true)
    private Integer userId;
    
    @ApiModelProperty(value = "活动ID", example = "1", required = true)
    private Integer activityId;
}