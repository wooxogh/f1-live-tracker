package com.f1tracker.replay.controller;

import com.f1tracker.replay.domain.SessionCacheStatus;
import com.f1tracker.replay.repository.SessionCacheRepository;
import com.f1tracker.replay.service.LocationCacheService;
import com.f1tracker.replay.service.SimulationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/replay")
@RequiredArgsConstructor
public class ReplayController {

    private final SessionCacheRepository sessionCacheRepository;
    private final SimulationService simulationService;
    private final LocationCacheService locationCacheService;

    @GetMapping("/sessions")
    public ResponseEntity<List<SessionCacheStatus>> getCachedSessions() {
        List<SessionCacheStatus> sessions = sessionCacheRepository.findByCachedTrueOrderBySessionDateDesc();
        return ResponseEntity.ok(sessions);
    }

    @PostMapping("/{sessionKey}/start")
    public ResponseEntity<Map<String, Object>> startReplay(
            @PathVariable int sessionKey,
            @RequestParam(defaultValue = "1") int speed) {
        log.info("Starting replay for session {} at speed {}x", sessionKey, speed);
        simulationService.start(sessionKey, speed);
        return ResponseEntity.ok(Map.of(
                "started", true,
                "sessionKey", sessionKey,
                "speed", speed
        ));
    }

    @PostMapping("/stop")
    public ResponseEntity<Map<String, Object>> stopReplay() {
        log.info("Stopping replay");
        simulationService.stop();
        return ResponseEntity.ok(Map.of("stopped", true));
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        return ResponseEntity.ok(Map.of(
                "running", simulationService.isRunning(),
                "sessionKey", simulationService.getCurrentSessionKey()
        ));
    }

    /**
     * 특정 세션 데이터를 수동으로 즉시 캐싱 (스케줄러 대기 없이)
     * 예: POST /api/v1/replay/cache/9165 (멜버른 2026 sessionKey)
     */
    @PostMapping("/cache/{sessionKey}")
    public ResponseEntity<Map<String, Object>> cacheSession(@PathVariable int sessionKey) {
        if (locationCacheService.isCached(sessionKey)) {
            return ResponseEntity.ok(Map.of("cached", true, "message", "Already cached"));
        }
        log.info("Manual cache triggered for session {}", sessionKey);
        new Thread(() -> locationCacheService.cacheSession(sessionKey)).start();
        return ResponseEntity.accepted().body(Map.of(
                "message", "Caching started in background",
                "sessionKey", sessionKey
        ));
    }
}
