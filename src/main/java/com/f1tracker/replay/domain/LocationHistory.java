package com.f1tracker.replay.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "location_history", indexes = {
    @Index(name = "idx_location_session_time", columnList = "sessionKey, recordedAt")
})
@Getter @Builder @NoArgsConstructor @AllArgsConstructor
public class LocationHistory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false) private int sessionKey;
    @Column(nullable = false) private int driverNumber;
    @Column(nullable = false) private Instant recordedAt;
    private int x;
    private int y;
    private int z;
}
