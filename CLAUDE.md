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
OpenF1 API(외부)를 폴링하여 F1 레이스 데이터를 수집하고, WebSocket(STOMP)으로 브라우저에 실시간 브로드캐스트하는 구조입니다.

```
OpenF1 API (https://api.openf1.org/v1)
    │
    ├── LocationPollingScheduler (3초마다)
    │       └── LocationBroadcastService → Redis 캐시 + WebSocket /topic/locations/{sessionKey}
    │
    └── TelemetryScheduler (15초마다)
            └── TeamRadioService → WebSocket /topic/radio/{sessionKey}
```

### Backend Package Structure
- `common/client/OpenF1Client` - 모든 외부 API 호출의 단일 진입점. RestClient 기반.
- `common/config/WebSocketConfig` - STOMP/SockJS 설정. 앱 prefix `/app`, 브로드캐스트 `/topic`
- `session` - 세션/드라이버 정보 조회 (REST)
- `location` - 드라이버 실시간 위치 폴링 및 WebSocket 브로드캐스트
- `telemetry` - 랩 데이터 및 팀 라디오 폴링 및 브로드캐스트
- `circuit` - 트랙 레이아웃 (PCA 회전 적용 후 MySQL 캐싱)

### Key Design Decisions
- **Redis**: 드라이버별 최신 위치를 `f1:location:{sessionKey}:{driverNumber}` 키로 캐싱. REST 폴백용.
- **TrackLayout**: PCA 분석으로 트랙 주축을 가로 정렬. 계산 결과는 MySQL에 캐싱 (meetingKey 기준 unique).
- **OpenF1 rate limit**: 30req/min. 폴링 주기를 `openf1.poll-interval-ms`로 조정 가능.
- **세션 감지**: 5분마다 현재 세션을 자동 갱신. 세션 없으면 스케줄러가 조기 종료.

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
