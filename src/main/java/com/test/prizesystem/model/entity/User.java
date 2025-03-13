package com.test.prizesystem.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.annotations.ApiModel;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

/**
 * 用户实体类
 * <p>
 * 用户基本信息以及抽奖和中奖配额，用于压测。
 * 
 * @author wu
 * @version 2.0
 */
@Data
@ApiModel(value = "用户信息", description = "用户基本信息与抽奖配额")
public class User implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private Integer id;
    
    private String username;
    
    private String password;
    
    private Integer drawQuota;
    
    private Integer drawCount;
    
    private Integer winQuota;
    
    private Integer winCount;
    
    private Date createTime;
    
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