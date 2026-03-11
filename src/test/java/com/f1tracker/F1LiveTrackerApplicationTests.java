package com.f1tracker;

import com.f1tracker.common.client.OpenF1Client;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;

@SpringBootTest
class F1LiveTrackerApplicationTests {

    @MockBean StringRedisTemplate stringRedisTemplate;
    @MockBean OpenF1Client openF1Client;

    @Test
    void contextLoads() {
    }
}
