package com.test.prizesystem.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("t_user_draw_record")
public class UserDrawRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Integer userId;
    private Integer activityId;
    private Date drawTime;
}
