package com.test.prizesystem.service;



import com.test.prizesystem.model.entity.Activity;
import com.test.prizesystem.model.entity.ActivityPrize;
import com.test.prizesystem.model.entity.Token;

import java.util.List;
import java.util.Map;

public interface TokenService {

    /**
     * 生成令牌
     */
    void generateTokens(Activity activity, List<ActivityPrize> activityPrizes);

    /**
     * 获取可用令牌
     */
    Token getAvailableToken(Integer activityId, long timestamp);

    /**
     * 获取下一个令牌（先到先得）
     */
    Token getNextToken(Integer activityId);

    /**
     * 使用令牌
     */
    boolean useToken(Long tokenId);

    /**
     * 获取活动的缓存令牌详细信息
     */
    Map<String, Object> getTokenDetails(Integer activityId);


}
