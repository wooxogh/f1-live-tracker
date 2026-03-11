import { DriverPosition, LapData } from '../types';
import { cn } from '../lib/utils';

interface DriverListProps {
  positions: Record<number, DriverPosition>;
  lapData: Record<number, LapData>;
}

function formatTime(seconds: number | null | undefined) {
  if (seconds == null) return '';
  if (seconds >= 60) {
    const m = Math.floor(seconds / 60);
    const s = (seconds % 60).toFixed(3).padStart(6, '0');
    return `${m}:${s}`;
  }
  return seconds.toFixed(3);
}

function getColorClass(status: 'purple' | 'green' | 'yellow' | null | undefined) {
  if (status === 'purple') return 'text-[#b138dd]';
  if (status === 'green') return 'text-[#00d2be]';
  if (status === 'yellow') return 'text-[#e8ff00]';
  return 'text-white/70';
}

export function DriverList({ positions, lapData }: DriverListProps) {
  // Sort by lap duration if available, otherwise by driver number
  const drivers = Object.values(positions).sort((a, b) => {
    const lapA = lapData[a.driverNumber]?.lap_duration || Infinity;
    const lapB = lapData[b.driverNumber]?.lap_duration || Infinity;
    if (lapA !== lapB) return lapA - lapB;
    return a.driverNumber - b.driverNumber;
  });

  return (
    <div className="w-[500px] flex flex-col bg-[#151619] border-l border-white/10 h-full overflow-hidden">
      <div className="p-4 border-b border-white/10 bg-black/20">
        <h2 className="text-sm font-semibold text-white/90 uppercase tracking-wider font-mono">
          Live Timing
        </h2>
      </div>
      
      {/* Table Header */}
      <div className="flex items-center px-4 py-2 border-b border-white/5 bg-[#1a1b1e] text-[10px] font-mono text-white/50 uppercase tracking-wider">
        <div className="w-8 text-center">Pos</div>
        <div className="w-24 pl-2">Driver</div>
        <div className="flex-1 text-right">S1</div>
        <div className="flex-1 text-right">S2</div>
        <div className="flex-1 text-right">S3</div>
        <div className="w-20 text-right pr-2">Lap</div>
      </div>

      <div className="flex-1 overflow-y-auto p-2 space-y-1 custom-scrollbar">
        {drivers.length === 0 ? (
          <div className="text-white/40 text-sm text-center py-8 font-mono">
            Waiting for driver data...
          </div>
        ) : (
          drivers.map((driver, index) => {
            const data = lapData[driver.driverNumber];
            return (
              <div 
                key={driver.driverNumber}
                className="flex items-center px-2 py-1.5 rounded hover:bg-white/5 transition-colors group text-sm font-mono"
              >
                {/* Position */}
                <div className="w-8 text-center text-white/70 font-bold">
                  {index + 1}
                </div>

                {/* Driver Info */}
                <div className="w-24 flex items-center gap-2 pl-2">
                  <div 
                    className="w-1 h-6 rounded-full" 
                    style={{ backgroundColor: `#${driver.teamColour}` }}
                  />
                  <span className="font-bold text-white tracking-wide">
                    {driver.nameAcronym}
                  </span>
                </div>

                {/* Sector 1 */}
                <div className={cn("flex-1 text-right", getColorClass(data?.s1_status))}>
                  {formatTime(data?.duration_sector_1)}
                </div>

                {/* Sector 2 */}
                <div className={cn("flex-1 text-right", getColorClass(data?.s2_status))}>
                  {formatTime(data?.duration_sector_2)}
                </div>

                {/* Sector 3 */}
                <div className={cn("flex-1 text-right", getColorClass(data?.s3_status))}>
                  {formatTime(data?.duration_sector_3)}
                </div>

                {/* Lap Time */}
                <div className="w-20 text-right pr-2 text-white font-bold">
                  {formatTime(data?.lap_duration)}
                </div>
              </div>
            );
          })
        )}
      </div>
    </div>
  );
}
