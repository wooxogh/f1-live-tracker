package com.f1tracker.location.dto;

import lombok.Builder;

import java.time.Instant;
import java.util.List;

@Builder
public record DriverLocationMessage(
        int sessionKey,
        Instant timestamp,
        List<DriverPosition> positions
) {
    @Builder
    public record DriverPosition(
            int driverNumber,
            String nameAcronym,
            String teamColour,
            double x,
            double y,
            double z
    ) {}
}
