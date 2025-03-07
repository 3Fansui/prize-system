package com.test.prizesystem.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("t_activity_rule")
public class ActivityRule {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer activityId;
    private Integer userLevel;
    private Integer maxDrawsDaily;
    private Integer maxWinsDaily;
    private Date createTime;
    private Date updateTime;
}
