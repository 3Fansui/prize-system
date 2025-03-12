package com.test.prizesystem.model.entity;

import io.swagger.annotations.ApiModel;
import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 奖品实体类
 * <p>
 * 该类表示抽奖系统中的奖品信息，包括奖品的个数、价格、库存等信息。
 * 奖品可以关联到不同的活动中，并计算其中奖的概率。
 * 
 * @author wu
 * @version 1.0
 */
@Data
@ApiModel(value = "奖品信息", description = "系统中的奖品信息详情")
public class Prize {
    private Integer id;
    
    private String name;
    
    private BigDecimal price;
    
    private Integer totalAmount;
    
    private Integer remainingAmount;
    
    private String imageUrl;
    
    private Date createTime;
    
    private Date updateTime;
}