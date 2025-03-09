package com.test.prizesystem.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import java.util.Date;

/**
 * 用户实体类
 * <p>
 * 用户基本信息以及抽奖和中奖配额，用于压测。
 * 
 * @author MCP生成
 * @version 1.0
 */
@Data
@ApiModel(value = "用户信息", description = "用户基本信息与抽奖配额")
public class User {
    @ApiModelProperty(value = "用户ID", example = "10086", position = 1)
    private Integer id;
    
    @ApiModelProperty(value = "用户名", example = "testuser", position = 2)
    private String username;
    
    @ApiModelProperty(value = "密码", example = "password", position = 3)
    private String password;
    
    @ApiModelProperty(value = "抽奖次数配额", example = "100", notes = "最大允许抽奖次数", position = 4)
    private Integer drawQuota;
    
    @ApiModelProperty(value = "已使用抽奖次数", example = "20", position = 5)
    private Integer drawCount;
    
    @ApiModelProperty(value = "中奖次数配额", example = "10", notes = "最大允许中奖次数", position = 6)
    private Integer winQuota;
    
    @ApiModelProperty(value = "已中奖次数", example = "5", position = 7)
    private Integer winCount;
    
    @ApiModelProperty(value = "创建时间", position = 8)
    private Date createTime;
    
    @ApiModelProperty(value = "最后登录时间", position = 9)
    private Date lastLoginTime;
    
    /**
     * 获取剩余抽奖次数
     */
    @JsonIgnore
    public int getRemainingDraws() {
        return drawQuota - drawCount;
    }
    
    /**
     * 获取剩余中奖次数
     */
    @JsonIgnore
    public int getRemainingWins() {
        return winQuota - winCount;
    }
    
    /**
     * 尝试增加抽奖次数
     * @return 是否成功
     */
    public boolean tryDraw() {
        if (drawCount >= drawQuota) {
            return false;
        }
        drawCount++;
        return true;
    }
    
    /**
     * 尝试增加中奖次数
     * @return 是否成功
     */
    public boolean tryWin() {
        if (winCount >= winQuota) {
            return false;
        }
        winCount++;
        return true;
    }
}