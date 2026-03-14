/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import { SessionHeader } from './components/SessionHeader';
import { TrackMap } from './components/TrackMap';
import { DriverList } from './components/DriverList';
import { SideNav } from './components/SideNav';
import { TeamRadio } from './components/TeamRadio';
import { useF1Data } from './hooks/useF1Data';
import { Loader2 } from 'lucide-react';

export default function App() {
  const {
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
  } = useF1Data();

  return (
    <div className="h-screen bg-[#050505] text-white flex font-sans overflow-hidden">
      <SideNav />

      <div className="flex-1 flex flex-col overflow-hidden">
        <SessionHeader
          session={session}
          status={status}
          mode={mode}
          replaySessions={replaySessions}
          speed={speed}
          onStartReplay={startReplay}
          onStopReplay={stopReplay}
          onSpeedChange={setSpeed}
        />

        <main className="flex-1 flex overflow-hidden">
          {/* Center Column: Track Map & Team Radio */}
          <div className="flex-1 flex flex-col min-w-0">
            {/* Top: Track Area - 항상 렌더링, 상태에 따라 내용만 변경 */}
            <div className="flex-[3] relative p-6 flex flex-col">
              {track ? (
                <TrackMap track={track} positions={positions} />
              ) : (
                <div className="w-full h-full flex flex-col items-center justify-center bg-[#111111] rounded-2xl border border-white/5">
                  {status === 'OFF_SEASON' ? (
                    <div className="text-center space-y-3">
                      <div className="w-10 h-10 bg-red-600 rounded flex items-center justify-center font-bold text-white italic tracking-tighter mx-auto">
                        F1
                      </div>
                      <h2 className="text-base font-medium text-white/70">No Active Session</h2>
                      <p className="text-white/40 font-mono text-xs">{loadingMessage}</p>
                    </div>
                  ) : (
                    <div className="flex flex-col items-center gap-3">
                      <Loader2 className="w-6 h-6 text-red-500 animate-spin" />
                      <p className="text-white/50 font-mono text-xs animate-pulse">{loadingMessage}</p>
                    </div>
                  )}
                </div>
              )}
            </div>

            {/* Bottom: Team Radio / Race Control - 항상 렌더링 */}
            <div className="flex-[2] border-t border-white/10 bg-[#0a0a0a] overflow-hidden">
              <TeamRadio entries={radioEntries} raceControlEntries={raceControlEntries} />
            </div>
          </div>

          {/* Right Sidebar: Live Timing - 항상 렌더링 */}
          <DriverList positions={positions} lapData={lapData} />
        </main>
      </div>
    </div>
  );
}
