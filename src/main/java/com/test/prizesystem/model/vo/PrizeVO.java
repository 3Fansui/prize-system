package com.test.prizesystem.model.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 奖品视图对象
 * <p>
 * 用于封装返回给前端的奖品信息，只包含需要展示的字段
 * 
 * @author MCP生成
 * @version 1.0
 */
@Data
@ApiModel(value = "奖品信息", description = "返回给前端展示的奖品信息")
public class PrizeVO {
    @ApiModelProperty(value = "奖品ID", example = "1", position = 1)
    private Integer id;
    
    @ApiModelProperty(value = "奖品名称", example = "苹果Iphone 15", position = 2)
    private String name;
    
    @ApiModelProperty(value = "奖品价值", example = "8999.00", position = 3)
    private BigDecimal price;
    
    @ApiModelProperty(value = "奖品图片URL", example = "https://example.com/images/iphone15.jpg", position = 4)
    private String imageUrl;
}
