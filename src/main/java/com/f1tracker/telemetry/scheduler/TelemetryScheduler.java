package com.f1tracker.telemetry.scheduler;

import com.f1tracker.telemetry.service.RaceControlService;
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
    private final RaceControlService raceControlService;
    private final StringRedisTemplate redisTemplate;

    private static final String REDIS_SESSION_KEY = "f1:current_session";

    @Scheduled(fixedRate = 15_000)
    public void pollTelemetry() {
        String sessionKeyStr = redisTemplate.opsForValue().get(REDIS_SESSION_KEY);
        if (sessionKeyStr == null) return;

        int sessionKey;
        try {
            sessionKey = Integer.parseInt(sessionKeyStr);
        } catch (NumberFormatException e) {
            log.error("Invalid session key: {}", sessionKeyStr);
            return;
        }

        try {
            teamRadioService.pollAndBroadcast(sessionKey);
        } catch (Exception e) {
            log.error("Failed to poll team radio: {}", e.getMessage());
        }

        try {
            raceControlService.pollAndBroadcast(sessionKey);
        } catch (Exception e) {
            log.error("Failed to poll race control: {}", e.getMessage());
        }
    }
}
