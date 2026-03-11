package com.f1tracker.telemetry.service;

import com.f1tracker.common.client.OpenF1Client;
import com.f1tracker.telemetry.dto.RaceControlMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class RaceControlService {

    private final SimpMessagingTemplate messagingTemplate;
    private final OpenF1Client openF1Client;

    private volatile String lastRaceControlDate = null;

    private static final DateTimeFormatter ISO_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS").withZone(ZoneOffset.UTC);

    public void pollAndBroadcast(int sessionKey) {
        List<Map<String, Object>> data = openF1Client.getRaceControl(sessionKey, lastRaceControlDate);
        if (data == null || data.isEmpty()) return;

        lastRaceControlDate = ISO_FMT.format(Instant.now());

        List<RaceControlMessage.RaceControlEntry> entries = data.stream()
                .map(r -> RaceControlMessage.RaceControlEntry.builder()
                        .date(String.valueOf(r.get("date")))
                        .category(r.get("category") != null ? String.valueOf(r.get("category")) : null)
                        .flag(r.get("flag") != null ? String.valueOf(r.get("flag")) : null)
                        .message(r.get("message") != null ? String.valueOf(r.get("message")) : null)
                        .build())
                .toList();

        messagingTemplate.convertAndSend("/topic/race-control/" + sessionKey,
                RaceControlMessage.builder()
                        .sessionKey(sessionKey)
                        .timestamp(Instant.now())
                        .entries(entries)
                        .build());

        log.debug("Broadcast {} race control messages for session {}", entries.size(), sessionKey);
    }

    public void reset() {
        lastRaceControlDate = null;
    }
}
