import React, { useState } from 'react';
import { useAudio } from '../context/AudioContext';
import { JumpInterval } from '../types';
import {
  Play,
  Pause,
  SkipBack,
  SkipForward,
  RotateCcw,
  RotateCw,
  Volume2,
  VolumeX,
  Gauge,
  SlidersHorizontal,
  Headphones,
  Smartphone,
  Check,
  Disc,
} from 'lucide-react';

interface PlaybackEngineControlsProps {
  onOpenLockScreenPreview: () => void;
  onOpenSleepTimerModal: () => void;
}

export const PlaybackEngineControls: React.FC<PlaybackEngineControlsProps> = ({
  onOpenLockScreenPreview,
  onOpenSleepTimerModal,
}) => {
  const {
    currentTrack,
    isPlaying,
    togglePlayPause,
    skipForward,
    skipBackward,
    playNextTrack,
    playPreviousTrack,
    volume,
    isMuted,
    setVolume,
    toggleMute,
    playbackRate,
    setPlaybackRate,
    jumpInterval,
    setJumpInterval,
    isPitchPreserved,
    sleepTimer,
  } = useAudio();

  const [isSpeedMenuOpen, setIsSpeedMenuOpen] = useState(false);
  const [isJumpMenuOpen, setIsJumpMenuOpen] = useState(false);

  const speedOptions = [0.5, 0.75, 1.0, 1.25, 1.5, 1.75, 2.0, 2.5];
  const jumpIntervalOptions: JumpInterval[] = [5, 10, 30];

  return (
    <div id="playback-engine-controls" className="w-full bg-zinc-950/90 border border-zinc-800 rounded-2xl p-4 shadow-2xl flex flex-col md:flex-row items-center justify-between gap-4">
      {/* Current Track Info & Artwork */}
      <div className="flex items-center space-x-3 w-full md:w-1/3 min-w-0">
        <div className="relative w-12 h-12 rounded-xl bg-gradient-to-br from-zinc-800 to-zinc-950 border border-zinc-700/60 flex items-center justify-center flex-shrink-0 shadow-inner overflow-hidden">
          <Disc className={`w-7 h-7 text-cyan-400 ${isPlaying ? 'animate-spin-slow' : ''}`} />
          {isPlaying && (
            <div className="absolute inset-0 bg-cyan-500/10 pointer-events-none" />
          )}
        </div>

        <div className="min-w-0 flex-1">
          <h2 className="text-sm font-bold text-zinc-100 truncate">
            {currentTrack?.title || 'No Audio Loaded'}
          </h2>
          <div className="flex items-center space-x-1.5 text-xs text-zinc-400 truncate">
            <span>{currentTrack?.artist || 'Select a track from library'}</span>
            {currentTrack?.album && (
              <>
                <span className="text-zinc-600">•</span>
                <span className="text-zinc-500 truncate">{currentTrack.album}</span>
              </>
            )}
          </div>
        </div>
      </div>

      {/* Main Transport Controls: Prev, Rewind (-X), Play/Pause, Forward (+X), Next */}
      <div className="flex items-center justify-center space-x-2 sm:space-x-3.5 w-full md:w-auto">
        {/* Previous Track */}
        <button
          id="prev-track-btn"
          onClick={playPreviousTrack}
          title="Previous Track"
          className="p-2 text-zinc-400 hover:text-zinc-100 hover:bg-zinc-850 rounded-xl transition-colors active:scale-95"
        >
          <SkipBack className="w-4 h-4 fill-current" />
        </button>

        {/* Rewind Skip by Interval */}
        <div className="relative">
          <button
            id="rewind-skip-btn"
            onClick={skipBackward}
            title={`Rewind ${jumpInterval}s`}
            className="flex items-center space-x-0.5 px-2.5 py-1.5 bg-zinc-900 hover:bg-zinc-800 border border-zinc-800 text-zinc-300 rounded-xl transition-all active:scale-95 text-xs font-mono"
          >
            <RotateCcw className="w-3.5 h-3.5 text-cyan-400" />
            <span>-{jumpInterval}s</span>
          </button>
        </div>

        {/* Main Play / Pause Button */}
        <button
          id="main-play-pause-btn"
          onClick={togglePlayPause}
          title={isPlaying ? 'Pause' : 'Play'}
          className="w-12 h-12 rounded-2xl bg-cyan-500 hover:bg-cyan-400 text-zinc-950 flex items-center justify-center shadow-[0_0_20px_rgba(6,182,212,0.4)] transition-all transform active:scale-90"
        >
          {isPlaying ? (
            <Pause className="w-6 h-6 fill-current" />
          ) : (
            <Play className="w-6 h-6 fill-current ml-0.5" />
          )}
        </button>

        {/* Fast-Forward Skip by Interval */}
        <div className="relative">
          <button
            id="fast-forward-skip-btn"
            onClick={skipForward}
            title={`Skip forward ${jumpInterval}s`}
            className="flex items-center space-x-0.5 px-2.5 py-1.5 bg-zinc-900 hover:bg-zinc-800 border border-zinc-800 text-zinc-300 rounded-xl transition-all active:scale-95 text-xs font-mono"
          >
            <span>+{jumpInterval}s</span>
            <RotateCw className="w-3.5 h-3.5 text-cyan-400" />
          </button>
        </div>

        {/* Next Track */}
        <button
          id="next-track-btn"
          onClick={playNextTrack}
          title="Next Track"
          className="p-2 text-zinc-400 hover:text-zinc-100 hover:bg-zinc-850 rounded-xl transition-colors active:scale-95"
        >
          <SkipForward className="w-4 h-4 fill-current" />
        </button>
      </div>

      {/* Auxiliary Settings: Speed, Jump Interval, Volume, Lock Screen Simulator, Sleep Timer */}
      <div className="flex items-center justify-end space-x-2.5 w-full md:w-1/3">
        {/* Variable Speed Selector */}
        <div className="relative">
          <button
            id="speed-control-btn"
            onClick={() => setIsSpeedMenuOpen(!isSpeedMenuOpen)}
            className="flex items-center space-x-1 px-2.5 py-1.5 rounded-lg bg-zinc-900 hover:bg-zinc-850 border border-zinc-800 text-xs font-mono text-cyan-300 transition-colors"
            title="Variable speed control with pitch correction"
          >
            <Gauge className="w-3.5 h-3.5" />
            <span>{playbackRate.toFixed(2)}x</span>
          </button>

          {isSpeedMenuOpen && (
            <div className="absolute bottom-full mb-2 right-0 bg-zinc-900 border border-zinc-700 rounded-xl p-2 shadow-2xl z-40 w-44 space-y-1">
              <div className="px-2 py-1 text-[10px] font-semibold text-zinc-400 uppercase tracking-wider border-b border-zinc-800 flex justify-between">
                <span>Playback Speed</span>
                <span className="text-emerald-400">Pitch Corrected</span>
              </div>
              <div className="grid grid-cols-2 gap-1 pt-1">
                {speedOptions.map(rate => (
                  <button
                    key={rate}
                    onClick={() => {
                      setPlaybackRate(rate);
                      setIsSpeedMenuOpen(false);
                    }}
                    className={`px-2 py-1 rounded text-xs font-mono text-left flex items-center justify-between ${
                      playbackRate === rate
                        ? 'bg-cyan-500/20 text-cyan-300 font-bold'
                        : 'text-zinc-300 hover:bg-zinc-800'
                    }`}
                  >
                    <span>{rate}x</span>
                    {playbackRate === rate && <Check className="w-3 h-3" />}
                  </button>
                ))}
              </div>
              <div className="pt-2 border-t border-zinc-800">
                <input
                  type="range"
                  min="0.5"
                  max="2.5"
                  step="0.05"
                  value={playbackRate}
                  onChange={e => setPlaybackRate(parseFloat(e.target.value))}
                  className="w-full h-1 bg-zinc-700 rounded-lg appearance-none cursor-pointer accent-cyan-400"
                />
              </div>
            </div>
          )}
        </div>

        {/* Jump Interval Selector (5s, 10s, 30s) */}
        <div className="relative">
          <button
            id="jump-interval-btn"
            onClick={() => setIsJumpMenuOpen(!isJumpMenuOpen)}
            className="flex items-center space-x-1 px-2.5 py-1.5 rounded-lg bg-zinc-900 hover:bg-zinc-850 border border-zinc-800 text-xs font-mono text-zinc-300 transition-colors"
            title="Configure jump intervals"
          >
            <SlidersHorizontal className="w-3.5 h-3.5 text-zinc-400" />
            <span>±{jumpInterval}s</span>
          </button>

          {isJumpMenuOpen && (
            <div className="absolute bottom-full mb-2 right-0 bg-zinc-900 border border-zinc-700 rounded-xl p-2 shadow-2xl z-40 w-36 space-y-1">
              <div className="px-2 py-1 text-[10px] font-semibold text-zinc-400 uppercase tracking-wider border-b border-zinc-800">
                Skip Interval
              </div>
              {jumpIntervalOptions.map(interval => (
                <button
                  key={interval}
                  onClick={() => {
                    setJumpInterval(interval);
                    setIsJumpMenuOpen(false);
                  }}
                  className={`w-full px-2 py-1 rounded text-xs font-mono flex items-center justify-between ${
                    jumpInterval === interval
                      ? 'bg-cyan-500/20 text-cyan-300 font-bold'
                      : 'text-zinc-300 hover:bg-zinc-800'
                  }`}
                >
                  <span>{interval} seconds</span>
                  {jumpInterval === interval && <Check className="w-3 h-3" />}
                </button>
              ))}
            </div>
          )}
        </div>

        {/* Volume & Mute */}
        <div className="flex items-center space-x-1.5">
          <button
            id="mute-toggle-btn"
            onClick={toggleMute}
            className="p-1.5 text-zinc-400 hover:text-zinc-200 transition-colors"
            title={isMuted ? 'Unmute' : 'Mute'}
          >
            {isMuted || volume === 0 ? (
              <VolumeX className="w-4 h-4 text-rose-400" />
            ) : (
              <Volume2 className="w-4 h-4" />
            )}
          </button>
          <input
            id="volume-slider"
            type="range"
            min="0"
            max="1"
            step="0.01"
            value={isMuted ? 0 : volume}
            onChange={e => setVolume(parseFloat(e.target.value))}
            className="w-16 sm:w-20 h-1 bg-zinc-800 rounded-lg appearance-none cursor-pointer accent-cyan-400"
          />
        </div>

        {/* Lock Screen Preview Button */}
        <button
          id="lockscreen-preview-btn"
          onClick={onOpenLockScreenPreview}
          title="Simulate Mobile Lock Screen & Notification Bar"
          className="p-1.5 text-zinc-400 hover:text-cyan-300 hover:bg-zinc-850 rounded-lg transition-colors border border-transparent hover:border-zinc-700"
        >
          <Smartphone className="w-4 h-4" />
        </button>
      </div>
    </div>
  );
};
