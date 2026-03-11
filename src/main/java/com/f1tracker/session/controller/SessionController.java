package com.f1tracker.session.controller;

import com.f1tracker.common.client.OpenF1Client;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class SessionController {

    private final OpenF1Client openF1Client;
    private final StringRedisTemplate redisTemplate;

    @Value("${openf1.override-session-key:-1}")
    private int overrideSessionKey;

    @GetMapping("/sessions/current")
    public ResponseEntity<Map<String, Object>> getCurrentSession() {
        if (overrideSessionKey != -1) {
            Map<String, Object> session = openF1Client.getSessionByKey(overrideSessionKey);
            if (session == null) return ResponseEntity.noContent().build();
            return ResponseEntity.ok(session);
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
