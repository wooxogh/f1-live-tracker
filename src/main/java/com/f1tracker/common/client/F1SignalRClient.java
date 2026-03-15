package com.f1tracker.common.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.f1tracker.location.service.LocationBroadcastService;
import com.f1tracker.telemetry.service.RaceControlService;
import com.f1tracker.telemetry.service.TeamRadioService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.net.CookieManager;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.Inflater;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(value = "f1.signalr.enabled", havingValue = "true", matchIfMissing = true)
public class F1SignalRClient {

    private static final String BASE_URL = "https://livetiming.formula1.com/signalr";
    private static final String CONNECTION_DATA = "[{\"name\":\"Streaming\"}]";
    private static final List<String> TOPICS = List.of(
            "Position.z", "DriverList", "RaceControlMessages", "TeamRadio", "SessionInfo"
    );
    private static final String REDIS_SESSION_KEY = "f1:current_session";

    private final LocationBroadcastService locationBroadcastService;
    private final TeamRadioService teamRadioService;
    private final RaceControlService raceControlService;
    private final StringRedisTemplate redisTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private CookieManager cookieManager;
    private HttpClient httpClient;
    private volatile WebSocket webSocket;
    private volatile int sessionKey = -1;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final AtomicBoolean reconnectScheduled = new AtomicBoolean(false);

    @PostConstruct
    public void init() {
        cookieManager = new CookieManager();
        httpClient = HttpClient.newBuilder()
                .cookieHandler(cookieManager)
                .build();
        scheduler.execute(this::connect);
    }

    @PreDestroy
    public void destroy() {
        scheduler.shutdownNow();
        if (webSocket != null) webSocket.abort();
    }

    // ── Connection lifecycle ──────────────────────────────────────────────────

    private void connect() {
        try {
            String token = negotiate();
            connectWebSocket(token);
            reconnectScheduled.set(false); // 연결 성공 시 플래그 해제
        } catch (Exception e) {
            log.error("SignalR connect failed: {}", e.getMessage());
            reconnectScheduled.set(false);
            scheduleReconnect();
        }
    }

    private String negotiate() throws Exception {
        String url = BASE_URL + "/negotiate?clientProtocol=1.5&connectionData="
                + URLEncoder.encode(CONNECTION_DATA, StandardCharsets.UTF_8);

        HttpResponse<String> response = httpClient.send(
                HttpRequest.newBuilder().uri(URI.create(url))
                        .header("User-Agent", "BestHTTP")
                        .GET().build(),
                HttpResponse.BodyHandlers.ofString()
        );

        if (response.statusCode() != 200) {
            log.error("SignalR negotiate failed: HTTP {} body={}", response.statusCode(), response.body());
            throw new IllegalStateException("SignalR negotiate failed with HTTP " + response.statusCode());
        }

        JsonNode json = objectMapper.readTree(response.body());
        JsonNode tokenNode = json.get("ConnectionToken");
        if (tokenNode == null || tokenNode.isNull()) {
            log.error("SignalR negotiate missing ConnectionToken. body={}", response.body());
            throw new IllegalStateException("SignalR negotiate response missing ConnectionToken");
        }
        return tokenNode.asText();
    }

    private void connectWebSocket(String token) throws Exception {
        String wsUrl = "wss://livetiming.formula1.com/signalr/connect"
                + "?clientProtocol=1.5"
                + "&transport=webSockets"
                + "&connectionToken=" + URLEncoder.encode(token, StandardCharsets.UTF_8)
                + "&connectionData=" + URLEncoder.encode(CONNECTION_DATA, StandardCharsets.UTF_8);

        String cookies = cookieManager.getCookieStore().getCookies().stream()
                .map(c -> c.getName() + "=" + c.getValue())
                .reduce((a, b) -> a + "; " + b)
                .orElse("");

        webSocket = httpClient.newWebSocketBuilder()
                .header("User-Agent", "BestHTTP")
                .header("Cookie", cookies)
                .buildAsync(URI.create(wsUrl), new Listener())
                .get(15, TimeUnit.SECONDS);

        log.info("F1 Live Timing connected");
    }

    private void subscribe(WebSocket ws) {
        try {
            String topicsJson = objectMapper.writeValueAsString(TOPICS);
            String msg = String.format(
                    "{\"H\":\"Streaming\",\"M\":\"Subscribe\",\"A\":[%s],\"I\":1}", topicsJson);
            ws.sendText(msg, true);
            log.info("Subscribed to topics: {}", TOPICS);
        } catch (Exception e) {
            log.error("Subscribe failed: {}", e.getMessage());
        }
    }

    private void scheduleReconnect() {
        if (!reconnectScheduled.compareAndSet(false, true)) {
            log.debug("Reconnect already scheduled, skipping duplicate.");
            return;
        }
        if (webSocket != null) {
            webSocket.abort();
            webSocket = null;
        }
        cookieManager.getCookieStore().removeAll();
        log.warn("Reconnecting in 10s...");
        schedule(this::connect, 10);
    }

    private void schedule(Runnable task, int delaySec) {
        scheduler.schedule(task, delaySec, TimeUnit.SECONDS);
    }

    // ── Message handling ──────────────────────────────────────────────────────

    private void handleMessage(String text) {
        if (text.isBlank() || "{}".equals(text.trim())) return; // KeepAlive

        try {
            JsonNode json = objectMapper.readTree(text);

            // Subscription ack: {"I":"1"}
            if (json.has("I") && !json.has("M")) return;

            JsonNode messages = json.get("M");
            if (messages == null || !messages.isArray()) return;

            for (JsonNode msg : messages) {
                if (!"Streaming".equalsIgnoreCase(msg.path("H").asText())) continue;
                if (!"feed".equalsIgnoreCase(msg.path("M").asText())) continue;

                JsonNode args = msg.get("A");
                if (args == null || !args.isArray() || args.size() < 2) continue;

                routeFeed(args.get(0).asText(), args.get(1));
            }
        } catch (Exception e) {
            log.debug("Message parse error: {}", e.getMessage());
        }
    }

    private void routeFeed(String topic, JsonNode data) {
        try {
            switch (topic) {
                case "SessionInfo"        -> handleSessionInfo(data);
                case "DriverList"         -> handleDriverList(data);
                case "Position.z"         -> handlePosition(data);
                case "RaceControlMessages" -> handleRaceControl(data);
                case "TeamRadio"          -> handleTeamRadio(data);
            }
        } catch (Exception e) {
            log.debug("Feed handler error [{}]: {}", topic, e.getMessage());
        }
    }

    // ── Feed handlers ─────────────────────────────────────────────────────────

    private void handleSessionInfo(JsonNode data) {
        if (!data.has("Key")) return;
        int key = data.get("Key").asInt();
        if (this.sessionKey != key) {
            this.sessionKey = key;
            redisTemplate.opsForValue().set(REDIS_SESSION_KEY, String.valueOf(key));
            log.info("Session updated: {}", key);
        }
    }

    private void handleDriverList(JsonNode data) {
        Map<Integer, Map<String, Object>> drivers = new HashMap<>();
        data.fields().forEachRemaining(entry -> {
            try {
                int num = Integer.parseInt(entry.getKey());
                JsonNode d = entry.getValue();
                Map<String, Object> driver = new HashMap<>();
                driver.put("driver_number", num);
                driver.put("name_acronym", d.path("Tla").asText("???"));
                driver.put("team_colour",  d.path("TeamColour").asText("FFFFFF"));
                driver.put("full_name",    d.path("FullName").asText(""));
                drivers.put(num, driver);
            } catch (NumberFormatException ignored) {}
        });
        if (!drivers.isEmpty()) {
            locationBroadcastService.updateDriverCache(drivers);
            teamRadioService.updateDriverCache(drivers);
            log.debug("Driver cache updated: {} drivers", drivers.size());
        }
    }

    private void handlePosition(JsonNode data) {
        if (sessionKey == -1) return;

        String decompressed = decompress(data.asText());
        if (decompressed == null) return;

        try {
            JsonNode posArr = objectMapper.readTree(decompressed).get("Position");
            if (posArr == null || !posArr.isArray() || posArr.isEmpty()) return;

            // 가장 최신 스냅샷만 처리
            JsonNode entries = posArr.get(posArr.size() - 1).get("Entries");
            if (entries == null) return;

            Map<Integer, Map<String, Object>> positions = new HashMap<>();
            entries.fields().forEachRemaining(e -> {
                try {
                    int num = Integer.parseInt(e.getKey());
                    JsonNode p = e.getValue();
                    positions.put(num, Map.of(
                            "x", p.path("X").asDouble(),
                            "y", p.path("Y").asDouble(),
                            "z", p.path("Z").asDouble()
                    ));
                } catch (NumberFormatException ignored) {}
            });

            if (!positions.isEmpty()) {
                locationBroadcastService.onPositionUpdate(sessionKey, positions);
            }
        } catch (Exception e) {
            log.debug("Position.z parse error: {}", e.getMessage());
        }
    }

    private void handleRaceControl(JsonNode data) {
        if (sessionKey == -1) return;
        JsonNode messages = data.get("Messages");
        if (messages == null) return;

        List<Map<String, Object>> entries = new ArrayList<>();
        messages.fields().forEachRemaining(e -> {
            JsonNode m = e.getValue();
            Map<String, Object> entry = new HashMap<>();
            entry.put("date",     m.path("Utc").asText(""));
            entry.put("category", m.has("Category") ? m.get("Category").asText() : null);
            entry.put("flag",     m.has("Flag")     ? m.get("Flag").asText()     : null);
            entry.put("message",  m.has("Message")  ? m.get("Message").asText()  : null);
            entries.add(entry);
        });

        if (!entries.isEmpty()) {
            raceControlService.onRaceControlUpdate(sessionKey, entries);
        }
    }

    private void handleTeamRadio(JsonNode data) {
        if (sessionKey == -1) return;
        JsonNode captures = data.get("Captures");
        if (captures == null || !captures.isArray()) return;

        List<Map<String, Object>> entries = new ArrayList<>();
        captures.forEach(c -> {
            Map<String, Object> entry = new HashMap<>();
            entry.put("date",          c.path("Utc").asText(""));
            entry.put("driver_number", parseIntSafe(c.path("RacingNumber").asText("0")));
            entry.put("recording_url", c.has("Path")
                    ? "https://livetiming.formula1.com/static/" + c.get("Path").asText()
                    : "");
            entries.add(entry);
        });

        if (!entries.isEmpty()) {
            teamRadioService.onTeamRadioUpdate(sessionKey, entries);
        }
    }

    // ── Utilities ─────────────────────────────────────────────────────────────

    private String decompress(String base64) {
        try {
            byte[] compressed = Base64.getDecoder().decode(base64);
            // raw deflate (no zlib header) — F1 live timing 포맷
            Inflater inflater = new Inflater(true);
            inflater.setInput(compressed);
            ByteArrayOutputStream out = new ByteArrayOutputStream(compressed.length * 4);
            byte[] buf = new byte[4096];
            while (!inflater.finished()) {
                int n = inflater.inflate(buf);
                if (n == 0) break;
                out.write(buf, 0, n);
            }
            inflater.end();
            return out.toString(StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.debug("Decompress failed: {}", e.getMessage());
            return null;
        }
    }

    private int parseIntSafe(String s) {
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return 0; }
    }

    // ── WebSocket listener ────────────────────────────────────────────────────

    private class Listener implements WebSocket.Listener {
        private final StringBuilder buf = new StringBuilder();

        @Override
        public void onOpen(WebSocket ws) {
            ws.request(1);
            subscribe(ws);
        }

        @Override
        public CompletionStage<?> onText(WebSocket ws, CharSequence data, boolean last) {
            buf.append(data);
            if (last) {
                handleMessage(buf.toString());
                buf.setLength(0);
            }
            ws.request(1);
            return null;
        }

        @Override
        public CompletionStage<?> onClose(WebSocket ws, int statusCode, String reason) {
            log.warn("SignalR closed [{} {}]", statusCode, reason);
            scheduleReconnect();
            return null;
        }

        @Override
        public void onError(WebSocket ws, Throwable error) {
            log.error("SignalR error: {}", error.getMessage());
            scheduleReconnect();
        }
    }
}
