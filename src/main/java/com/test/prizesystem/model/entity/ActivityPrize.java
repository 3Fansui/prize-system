package com.test.prizesystem.model.entity;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("t_activity_prize")
public class ActivityPrize {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer activityId;
    private Integer prizeId;
    private Integer amount;
    private Date createTime;
}
