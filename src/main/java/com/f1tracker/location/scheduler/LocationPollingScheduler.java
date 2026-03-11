package com.f1tracker.location.scheduler;

import com.f1tracker.common.client.OpenF1Client;
import com.f1tracker.location.service.LocationBroadcastService;
import com.f1tracker.telemetry.service.TeamRadioService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${openf1.override-session-key:-1}")
    private int overrideSessionKey;

    private static final String REDIS_SESSION_KEY = "f1:current_session";
    private final AtomicInteger currentSessionKey = new AtomicInteger(-1);

    @PostConstruct
    public void init() {
        if (overrideSessionKey != -1) {
            log.info("Override session key set: {}", overrideSessionKey);
            currentSessionKey.set(overrideSessionKey);
            redisTemplate.opsForValue().set(REDIS_SESSION_KEY, String.valueOf(overrideSessionKey));
            broadcastService.refreshDriverCache(overrideSessionKey);
            teamRadioService.refreshDriverCache(overrideSessionKey);

            // 과거 세션의 경우 세션 시작 시점부터 폴링
            Map<String, Object> session = openF1Client.getSessionByKey(overrideSessionKey);
            if (session != null) {
                String dateStart = String.valueOf(session.get("date_start"));
                String normalized = dateStart.substring(0, Math.min(19, dateStart.length())) + ".000";
                broadcastService.setInitialPollDate(normalized);
                log.info("Past session detected, polling from: {}", normalized);
            }
            return;
        }
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
