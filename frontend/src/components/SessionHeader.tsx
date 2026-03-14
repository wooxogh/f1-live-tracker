import { Session, ReplaySession } from '../types';
import { ConnectionStatus } from '../hooks/useF1Data';
import { Activity, Wifi, WifiOff, AlertCircle, PlayCircle, StopCircle } from 'lucide-react';
import { cn } from '../lib/utils';

interface SessionHeaderProps {
  session: Session | null;
  status: ConnectionStatus;
  mode: 'live' | 'replay';
  replaySessions: ReplaySession[];
  speed: number;
  onStartReplay: (sessionKey: number, speed: number) => void;
  onStopReplay: () => void;
  onSpeedChange: (speed: number) => void;
}

export function SessionHeader({
  session,
  status,
  mode,
  replaySessions,
  speed,
  onStartReplay,
  onStopReplay,
  onSpeedChange,
}: SessionHeaderProps) {
  const getStatusConfig = () => {
    switch (status) {
      case 'LIVE':
        return { icon: Activity, color: 'text-red-500', bg: 'bg-red-500/10', text: 'LIVE' };
      case 'CONNECTING':
        return { icon: Wifi, color: 'text-yellow-500', bg: 'bg-yellow-500/10', text: 'CONNECTING...' };
      case 'DISCONNECTED':
        return { icon: WifiOff, color: 'text-orange-500', bg: 'bg-orange-500/10', text: 'RECONNECTING (5s)' };
      case 'OFF_SEASON':
        return { icon: AlertCircle, color: 'text-white/40', bg: 'bg-white/5', text: 'OFFLINE' };
    }
  };

  const config = getStatusConfig();
  const StatusIcon = config.icon;

  const handleSessionSelect = (e: React.ChangeEvent<HTMLSelectElement>) => {
    const val = e.target.value;
    if (!val) return;
    onStartReplay(Number(val), speed);
  };

  return (
    <header className="h-20 flex items-center justify-between px-8 shrink-0 max-w-[1600px] mx-auto w-full">
      <div className="flex items-center gap-6">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 bg-gradient-to-br from-red-500 to-red-700 rounded-xl flex items-center justify-center font-bold text-white italic tracking-tighter shadow-lg shadow-red-500/20 border border-red-400/20">
            F1
          </div>
          <span className="font-mono text-[11px] text-white/40 tracking-[0.2em] uppercase font-semibold">
            Live Tracker
          </span>
        </div>

        {session && (
          <>
            <div className="w-px h-8 bg-white/5" />
            <div className="flex flex-col justify-center gap-1">
              <h1 className="text-white/90 font-medium text-base leading-none tracking-tight">
                {session.session_name}
              </h1>
              <div className="text-white/40 font-mono text-[10px] uppercase tracking-widest flex items-center gap-2">
                <span>{session.location}</span>
                <span className="w-1 h-1 rounded-full bg-white/20" />
                <span>{session.country_name}</span>
              </div>
            </div>
          </>
        )}

        {/* Mode badge */}
        <div className={cn(
          "flex items-center gap-1.5 px-3 py-1 rounded-full border text-[10px] font-mono font-bold tracking-[0.15em]",
          mode === 'live'
            ? 'bg-red-500/10 border-red-500/20 text-red-500'
            : 'bg-blue-500/10 border-blue-500/20 text-blue-400'
        )}>
          {mode === 'live' ? 'LIVE' : 'REPLAY'}
        </div>

        {/* Replay session selector */}
        {replaySessions.length > 0 && (
          <>
            <div className="w-px h-8 bg-white/5" />
            <select
              value=""
              onChange={handleSessionSelect}
              className="bg-white/5 border border-white/10 text-white/70 text-xs font-mono rounded-lg px-3 py-1.5 cursor-pointer hover:bg-white/10 transition-colors focus:outline-none focus:ring-1 focus:ring-white/20"
            >
              <option value="" disabled>Past Races…</option>
              {replaySessions.map(s => (
                <option key={s.sessionKey} value={s.sessionKey} className="bg-[#111] text-white">
                  {s.meetingName} — {s.sessionName}
                </option>
              ))}
            </select>
          </>
        )}
      </div>

      <div className="flex items-center gap-3">
        {/* Replay controls */}
        {mode === 'replay' && (
          <div className="flex items-center gap-2">
            {[1, 2, 5].map(s => (
              <button
                key={s}
                onClick={() => onSpeedChange(s)}
                className={cn(
                  "px-3 py-1 rounded-lg text-[10px] font-mono font-bold tracking-wider border transition-colors",
                  speed === s
                    ? 'bg-blue-500/20 border-blue-500/40 text-blue-300'
                    : 'bg-white/5 border-white/10 text-white/40 hover:bg-white/10 hover:text-white/70'
                )}
              >
                {s}×
              </button>
            ))}
            <button
              onClick={onStopReplay}
              className="flex items-center gap-1.5 px-3 py-1 rounded-lg border bg-white/5 border-white/10 text-white/60 hover:bg-red-500/10 hover:border-red-500/20 hover:text-red-400 text-[10px] font-mono font-bold tracking-wider transition-colors"
            >
              <StopCircle className="w-3 h-3" />
              STOP
            </button>
          </div>
        )}

        {mode === 'live' && replaySessions.length > 0 && (
          <button
            onClick={() => {
              const first = replaySessions[0];
              if (first) onStartReplay(first.sessionKey, speed);
            }}
            className="flex items-center gap-1.5 px-3 py-1 rounded-lg border bg-blue-500/10 border-blue-500/20 text-blue-400 hover:bg-blue-500/20 text-[10px] font-mono font-bold tracking-wider transition-colors"
          >
            <PlayCircle className="w-3 h-3" />
            REPLAY
          </button>
        )}

        {/* Connection status */}
        <div className={cn(
          "flex items-center gap-2.5 px-4 py-2 rounded-full border shadow-sm backdrop-blur-md",
          config.bg,
          status === 'LIVE' ? 'border-red-500/20' : 'border-white/5'
        )}>
          <StatusIcon className={cn("w-3.5 h-3.5", config.color, status === 'LIVE' && 'animate-pulse')} />
          <span className={cn("text-[10px] font-mono font-bold tracking-[0.15em]", config.color)}>
            {config.text}
          </span>
        </div>
      </div>
    </header>
  );
}
