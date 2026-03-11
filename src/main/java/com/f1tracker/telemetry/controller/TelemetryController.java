package com.f1tracker.telemetry.controller;

import com.f1tracker.common.client.OpenF1Client;
import com.f1tracker.telemetry.dto.TeamRadioMessage;
import com.f1tracker.telemetry.service.TeamRadioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class TelemetryController {

    private final OpenF1Client openF1Client;
    private final TeamRadioService teamRadioService;

    /**
     * 세션의 드라이버별 최신 랩 데이터 반환
     * key: driverNumber, value: 해당 드라이버의 가장 최근 랩
     */
    @GetMapping("/sessions/{sessionKey}/laps/latest")
    public ResponseEntity<Map<Integer, Map<String, Object>>> getLatestLaps(@PathVariable int sessionKey) {
        List<Map<String, Object>> allLaps = openF1Client.getLapsForSession(sessionKey);

        if (allLaps == null || allLaps.isEmpty()) return ResponseEntity.ok(Map.of());

        // 드라이버별 가장 높은 lap_number를 가진 랩만 추출
        Map<Integer, Map<String, Object>> latestByDriver = new LinkedHashMap<>();
        for (Map<String, Object> lap : allLaps) {
            Object driverNumObj = lap.get("driver_number");
            Object lapNumObj   = lap.get("lap_number");
            if (driverNumObj == null || lapNumObj == null) continue;

            int driverNumber = Integer.parseInt(String.valueOf(driverNumObj).replace(".0", ""));
            int lapNumber    = Integer.parseInt(String.valueOf(lapNumObj).replace(".0", ""));

            Map<String, Object> existing = latestByDriver.get(driverNumber);
            if (existing == null) {
                latestByDriver.put(driverNumber, lap);
            } else {
                int existingLap = Integer.parseInt(String.valueOf(existing.get("lap_number")).replace(".0", ""));
                if (lapNumber > existingLap) {
                    latestByDriver.put(driverNumber, lap);
                }
            }
        }

        return ResponseEntity.ok(latestByDriver);
    }

    /**
     * 세션의 최근 팀 라디오 메시지 반환 (초기 로드용)
     */
    @GetMapping("/sessions/{sessionKey}/radio/recent")
    public ResponseEntity<List<Map<String, Object>>> getRecentRadio(@PathVariable int sessionKey) {
        List<Map<String, Object>> radio = openF1Client.getTeamRadio(sessionKey, null);
        if (radio == null || radio.isEmpty()) return ResponseEntity.ok(List.of());

        // 최신 20개만 반환
        int fromIdx = Math.max(0, radio.size() - 20);
        return ResponseEntity.ok(radio.subList(fromIdx, radio.size()));
    }

    /**
     * 세션의 레이스 컨트롤 메시지 반환 (초기 로드용)
     */
    @GetMapping("/sessions/{sessionKey}/race-control/recent")
    public ResponseEntity<List<Map<String, Object>>> getRecentRaceControl(@PathVariable int sessionKey) {
        List<Map<String, Object>> data = openF1Client.getRaceControl(sessionKey, null);
        if (data == null || data.isEmpty()) return ResponseEntity.ok(List.of());

        int fromIdx = Math.max(0, data.size() - 20);
        return ResponseEntity.ok(data.subList(fromIdx, data.size()));
    }
}
