package com.f1tracker.telemetry.controller;

import com.f1tracker.common.client.OpenF1Client;
import com.f1tracker.telemetry.service.TeamRadioService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TelemetryController.class)
class TelemetryControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean OpenF1Client openF1Client;
    @MockBean TeamRadioService teamRadioService;

    @Test
    @DisplayName("최신 랩 조회 - 드라이버별 최신 랩 반환")
    void getLatestLaps_returnsLatestPerDriver() throws Exception {
        List<Map<String, Object>> laps = List.of(
                Map.of("driver_number", 1, "lap_number", 10, "lap_duration", 95.123),
                Map.of("driver_number", 1, "lap_number", 12, "lap_duration", 94.567),
                Map.of("driver_number", 44, "lap_number", 11, "lap_duration", 96.000)
        );
        given(openF1Client.getLapsForSession(anyInt())).willReturn(laps);

        mockMvc.perform(get("/api/v1/sessions/9158/laps/latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$['1'].lap_number").value(12))   // VER 최신 랩
                .andExpect(jsonPath("$['44'].lap_number").value(11)); // HAM 최신 랩
    }

    @Test
    @DisplayName("최신 랩 조회 - 랩 데이터 없을 때 빈 맵 반환")
    void getLatestLaps_noData_returnsEmptyMap() throws Exception {
        given(openF1Client.getLapsForSession(anyInt())).willReturn(List.of());

        mockMvc.perform(get("/api/v1/sessions/9158/laps/latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @DisplayName("팀 라디오 조회 - 최신 20개 반환")
    void getRecentRadio_returnsLast20() throws Exception {
        List<Map<String, Object>> radio = List.of(
                Map.of("driver_number", 1, "date", "2024-03-02T14:00:00", "recording_url", "url1"),
                Map.of("driver_number", 44, "date", "2024-03-02T14:01:00", "recording_url", "url2")
        );
        given(openF1Client.getTeamRadio(anyInt(), isNull())).willReturn(radio);

        mockMvc.perform(get("/api/v1/sessions/9158/radio/recent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].driver_number").value(1));
    }

    @Test
    @DisplayName("팀 라디오 조회 - 라디오 없을 때 빈 리스트 반환")
    void getRecentRadio_noData_returnsEmptyList() throws Exception {
        given(openF1Client.getTeamRadio(anyInt(), isNull())).willReturn(List.of());

        mockMvc.perform(get("/api/v1/sessions/9158/radio/recent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @DisplayName("팀 라디오 조회 - 20개 초과 시 마지막 20개만 반환")
    void getRecentRadio_over20_returnsLast20() throws Exception {
        List<Map<String, Object>> radio = new java.util.ArrayList<>();
        for (int i = 1; i <= 25; i++) {
            radio.add(Map.of("driver_number", i, "date", "2024-03-02T14:00:0" + (i % 10)));
        }
        given(openF1Client.getTeamRadio(anyInt(), isNull())).willReturn(radio);

        mockMvc.perform(get("/api/v1/sessions/9158/radio/recent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(20));
    }
}
