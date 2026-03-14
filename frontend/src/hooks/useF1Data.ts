import { useState, useEffect, useRef, useCallback } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { Session, TrackPoint, DriverPosition, LapData, RadioEntry, RaceControlEntry, ReplaySession } from '../types';
import { DRIVERS_2026 } from '../lib/drivers';

export type ConnectionStatus = 'CONNECTING' | 'LIVE' | 'DISCONNECTED' | 'OFF_SEASON';

function segmentsToStatus(segments: number[] | null): 'purple' | 'green' | 'yellow' | null {
  if (!segments || segments.length === 0) return null;
  if (segments.some(s => s === 2051)) return 'purple';
  if (segments.some(s => s === 2049)) return 'green';
  return 'yellow';
}

export function useF1Data() {
  const [status, setStatus]               = useState<ConnectionStatus>('CONNECTING');
  const [session, setSession]             = useState<Session | null>(null);
  const [track, setTrack]                 = useState<TrackPoint[] | null>(null);
  const [positions, setPositions]         = useState<Record<number, DriverPosition>>({});
  const [lapData, setLapData]             = useState<Record<number, LapData>>({});
  const [radioEntries, setRadioEntries]       = useState<RadioEntry[]>([]);
  const [raceControlEntries, setRaceControlEntries] = useState<RaceControlEntry[]>([]);
  const [loadingMessage, setLoadingMessage] = useState<string>('세션 정보 불러오는 중...');
  const [mode, setMode]                   = useState<'live' | 'replay'>('live');
  const [replaySessions, setReplaySessions] = useState<ReplaySession[]>([]);
  const [speed, setSpeed]                 = useState<number>(1);

  const stompClientRef   = useRef<Client | null>(null);
  const lapPollRef       = useRef<ReturnType<typeof setInterval> | null>(null);
  const liveSessionRef   = useRef<Session | null>(null);
  const liveTrackRef     = useRef<TrackPoint[] | null>(null);
  const replaySubRef     = useRef<{ unsubscribe: () => void } | null>(null);

  // Fetch available replay sessions on mount
  useEffect(() => {
    fetch('/api/v1/replay/sessions')
      .then(res => res.ok ? res.json() : [])
      .then((data: Array<Record<string, unknown>>) => {
        const sessions: ReplaySession[] = data.map(s => ({
          sessionKey:  s.sessionKey  as number,
          sessionName: s.sessionName as string,
          meetingName: s.meetingName as string,
          location:    s.location    as string,
          countryName: s.countryName as string,
          sessionDate: s.sessionDate as string,
        }));
        setReplaySessions(sessions);
      })
      .catch(() => { /* ignore */ });
  }, []);

  useEffect(() => {
    let isMounted = true;

    const fetchInitialData = async () => {
      try {
        setLoadingMessage('세션 정보 불러오는 중...');
        const sessionRes = await fetch('/api/v1/sessions/current');
        if (!sessionRes.ok) throw new Error('No session');
        const sessionData: Session = await sessionRes.json();
        if (!isMounted) return;
        setSession(sessionData);
        liveSessionRef.current = sessionData;

        setLoadingMessage('트랙 데이터 불러오는 중...');
        const trackRes = await fetch(`/api/v1/sessions/${sessionData.session_key}/track`);
        const trackData: TrackPoint[] = await trackRes.json();
        if (!isMounted) return;
        if (!trackData || trackData.length === 0) throw new Error('No track data');
        setTrack(trackData);
        liveTrackRef.current = trackData;

        // 팀 라디오 + 레이스 컨트롤 초기 로드
        fetchRecentRadio(sessionData.session_key);
        fetchRecentRaceControl(sessionData.session_key);

        setLoadingMessage('실시간 데이터 연결 중...');
        connectWebSocket(sessionData.session_key);
        startLapPolling(sessionData.session_key);

      } catch {
        if (!isMounted) return;
        setStatus('OFF_SEASON');
        setLoadingMessage('현재 진행 중인 세션이 없습니다.');
      }
    };

    const fetchRecentRadio = async (sessionKey: string) => {
      try {
        const res = await fetch(`/api/v1/sessions/${sessionKey}/radio/recent`);
        if (!res.ok || !isMounted) return;
        const data = await res.json();
        const entries: RadioEntry[] = data.map((r: Record<string, unknown>) => {
          const driverNumber = r.driver_number as number;
          const fallback = DRIVERS_2026.find(d => d.driverNumber === driverNumber);
          return {
            driverNumber,
            nameAcronym:  r.name_acronym as string ?? fallback?.nameAcronym ?? '???',
            teamColour:   r.team_colour  as string ?? fallback?.teamColour  ?? 'FFFFFF',
            date:         r.date         as string,
            recordingUrl: r.recording_url as string,
          };
        });
        if (isMounted) setRadioEntries(entries.reverse()); // 최신순
      } catch { /* ignore */ }
    };

    const fetchRecentRaceControl = async (sessionKey: string) => {
      try {
        const res = await fetch(`/api/v1/sessions/${sessionKey}/race-control/recent`);
        if (!res.ok || !isMounted) return;
        const data: Record<string, unknown>[] = await res.json();
        const entries: RaceControlEntry[] = data.map(r => ({
          date:     r.date     as string,
          category: r.category as string | null,
          flag:     r.flag     as string | null,
          message:  r.message  as string | null,
        }));
        if (isMounted) setRaceControlEntries(entries.reverse());
      } catch { /* ignore */ }
    };

    const connectWebSocket = (sessionKey: string) => {
      const socket = new SockJS('/ws');
      const client = new Client({
        webSocketFactory: () => socket as WebSocket,
        reconnectDelay: 5000,
        onConnect: () => {
          if (!isMounted) return;
          setStatus('LIVE');

          client.subscribe(`/topic/locations/${sessionKey}`, (message) => {
            const data = JSON.parse(message.body);
            updatePositions(data.positions);
          });

          client.subscribe(`/topic/radio/${sessionKey}`, (message) => {
            const data = JSON.parse(message.body);
            if (!data.entries?.length) return;
            setRadioEntries(prev => [...data.entries.reverse(), ...prev].slice(0, 50));
          });

          client.subscribe(`/topic/race-control/${sessionKey}`, (message) => {
            const data = JSON.parse(message.body);
            if (!data.entries?.length) return;
            setRaceControlEntries(prev => [...data.entries.reverse(), ...prev].slice(0, 50));
          });
        },
        onDisconnect: () => { if (isMounted) setStatus('DISCONNECTED'); },
        onWebSocketClose: () => { if (isMounted) setStatus('DISCONNECTED'); },
      });
      client.activate();
      stompClientRef.current = client;
    };

    const startLapPolling = (sessionKey: string) => {
      const poll = async () => {
        try {
          const res = await fetch(`/api/v1/sessions/${sessionKey}/laps/latest`);
          if (!res.ok || !isMounted) return;
          const raw: Record<string, Record<string, unknown>> = await res.json();

          const parsed: Record<number, LapData> = {};
          Object.entries(raw).forEach(([driverNum, lap]) => {
            const num = parseInt(driverNum);
            const s1 = lap.segments_sector_1 as number[] | null;
            const s2 = lap.segments_sector_2 as number[] | null;
            const s3 = lap.segments_sector_3 as number[] | null;
            parsed[num] = {
              driver_number:     num,
              lap_number:        lap.lap_number        as number | null,
              duration_sector_1: lap.duration_sector_1 as number | null,
              duration_sector_2: lap.duration_sector_2 as number | null,
              duration_sector_3: lap.duration_sector_3 as number | null,
              lap_duration:      lap.lap_duration      as number | null,
              segments_sector_1: s1,
              segments_sector_2: s2,
              segments_sector_3: s3,
              s1_status: segmentsToStatus(s1),
              s2_status: segmentsToStatus(s2),
              s3_status: segmentsToStatus(s3),
            };
          });

          if (isMounted) setLapData(parsed);
        } catch { /* ignore */ }
      };

      poll();
      lapPollRef.current = setInterval(poll, 5000);
    };

    const updatePositions = (newPositions: DriverPosition[]) => {
      setPositions(prev => {
        const next = { ...prev };
        newPositions.forEach(p => { next[p.driverNumber] = p; });
        return next;
      });
    };

    fetchInitialData();

    return () => {
      isMounted = false;
      stompClientRef.current?.deactivate();
      if (lapPollRef.current) clearInterval(lapPollRef.current);
    };
  }, []);

  const startReplay = useCallback(async (sessionKey: number, replaySpeed: number) => {
    // Start backend simulation
    await fetch(`/api/v1/replay/${sessionKey}/start?speed=${replaySpeed}`, { method: 'POST' });

    // Load track data for replay session
    try {
      const trackRes = await fetch(`/api/v1/sessions/${sessionKey}/track`);
      if (trackRes.ok) {
        const trackData: TrackPoint[] = await trackRes.json();
        if (trackData && trackData.length > 0) setTrack(trackData);
      }
    } catch { /* ignore */ }

    // Update session info from replaySessions list
    const replayMeta = replaySessions.find(s => s.sessionKey === sessionKey);
    if (replayMeta) {
      setSession({
        session_key:  String(replayMeta.sessionKey),
        session_name: replayMeta.sessionName,
        location:     replayMeta.location,
        country_name: replayMeta.countryName,
      });
    }

    // Unsubscribe previous replay subscription if any
    if (replaySubRef.current) {
      replaySubRef.current.unsubscribe();
      replaySubRef.current = null;
    }

    // Subscribe to replay session topic via existing STOMP client
    const client = stompClientRef.current;
    if (client && client.connected) {
      const sub = client.subscribe(`/topic/locations/${sessionKey}`, (message) => {
        const data = JSON.parse(message.body);
        setPositions(() => {
          const next: Record<number, DriverPosition> = {};
          (data.positions as DriverPosition[]).forEach(p => { next[p.driverNumber] = p; });
          return next;
        });
      });
      replaySubRef.current = sub;
    }

    setSpeed(replaySpeed);
    setMode('replay');
  }, [replaySessions]);

  const stopReplay = useCallback(async () => {
    await fetch('/api/v1/replay/stop', { method: 'POST' });

    // Unsubscribe replay subscription
    if (replaySubRef.current) {
      replaySubRef.current.unsubscribe();
      replaySubRef.current = null;
    }

    // Restore live session data
    if (liveSessionRef.current) setSession(liveSessionRef.current);
    if (liveTrackRef.current) setTrack(liveTrackRef.current);
    setPositions({});
    setMode('live');
  }, []);

  return {
    status,
    session,
    track,
    positions,
    lapData,
    radioEntries,
    raceControlEntries,
    loadingMessage,
    mode,
    replaySessions,
    startReplay,
    stopReplay,
    speed,
    setSpeed,
  };
}
