package com.test.prizesystem.model.vo;

import io.swagger.annotations.ApiModel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 抽奖响应VO
 * <p>
 * 封装抽奖结果信息，返回给前端展示
 * 
 * @author wu
 * @version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(value = "抽奖结果", description = "用户抽奖的返回结果")
public class DrawResponse {
    private Boolean success;
    
    private String message;
    
    private PrizeVO prize;

    public DrawResponse(Boolean success, String message) {
        this.success = success;
        this.message = message;
    }
}
