# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

### Backend (Spring Boot)
```bash
./gradlew bootRun          # 로컬 서버 실행 (port 8080)
./gradlew build            # 빌드
./gradlew test             # 전체 테스트 실행
./gradlew test --tests "com.f1tracker.session.controller.SessionControllerTest"  # 단일 테스트 클래스 실행
```

### Frontend (React + Vite)
```bash
cd frontend
npm install
npm run dev    # 개발 서버 실행 (port 3000)
npm run build  # 프로덕션 빌드
```

### Docker
```bash
docker-compose up -d       # 전체 스택 실행 (app + redis)
docker-compose pull        # 최신 이미지 pull
docker image prune -f      # 미사용 이미지 정리
```

## Architecture

### Overview
F1 공식 라이브 타이밍 SignalR API로 실시간 데이터를 수신하고, WebSocket(STOMP)으로 브라우저에 브로드캐스트하는 구조입니다. OpenF1 API는 트랙 레이아웃, 랩 데이터 등 REST 조회용으로만 사용합니다.

```
F1 Live Timing (livetiming.formula1.com/signalr)
    │  WebSocket push
    └── F1SignalRClient
            ├── Position.z     → LocationBroadcastService → Redis 캐시 + WebSocket /topic/locations/{sessionKey}
            ├── DriverList     → LocationBroadcastService / TeamRadioService 드라이버 캐시 갱신
            ├── TeamRadio      → TeamRadioService → WebSocket /topic/radio/{sessionKey}
            ├── RaceControlMessages → RaceControlService → WebSocket /topic/race-control/{sessionKey}
            └── SessionInfo    → Redis f1:current_session 갱신

OpenF1 API (https://api.openf1.org/v1)  — REST 조회 전용
    ├── /sessions        → SessionController (현재 세션 정보)
    ├── /drivers         → SessionController, SimulationService (드라이버 목록)
    ├── /laps            → TelemetryController (랩 데이터)
    ├── /location        → TrackLayoutService (트랙 레이아웃 PCA), LocationCacheService (리플레이 캐싱)
    └── /sessions?session_type=Race → SessionCacheScheduler (완료 세션 감지)

Replay (과거 레이스 시뮬레이션)
    └── SessionCacheScheduler (매일 새벽 3시)
            └── LocationCacheService → MySQL location_history 저장
    └── SimulationService → DB에서 읽어 /topic/locations/{sessionKey} 재생 (속도 배율 지원)
```

### Backend Package Structure
- `common/client/OpenF1Client` - OpenF1 외부 API 호출 단일 진입점. RestClient 기반 (readTimeout 5분).
- `common/client/F1SignalRClient` - F1 라이브 타이밍 SignalR WebSocket 클라이언트. negotiate → 연결 → 구독 → 자동 재연결.
- `common/config/WebSocketConfig` - STOMP/SockJS 설정. 앱 prefix `/app`, 브로드캐스트 `/topic`
- `session` - 세션/드라이버 정보 조회 (REST)
- `location` - 드라이버 실시간 위치 수신 및 WebSocket 브로드캐스트 (push 방식)
- `telemetry` - 랩 데이터(REST), 팀 라디오/레이스컨트롤 SignalR push 브로드캐스트
- `circuit` - 트랙 레이아웃 (PCA 회전 적용 후 MySQL 캐싱)
- `replay` - 과거 레이스 위치 데이터 캐싱 및 시뮬레이션 재생

### Key Design Decisions
- **SignalR**: F1 라이브 타이밍 SignalR 1.x 프로토콜을 Java `java.net.http.WebSocket`으로 직접 구현. Position.z는 zlib raw deflate 압축.
- **Redis**: 현재 세션키 `f1:current_session`, 드라이버별 최신 위치 `f1:location:{sessionKey}:{driverNumber}` 캐싱.
- **TrackLayout**: PCA 분석으로 트랙 주축을 가로 정렬. 계산 결과는 MySQL에 캐싱 (meetingKey 기준 unique).
- **Replay 캐싱**: 레이스 종료 다음날 새벽 3시 스케줄러가 OpenF1에서 위치 데이터 fetch → MySQL batch insert (500개 단위). 수동 캐싱: `POST /api/v1/replay/cache/{sessionKey}`.
- **Simulation**: DB에서 위치 데이터를 500ms tick으로 읽어 speed 배율(1×/2×/5×)만큼 가상 시간을 진행하며 WebSocket 브로드캐스트.

### Frontend
React + Vite + Tailwind CSS. STOMP over SockJS로 백엔드에 WebSocket 연결. Canvas로 트랙 및 드라이버 위치 시각화.

### Infrastructure
- **배포**: GitHub Actions → Docker Hub → EC2 (main 브랜치 push 시 자동 배포)
- **DB**: RDS MySQL (프로덕션), H2 인메모리 (테스트)
- **프론트엔드**: Vercel (GitHub 연동 자동 배포)


### Branch Naming
이슈 번호를 반드시 포함: `{type}/#{issue-number}-{description}` (예: `feat/#2-lap-chart`)

### Environment Variables
`.env` 파일 필요 (`.env.example` 참고):
- `DOCKERHUB_USERNAME`, `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`
- 테스트 시에는 `src/test/resources/application.yml`의 H2 설정이 자동 적용됨
