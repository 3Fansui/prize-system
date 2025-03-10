package com.test.prizesystem.model.vo;

import io.swagger.annotations.ApiModel;
import lombok.Data;

/**
 * 奖品视图对象
 * <p>
 * 用于封装返回给前端的奖品信息，只包含需要展示的字段
 * 
 * @author MCP生成
 * @version 2.0
 */
@Data
@ApiModel(value = "奖品信息", description = "返回给前端展示的奖品信息")
public class PrizeVO {
    private Integer id;
    
    private String name;
}