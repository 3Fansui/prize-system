package com.test.prizesystem.util;

/**
 * 红黑树存储名称枚举
 * <p>
 * 集中管理系统中所有红黑树的名称，避免使用字符串常量带来的错误。
 * 
 * @author wu
 * @version 1.0
 */
public enum TreeNames {
    /** 活动信息存储 */
    ACTIVITIES("activities"),
    
    /** 奖品信息存储 */
    PRIZES("prizes"),
    
    /** 活动奖品关联存储 */
    ACTIVITY_PRIZES("activity_prizes"),
    
    /** 活动规则存储 */
    ACTIVITY_RULES("activity_rules"),
    
    /** 用户信息存储 */
    USERS("users"),
    
    /** 用户中奖记录存储 */
    USER_DRAW_RECORDS("user_draw_records");
    
    private final String treeName;
    
    TreeNames(String treeName) {
        this.treeName = treeName;
    }
    
    /**
     * 获取树名称
     */
    public String getTreeName() {
        return treeName;
    }
    
    @Override
    public String toString() {
        return treeName;
    }
}