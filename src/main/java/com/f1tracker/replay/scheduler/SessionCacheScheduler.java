package com.f1tracker.replay.scheduler;

import com.f1tracker.common.client.OpenF1Client;
import com.f1tracker.replay.domain.SessionCacheStatus;
import com.f1tracker.replay.repository.SessionCacheRepository;
import com.f1tracker.replay.service.LocationCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class SessionCacheScheduler {

    private final OpenF1Client openF1Client;
    private final SessionCacheRepository sessionCacheRepository;
    private final LocationCacheService locationCacheService;

    @Scheduled(cron = "${replay.cache-cron:0 0 3 * * *}")
    public void cacheRecentRaceSessions() {
        int year = java.time.Year.now().getValue();
        log.info("SessionCacheScheduler: starting cache job for year {}", year);

        List<Map<String, Object>> sessions = openF1Client.getRaceSessionsByYear(year);
        if (sessions == null || sessions.isEmpty()) {
            log.info("SessionCacheScheduler: no race sessions found for year {}", year);
            return;
        }

        Instant threshold = Instant.now().minus(24, ChronoUnit.HOURS);

        for (Map<String, Object> sessionData : sessions) {
            try {
                Object sessionKeyObj = sessionData.get("session_key");
                if (sessionKeyObj == null) continue;

                int sessionKey = Integer.parseInt(String.valueOf(sessionKeyObj).replace(".0", ""));

                // 종료 시간이 24시간 이상 지났는지 확인
                Object dateEndObj = sessionData.get("date_end");
                if (dateEndObj == null) continue;

                String dateEndStr = String.valueOf(dateEndObj);
                if (dateEndStr.length() < 19) continue;

                Instant dateEnd;
                try {
                    dateEnd = Instant.parse(dateEndStr.substring(0, 19) + "Z");
                } catch (Exception e) {
                    log.debug("Failed to parse date_end '{}' for session {}", dateEndStr, sessionKey);
                    continue;
                }

                if (!dateEnd.isBefore(threshold)) {
                    log.debug("Session {} not yet finished 24h ago, skipping", sessionKey);
                    continue;
                }

                // SessionCacheStatus에 이미 있으면 스킵
                if (sessionCacheRepository.existsById(sessionKey)) {
                    log.debug("Session {} already in cache status, skipping", sessionKey);
                    continue;
                }

                // SessionCacheStatus 생성 (cached=false)
                String sessionName  = sessionData.get("session_name")  != null ? String.valueOf(sessionData.get("session_name"))  : null;
                String meetingName  = sessionData.get("meeting_name")   != null ? String.valueOf(sessionData.get("meeting_name"))   : null;
                String location     = sessionData.get("location")       != null ? String.valueOf(sessionData.get("location"))       : null;
                String countryName  = sessionData.get("country_name")   != null ? String.valueOf(sessionData.get("country_name"))   : null;
                Object sessionDateObj = sessionData.get("date_start");
                Instant sessionDate = null;
                if (sessionDateObj != null) {
                    String sdStr = String.valueOf(sessionDateObj);
                    if (sdStr.length() >= 19) {
                        try { sessionDate = Instant.parse(sdStr.substring(0, 19) + "Z"); } catch (Exception ignored) {}
                    }
                }

                SessionCacheStatus status = SessionCacheStatus.builder()
                        .sessionKey(sessionKey)
                        .sessionName(sessionName)
                        .meetingName(meetingName)
                        .location(location)
                        .countryName(countryName)
                        .sessionDate(sessionDate)
                        .cached(false)
                        .build();
                sessionCacheRepository.save(status);

                log.info("Caching location data for session {} ({})", sessionKey, meetingName);

                try {
                    locationCacheService.cacheSession(sessionKey);
                    log.info("Successfully cached session {}", sessionKey);
                } catch (Exception e) {
                    log.error("Failed to cache session {}: {}", sessionKey, e.getMessage());
                }

            } catch (Exception e) {
                log.error("Error processing session entry: {}", e.getMessage());
            }
        }

        log.info("SessionCacheScheduler: job complete");
    }
}
