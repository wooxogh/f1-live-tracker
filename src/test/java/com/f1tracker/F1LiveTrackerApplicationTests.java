package com.f1tracker;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.client.RestClient;

@SpringBootTest
class F1LiveTrackerApplicationTests {

    @MockBean StringRedisTemplate stringRedisTemplate;
    @MockBean(name = "openF1RestClient") RestClient openF1RestClient;

    @Test
    void contextLoads() {
    }
}
