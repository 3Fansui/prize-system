package com.test.prizesystem.controller;

import com.test.prizesystem.model.dto.DrawRequest;
import com.test.prizesystem.model.vo.DrawResponse;
import com.test.prizesystem.service.DrawService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@RestController
@RequestMapping("/api/test")
public class TestController {

    @Autowired
    private DrawService drawService;

    private AtomicInteger counter = new AtomicInteger(0);

    /**
     * 测试单次抽奖
     */
    @GetMapping("/draw/{activityId}")
    public DrawResponse testDraw(@PathVariable Integer activityId) {
        DrawRequest request = new DrawRequest();
        request.setUserId(new Random().nextInt(10000) + 1);
        request.setActivityId(activityId);

        return drawService.draw(request);
    }

    /**
     * 批量测试抽奖，返回统计结果
     */
    @GetMapping("/batch/{activityId}/{count}")
    public Object batchTest(@PathVariable Integer activityId, @PathVariable Integer count) {
        int success = 0;
        int fail = 0;
        int win = 0;

        List<DrawResponse> responses = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            DrawRequest request = new DrawRequest();
            request.setUserId(new Random().nextInt(1000) + 1);
            request.setActivityId(activityId);

            DrawResponse response = drawService.draw(request);
            responses.add(response);

            if (response.getSuccess()) {
                success++;
                if (response.getPrize() != null) {
                    win++;
                }
            } else {
                fail++;
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("total", count);
        result.put("successCount", success);
        result.put("failCount", fail);
        result.put("winCount", win);
        result.put("winRate", win * 100.0 / count);
        result.put("detail", responses);
        return result;
    }

    /**
     * 性能测试接口
     */
    @GetMapping("/performance/{activityId}")
    public Object performanceTest(@PathVariable Integer activityId) {
        long startTime = System.currentTimeMillis();

        DrawRequest request = new DrawRequest();
        request.setUserId(counter.incrementAndGet());
        request.setActivityId(activityId);

        DrawResponse response = drawService.draw(request);

        long endTime = System.currentTimeMillis();

        return new Object() {
            public final DrawResponse result = response;
            public final long costTime = endTime - startTime;
        };
    }
}
