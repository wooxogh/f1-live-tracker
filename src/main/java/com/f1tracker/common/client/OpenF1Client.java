package com.f1tracker.common.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OpenF1Client {

    private final RestClient openF1RestClient;

    public Map<String, Object> getLatestSession() {
        List<Map<String, Object>> sessions = openF1RestClient.get()
                .uri("/sessions?session_key=latest")
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
        if (sessions == null || sessions.isEmpty()) return null;
        return sessions.get(0);
    }

    public Map<String, Object> getSessionByKey(int sessionKey) {
        try {
            List<Map<String, Object>> sessions = openF1RestClient.get()
                    .uri("/sessions?session_key={key}", sessionKey)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            if (sessions == null || sessions.isEmpty()) return null;
            return sessions.get(0);
        } catch (Exception e) {
            log.error("Failed to fetch session {}: {}", sessionKey, e.getMessage());
            return null;
        }
    }

    public List<Map<String, Object>> getDrivers(int sessionKey) {
        return openF1RestClient.get()
                .uri("/drivers?session_key={key}", sessionKey)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    public List<Map<String, Object>> getLocationsByDriver(int sessionKey, int driverNumber,
                                                           String dateFrom, String dateTo) {
        try {
            return openF1RestClient.get()
                    .uri("/location?session_key={key}&driver_number={driver}&date>={from}&date<={to}",
                            sessionKey, driverNumber, dateFrom, dateTo)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
        } catch (Exception e) {
            log.error("Failed to fetch track layout for session {}: {}", sessionKey, e.getMessage());
            return List.of();
        }
    }

    public List<Map<String, Object>> getLatestLocations(int sessionKey, String afterDate) {
        try {
            return openF1RestClient.get()
                    .uri("/location?session_key={key}&date>={date}", sessionKey, afterDate)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
        } catch (Exception e) {
            log.debug("No location data for session {} ({})", sessionKey, e.getMessage());
            return List.of();
        }
    }

    public List<Map<String, Object>> getLaps(int sessionKey, int driverNumber) {
        try {
            return openF1RestClient.get()
                    .uri("/laps?session_key={key}&driver_number={driver}", sessionKey, driverNumber)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
        } catch (Exception e) {
            log.debug("No lap data for session {} driver {} ({})", sessionKey, driverNumber, e.getMessage());
            return List.of();
        }
    }

    public List<Map<String, Object>> getTeamRadio(int sessionKey, String afterDate) {
        try {
            if (afterDate != null) {
                return openF1RestClient.get()
                        .uri("/team_radio?session_key={key}&date>={date}", sessionKey, afterDate)
                        .retrieve()
                        .body(new ParameterizedTypeReference<>() {});
            }
            return openF1RestClient.get()
                    .uri("/team_radio?session_key={key}", sessionKey)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
        } catch (Exception e) {
            log.debug("No team radio for session {} ({})", sessionKey, e.getMessage());
            return List.of();
        }
    }

    public List<Map<String, Object>> getLapsForSession(int sessionKey) {
        try {
            return openF1RestClient.get()
                    .uri("/laps?session_key={key}", sessionKey)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
        } catch (Exception e) {
            log.debug("No lap data for session {} ({})", sessionKey, e.getMessage());
            return List.of();
        }
    }

    public List<Map<String, Object>> getRaceControl(int sessionKey, String afterDate) {
        try {
            if (afterDate != null) {
                return openF1RestClient.get()
                        .uri("/race_control?session_key={key}&date>={date}", sessionKey, afterDate)
                        .retrieve()
                        .body(new ParameterizedTypeReference<>() {});
            }
            return openF1RestClient.get()
                    .uri("/race_control?session_key={key}", sessionKey)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
        } catch (Exception e) {
            log.debug("No race control data for session {} ({})", sessionKey, e.getMessage());
            return List.of();
        }
    }
}
