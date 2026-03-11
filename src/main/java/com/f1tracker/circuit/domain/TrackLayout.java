package com.f1tracker.circuit.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "track_layouts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class TrackLayout {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Integer meetingKey;

    private String circuitShortName;

    @Column(columnDefinition = "LONGTEXT", nullable = false)
    private String pointsJson;

    // PCA로 계산된 최적 회전각 (도) - 이미 적용된 상태로 저장됨
    @Column(nullable = false)
    private Double optimalRotationDeg;

    @Column(nullable = false)
    private Instant createdAt;
}
