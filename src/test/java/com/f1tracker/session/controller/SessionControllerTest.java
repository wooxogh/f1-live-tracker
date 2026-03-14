package com.f1tracker.session.controller;

import com.f1tracker.common.client.OpenF1Client;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SessionController.class)
class SessionControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean OpenF1Client openF1Client;
    @MockBean StringRedisTemplate redisTemplate;

    @Test
    @DisplayName("현재 세션 조회 - 세션 있을 때 200 반환")
    void getCurrentSession_returnsSession() throws Exception {
        Map<String, Object> session = Map.of(
                "session_key", 9158,
                "session_name", "Race",
                "location", "Bahrain"
        );
        ValueOperations<String, String> valueOps = org.mockito.Mockito.mock(ValueOperations.class);
        given(redisTemplate.opsForValue()).willReturn(valueOps);
        given(valueOps.get("f1:current_session")).willReturn(null); // Redis miss → fallback
        given(openF1Client.getLatestSession()).willReturn(session);

        mockMvc.perform(get("/api/v1/sessions/current"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.session_key").value(9158))
                .andExpect(jsonPath("$.session_name").value("Race"))
                .andExpect(jsonPath("$.location").value("Bahrain"));
    }

    @Test
    @DisplayName("현재 세션 조회 - 세션 없을 때 204 반환")
    void getCurrentSession_noSession_returns204() throws Exception {
        ValueOperations<String, String> valueOps = org.mockito.Mockito.mock(ValueOperations.class);
        given(redisTemplate.opsForValue()).willReturn(valueOps);
        given(valueOps.get("f1:current_session")).willReturn(null); // Redis miss → fallback
        given(openF1Client.getLatestSession()).willReturn(null);

        mockMvc.perform(get("/api/v1/sessions/current"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("드라이버 목록 조회 - 드라이버 반환")
    void getDrivers_returnsDriverList() throws Exception {
        List<Map<String, Object>> drivers = List.of(
                Map.of("driver_number", 1, "name_acronym", "VER", "team_name", "Red Bull Racing"),
                Map.of("driver_number", 44, "name_acronym", "HAM", "team_name", "Ferrari")
        );
        given(openF1Client.getDrivers(anyInt())).willReturn(drivers);

        mockMvc.perform(get("/api/v1/sessions/9158/drivers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name_acronym").value("VER"))
                .andExpect(jsonPath("$[1].name_acronym").value("HAM"));
    }

    @Test
    @DisplayName("위치 캐시 조회 - Redis에 데이터 없을 때 빈 맵 반환")
    void getLocations_noCache_returnsEmptyMap() throws Exception {
        given(redisTemplate.keys("f1:location:9158:*")).willReturn(Set.of());

        mockMvc.perform(get("/api/v1/sessions/9158/locations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @DisplayName("위치 캐시 조회 - Redis에 데이터 있을 때 반환")
    void getLocations_withCache_returnsData() throws Exception {
        String key = "f1:location:9158:1";
        ValueOperations<String, String> valueOps = org.mockito.Mockito.mock(ValueOperations.class);

        given(redisTemplate.keys("f1:location:9158:*")).willReturn(Set.of(key));
        given(redisTemplate.opsForValue()).willReturn(valueOps);
        given(valueOps.get(key)).willReturn("{\"x\":1000,\"y\":2000}");

        mockMvc.perform(get("/api/v1/sessions/9158/locations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$['f1:location:9158:1']").exists());
    }
}
