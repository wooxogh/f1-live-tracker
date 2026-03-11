package com.f1tracker.circuit.service;

import com.f1tracker.circuit.domain.TrackLayout;
import com.f1tracker.circuit.repository.TrackLayoutRepository;
import com.f1tracker.common.client.OpenF1Client;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrackLayoutService {

    private final TrackLayoutRepository repository;
    private final OpenF1Client openF1Client;
    private final ObjectMapper objectMapper;

    private static final DateTimeFormatter ISO_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS").withZone(ZoneOffset.UTC);

    /**
     * DB에 있으면 즉시 반환, 없으면 OpenF1에서 fetch 후 저장
     */
    public List<Map<String, Object>> getTrackPoints(int sessionKey) {
        Map<String, Object> session = openF1Client.getSessionByKey(sessionKey);
        if (session == null) return List.of();

        int meetingKey = Integer.parseInt(String.valueOf(session.get("meeting_key")).replace(".0", ""));
        String circuitShortName = String.valueOf(session.get("circuit_short_name"));

        return repository.findByMeetingKey(meetingKey)
                .map(layout -> {
                    log.info("Track layout loaded from DB: {} (rotation: {}°)",
                            circuitShortName, layout.getOptimalRotationDeg());
                    return deserialize(layout.getPointsJson());
                })
                .orElseGet(() -> fetchAndSave(sessionKey, session, meetingKey, circuitShortName));
    }

    private List<Map<String, Object>> fetchAndSave(int sessionKey, Map<String, Object> session,
                                                    int meetingKey, String circuitShortName) {
        List<Map<String, Object>> drivers = openF1Client.getDrivers(sessionKey);
        if (drivers == null || drivers.isEmpty()) return List.of();

        int driverNumber = Integer.parseInt(
                String.valueOf(drivers.get(0).get("driver_number")).replace(".0", ""));

        String dateStart = String.valueOf(session.get("date_start"));
        String normalized = dateStart.substring(0, Math.min(19, dateStart.length())) + "Z";
        Instant start = Instant.parse(normalized);
        String dateFrom = ISO_FMT.format(start);
        String dateTo = ISO_FMT.format(start.plus(2, ChronoUnit.MINUTES));

        List<Map<String, Object>> locations = openF1Client.getLocationsByDriver(
                sessionKey, driverNumber, dateFrom, dateTo);
        if (locations == null || locations.isEmpty()) return List.of();

        List<Map<String, Object>> sorted = locations.stream()
                .sorted(Comparator.comparing(loc -> String.valueOf(loc.get("date"))))
                .toList();

        List<double[]> rawPoints = new ArrayList<>();
        for (int i = 0; i < sorted.size(); i += 3) {
            Map<String, Object> loc = sorted.get(i);
            double x = toDouble(loc.get("x"));
            double y = toDouble(loc.get("y"));
            rawPoints.add(new double[]{x, y});
        }

        if (rawPoints.isEmpty()) return List.of();

        // PCA로 최적 회전각 계산 후 포인트에 미리 적용
        double rotationDeg = calcOptimalRotation(rawPoints);
        List<Map<String, Object>> trackPoints = applyRotation(rawPoints, rotationDeg);

        try {
            String json = objectMapper.writeValueAsString(trackPoints);
            repository.save(TrackLayout.builder()
                    .meetingKey(meetingKey)
                    .circuitShortName(circuitShortName)
                    .pointsJson(json)
                    .optimalRotationDeg(rotationDeg)
                    .createdAt(Instant.now())
                    .build());
            log.info("Track layout saved to DB: {} ({} points, rotation: {}°)",
                    circuitShortName, trackPoints.size(), String.format("%.1f", rotationDeg));
        } catch (Exception e) {
            log.error("Failed to save track layout: {}", e.getMessage());
        }

        return trackPoints;
    }

    /**
     * PCA(주성분 분석)로 트랙 포인트의 주축 각도를 계산
     * → 주축이 가로가 되도록 회전하면 캔버스 공간을 최대한 활용
     */
    private double calcOptimalRotation(List<double[]> points) {
        int n = points.size();
        double meanX = 0, meanY = 0;
        for (double[] p : points) { meanX += p[0]; meanY += p[1]; }
        meanX /= n; meanY /= n;

        // 공분산 행렬 계산
        double cxx = 0, cxy = 0, cyy = 0;
        for (double[] p : points) {
            double dx = p[0] - meanX;
            double dy = p[1] - meanY;
            cxx += dx * dx;
            cxy += dx * dy;
            cyy += dy * dy;
        }

        // 주축 각도 (PCA 첫 번째 고유벡터 방향)
        double angle = 0.5 * Math.atan2(2 * cxy, cxx - cyy);
        return Math.toDegrees(angle);
    }

    /**
     * 포인트 리스트에 회전 적용 후 Map 리스트로 변환
     */
    private List<Map<String, Object>> applyRotation(List<double[]> points, double angleDeg) {
        double rad = Math.toRadians(-angleDeg); // 주축을 가로로 맞추려면 음수 회전
        double cos = Math.cos(rad);
        double sin = Math.sin(rad);

        List<Map<String, Object>> result = new ArrayList<>();
        for (double[] p : points) {
            double rx = p[0] * cos - p[1] * sin;
            double ry = p[0] * sin + p[1] * cos;
            result.add(Map.of("x", rx, "y", ry));
        }
        return result;
    }

    private double toDouble(Object val) {
        if (val == null) return 0.0;
        try { return Double.parseDouble(String.valueOf(val)); }
        catch (NumberFormatException e) { return 0.0; }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> deserialize(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            log.error("Failed to deserialize track points: {}", e.getMessage());
            return List.of();
        }
    }
}
