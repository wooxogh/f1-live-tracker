# F1 Live Tracker

F1 레이스 중 드라이버의 실시간 위치를 Canvas에 시각화하는 웹 애플리케이션입니다.
[OpenF1 API](https://openf1.org)를 통해 위치 데이터를 1초마다 폴링하고, WebSocket(STOMP)으로 브라우저에 실시간 브로드캐스트합니다.

## 아키텍처

```
OpenF1 API
    │ (1초마다 폴링)
    ▼
LocationPollingScheduler
    │
    ▼
LocationBroadcastService ──── Redis (최신 위치 캐시)
    │
    ▼ WebSocket (STOMP)
브라우저 (Canvas 시각화)
```

## 기술 스택

| 구분 | 기술 |
|------|------|
| Language | Java 17 |
| Framework | Spring Boot 3.5.0 |
| Build | Gradle |
| DB | MySQL 8 |
| Cache | Redis |
| Realtime | WebSocket (STOMP / SockJS) |
| External API | OpenF1 API v1 |

## 주요 기능

- **실시간 위치 추적**: OpenF1 API에서 드라이버 GPS 좌표를 1초 간격으로 폴링
- **WebSocket 브로드캐스트**: 전체 드라이버 위치를 `/topic/locations/{sessionKey}` 토픽으로 전송
- **Canvas 시각화**: 팀 컬러로 구분된 드라이버 점과 약어(Acronym) 표시
- **세션 자동 감지**: 5분마다 현재 진행 중인 세션을 자동으로 갱신
- **Redis 캐싱**: 드라이버별 최신 위치를 Redis에 저장 (REST 폴백용)

## 실행 방법

### 사전 요구사항

- JDK 17+
- MySQL 8
- Redis

### 환경 변수

```bash
export DB_PASSWORD=your_mysql_password
```

### DB 설정

```sql
CREATE DATABASE f1tracker CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 실행

```bash
./gradlew bootRun
```

서버 기동 후 http://localhost:8080 접속

## API 엔드포인트

| Method | URL | 설명 |
|--------|-----|------|
| GET | `/api/v1/sessions/current` | 현재 세션 정보 조회 |
| GET | `/api/v1/sessions/{sessionKey}/drivers` | 세션 드라이버 목록 |
| GET | `/api/v1/sessions/{sessionKey}/locations` | Redis에서 최신 위치 조회 (REST 폴백) |

## WebSocket

- **엔드포인트**: `ws://localhost:8080/ws` (SockJS)
- **구독 토픽**: `/topic/locations/{sessionKey}`

### 메시지 형식

```json
{
  "sessionKey": 9999,
  "timestamp": "2025-03-16T14:00:00Z",
  "positions": [
    {
      "driverNumber": 1,
      "nameAcronym": "VER",
      "teamColour": "3671C6",
      "x": 1234.5,
      "y": 567.8,
      "z": 0.0
    }
  ]
}
```

## 설정 (application.yml)

```yaml
openf1:
  base-url: https://api.openf1.org/v1
  poll-interval-ms: 1000  # 위치 폴링 주기 (ms)
```

## 참고

- OpenF1 API는 레이스 세션이 진행 중일 때만 실시간 데이터를 제공합니다.
- 세션이 없는 경우 화면에 "현재 진행 중인 세션 없음" 메시지가 표시됩니다.
- OpenF1 API 공식 문서: https://openf1.org/#introduction
