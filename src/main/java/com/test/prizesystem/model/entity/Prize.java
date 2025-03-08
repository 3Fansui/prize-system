package com.test.prizesystem.model.entity;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 奖品实体类
 * <p>
 * 该类表示抽奖系统中的奖品信息，包括奖品的个数、价格、库存等信息。
 * 奖品可以关联到不同的活动中，并计算其中奖的概率。
 * 
 * @author MCP生成
 * @version 1.0
 */
@Data
@ApiModel(value = "奖品信息", description = "系统中的奖品信息详情")
public class Prize {
    @ApiModelProperty(value = "奖品ID", example = "1", position = 1)
    private Integer id;
    
    @ApiModelProperty(value = "奖品名称", example = "苹果Iphone 15", required = true, position = 2)
    private String name;
    
    @ApiModelProperty(value = "奖品价值", example = "8999.00", notes = "奖品市场价值", position = 3)
    private BigDecimal price;
    
    @ApiModelProperty(value = "奖品总数量", example = "10", notes = "活动中奖品的总数量", position = 4)
    private Integer totalAmount;
    
    @ApiModelProperty(value = "奖品剩余数量", example = "8", notes = "当前剩余可供抽取的数量", position = 5)
    private Integer remainingAmount;
    
    @ApiModelProperty(value = "奖品图片URL", example = "https://example.com/images/iphone15.jpg", position = 6)
    private String imageUrl;
    
    @ApiModelProperty(value = "创建时间", position = 7)
    private Date createTime;
    
    @ApiModelProperty(value = "更新时间", position = 8)
    private Date updateTime;
}