package com.f1tracker.replay.repository;

import com.f1tracker.replay.domain.SessionCacheStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SessionCacheRepository extends JpaRepository<SessionCacheStatus, Integer> {
    List<SessionCacheStatus> findByCachedTrueOrderBySessionDateDesc();
}
