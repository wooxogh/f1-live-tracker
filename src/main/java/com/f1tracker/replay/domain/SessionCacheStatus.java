package com.f1tracker.replay.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "session_cache_status")
@Getter @Builder @NoArgsConstructor @AllArgsConstructor
public class SessionCacheStatus {
    @Id private int sessionKey;
    private String sessionName;   // e.g., "Race"
    private String meetingName;   // e.g., "Chinese Grand Prix"
    private String location;
    private String countryName;
    private Instant sessionDate;
    private boolean cached;
}
