package com.f1tracker.session.controller;

import com.f1tracker.common.client.OpenF1Client;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class SessionController {

    private final OpenF1Client openF1Client;
    private final StringRedisTemplate redisTemplate;

    private static final String REDIS_SESSION_KEY = "f1:current_session";

    @GetMapping("/sessions/current")
    public ResponseEntity<Map<String, Object>> getCurrentSession() {
        String sessionKeyStr = null;
        try {
            sessionKeyStr = redisTemplate.opsForValue().get(REDIS_SESSION_KEY);
        } catch (Exception e) {
            log.warn("Failed to read session key from Redis, falling back to OpenF1: {}", e.getMessage());
        }
        if (sessionKeyStr != null) {
            try {
                Map<String, Object> session = openF1Client.getSessionByKey(Integer.parseInt(sessionKeyStr));
                if (session != null) return ResponseEntity.ok(session);
            } catch (NumberFormatException ignored) {}
        }
        Map<String, Object> session = openF1Client.getLatestSession();
        if (session == null) return ResponseEntity.noContent().build();
        return ResponseEntity.ok(session);
    }

    @GetMapping("/sessions/{sessionKey}/drivers")
    public ResponseEntity<List<Map<String, Object>>> getDrivers(@PathVariable int sessionKey) {
        List<Map<String, Object>> drivers = openF1Client.getDrivers(sessionKey);
        return ResponseEntity.ok(drivers);
    }

    @GetMapping("/sessions/{sessionKey}/locations")
    public ResponseEntity<Map<String, String>> getLocations(@PathVariable int sessionKey) {
        String pattern = "f1:location:" + sessionKey + ":*";
        var keys = redisTemplate.keys(pattern);
        if (keys == null || keys.isEmpty()) return ResponseEntity.ok(Map.of());

        Map<String, String> result = new HashMap<>();
        keys.forEach(key -> {
            String val = redisTemplate.opsForValue().get(key);
            if (val != null) result.put(key, val);
        });
        return ResponseEntity.ok(result);
    }
}
