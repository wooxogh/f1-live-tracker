package com.f1tracker.telemetry.scheduler;

import com.f1tracker.telemetry.service.TeamRadioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TelemetryScheduler {

    private final TeamRadioService teamRadioService;
    private final StringRedisTemplate redisTemplate;

    private static final String REDIS_SESSION_KEY = "f1:current_session";

    @Scheduled(fixedRate = 15_000)
    public void pollTeamRadio() {
        String sessionKeyStr = redisTemplate.opsForValue().get(REDIS_SESSION_KEY);
        if (sessionKeyStr == null) return;

        try {
            int sessionKey = Integer.parseInt(sessionKeyStr);
            teamRadioService.pollAndBroadcast(sessionKey);
        } catch (Exception e) {
            log.error("Failed to poll team radio: {}", e.getMessage());
        }
    }
}
