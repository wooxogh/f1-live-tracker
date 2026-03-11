package com.f1tracker.telemetry.dto;

import lombok.Builder;

import java.time.Instant;
import java.util.List;

@Builder
public record RaceControlMessage(
        int sessionKey,
        Instant timestamp,
        List<RaceControlEntry> entries
) {
    @Builder
    public record RaceControlEntry(
            String date,
            String category,
            String flag,
            String message
    ) {}
}
