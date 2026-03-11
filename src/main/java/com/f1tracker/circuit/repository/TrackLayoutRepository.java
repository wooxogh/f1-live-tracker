package com.f1tracker.circuit.repository;

import com.f1tracker.circuit.domain.TrackLayout;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TrackLayoutRepository extends JpaRepository<TrackLayout, Long> {
    Optional<TrackLayout> findByMeetingKey(Integer meetingKey);
}
