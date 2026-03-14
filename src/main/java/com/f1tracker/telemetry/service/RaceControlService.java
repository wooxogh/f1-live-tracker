package com.f1tracker.telemetry.service;

import com.f1tracker.telemetry.dto.RaceControlMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class RaceControlService {

    private final SimpMessagingTemplate messagingTemplate;

    public void onRaceControlUpdate(int sessionKey, List<Map<String, Object>> messages) {
        List<RaceControlMessage.RaceControlEntry> entries = messages.stream()
                .map(m -> RaceControlMessage.RaceControlEntry.builder()
                        .date(String.valueOf(m.get("date")))
                        .category(m.get("category") != null ? String.valueOf(m.get("category")) : null)
                        .flag(m.get("flag")         != null ? String.valueOf(m.get("flag"))     : null)
                        .message(m.get("message")   != null ? String.valueOf(m.get("message"))  : null)
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
}
