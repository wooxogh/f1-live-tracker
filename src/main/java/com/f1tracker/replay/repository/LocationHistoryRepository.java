package com.f1tracker.replay.repository;

import com.f1tracker.replay.domain.LocationHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LocationHistoryRepository extends JpaRepository<LocationHistory, Long> {
    List<LocationHistory> findBySessionKeyOrderByRecordedAt(int sessionKey);
    boolean existsBySessionKey(int sessionKey);
    long countBySessionKey(int sessionKey);
}
