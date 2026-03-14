package com.f1tracker.replay.service;

import com.f1tracker.common.client.OpenF1Client;
import com.f1tracker.replay.domain.LocationHistory;
import com.f1tracker.replay.domain.SessionCacheStatus;
import com.f1tracker.replay.repository.LocationHistoryRepository;
import com.f1tracker.replay.repository.SessionCacheRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class LocationCacheService {

    private final LocationHistoryRepository locationHistoryRepository;
    private final SessionCacheRepository sessionCacheRepository;
    private final OpenF1Client openF1Client;

    private static final int BATCH_SIZE = 500;

    public boolean isCached(int sessionKey) {
        return locationHistoryRepository.existsBySessionKey(sessionKey);
    }

    @Transactional
    public void cacheSession(int sessionKey) {
        log.info("Starting location cache for session {}", sessionKey);

        List<Map<String, Object>> rawLocations = openF1Client.getLocationsForSession(sessionKey);
        if (rawLocations == null || rawLocations.isEmpty()) {
            log.warn("No location data returned for session {}", sessionKey);
            return;
        }

        log.info("Fetched {} location records for session {}", rawLocations.size(), sessionKey);

        List<LocationHistory> batch = new ArrayList<>(BATCH_SIZE);
        int savedCount = 0;

        for (Map<String, Object> raw : rawLocations) {
            try {
                Object driverNumberObj = raw.get("driver_number");
                Object dateObj = raw.get("date");
                Object xObj = raw.get("x");
                Object yObj = raw.get("y");
                Object zObj = raw.get("z");

                if (driverNumberObj == null || dateObj == null) continue;

                int driverNumber = Integer.parseInt(String.valueOf(driverNumberObj).replace(".0", ""));
                String dateStr = String.valueOf(dateObj);
                Instant recordedAt = parseDate(dateStr);
                if (recordedAt == null) continue;

                int x = xObj != null ? (int) Math.round(Double.parseDouble(String.valueOf(xObj))) : 0;
                int y = yObj != null ? (int) Math.round(Double.parseDouble(String.valueOf(yObj))) : 0;
                int z = zObj != null ? (int) Math.round(Double.parseDouble(String.valueOf(zObj))) : 0;

                batch.add(LocationHistory.builder()
                        .sessionKey(sessionKey)
                        .driverNumber(driverNumber)
                        .recordedAt(recordedAt)
                        .x(x)
                        .y(y)
                        .z(z)
                        .build());

                if (batch.size() >= BATCH_SIZE) {
                    locationHistoryRepository.saveAll(batch);
                    savedCount += batch.size();
                    log.debug("Saved batch of {} records for session {} (total: {})", batch.size(), sessionKey, savedCount);
                    batch.clear();
                }
            } catch (Exception e) {
                log.debug("Failed to parse location record for session {}: {}", sessionKey, e.getMessage());
            }
        }

        if (!batch.isEmpty()) {
            locationHistoryRepository.saveAll(batch);
            savedCount += batch.size();
        }

        log.info("Cached {} location records for session {}", savedCount, sessionKey);

        // Update SessionCacheStatus to cached = true
        sessionCacheRepository.findById(sessionKey).ifPresent(status -> {
            SessionCacheStatus updated = SessionCacheStatus.builder()
                    .sessionKey(status.getSessionKey())
                    .sessionName(status.getSessionName())
                    .meetingName(status.getMeetingName())
                    .location(status.getLocation())
                    .countryName(status.getCountryName())
                    .sessionDate(status.getSessionDate())
                    .cached(true)
                    .build();
            sessionCacheRepository.save(updated);
            log.info("Updated SessionCacheStatus to cached=true for session {}", sessionKey);
        });
    }

    private Instant parseDate(String dateStr) {
        if (dateStr == null || dateStr.length() < 19) return null;
        try {
            // Take first 19 chars (yyyy-MM-ddTHH:mm:ss) and append Z
            String normalized = dateStr.substring(0, 19) + "Z";
            return Instant.parse(normalized);
        } catch (Exception e) {
            log.debug("Failed to parse date '{}': {}", dateStr, e.getMessage());
            return null;
        }
    }
}
