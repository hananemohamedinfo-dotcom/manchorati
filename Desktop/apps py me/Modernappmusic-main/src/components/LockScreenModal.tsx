import React from 'react';
import { useAudio } from '../context/AudioContext';
import { formatTime } from '../utils/formatters';
import {
  Smartphone,
  Play,
  Pause,
  SkipBack,
  SkipForward,
  Repeat,
  RotateCcw,
  RotateCw,
  X,
  Volume2,
  Radio,
  Headphones,
  Sliders,
} from 'lucide-react';

interface LockScreenModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export const LockScreenModal: React.FC<LockScreenModalProps> = ({ isOpen, onClose }) => {
  const {
    currentTrack,
    isPlaying,
    currentTime,
    duration,
    togglePlayPause,
    seekTo,
    skipBackward,
    skipForward,
    playNextTrack,
    playPreviousTrack,
    loopState,
    toggleLoopActive,
    jumpInterval,
  } = useAudio();

  if (!isOpen) return null;

  const trackDuration = Math.max(duration || 1, currentTrack?.duration || 1);
  const progressPercent = Math.min(100, Math.max(0, (currentTime / trackDuration) * 100));

  const now = new Date();
  const timeStr = now.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  const dateStr = now.toLocaleDateString([], { weekday: 'long', month: 'short', day: 'numeric' });

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/85 backdrop-blur-md p-4 animate-in fade-in">
      <div className="relative w-full max-w-sm bg-black border border-zinc-800 rounded-[40px] p-6 shadow-2xl overflow-hidden text-zinc-100 flex flex-col justify-between min-h-[640px]">
        {/* Close button */}
        <button
          onClick={onClose}
          className="absolute top-4 right-4 p-1.5 rounded-full bg-zinc-900/80 text-zinc-400 hover:text-zinc-100 z-20"
        >
          <X className="w-4 h-4" />
        </button>

        {/* Lock Screen Status Bar */}
        <div className="flex items-center justify-between text-xs text-zinc-400 pt-1 px-2 font-medium">
          <span>{timeStr}</span>
          <div className="flex items-center space-x-1.5">
            <Radio className="w-3.5 h-3.5 text-cyan-400" />
            <Headphones className="w-3.5 h-3.5 text-zinc-300" />
            <span className="text-[10px] font-mono">100%</span>
          </div>
        </div>

        {/* Lock Screen Clock Display */}
        <div className="text-center py-6">
          <div className="text-5xl font-extralight tracking-tight text-zinc-100">{timeStr}</div>
          <div className="text-xs text-zinc-400 mt-1">{dateStr}</div>
        </div>

        {/* Rich Media Notification Card (Simulating Android/iOS Media Player) */}
        <div className="bg-zinc-900/90 border border-zinc-700/80 rounded-3xl p-4 shadow-xl space-y-3 backdrop-blur-lg">
          {/* Header row: App Name & A-B Loop Indicator */}
          <div className="flex items-center justify-between text-[11px] text-zinc-400">
            <div className="flex items-center space-x-1.5">
              <span className="w-2 h-2 rounded-full bg-cyan-400 animate-pulse" />
              <span className="font-semibold text-zinc-300">A-B Loop Player</span>
            </div>
            {loopState.isActive && loopState.pointA !== null && loopState.pointB !== null && (
              <span className="px-2 py-0.5 rounded-full bg-cyan-950 border border-cyan-700 text-cyan-300 font-mono text-[10px]">
                A-B [{formatTime(loopState.pointA, false)}-{formatTime(loopState.pointB, false)}]
              </span>
            )}
          </div>

          {/* Track Info */}
          <div className="space-y-0.5">
            <h3 className="font-bold text-sm text-zinc-100 truncate">
              {currentTrack?.title || 'Unknown Track'}
            </h3>
            <p className="text-xs text-zinc-400 truncate">
              {currentTrack?.artist || 'Unknown Artist'} — {currentTrack?.album || 'Audio'}
            </p>
          </div>

          {/* Mini Waveform / Scrubber */}
          <div className="space-y-1">
            <div
              className="relative h-2 bg-zinc-800 rounded-full cursor-pointer overflow-hidden"
              onClick={e => {
                const rect = e.currentTarget.getBoundingClientRect();
                const ratio = (e.clientX - rect.left) / rect.width;
                seekTo(ratio * trackDuration);
              }}
            >
              {/* Highlight Loop region if set */}
              {loopState.pointA !== null && loopState.pointB !== null && (
                <div
                  className="absolute top-0 bottom-0 bg-cyan-500/30"
                  style={{
                    left: `${(loopState.pointA / trackDuration) * 100}%`,
                    width: `${((loopState.pointB - loopState.pointA) / trackDuration) * 100}%`,
                  }}
                />
              )}
              {/* Active Progress */}
              <div
                className="h-full bg-gradient-to-r from-cyan-500 to-blue-500 rounded-full"
                style={{ width: `${progressPercent}%` }}
              />
            </div>
            <div className="flex justify-between text-[10px] font-mono text-zinc-400">
              <span>{formatTime(currentTime, false)}</span>
              <span>{formatTime(trackDuration, false)}</span>
            </div>
          </div>

          {/* Lock Screen Playback Controls */}
          <div className="flex items-center justify-between pt-1">
            {/* A-B Loop Toggle from Lockscreen */}
            <button
              id="lockscreen-loop-toggle"
              onClick={toggleLoopActive}
              disabled={loopState.pointA === null || loopState.pointB === null}
              className={`p-2 rounded-xl border transition-colors ${
                loopState.isActive
                  ? 'bg-cyan-500/20 text-cyan-300 border-cyan-500/60'
                  : 'bg-zinc-800 text-zinc-400 border-zinc-700 disabled:opacity-30'
              }`}
              title="Toggle A-B loop"
            >
              <Repeat className="w-4 h-4" />
            </button>

            {/* Rewind */}
            <button
              id="lockscreen-rewind-btn"
              onClick={skipBackward}
              className="p-2 text-zinc-300 hover:text-white transition-colors"
              title={`Rewind ${jumpInterval}s`}
            >
              <RotateCcw className="w-4 h-4" />
            </button>

            {/* Play/Pause */}
            <button
              id="lockscreen-play-pause-btn"
              onClick={togglePlayPause}
              className="w-11 h-11 rounded-full bg-cyan-500 text-black flex items-center justify-center font-bold shadow-lg active:scale-95 transition-transform"
            >
              {isPlaying ? (
                <Pause className="w-5 h-5 fill-current" />
              ) : (
                <Play className="w-5 h-5 fill-current ml-0.5" />
              )}
            </button>

            {/* Forward */}
            <button
              id="lockscreen-forward-btn"
              onClick={skipForward}
              className="p-2 text-zinc-300 hover:text-white transition-colors"
              title={`Forward ${jumpInterval}s`}
            >
              <RotateCw className="w-4 h-4" />
            </button>

            {/* Next Track */}
            <button
              id="lockscreen-next-btn"
              onClick={playNextTrack}
              className="p-2 text-zinc-300 hover:text-white transition-colors"
            >
              <SkipForward className="w-4 h-4 fill-current" />
            </button>
          </div>
        </div>

        {/* Lock Screen Bottom Help */}
        <div className="text-center text-[11px] text-zinc-500 pb-2">
          <span>Active background MediaSession service</span>
        </div>
      </div>
    </div>
  );
};
