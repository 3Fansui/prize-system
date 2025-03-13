package com.test.prizesystem.model.entity;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.io.Serializable;
import java.util.Date;

/**
 * 用户中奖记录实体类
 * <p>
 * 记录用户的中奖信息，包含用户ID、活动ID、奖品ID和名称及中奖时间等信息。
 * 该记录用于后续的奖品发放、统计分析等，同时也会被持久化到磁盘。
 *
 * @author wu
 * @version 3.0
 */
@Data
@ApiModel(value = "用户中奖记录", description = "记录用户成功中奖的信息")
public class UserDrawRecord implements Serializable {
    private static final long serialVersionUID = 1L;
    
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
     * 获取中奖时间 (为了兼容CacheController)
     * @return 中奖时间
     */
    @JsonIgnore
    public Date getDrawTime() {
        return winTime;
    }
}