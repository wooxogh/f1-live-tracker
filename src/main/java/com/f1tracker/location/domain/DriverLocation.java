package com.f1tracker.location.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "driver_locations", indexes = {
        @Index(name = "idx_session_driver_time", columnList = "sessionKey, driverNumber, date")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class DriverLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer sessionKey;
    private Integer driverNumber;
    private Double x;
    private Double y;
    private Double z;
    private Instant date;
}
