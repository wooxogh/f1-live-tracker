package com.f1tracker.telemetry.service;

import com.f1tracker.common.client.OpenF1Client;
import com.f1tracker.telemetry.dto.TeamRadioMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
public class TeamRadioService {

    private final SimpMessagingTemplate messagingTemplate;
    private final OpenF1Client openF1Client;

    private final Map<Integer, Map<String, Object>> driverCache = new ConcurrentHashMap<>();
    private volatile String lastRadioDate = null;

    private static final DateTimeFormatter ISO_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS").withZone(ZoneOffset.UTC);

    public void pollAndBroadcast(int sessionKey) {
        List<Map<String, Object>> radios = openF1Client.getTeamRadio(sessionKey, lastRadioDate);
        if (radios == null || radios.isEmpty()) return;

        lastRadioDate = ISO_FMT.format(Instant.now());

        List<TeamRadioMessage.RadioEntry> entries = radios.stream()
                .map(r -> {
                    int driverNumber = toInt(r.get("driver_number"));
                    Map<String, Object> driver = driverCache.get(driverNumber);
                    return TeamRadioMessage.RadioEntry.builder()
                            .driverNumber(driverNumber)
                            .nameAcronym(driver != null ? String.valueOf(driver.get("name_acronym")) : "???")
                            .teamColour(driver != null ? String.valueOf(driver.get("team_colour")) : "FFFFFF")
                            .date(String.valueOf(r.get("date")))
                            .recordingUrl(String.valueOf(r.get("recording_url")))
                            .build();
                })
                .toList();

        TeamRadioMessage message = TeamRadioMessage.builder()
                .sessionKey(sessionKey)
                .timestamp(Instant.now())
                .entries(entries)
                .build();

        messagingTemplate.convertAndSend("/topic/radio/" + sessionKey, message);
        log.debug("Broadcast {} team radio messages for session {}", entries.size(), sessionKey);
    }

    public void refreshDriverCache(int sessionKey) {
        List<Map<String, Object>> drivers = openF1Client.getDrivers(sessionKey);
        if (drivers == null) return;
        driverCache.clear();
        lastRadioDate = null;
        drivers.forEach(d -> {
            int num = toInt(d.get("driver_number"));
            driverCache.put(num, d);
        });
    }

    private int toInt(Object val) {
        if (val == null) return 0;
        try { return Integer.parseInt(String.valueOf(val).replace(".0", "")); }
        catch (NumberFormatException e) { return 0; }
    }
}
