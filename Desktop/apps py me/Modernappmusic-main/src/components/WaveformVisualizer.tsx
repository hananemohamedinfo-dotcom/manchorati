import React, { useRef, useEffect, useState, useCallback } from 'react';
import { useAudio } from '../context/AudioContext';
import { formatTime } from '../utils/formatters';
import { ZoomIn, ZoomOut, Maximize2, Flag } from 'lucide-react';

interface WaveformVisualizerProps {
  height?: number;
}

export const WaveformVisualizer: React.FC<WaveformVisualizerProps> = ({ height = 150 }) => {
  const {
    currentTrack,
    currentTime,
    duration,
    seekTo,
    loopState,
    setPointA,
    setPointB,
    isPlaying,
    getLiveAudioData,
  } = useAudio();

  const canvasRef = useRef<HTMLCanvasElement | null>(null);
  const containerRef = useRef<HTMLDivElement | null>(null);

  // Zoom factor: 1x, 2x, 4x, 8x
  const [zoom, setZoom] = useState<number>(1);
  const [scrollOffset, setScrollOffset] = useState<number>(0); // in seconds
  const [isDraggingPlayhead, setIsDraggingPlayhead] = useState<boolean>(false);
  const [draggingHandle, setDraggingHandle] = useState<'A' | 'B' | null>(null);
  const [hoveredTime, setHoveredTime] = useState<number | null>(null);

  const trackDuration = Math.max(duration || 1, currentTrack?.duration || 1);
  const peaks = currentTrack?.waveformPeaks || [];

  // Auto-scroll when zoomed and playing
  useEffect(() => {
    if (zoom > 1 && isPlaying) {
      const windowDuration = trackDuration / zoom;
      if (currentTime < scrollOffset || currentTime > scrollOffset + windowDuration * 0.85) {
        setScrollOffset(Math.max(0, Math.min(trackDuration - windowDuration, currentTime - windowDuration * 0.2)));
      }
    }
  }, [currentTime, isPlaying, zoom, trackDuration, scrollOffset]);

  // Convert time to canvas X coordinate
  const timeToX = useCallback((time: number, width: number): number => {
    const windowDuration = trackDuration / zoom;
    const relTime = time - scrollOffset;
    return (relTime / windowDuration) * width;
  }, [trackDuration, zoom, scrollOffset]);

  // Convert canvas X coordinate to time
  const xToTime = useCallback((x: number, width: number): number => {
    const windowDuration = trackDuration / zoom;
    const time = scrollOffset + (x / width) * windowDuration;
    return Math.max(0, Math.min(trackDuration, time));
  }, [trackDuration, zoom, scrollOffset]);

  // Render loop using requestAnimationFrame
  useEffect(() => {
    let animId: number;

    const render = () => {
      const canvas = canvasRef.current;
      if (!canvas) return;
      const ctx = canvas.getContext('2d');
      if (!ctx) return;

      const width = canvas.width;
      const h = canvas.height;
      const centerY = h / 2;

      // 1. Clear canvas with deep AMOLED pitch black background
      ctx.fillStyle = '#09090b';
      ctx.fillRect(0, 0, width, h);

      // Draw subtle grid & time markers
      ctx.strokeStyle = '#27272a';
      ctx.lineWidth = 1;
      const windowDuration = trackDuration / zoom;
      const stepSec = zoom >= 4 ? 1 : zoom >= 2 ? 2 : 5;
      const firstSec = Math.floor(scrollOffset / stepSec) * stepSec;

      ctx.fillStyle = '#71717a';
      ctx.font = '10px ui-monospace, SFMono-Regular, monospace';

      for (let s = firstSec; s <= scrollOffset + windowDuration; s += stepSec) {
        const x = timeToX(s, width);
        if (x >= 0 && x <= width) {
          ctx.beginPath();
          ctx.moveTo(x, h - 16);
          ctx.lineTo(x, h - 2);
          ctx.stroke();

          // Time label
          if (s >= 0) {
            const label = formatTime(s, false);
            ctx.fillText(label, x + 3, h - 5);
          }
        }
      }

      // 2. Draw A-B Loop Region Highlight (if Point A and B exist)
      const { pointA, pointB, isActive } = loopState;
      if (pointA !== null && pointB !== null && pointB > pointA) {
        const xA = Math.max(0, timeToX(pointA, width));
        const xB = Math.min(width, timeToX(pointB, width));
        const regionWidth = Math.max(2, xB - xA);

        // Gradient backdrop for loop area
        const loopGrad = ctx.createLinearGradient(xA, 0, xB, 0);
        if (isActive) {
          loopGrad.addColorStop(0, 'rgba(6, 182, 212, 0.18)');
          loopGrad.addColorStop(1, 'rgba(14, 165, 233, 0.25)');
        } else {
          loopGrad.addColorStop(0, 'rgba(113, 113, 122, 0.12)');
          loopGrad.addColorStop(1, 'rgba(113, 113, 122, 0.18)');
        }

        ctx.fillStyle = loopGrad;
        ctx.fillRect(xA, 16, regionWidth, h - 36);

        // Top and bottom accent border
        ctx.strokeStyle = isActive ? '#06b6d4' : '#71717a';
        ctx.lineWidth = 2;
        ctx.strokeRect(xA, 16, regionWidth, h - 36);

        // Subtle diagonal loop striping
        ctx.save();
        ctx.beginPath();
        ctx.rect(xA, 16, regionWidth, h - 36);
        ctx.clip();
        ctx.strokeStyle = isActive ? 'rgba(6, 182, 212, 0.08)' : 'rgba(255, 255, 255, 0.03)';
        ctx.lineWidth = 1;
        for (let stripeX = xA - h; stripeX < xB + h; stripeX += 16) {
          ctx.beginPath();
          ctx.moveTo(stripeX, 16);
          ctx.lineTo(stripeX + h, h - 20);
          ctx.stroke();
        }
        ctx.restore();
      }

      // 3. Draw Waveform Peaks
      const totalPeaks = peaks.length > 0 ? peaks.length : 160;
      const startIdx = Math.floor((scrollOffset / trackDuration) * totalPeaks);
      const endIdx = Math.ceil(((scrollOffset + windowDuration) / trackDuration) * totalPeaks);
      const visiblePeakCount = Math.max(1, endIdx - startIdx);
      const barWidth = Math.max(2, (width / visiblePeakCount) * 0.7);

      for (let i = startIdx; i <= endIdx; i++) {
        if (i < 0 || i >= totalPeaks) continue;
        const peakVal = peaks[i] !== undefined ? peaks[i] : 0.25;
        const peakTime = (i / totalPeaks) * trackDuration;
        const x = timeToX(peakTime, width);

        // Check if bar is inside A-B loop
        const inLoop = pointA !== null && pointB !== null && peakTime >= pointA && peakTime <= pointB;
        const hasPassed = peakTime <= currentTime;

        let barHeight = peakVal * (h - 50);
        if (barHeight < 4) barHeight = 4;

        if (inLoop && isActive) {
          ctx.fillStyle = hasPassed ? '#22d3ee' : '#0891b2';
        } else if (hasPassed) {
          ctx.fillStyle = '#a1a1aa';
        } else {
          ctx.fillStyle = '#3f3f46';
        }

        // Draw symmetric peak bar
        const r = Math.min(2, barWidth / 2);
        const topY = centerY - barHeight / 2;
        ctx.beginPath();
        ctx.roundRect(x - barWidth / 2, topY, barWidth, barHeight, [r]);
        ctx.fill();
      }

      // 4. Live Audio Frequency Overlay if playing
      if (isPlaying) {
        const liveData = getLiveAudioData();
        if (liveData) {
          const { freqData } = liveData;
          ctx.beginPath();
          ctx.strokeStyle = 'rgba(34, 211, 238, 0.4)';
          ctx.lineWidth = 1.5;
          const sliceWidth = width / freqData.length;
          for (let f = 0; f < freqData.length; f++) {
            const v = freqData[f] / 255;
            const y = centerY + (v - 0.5) * (h * 0.6);
            if (f === 0) ctx.moveTo(0, y);
            else ctx.lineTo(f * sliceWidth, y);
          }
          ctx.stroke();
        }
      }

      // 5. Point A Marker Line & Drag Handle
      if (pointA !== null) {
        const xA = timeToX(pointA, width);
        if (xA >= -10 && xA <= width + 10) {
          // Glow
          ctx.strokeStyle = '#06b6d4';
          ctx.lineWidth = 2.5;
          ctx.beginPath();
          ctx.moveTo(xA, 14);
          ctx.lineTo(xA, h - 16);
          ctx.stroke();

          // Handle Flag "A"
          ctx.fillStyle = '#06b6d4';
          ctx.beginPath();
          ctx.roundRect(xA - 14, 0, 28, 18, [4]);
          ctx.fill();

          ctx.fillStyle = '#000000';
          ctx.font = 'bold 11px ui-sans-serif, system-ui, sans-serif';
          ctx.textAlign = 'center';
          ctx.fillText('A', xA, 13);
        }
      }

      // 6. Point B Marker Line & Drag Handle
      if (pointB !== null) {
        const xB = timeToX(pointB, width);
        if (xB >= -10 && xB <= width + 10) {
          ctx.strokeStyle = '#f59e0b';
          ctx.lineWidth = 2.5;
          ctx.beginPath();
          ctx.moveTo(xB, 14);
          ctx.lineTo(xB, h - 16);
          ctx.stroke();

          // Handle Flag "B"
          ctx.fillStyle = '#f59e0b';
          ctx.beginPath();
          ctx.roundRect(xB - 14, 0, 28, 18, [4]);
          ctx.fill();

          ctx.fillStyle = '#000000';
          ctx.font = 'bold 11px ui-sans-serif, system-ui, sans-serif';
          ctx.textAlign = 'center';
          ctx.fillText('B', xB, 13);
        }
      }

      // 7. Current Playhead Indicator
      const playheadX = timeToX(currentTime, width);
      if (playheadX >= -10 && playheadX <= width + 10) {
        // Red / White glowing needle
        ctx.strokeStyle = '#f43f5e';
        ctx.lineWidth = 2;
        ctx.beginPath();
        ctx.moveTo(playheadX, 0);
        ctx.lineTo(playheadX, h - 14);
        ctx.stroke();

        // Playhead top diamond
        ctx.fillStyle = '#f43f5e';
        ctx.beginPath();
        ctx.arc(playheadX, 6, 5, 0, Math.PI * 2);
        ctx.fill();
        ctx.strokeStyle = '#ffffff';
        ctx.lineWidth = 1.5;
        ctx.stroke();
      }

      // 8. Hover tooltip time guide
      if (hoveredTime !== null && !isDraggingPlayhead && !draggingHandle) {
        const hoverX = timeToX(hoveredTime, width);
        ctx.strokeStyle = 'rgba(255, 255, 255, 0.25)';
        ctx.setLineDash([3, 3]);
        ctx.beginPath();
        ctx.moveTo(hoverX, 16);
        ctx.lineTo(hoverX, h - 16);
        ctx.stroke();
        ctx.setLineDash([]);
      }

      animId = requestAnimationFrame(render);
    };

    animId = requestAnimationFrame(render);
    return () => cancelAnimationFrame(animId);
  }, [
    peaks,
    trackDuration,
    zoom,
    scrollOffset,
    currentTime,
    loopState,
    timeToX,
    isPlaying,
    getLiveAudioData,
    hoveredTime,
    isDraggingPlayhead,
    draggingHandle,
  ]);

  // Handle Canvas Resizing
  useEffect(() => {
    const handleResize = () => {
      const canvas = canvasRef.current;
      const container = containerRef.current;
      if (canvas && container) {
        canvas.width = container.clientWidth;
        canvas.height = height;
      }
    };
    handleResize();
    window.addEventListener('resize', handleResize);
    return () => window.removeEventListener('resize', handleResize);
  }, [height]);

  // Mouse Interaction: Click or Drag to seek / move handles
  const handleMouseDown = (e: React.MouseEvent<HTMLCanvasElement>) => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const rect = canvas.getBoundingClientRect();
    const x = e.clientX - rect.left;
    const clickedTime = xToTime(x, canvas.width);

    // Check if clicked near Point A handle (within 16px)
    if (loopState.pointA !== null) {
      const xA = timeToX(loopState.pointA, canvas.width);
      if (Math.abs(x - xA) <= 16) {
        setDraggingHandle('A');
        return;
      }
    }

    // Check if clicked near Point B handle
    if (loopState.pointB !== null) {
      const xB = timeToX(loopState.pointB, canvas.width);
      if (Math.abs(x - xB) <= 16) {
        setDraggingHandle('B');
        return;
      }
    }

    // Otherwise dragging playhead
    setIsDraggingPlayhead(true);
    seekTo(clickedTime);
  };

  const handleMouseMove = (e: React.MouseEvent<HTMLCanvasElement>) => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const rect = canvas.getBoundingClientRect();
    const x = Math.max(0, Math.min(canvas.width, e.clientX - rect.left));
    const targetTime = xToTime(x, canvas.width);

    setHoveredTime(targetTime);

    if (draggingHandle === 'A') {
      setPointA(targetTime);
    } else if (draggingHandle === 'B') {
      setPointB(targetTime);
    } else if (isDraggingPlayhead) {
      seekTo(targetTime);
    }
  };

  const handleMouseUp = () => {
    setIsDraggingPlayhead(false);
    setDraggingHandle(null);
  };

  const handleMouseLeave = () => {
    setIsDraggingPlayhead(false);
    setDraggingHandle(null);
    setHoveredTime(null);
  };

  // Zoom controls
  const handleZoomIn = () => {
    setZoom(prev => {
      const next = Math.min(8, prev * 2);
      const windowDuration = trackDuration / next;
      setScrollOffset(Math.max(0, Math.min(trackDuration - windowDuration, currentTime - windowDuration / 2)));
      return next;
    });
  };

  const handleZoomOut = () => {
    setZoom(prev => {
      const next = Math.max(1, prev / 2);
      const windowDuration = trackDuration / next;
      setScrollOffset(Math.max(0, Math.min(trackDuration - windowDuration, currentTime - windowDuration / 2)));
      return next;
    });
  };

  const handleResetZoom = () => {
    setZoom(1);
    setScrollOffset(0);
  };

  return (
    <div id="waveform-container" ref={containerRef} className="relative w-full bg-zinc-950 rounded-xl p-3 border border-zinc-800/80 shadow-2xl select-none">
      {/* Top Waveform Header bar */}
      <div className="flex items-center justify-between pb-2 text-xs text-zinc-400">
        <div className="flex items-center space-x-2 font-mono">
          <span className="text-rose-400 font-bold tracking-wider">{formatTime(currentTime, true)}</span>
          <span className="text-zinc-600">/</span>
          <span className="text-zinc-400">{formatTime(trackDuration, true)}</span>
          {hoveredTime !== null && (
            <span className="hidden sm:inline-block ml-3 px-2 py-0.5 rounded bg-zinc-800/80 text-zinc-300">
              Hover: {formatTime(hoveredTime, true)}
            </span>
          )}
        </div>

        {/* Zoom & Quick Loop Info */}
        <div className="flex items-center space-x-1.5">
          {loopState.isActive && loopState.pointA !== null && loopState.pointB !== null && (
            <div className="flex items-center space-x-1 px-2 py-0.5 mr-2 rounded bg-cyan-950/60 border border-cyan-800/50 text-cyan-300 text-[11px] font-mono">
              <Flag className="w-3 h-3 text-cyan-400" />
              <span>A-B: {(loopState.pointB - loopState.pointA).toFixed(2)}s</span>
              {loopState.targetCount > 0 ? (
                <span className="text-cyan-400 font-semibold">({loopState.completedCount}/{loopState.targetCount})</span>
              ) : (
                <span className="text-cyan-400 font-semibold">({loopState.completedCount}x ∞)</span>
              )}
            </div>
          )}

          <div className="flex items-center bg-zinc-900 border border-zinc-800 rounded-lg p-0.5">
            <button
              id="zoom-out-btn"
              onClick={handleZoomOut}
              disabled={zoom <= 1}
              title="Zoom out"
              className="p-1 hover:bg-zinc-800 text-zinc-400 hover:text-zinc-100 disabled:opacity-30 rounded transition-colors"
            >
              <ZoomOut className="w-3.5 h-3.5" />
            </button>
            <span className="px-1.5 text-[11px] font-mono text-zinc-300 font-medium">{zoom}x</span>
            <button
              id="zoom-in-btn"
              onClick={handleZoomIn}
              disabled={zoom >= 8}
              title="Zoom in"
              className="p-1 hover:bg-zinc-800 text-zinc-400 hover:text-zinc-100 disabled:opacity-30 rounded transition-colors"
            >
              <ZoomIn className="w-3.5 h-3.5" />
            </button>
            {zoom > 1 && (
              <button
                id="reset-zoom-btn"
                onClick={handleResetZoom}
                title="Fit to track"
                className="p-1 hover:bg-zinc-800 text-zinc-400 hover:text-zinc-100 rounded ml-0.5 transition-colors"
              >
                <Maximize2 className="w-3.5 h-3.5" />
              </button>
            )}
          </div>
        </div>
      </div>

      {/* Main Interactive Waveform Canvas */}
      <div className="relative cursor-crosshair overflow-hidden rounded-lg">
        <canvas
          id="waveform-canvas"
          ref={canvasRef}
          onMouseDown={handleMouseDown}
          onMouseMove={handleMouseMove}
          onMouseUp={handleMouseUp}
          onMouseLeave={handleMouseLeave}
          className="w-full block"
          style={{ height: `${height}px` }}
        />

        {/* Scroll mini-slider if zoomed */}
        {zoom > 1 && (
          <div className="absolute bottom-0 left-0 right-0 h-1.5 bg-zinc-900/90">
            <div
              className="h-full bg-cyan-500 rounded cursor-pointer"
              style={{
                width: `${(1 / zoom) * 100}%`,
                transform: `translateX(${(scrollOffset / trackDuration) * zoom * 100}%)`,
              }}
            />
          </div>
        )}
      </div>

      {/* Dragging Help hints */}
      <div className="flex items-center justify-between pt-1.5 text-[10px] text-zinc-500 font-mono">
        <span>Click or drag to scrub playhead</span>
        <span>Drag A or B markers to adjust bounds</span>
      </div>
    </div>
  );
};
