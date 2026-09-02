import React, { useState } from 'react';
import { useAudio } from '../context/AudioContext';
import { Moon, Clock, VolumeX, Check, AlertCircle, X } from 'lucide-react';
import { formatTime } from '../utils/formatters';

interface SleepTimerModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export const SleepTimerModal: React.FC<SleepTimerModalProps> = ({ isOpen, onClose }) => {
  const { sleepTimer, startSleepTimer, setEndOfTrackTimer, cancelSleepTimer, currentTrack } = useAudio();

  const [selectedMinutes, setSelectedMinutes] = useState<number>(30);
  const [customMinutesInput, setCustomMinutesInput] = useState<string>('20');
  const [isCustomMode, setIsCustomMode] = useState<boolean>(false);
  const [fadeDuration, setFadeDuration] = useState<number>(30); // 30 or 60 seconds

  if (!isOpen) return null;

  const presets = [15, 30, 45, 60];

  const handleStartCountdown = () => {
    const mins = isCustomMode ? Math.max(1, parseInt(customMinutesInput) || 15) : selectedMinutes;
    startSleepTimer(mins, fadeDuration);
    onClose();
  };

  const handleSetEndOfTrack = () => {
    setEndOfTrackTimer(fadeDuration);
    onClose();
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 backdrop-blur-sm p-4 animate-in fade-in">
      <div className="bg-zinc-900 border border-zinc-700/80 rounded-2xl max-w-md w-full p-5 shadow-2xl space-y-4">
        {/* Header */}
        <div className="flex items-center justify-between pb-2 border-b border-zinc-800">
          <div className="flex items-center space-x-2">
            <div className="p-2 rounded-xl bg-indigo-950/80 text-indigo-400 border border-indigo-800/60">
              <Moon className="w-5 h-5" />
            </div>
            <div>
              <h2 className="font-bold text-base text-zinc-100">Sleep Timer</h2>
              <p className="text-xs text-zinc-400">Scheduled playback & gradual volume fade-out</p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-1 rounded-lg text-zinc-400 hover:text-zinc-200 hover:bg-zinc-800"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Active Timer Status Banner */}
        {sleepTimer.active && (
          <div className="p-3 bg-indigo-950/40 border border-indigo-500/50 rounded-xl flex items-center justify-between">
            <div className="space-y-0.5">
              <div className="flex items-center space-x-2 text-xs font-semibold text-indigo-300">
                <Clock className="w-3.5 h-3.5 animate-pulse" />
                <span>
                  {sleepTimer.mode === 'countdown'
                    ? `Active: ${formatTime(sleepTimer.remainingSeconds, false)} remaining`
                    : 'Active: Stopping at end of track'}
                </span>
              </div>
              {sleepTimer.isFading && (
                <div className="flex items-center space-x-1 text-[11px] text-amber-400 font-mono">
                  <VolumeX className="w-3 h-3 animate-bounce" />
                  <span>Fading out audio smoothly...</span>
                </div>
              )}
            </div>
            <button
              onClick={cancelSleepTimer}
              className="px-3 py-1 bg-red-950/60 hover:bg-red-900 border border-red-800 text-red-300 rounded-lg text-xs font-medium"
            >
              Cancel
            </button>
          </div>
        )}

        {/* Countdown Presets */}
        <div className="space-y-2">
          <label className="block text-xs font-semibold text-zinc-300 uppercase tracking-wider">
            Countdown Presets
          </label>
          <div className="grid grid-cols-4 gap-2">
            {presets.map(mins => (
              <button
                key={mins}
                onClick={() => {
                  setSelectedMinutes(mins);
                  setIsCustomMode(false);
                }}
                className={`py-2 px-1 text-center rounded-xl text-xs font-semibold border transition-all ${
                  !isCustomMode && selectedMinutes === mins
                    ? 'bg-indigo-600 text-white border-indigo-400 shadow-md'
                    : 'bg-zinc-950 border-zinc-800 text-zinc-300 hover:border-zinc-700'
                }`}
              >
                {mins} min
              </button>
            ))}
          </div>

          {/* Custom Time */}
          <div className="pt-1">
            <button
              onClick={() => setIsCustomMode(true)}
              className={`w-full p-2 rounded-xl text-xs flex items-center justify-between border transition-all ${
                isCustomMode
                  ? 'bg-zinc-950 border-indigo-500 text-indigo-300'
                  : 'bg-zinc-950/60 border-zinc-800 text-zinc-400 hover:text-zinc-200'
              }`}
            >
              <span>Custom Duration:</span>
              <div className="flex items-center space-x-1.5" onClick={e => e.stopPropagation()}>
                <input
                  type="number"
                  min="1"
                  max="480"
                  value={customMinutesInput}
                  onFocus={() => setIsCustomMode(true)}
                  onChange={e => {
                    setCustomMinutesInput(e.target.value);
                    setIsCustomMode(true);
                  }}
                  className="w-16 px-2 py-1 bg-zinc-900 border border-zinc-700 rounded-lg text-xs font-mono text-center text-zinc-100"
                />
                <span className="text-zinc-400 text-xs">minutes</span>
              </div>
            </button>
          </div>
        </div>

        {/* End of Track Option */}
        <div className="pt-2 border-t border-zinc-800">
          <label className="block text-xs font-semibold text-zinc-300 uppercase tracking-wider mb-2">
            Track Termination
          </label>
          <button
            id="end-of-track-btn"
            onClick={handleSetEndOfTrack}
            className="w-full p-3 rounded-xl bg-zinc-950 hover:bg-zinc-850 border border-zinc-800 hover:border-indigo-500/60 text-left transition-all flex items-center justify-between group"
          >
            <div>
              <div className="text-xs font-bold text-zinc-200 group-hover:text-indigo-300">
                End of Current Track
              </div>
              <div className="text-[11px] text-zinc-400">
                Fade out smoothly and pause after "{currentTrack?.title?.substring(0, 30) || 'Current song'}" finishes.
              </div>
            </div>
            <Check className="w-4 h-4 text-indigo-400 opacity-0 group-hover:opacity-100 transition-opacity" />
          </button>
        </div>

        {/* Gradual Volume Fade-Out Settings */}
        <div className="pt-2 border-t border-zinc-800 space-y-1.5">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-zinc-300 uppercase tracking-wider">
              Gradual Volume Fade-Out
            </span>
            <span className="text-[11px] text-indigo-400 font-mono">{fadeDuration}s before stop</span>
          </div>
          <p className="text-[11px] text-zinc-400">
            Prevents sudden waking by lowering gain smoothly over the final segment.
          </p>
          <div className="flex items-center space-x-2 pt-1">
            {[30, 60].map(durationSec => (
              <button
                key={durationSec}
                onClick={() => setFadeDuration(durationSec)}
                className={`flex-1 py-1.5 px-2 rounded-lg text-xs font-mono border transition-all ${
                  fadeDuration === durationSec
                    ? 'bg-indigo-950/80 border-indigo-500 text-indigo-300 font-bold'
                    : 'bg-zinc-950 border-zinc-800 text-zinc-400 hover:text-zinc-200'
                }`}
              >
                {durationSec}s Fade
              </button>
            ))}
          </div>
        </div>

        {/* Footer Actions */}
        <div className="flex items-center justify-end space-x-2 pt-3 border-t border-zinc-800">
          <button
            onClick={onClose}
            className="px-4 py-2 bg-zinc-800 hover:bg-zinc-700 text-zinc-300 text-xs font-medium rounded-xl"
          >
            Close
          </button>
          <button
            id="start-sleep-timer-btn"
            onClick={handleStartCountdown}
            className="px-5 py-2 bg-indigo-600 hover:bg-indigo-500 text-white text-xs font-bold rounded-xl shadow-lg"
          >
            Start Countdown Timer
          </button>
        </div>
      </div>
    </div>
  );
};
