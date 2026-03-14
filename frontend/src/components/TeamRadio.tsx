import { useState, useMemo } from 'react';
import { Mic, Play, ChevronLeft, ChevronRight, Radio } from 'lucide-react';
import { RadioEntry, RaceControlEntry } from '../types';
import { DRIVERS_2026 } from '../lib/drivers';

interface TeamRadioProps {
  entries: RadioEntry[];
  raceControlEntries: RaceControlEntry[];
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

function getFlagColour(flag: string | null): string {
  if (!flag) return 'FFFFFF';
  if (flag.includes('RED'))    return 'EF4444';
  if (flag.includes('YELLOW')) return 'EAB308';
  if (flag.includes('GREEN'))  return '22C55E';
  if (flag.includes('SAFETY')) return 'F97316';
  return 'AAAAAA';
}

const ALL = 'ALL';
const OPTIONS = [
  { nameAcronym: ALL, teamColour: 'FFFFFF' },
  ...DRIVERS_2026.map(d => ({ nameAcronym: d.nameAcronym, teamColour: d.teamColour })),
];

type FeedItem =
  | { type: 'radio'; data: RadioEntry }
  | { type: 'rc'; data: RaceControlEntry };

export function TeamRadio({ entries, raceControlEntries }: TeamRadioProps) {
  const [filterIndex, setFilterIndex] = useState(0);

  const selected = OPTIONS[filterIndex];

  // 라디오 + 레이스 컨트롤 합쳐서 시간순 정렬 (최신 위)
  const feed = useMemo<FeedItem[]>(() => {
    const filteredRadio = selected.nameAcronym === ALL
      ? entries
      : entries.filter(e => e.nameAcronym === selected.nameAcronym);

    const radioItems: FeedItem[] = filteredRadio.map(d => ({ type: 'radio', data: d }));
    const rcItems: FeedItem[]    = raceControlEntries.map(d => ({ type: 'rc', data: d }));

    return [...radioItems, ...rcItems].sort((a, b) =>
      new Date(b.data.date).getTime() - new Date(a.data.date).getTime()
    );
  }, [entries, raceControlEntries, selected]);

  const handlePrev = () => setFilterIndex(i => (i - 1 + OPTIONS.length) % OPTIONS.length);
  const handleNext = () => setFilterIndex(i => (i + 1) % OPTIONS.length);

  const handlePlay = (url: string) => {
    const audio = new Audio(url);
    audio.play().catch(() => {});
  };

  return (
    <div className="h-full flex flex-col">
      {/* Header */}
      <div className="p-3 border-b border-white/10 bg-[#151619] flex items-center justify-between shrink-0">
        <div className="flex items-center gap-2">
          <Mic size={16} className="text-white/70" />
          <h2 className="text-sm font-semibold text-white/90 uppercase tracking-wider font-mono">
            Team Radio
          </h2>
        </div>

        {/* Driver Filter Slider */}
        <div className="flex items-center gap-2">
          <button
            onClick={handlePrev}
            aria-label="이전 드라이버"
            className="p-1 rounded hover:bg-white/10 transition-colors text-white/50 hover:text-white/90"
          >
            <ChevronLeft size={14} />
          </button>

          <div className="flex items-center gap-1.5 w-16 justify-center">
            {selected.nameAcronym !== ALL && (
              <div
                className="w-1.5 h-1.5 rounded-full shrink-0"
                style={{ backgroundColor: `#${selected.teamColour}` }}
              />
            )}
            <span
              className="text-xs font-bold font-mono tracking-wider"
              style={{
                color: selected.nameAcronym === ALL
                  ? 'rgba(255,255,255,0.7)'
                  : `#${selected.teamColour}`,
              }}
            >
              {selected.nameAcronym}
            </span>
          </div>

          <button
            onClick={handleNext}
            aria-label="다음 드라이버"
            className="p-1 rounded hover:bg-white/10 transition-colors text-white/50 hover:text-white/90"
          >
            <ChevronRight size={14} />
          </button>
        </div>
      </div>

      {/* Feed */}
      <div className="flex-1 overflow-y-auto p-4 space-y-3 custom-scrollbar">
        {feed.length === 0 ? (
          <div className="text-white/30 text-xs font-mono text-center py-6">
            No messages yet
          </div>
        ) : (
          feed.map((item) => {
            const key = item.type === 'radio'
              ? `radio-${item.data.date}-${item.data.driverNumber}`
              : `rc-${item.data.date}`;
            return item.type === 'radio' ? (
              <div key={key} className="flex gap-3 text-sm font-mono items-start">
                <div className="text-white/40 w-16 shrink-0 text-xs pt-0.5">
                  {formatTime(item.data.date)}
                </div>
                <div
                  className="flex-1 flex items-center justify-between gap-2 bg-white/5 px-3 py-2 rounded-lg border-l-2"
                  style={{ borderColor: `#${item.data.teamColour}` }}
                >
                  <div className="flex items-center gap-2 min-w-0">
                    <div className="w-1.5 h-1.5 rounded-full shrink-0" style={{ backgroundColor: `#${item.data.teamColour}` }} />
                    <span className="font-bold text-white/90 shrink-0">{item.data.nameAcronym}</span>
                    <span className="text-white/40 text-xs truncate">{item.data.recordingUrl.split('/').pop()}</span>
                  </div>
                  <button
                    onClick={() => handlePlay(item.data.recordingUrl)}
                    className="shrink-0 p-1.5 rounded-full bg-white/10 hover:bg-white/20 transition-colors"
                  >
                    <Play size={12} className="text-white/80" />
                  </button>
                </div>
              </div>
            ) : (
              <div key={key} className="flex gap-3 text-sm font-mono items-start">
                <div className="text-white/40 w-16 shrink-0 text-xs pt-0.5">
                  {formatTime(item.data.date)}
                </div>
                <div
                  className="flex-1 flex items-center gap-2 bg-white/5 px-3 py-2 rounded-lg border-l-2"
                  style={{ borderColor: `#${getFlagColour(item.data.flag)}` }}
                >
                  <Radio size={10} className="shrink-0 text-white/50" />
                  <span className="text-white/50 text-xs shrink-0">RC</span>
                  <span className="text-white/80 text-xs">{item.data.message}</span>
                </div>
              </div>
            );
          })
        )}
      </div>
    </div>
  );
}
