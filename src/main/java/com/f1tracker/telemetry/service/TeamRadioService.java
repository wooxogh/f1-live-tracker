package com.f1tracker.telemetry.service;

import com.f1tracker.telemetry.dto.TeamRadioMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class TeamRadioService {

    private final SimpMessagingTemplate messagingTemplate;

    private final Map<Integer, Map<String, Object>> driverCache = new ConcurrentHashMap<>();

    public void onTeamRadioUpdate(int sessionKey, List<Map<String, Object>> captures) {
        List<TeamRadioMessage.RadioEntry> entries = captures.stream()
                .map(r -> {
                    int driverNumber = toInt(r.get("driver_number"));
                    if (driverNumber == 0) return null;
                    Map<String, Object> driver = driverCache.get(driverNumber);
                    return TeamRadioMessage.RadioEntry.builder()
                            .driverNumber(driverNumber)
                            .nameAcronym(driver != null ? String.valueOf(driver.get("name_acronym")) : "???")
                            .teamColour(driver != null ? String.valueOf(driver.get("team_colour")) : "FFFFFF")
                            .date(String.valueOf(r.get("date")))
                            .recordingUrl(String.valueOf(r.get("recording_url")))
                            .build();
                })
                .filter(Objects::nonNull)
                .toList();

        messagingTemplate.convertAndSend("/topic/radio/" + sessionKey,
                TeamRadioMessage.builder()
                        .sessionKey(sessionKey)
                        .timestamp(Instant.now())
                        .entries(entries)
                        .build());

        log.debug("Broadcast {} team radio for session {}", entries.size(), sessionKey);
    }

    public void updateDriverCache(Map<Integer, Map<String, Object>> drivers) {
        driverCache.clear();
        driverCache.putAll(drivers);
    }

    private int toInt(Object val) {
        if (val == null) return 0;
        try { return Integer.parseInt(String.valueOf(val).replace(".0", "")); }
        catch (NumberFormatException e) { return 0; }
    }
}
