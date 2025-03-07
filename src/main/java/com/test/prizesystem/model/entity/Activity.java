
package com.test.prizesystem.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;


import java.util.Date;

@Data
@TableName("t_activity")
public class Activity {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String title;
    private Date startTime;
    private Date endTime;
    private Integer type;
    private Integer status;
    private Integer probability;
    private Date createTime;
    private Date updateTime;
}