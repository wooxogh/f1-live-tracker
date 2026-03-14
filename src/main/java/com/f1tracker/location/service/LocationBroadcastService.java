package com.f1tracker.location.service;

import com.f1tracker.location.dto.DriverLocationMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class LocationBroadcastService {

    private final SimpMessagingTemplate messagingTemplate;
    private final StringRedisTemplate redisTemplate;

    private final Map<Integer, Map<String, Object>> driverCache = new ConcurrentHashMap<>();

    private static final String REDIS_LOCATION_PREFIX = "f1:location:";

    public void onPositionUpdate(int sessionKey, Map<Integer, Map<String, Object>> positions) {
        positions.forEach((driverNumber, pos) -> {
            String redisKey = REDIS_LOCATION_PREFIX + sessionKey + ":" + driverNumber;
            redisTemplate.opsForValue().set(redisKey,
                    pos.get("x") + "," + pos.get("y") + "," + pos.get("z"));
        });

        List<DriverLocationMessage.DriverPosition> driverPositions = positions.entrySet().stream()
                .map(entry -> {
                    int num = entry.getKey();
                    Map<String, Object> pos = entry.getValue();
                    Map<String, Object> driver = driverCache.get(num);
                    return DriverLocationMessage.DriverPosition.builder()
                            .driverNumber(num)
                            .nameAcronym(driver != null ? String.valueOf(driver.get("name_acronym")) : "???")
                            .teamColour(driver != null ? String.valueOf(driver.get("team_colour")) : "FFFFFF")
                            .x(toDouble(pos.get("x")))
                            .y(toDouble(pos.get("y")))
                            .z(toDouble(pos.get("z")))
                            .build();
                })
                .toList();

        messagingTemplate.convertAndSend("/topic/locations/" + sessionKey,
                DriverLocationMessage.builder()
                        .sessionKey(sessionKey)
                        .timestamp(Instant.now())
                        .positions(driverPositions)
                        .build());

        log.debug("Broadcast {} positions for session {}", driverPositions.size(), sessionKey);
    }

    public void updateDriverCache(Map<Integer, Map<String, Object>> drivers) {
        driverCache.putAll(drivers);
    }

    private double toDouble(Object val) {
        if (val == null) return 0.0;
        try { return Double.parseDouble(String.valueOf(val)); }
        catch (NumberFormatException e) { return 0.0; }
    }
}
