package com.f1tracker.location.service;

import com.f1tracker.common.client.OpenF1Client;
import com.f1tracker.location.dto.DriverLocationMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class LocationBroadcastService {

    private final SimpMessagingTemplate messagingTemplate;
    private final StringRedisTemplate redisTemplate;
    private final OpenF1Client openF1Client;

    private final Map<Integer, Map<String, Object>> driverCache = new ConcurrentHashMap<>();
    private final Map<Integer, String> lastDateByDriver = new ConcurrentHashMap<>();

    private volatile String lastPollDate = null;

    private static final String REDIS_LOCATION_PREFIX = "f1:location:";
    private static final DateTimeFormatter ISO_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS").withZone(ZoneOffset.UTC);

    public void pollAndBroadcast(int sessionKey) {
        String afterDate = lastPollDate != null
                ? lastPollDate
                : ISO_FMT.format(Instant.now().minusSeconds(5));

        lastPollDate = ISO_FMT.format(Instant.now());

        List<Map<String, Object>> locations = openF1Client.getLatestLocations(sessionKey, afterDate);
        if (locations == null || locations.isEmpty()) return;

        Map<Integer, Map<String, Object>> latestByDriver = new ConcurrentHashMap<>();
        for (Map<String, Object> loc : locations) {
            Integer driverNumber = toInt(loc.get("driver_number"));
            if (driverNumber == null) continue;

            String date = String.valueOf(loc.get("date"));
            String existing = lastDateByDriver.get(driverNumber);

            if (existing == null || date.compareTo(existing) > 0) {
                latestByDriver.put(driverNumber, loc);
                lastDateByDriver.put(driverNumber, date);
            }
        }

        if (latestByDriver.isEmpty()) return;

        latestByDriver.forEach((driverNumber, loc) -> {
            String redisKey = REDIS_LOCATION_PREFIX + sessionKey + ":" + driverNumber;
            redisTemplate.opsForValue().set(redisKey,
                    loc.get("x") + "," + loc.get("y") + "," + loc.get("z"));
        });

        List<DriverLocationMessage.DriverPosition> positions = latestByDriver.entrySet().stream()
                .map(entry -> {
                    int driverNumber = entry.getKey();
                    Map<String, Object> loc = entry.getValue();
                    Map<String, Object> driver = driverCache.get(driverNumber);

                    return DriverLocationMessage.DriverPosition.builder()
                            .driverNumber(driverNumber)
                            .nameAcronym(driver != null ? String.valueOf(driver.get("name_acronym")) : "???")
                            .teamColour(driver != null ? String.valueOf(driver.get("team_colour")) : "FFFFFF")
                            .x(toDouble(loc.get("x")))
                            .y(toDouble(loc.get("y")))
                            .z(toDouble(loc.get("z")))
                            .build();
                })
                .toList();

        DriverLocationMessage message = DriverLocationMessage.builder()
                .sessionKey(sessionKey)
                .timestamp(Instant.now())
                .positions(positions)
                .build();

        messagingTemplate.convertAndSend("/topic/locations/" + sessionKey, message);
        log.debug("Broadcast {} driver positions for session {}", positions.size(), sessionKey);
    }

    public void refreshDriverCache(int sessionKey) {
        List<Map<String, Object>> drivers = openF1Client.getDrivers(sessionKey);
        if (drivers == null) return;
        driverCache.clear();
        lastDateByDriver.clear();
        lastPollDate = null;
        drivers.forEach(d -> {
            Integer num = toInt(d.get("driver_number"));
            if (num != null) driverCache.put(num, d);
        });
        log.info("Driver cache refreshed: {} drivers", driverCache.size());
    }

    private Integer toInt(Object val) {
        if (val == null) return null;
        try { return Integer.parseInt(String.valueOf(val).replace(".0", "")); }
        catch (NumberFormatException e) { return null; }
    }

    private double toDouble(Object val) {
        if (val == null) return 0.0;
        try { return Double.parseDouble(String.valueOf(val)); }
        catch (NumberFormatException e) { return 0.0; }
    }
}
