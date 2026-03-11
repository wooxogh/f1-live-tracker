package com.f1tracker.session.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "sessions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Session {

    @Id
    private Integer sessionKey;

    private Integer meetingKey;
    private String sessionName;
    private String sessionType;
    private String location;
    private String countryName;
    private String circuitShortName;
    private Instant dateStart;
    private Instant dateEnd;
    private Integer year;
}
