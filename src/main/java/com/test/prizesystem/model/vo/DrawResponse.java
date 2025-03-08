package com.test.prizesystem.model.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 抽奖响应VO
 * <p>
 * 封装抽奖结果信息，返回给前端展示
 * 
 * @author MCP生成
 * @version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(value = "抽奖结果", description = "用户抽奖的返回结果")
public class DrawResponse {
    @ApiModelProperty(value = "是否成功", example = "true", position = 1)
    private Boolean success;
    
    @ApiModelProperty(value = "结果消息", example = "抽奖成功", position = 2)
    private String message;
    
    @ApiModelProperty(value = "奖品信息", notes = "如果未中奖则为null", position = 3)
    private PrizeVO prize;

    public DrawResponse(Boolean success, String message) {
        this.success = success;
        this.message = message;
    }
}
