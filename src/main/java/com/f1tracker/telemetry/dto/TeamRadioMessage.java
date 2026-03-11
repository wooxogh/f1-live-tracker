package com.f1tracker.telemetry.dto;

import lombok.Builder;

import java.time.Instant;
import java.util.List;

@Builder
public record TeamRadioMessage(
        int sessionKey,
        Instant timestamp,
        List<RadioEntry> entries
) {
    @Builder
    public record RadioEntry(
            int driverNumber,
            String nameAcronym,
            String teamColour,
            String date,
            String recordingUrl
    ) {}
}
