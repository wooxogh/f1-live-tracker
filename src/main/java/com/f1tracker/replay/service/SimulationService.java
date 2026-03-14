package com.f1tracker.replay.service;

import com.f1tracker.common.client.OpenF1Client;
import com.f1tracker.location.dto.DriverLocationMessage;
import com.f1tracker.replay.domain.LocationHistory;
import com.f1tracker.replay.repository.LocationHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
@RequiredArgsConstructor
public class SimulationService {

    private final LocationHistoryRepository locationRepo;
    private final OpenF1Client openF1Client;
    private final SimpMessagingTemplate messaging;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private volatile Thread simulationThread;
    private volatile int currentSessionKey = -1;

    public boolean isRunning() { return running.get(); }
    public int getCurrentSessionKey() { return currentSessionKey; }

    public void start(int sessionKey, int speed) {
        stop();
        running.set(true);
        currentSessionKey = sessionKey;
        simulationThread = new Thread(() -> runSimulation(sessionKey, speed));
        simulationThread.setDaemon(true);
        simulationThread.start();
    }

    public void stop() {
        running.set(false);
        currentSessionKey = -1;
        if (simulationThread != null) {
            simulationThread.interrupt();
            simulationThread = null;
        }
    }

    private void runSimulation(int sessionKey, int speed) {
        // 1. 드라이버 정보 로드
        Map<Integer, Map<String, Object>> driverCache = new HashMap<>();
        List<Map<String, Object>> drivers = openF1Client.getDrivers(sessionKey);
        if (drivers != null) {
            drivers.forEach(d -> {
                try {
                    int num = Integer.parseInt(String.valueOf(d.get("driver_number")).replace(".0", ""));
                    driverCache.put(num, d);
                } catch (NumberFormatException ignored) {}
            });
        }

        // 2. 위치 데이터 로드
        List<LocationHistory> data = locationRepo.findBySessionKeyOrderByRecordedAt(sessionKey);
        if (data.isEmpty()) { running.set(false); return; }

        log.info("Simulation start: session={}, rows={}, speed={}x", sessionKey, data.size(), speed);

        Instant virtualTime = data.get(0).getRecordedAt();
        Instant endTime = data.get(data.size() - 1).getRecordedAt();
        long tickMs = 500L; // 500ms 실시간 간격
        long virtualTickMs = tickMs * speed; // 가상 시간 진행량

        int cursor = 0;

        while (running.get() && virtualTime.isBefore(endTime)) {
            Instant nextVirtualTime = virtualTime.plusMillis(virtualTickMs);

            // 이번 tick 내 각 드라이버 최신 위치 수집
            Map<Integer, LocationHistory> snapshot = new HashMap<>();
            while (cursor < data.size() &&
                   !data.get(cursor).getRecordedAt().isAfter(nextVirtualTime)) {
                LocationHistory loc = data.get(cursor);
                snapshot.put(loc.getDriverNumber(), loc);
                cursor++;
            }

            if (!snapshot.isEmpty()) {
                broadcastSnapshot(sessionKey, snapshot, driverCache);
            }

            virtualTime = nextVirtualTime;

            try { Thread.sleep(tickMs); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); break; }
        }

        running.set(false);
        log.info("Simulation finished: session={}", sessionKey);
    }

    private void broadcastSnapshot(int sessionKey,
                                   Map<Integer, LocationHistory> snapshot,
                                   Map<Integer, Map<String, Object>> driverCache) {
        List<DriverLocationMessage.DriverPosition> positions = snapshot.entrySet().stream()
                .map(e -> {
                    int num = e.getKey();
                    LocationHistory loc = e.getValue();
                    Map<String, Object> driver = driverCache.get(num);
                    return DriverLocationMessage.DriverPosition.builder()
                            .driverNumber(num)
                            .nameAcronym(driver != null ? String.valueOf(driver.get("name_acronym")) : "???")
                            .teamColour(driver != null ? String.valueOf(driver.get("team_colour")) : "FFFFFF")
                            .x((double) loc.getX())
                            .y((double) loc.getY())
                            .z((double) loc.getZ())
                            .build();
                })
                .toList();

        messaging.convertAndSend("/topic/locations/" + sessionKey,
                DriverLocationMessage.builder()
                        .sessionKey(sessionKey)
                        .timestamp(Instant.now())
                        .positions(positions)
                        .build());
    }
}
