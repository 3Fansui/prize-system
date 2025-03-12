package com.test.prizesystem.model.persistence;

import com.test.prizesystem.util.RedBlackTree;
import lombok.Data;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * 持久化数据模型
 * <p>
 * 该类用于封装需要持久化的红黑树数据。
 * 系统会定期将该数据持久化到磁盘中，并在启动时进行加载。
 * 
 * @author wu
 */
@Data
public class PersistentData implements Serializable {
    private static final long serialVersionUID = 1L;
    
    // 持久化时间戳
    private long timestamp;
    
    // 红黑树数据映射
    private Map<String, RedBlackTree> trees = new HashMap<>();
}