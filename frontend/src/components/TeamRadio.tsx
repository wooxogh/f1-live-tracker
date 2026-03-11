import { Mic, Play } from 'lucide-react';
import { RadioEntry } from '../types';

interface TeamRadioProps {
  entries: RadioEntry[];
}

function formatTime(dateStr: string): string {
  try {
    return new Date(dateStr).toLocaleTimeString('ko-KR', {
      hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: false,
    });
  } catch {
    return '';
  }
}

export function TeamRadio({ entries }: TeamRadioProps) {
  const handlePlay = (url: string) => {
    const audio = new Audio(url);
    audio.play().catch(() => {});
  };

  return (
    <div className="h-full flex flex-col">
      <div className="p-3 border-b border-white/10 bg-[#151619] flex items-center gap-2 shrink-0">
        <Mic size={16} className="text-white/70" />
        <h2 className="text-sm font-semibold text-white/90 uppercase tracking-wider font-mono">
          Team Radio
        </h2>
      </div>

      <div className="flex-1 overflow-y-auto p-4 space-y-3 custom-scrollbar">
        {entries.length === 0 ? (
          <div className="text-white/30 text-xs font-mono text-center py-6">
            No radio messages yet
          </div>
        ) : (
          entries.map((entry, idx) => (
            <div key={idx} className="flex gap-3 text-sm font-mono items-start">
              <div className="text-white/40 w-16 shrink-0 text-xs pt-0.5">
                {formatTime(entry.date)}
              </div>

              <div
                className="flex-1 flex items-center justify-between gap-2 bg-white/5 px-3 py-2 rounded-lg border-l-2"
                style={{ borderColor: `#${entry.teamColour}` }}
              >
                <div className="flex items-center gap-2 min-w-0">
                  <div
                    className="w-1.5 h-1.5 rounded-full shrink-0"
                    style={{ backgroundColor: `#${entry.teamColour}` }}
                  />
                  <span className="font-bold text-white/90 shrink-0">{entry.nameAcronym}</span>
                  <span className="text-white/40 text-xs truncate">
                    {entry.recordingUrl.split('/').pop()}
                  </span>
                </div>

                <button
                  onClick={() => handlePlay(entry.recordingUrl)}
                  className="shrink-0 p-1.5 rounded-full bg-white/10 hover:bg-white/20 transition-colors"
                  title="Play"
                >
                  <Play size={12} className="text-white/80" />
                </button>
              </div>
            </div>
          ))
        )}
      </div>
    </div>
  );
}
