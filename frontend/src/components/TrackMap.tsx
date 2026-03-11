import { useEffect, useRef } from 'react';
import { TrackPoint, DriverPosition } from '../types';

interface TrackMapProps {
  track: TrackPoint[];
  positions: Record<number, DriverPosition>;
}

export function TrackMap({ track, positions }: TrackMapProps) {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const containerRef = useRef<HTMLDivElement>(null);
  
  // Store current positions for lerping
  const currentPositionsRef = useRef<Record<number, { x: number, y: number }>>({});
  const animationFrameRef = useRef<number | undefined>(undefined);

  useEffect(() => {
    const canvas = canvasRef.current;
    const container = containerRef.current;
    if (!canvas || !container || track.length === 0) return;

    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    // Calculate bounds
    let minX = Infinity, maxX = -Infinity, minY = Infinity, maxY = -Infinity;
    track.forEach(p => {
      if (p.x < minX) minX = p.x;
      if (p.x > maxX) maxX = p.x;
      if (p.y < minY) minY = p.y;
      if (p.y > maxY) maxY = p.y;
    });

    const trackWidth = maxX - minX;
    const trackHeight = maxY - minY;

    const resizeCanvas = () => {
      const rect = container.getBoundingClientRect();
      // Handle high DPI displays
      const dpr = window.devicePixelRatio || 1;
      canvas.width = rect.width * dpr;
      canvas.height = rect.height * dpr;
      ctx.scale(dpr, dpr);
      canvas.style.width = `${rect.width}px`;
      canvas.style.height = `${rect.height}px`;
    };

    window.addEventListener('resize', resizeCanvas);
    resizeCanvas();

    const render = () => {
      const rect = container.getBoundingClientRect();
      const width = rect.width;
      const height = rect.height;

      // Clear canvas
      ctx.clearRect(0, 0, width, height);

      // Padding
      const padding = 50;
      const availableWidth = width - padding * 2;
      const availableHeight = height - padding * 2;

      // Scale to fit
      const scale = Math.min(availableWidth / trackWidth, availableHeight / trackHeight);

      // Center offset
      const offsetX = (width - trackWidth * scale) / 2;
      const offsetY = (height - trackHeight * scale) / 2;

      const transformPoint = (x: number, y: number) => {
        return {
          cx: (x - minX) * scale + offsetX,
          cy: height - ((y - minY) * scale + offsetY),
        };
      };

      // 1. Draw Track Outline Glow
      ctx.beginPath();
      track.forEach((p, i) => {
        const { cx, cy } = transformPoint(p.x, p.y);
        if (i === 0) ctx.moveTo(cx, cy);
        else ctx.lineTo(cx, cy);
      });
      ctx.closePath();
      
      ctx.lineJoin = 'round';
      ctx.lineCap = 'round';
      
      // Glow effect
      ctx.shadowColor = 'rgba(255, 255, 255, 0.2)';
      ctx.shadowBlur = 15;
      ctx.lineWidth = 12;
      ctx.strokeStyle = 'rgba(255, 255, 255, 0.1)';
      ctx.stroke();

      // 2. Draw Track Surface
      ctx.shadowBlur = 0;
      ctx.lineWidth = 8;
      ctx.strokeStyle = '#333333';
      ctx.stroke();

      // 3. Draw Center Dashed Line
      ctx.lineWidth = 1;
      ctx.strokeStyle = '#ffffff';
      ctx.setLineDash([5, 5]);
      ctx.stroke();
      ctx.setLineDash([]); // Reset

      // Reset text alignment for driver labels
      ctx.textAlign = 'left';
      ctx.textBaseline = 'alphabetic';

      // Update and Draw Drivers
      const currentPositions = currentPositionsRef.current;
      
      Object.values(positions).forEach(targetPos => {
        const { driverNumber, teamColour, nameAcronym, x, y } = targetPos;
        
        if (!currentPositions[driverNumber]) {
          currentPositions[driverNumber] = { x, y };
        } else {
          // Lerp (Linear Interpolation)
          currentPositions[driverNumber].x += (x - currentPositions[driverNumber].x) * 0.1;
          currentPositions[driverNumber].y += (y - currentPositions[driverNumber].y) * 0.1;
        }

        const current = currentPositions[driverNumber];
        const { cx, cy } = transformPoint(current.x, current.y);

        // Draw Driver Circle
        ctx.beginPath();
        ctx.arc(cx, cy, 6, 0, 2 * Math.PI);
        ctx.fillStyle = `#${teamColour}`;
        ctx.fill();

        // Draw Driver Label
        ctx.font = 'bold 10px Inter, sans-serif';
        const textWidth = ctx.measureText(nameAcronym).width;
        
        // Label Background
        ctx.fillStyle = 'rgba(0, 0, 0, 0.7)';
        ctx.beginPath();
        ctx.roundRect(cx + 10, cy - 10, textWidth + 8, 16, 4);
        ctx.fill();

        // Label Text
        ctx.fillStyle = '#ffffff';
        ctx.fillText(nameAcronym, cx + 14, cy + 2);
      });

      animationFrameRef.current = requestAnimationFrame(render);
    };

    render();

    return () => {
      window.removeEventListener('resize', resizeCanvas);
      if (animationFrameRef.current) {
        cancelAnimationFrame(animationFrameRef.current);
      }
    };
  }, [track, positions]);

  return (
    <div ref={containerRef} className="w-full h-full relative bg-[#111111] rounded-2xl overflow-hidden shadow-2xl border border-white/5">
      <canvas ref={canvasRef} className="absolute inset-0" />
      
      {/* Grid Overlay for technical feel */}
      <div className="absolute inset-0 pointer-events-none opacity-[0.03]" 
           style={{ backgroundImage: 'linear-gradient(#fff 1px, transparent 1px), linear-gradient(90deg, #fff 1px, transparent 1px)', backgroundSize: '40px 40px' }}>
      </div>
    </div>
  );
}
