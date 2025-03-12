package com.test.prizesystem.model.entity;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import java.util.Date;

/**
 * 用户中奖记录实体类
 * <p>
 * 记录用户成功中奖的信息，包含用户、活动、奖品及中奖时间等数据。
 * 该记录用于后续的奖品发放、统计分析等。
 * 
 * @author wu
 * @version 1.0
 */
@Data
@ApiModel(value = "用户中奖记录", description = "记录用户成功中奖的信息")
public class UserPrizeRecord {
    @ApiModelProperty(value = "ID", example = "1000", position = 1)
    private Long id;
    
    @ApiModelProperty(value = "用户ID", example = "10086", position = 2)
    private Integer userId;
    
    @ApiModelProperty(value = "活动ID", example = "1", position = 3)
    private Integer activityId;
    
    @ApiModelProperty(value = "奖品ID", example = "2", position = 4)
    private Integer prizeId;
    
    @ApiModelProperty(value = "奖品名称", example = "iPhone 14", position = 5)
    private String prizeName;
    
    @ApiModelProperty(value = "中奖时间", example = "2025-01-01 00:00:00", position = 6)
    private Date winTime;
    
    /**
     * 获取中奖时间 (为了兼容JSON反序列化)
     * @return 中奖时间
     */
    public Date getDrawTime() {
        return winTime;
    }

    /**
     * 设置中奖时间 (为了兼容JSON反序列化)
     * @param drawTime 中奖时间
     */
    public void setDrawTime(Date drawTime) {
        // 可以选择性地将drawTime同步到winTime
        // this.winTime = drawTime;
    }
}