package com.f1tracker.circuit.service;

import com.f1tracker.circuit.domain.TrackLayout;
import com.f1tracker.circuit.repository.TrackLayoutRepository;
import com.f1tracker.common.client.OpenF1Client;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TrackLayoutServiceTest {

    @Mock TrackLayoutRepository repository;
    @Mock OpenF1Client openF1Client;
    @Spy ObjectMapper objectMapper;

    @InjectMocks TrackLayoutService trackLayoutService;

    @Test
    @DisplayName("DB에 트랙 레이아웃 있을 때 캐시에서 반환")
    void getTrackPoints_cachedInDb_returnsFromDb() throws Exception {
        Map<String, Object> session = Map.of(
                "meeting_key", 1229,
                "circuit_short_name", "Bahrain",
                "date_start", "2024-03-02T11:00:00"
        );
        List<Map<String, Object>> points = List.of(
                Map.of("x", 100.0, "y", 200.0),
                Map.of("x", 150.0, "y", 250.0)
        );
        String json = new ObjectMapper().writeValueAsString(points);
        TrackLayout layout = TrackLayout.builder()
                .meetingKey(1229)
                .circuitShortName("Bahrain")
                .pointsJson(json)
                .optimalRotationDeg(15.0)
                .build();

        given(openF1Client.getSessionByKey(9158)).willReturn(session);
        given(repository.findByMeetingKey(1229)).willReturn(Optional.of(layout));

        List<Map<String, Object>> result = trackLayoutService.getTrackPoints(9158);

        assertThat(result).hasSize(2);
        verify(openF1Client, never()).getDrivers(anyInt());
    }

    @Test
    @DisplayName("세션 없을 때 빈 리스트 반환")
    void getTrackPoints_noSession_returnsEmpty() {
        given(openF1Client.getSessionByKey(anyInt())).willReturn(null);

        List<Map<String, Object>> result = trackLayoutService.getTrackPoints(9999);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("DB 미캐시, 위치 데이터 없을 때 빈 리스트 반환")
    void getTrackPoints_noLocationData_returnsEmpty() {
        Map<String, Object> session = Map.of(
                "meeting_key", 1229,
                "circuit_short_name", "Bahrain",
                "date_start", "2024-03-02T11:00:00"
        );
        List<Map<String, Object>> drivers = List.of(
                Map.of("driver_number", 1)
        );

        given(openF1Client.getSessionByKey(9158)).willReturn(session);
        given(repository.findByMeetingKey(1229)).willReturn(Optional.empty());
        given(openF1Client.getDrivers(9158)).willReturn(drivers);
        given(openF1Client.getLocationsByDriver(anyInt(), anyInt(), anyString(), anyString()))
                .willReturn(List.of());

        List<Map<String, Object>> result = trackLayoutService.getTrackPoints(9158);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("DB 미캐시, 위치 데이터 있을 때 PCA 회전 적용 후 반환")
    void getTrackPoints_fetchesAndSavesWithRotation() {
        Map<String, Object> session = Map.of(
                "meeting_key", 1229,
                "circuit_short_name", "Bahrain",
                "date_start", "2024-03-02T11:00:00"
        );
        List<Map<String, Object>> drivers = List.of(Map.of("driver_number", 1));
        List<Map<String, Object>> locations = buildMockLocations();

        given(openF1Client.getSessionByKey(9158)).willReturn(session);
        given(repository.findByMeetingKey(1229)).willReturn(Optional.empty());
        given(openF1Client.getDrivers(9158)).willReturn(drivers);
        given(openF1Client.getLocationsByDriver(anyInt(), anyInt(), anyString(), anyString()))
                .willReturn(locations);

        List<Map<String, Object>> result = trackLayoutService.getTrackPoints(9158);

        assertThat(result).isNotEmpty();
        assertThat(result.get(0)).containsKeys("x", "y");
        verify(repository).save(any(TrackLayout.class));
    }

    private List<Map<String, Object>> buildMockLocations() {
        List<Map<String, Object>> locations = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            locations.add(Map.of(
                    "x", (double)(i * 100),
                    "y", (double)(i * 50),
                    "date", "2024-03-02T11:00:0" + i
            ));
        }
        return locations;
    }
}
