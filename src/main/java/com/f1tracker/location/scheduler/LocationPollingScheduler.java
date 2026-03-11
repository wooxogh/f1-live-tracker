package com.f1tracker.location.scheduler;

import com.f1tracker.common.client.OpenF1Client;
import com.f1tracker.location.service.LocationBroadcastService;
import com.f1tracker.telemetry.service.TeamRadioService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class LocationPollingScheduler {

    private final OpenF1Client openF1Client;
    private final LocationBroadcastService broadcastService;
    private final TeamRadioService teamRadioService;
    private final StringRedisTemplate redisTemplate;

    private static final String REDIS_SESSION_KEY = "f1:current_session";
    private final AtomicInteger currentSessionKey = new AtomicInteger(-1);

    @PostConstruct
    public void init() {
        refreshSession();
    }

    @Scheduled(fixedRateString = "${openf1.poll-interval-ms:3000}")
    public void pollLocations() {
        int sessionKey = currentSessionKey.get();
        if (sessionKey == -1) return;
        broadcastService.pollAndBroadcast(sessionKey);
    }

    @Scheduled(fixedRate = 300_000)
    public void refreshSession() {
        try {
            Map<String, Object> session = openF1Client.getLatestSession();
            if (session == null) {
                log.info("No active session found");
                return;
            }

            Object keyObj = session.get("session_key");
            if (keyObj == null) return;

            int sessionKey = Integer.parseInt(String.valueOf(keyObj).replace(".0", ""));

            if (currentSessionKey.get() != sessionKey) {
                currentSessionKey.set(sessionKey);
                redisTemplate.opsForValue().set(REDIS_SESSION_KEY, String.valueOf(sessionKey));
                broadcastService.refreshDriverCache(sessionKey);
                teamRadioService.refreshDriverCache(sessionKey);
                log.info("Session updated: {} ({})", sessionKey, session.get("session_name"));
            }
        } catch (Exception e) {
            log.error("Failed to refresh session: {}", e.getMessage());
        }
    }
}
