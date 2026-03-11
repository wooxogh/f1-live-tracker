import { Home, Activity, Flag, Settings } from 'lucide-react';

export function SideNav() {
  return (
    <nav className="w-16 flex flex-col items-center py-4 bg-[#0a0a0a] border-r border-white/10 gap-8 z-20 shrink-0">
      <div className="w-10 h-10 bg-red-600 rounded flex items-center justify-center font-bold text-white italic tracking-tighter">
        F1
      </div>
      <div className="flex flex-col gap-6 text-white/40">
        <button className="p-2 hover:text-white hover:bg-white/10 rounded-xl transition-colors text-white bg-white/10" title="Live Timing">
          <Activity size={20} />
        </button>
        <button className="p-2 hover:text-white hover:bg-white/10 rounded-xl transition-colors" title="Dashboard">
          <Home size={20} />
        </button>
        <button className="p-2 hover:text-white hover:bg-white/10 rounded-xl transition-colors" title="Results">
          <Flag size={20} />
        </button>
      </div>
      <div className="mt-auto">
        <button className="p-2 hover:text-white hover:bg-white/10 rounded-xl transition-colors text-white/40" title="Settings">
          <Settings size={20} />
        </button>
      </div>
    </nav>
  );
}
