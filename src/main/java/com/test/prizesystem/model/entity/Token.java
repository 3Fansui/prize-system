package com.test.prizesystem.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("t_token")
public class Token {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Integer activityId;
    private Integer prizeId;
    private Long tokenTimestamp;
    private Integer status;
    private Date createTime;
    private Date updateTime;
}
