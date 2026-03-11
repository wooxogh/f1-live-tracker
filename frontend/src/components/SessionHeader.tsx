import { Session } from '../types';
import { ConnectionStatus } from '../hooks/useF1Data';
import { Activity, Wifi, WifiOff, AlertCircle } from 'lucide-react';
import { cn } from '../lib/utils';

interface SessionHeaderProps {
  session: Session | null;
  status: ConnectionStatus;
}

export function SessionHeader({ session, status }: SessionHeaderProps) {
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
      </div>

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
    </header>
  );
}
