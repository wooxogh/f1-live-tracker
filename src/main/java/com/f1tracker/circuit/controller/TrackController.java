package com.f1tracker.circuit.controller;

import com.f1tracker.circuit.service.TrackLayoutService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class TrackController {

    private final TrackLayoutService trackLayoutService;

    @GetMapping("/sessions/{sessionKey}/track")
    public ResponseEntity<List<Map<String, Object>>> getTrackLayout(@PathVariable int sessionKey) {
        List<Map<String, Object>> points = trackLayoutService.getTrackPoints(sessionKey);
        return ResponseEntity.ok(points);
    }
}
