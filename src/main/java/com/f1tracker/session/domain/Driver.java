package com.f1tracker.session.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "drivers")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Driver {

    @Id
    @Column(name = "driver_number")
    private Integer driverNumber;

    private String broadcastName;
    private String fullName;
    private String nameAcronym;
    private String teamName;
    private String teamColour;
    private String countryCode;
    private Integer sessionKey;
    private Integer meetingKey;
}
