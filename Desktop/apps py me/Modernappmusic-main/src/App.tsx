import React, { useState } from 'react';
import { AudioProvider, useAudio } from './context/AudioContext';
import { WaveformVisualizer } from './components/WaveformVisualizer';
import { ABLoopControls } from './components/ABLoopControls';
import { PlaybackEngineControls } from './components/PlaybackEngineControls';
import { LibraryManager } from './components/LibraryManager';
import { SleepTimerModal } from './components/SleepTimerModal';
import { LockScreenModal } from './components/LockScreenModal';
import { formatTime } from './utils/formatters';
import {
  Repeat,
  Moon,
  Headphones,
  Smartphone,
  Info,
  ShieldCheck,
  Sparkles,
  AlertTriangle,
  Radio,
} from 'lucide-react';

const MainPlayerView: React.FC = () => {
  const {
    currentTrack,
    sleepTimer,
    bluetoothDisconnectedNotice,
    dismissBluetoothNotice,
    isPitchPreserved,
  } = useAudio();

  const [isSleepModalOpen, setIsSleepModalOpen] = useState(false);
  const [isLockScreenModalOpen, setIsLockScreenModalOpen] = useState(false);

  return (
    <div className="min-h-screen bg-black text-zinc-100 flex flex-col selection:bg-cyan-500 selection:text-black">
      {/* Top Navigation Bar */}
      <header className="sticky top-0 z-30 bg-zinc-950/90 border-b border-zinc-800/80 backdrop-blur-md px-4 py-3">
        <div className="max-w-7xl mx-auto flex items-center justify-between">
          {/* Logo & Title */}
          <div className="flex items-center space-x-2.5">
            <div className="w-8 h-8 rounded-xl bg-gradient-to-tr from-cyan-500 to-blue-600 flex items-center justify-center shadow-[0_0_15px_rgba(6,182,212,0.4)]">
              <Repeat className="w-4 h-4 text-zinc-950 font-bold" />
            </div>
            <div>
              <h1 className="text-sm sm:text-base font-bold tracking-tight text-zinc-100 flex items-center space-x-1.5">
                <span>A-B Loop Player</span>
                <span className="hidden sm:inline-block px-1.5 py-0.2 text-[10px] font-mono rounded bg-cyan-950 text-cyan-400 border border-cyan-800">
                  PRECISION REPEATER
                </span>
              </h1>
              <p className="text-[11px] text-zinc-400 hidden sm:block">
                Millisecond-accurate looping • Pitch-corrected speed • Storage scanner
              </p>
            </div>
          </div>

          {/* Quick Header Badges & Actions */}
          <div className="flex items-center space-x-2 sm:space-x-3">
            {/* Bluetooth / Headset auto-pause badge */}
            <div
              className="hidden md:flex items-center space-x-1 px-2.5 py-1 rounded-lg bg-zinc-900 border border-zinc-800 text-[11px] text-zinc-400"
              title="Bluetooth disconnect auto-pause active"
            >
              <Headphones className="w-3.5 h-3.5 text-cyan-400" />
              <span>Auto-Pause Guard</span>
            </div>

            {/* Sleep Timer Indicator & Trigger */}
            <button
              id="header-sleep-timer-btn"
              onClick={() => setIsSleepModalOpen(true)}
              className={`flex items-center space-x-1.5 px-3 py-1.5 rounded-xl text-xs font-medium border transition-colors ${
                sleepTimer.active
                  ? 'bg-indigo-950 text-indigo-300 border-indigo-500/80 shadow-[0_0_12px_rgba(99,102,241,0.25)] animate-pulse'
                  : 'bg-zinc-900 text-zinc-300 border-zinc-800 hover:border-zinc-700'
              }`}
            >
              <Moon className="w-3.5 h-3.5 text-indigo-400" />
              <span>
                {sleepTimer.active
                  ? sleepTimer.mode === 'countdown'
                    ? formatTime(sleepTimer.remainingSeconds, false)
                    : 'End of Song'
                  : 'Sleep Timer'}
              </span>
            </button>

            {/* Lock Screen Background Notification preview */}
            <button
              id="header-lockscreen-btn"
              onClick={() => setIsLockScreenModalOpen(true)}
              className="flex items-center space-x-1 px-3 py-1.5 rounded-xl bg-zinc-900 hover:bg-zinc-850 text-zinc-300 hover:text-white border border-zinc-800 text-xs font-medium transition-colors"
              title="Simulate Lock Screen & Background Notification controls"
            >
              <Smartphone className="w-3.5 h-3.5 text-cyan-400" />
              <span className="hidden sm:inline">Lock Screen</span>
            </button>
          </div>
        </div>
      </header>

      {/* Bluetooth Disconnect Notice Toast */}
      {bluetoothDisconnectedNotice && (
        <div className="bg-amber-950/90 border-b border-amber-800 text-amber-200 px-4 py-2 text-xs flex items-center justify-between animate-in slide-in-from-top duration-200">
          <div className="flex items-center space-x-2">
            <AlertTriangle className="w-4 h-4 text-amber-400 flex-shrink-0" />
            <span>
              <strong>Audio Device Disconnected:</strong> Playback was automatically paused to prevent accidental speaker output.
            </span>
          </div>
          <button
            onClick={dismissBluetoothNotice}
            className="px-2 py-0.5 bg-amber-900 text-amber-100 rounded text-[11px] hover:bg-amber-800"
          >
            Dismiss
          </button>
        </div>
      )}

      {/* Main Workspace Layout */}
      <main className="flex-1 max-w-7xl w-full mx-auto p-3 sm:p-5 lg:p-6 space-y-5">
        {/* SECTION 1: Interactive Waveform Visualizer */}
        <section aria-label="Audio Waveform Display">
          <WaveformVisualizer height={145} />
        </section>

        {/* SECTION 2: A-B Loop Controls & Segment Repeater */}
        <section aria-label="A-B Loop & Segment Repeater">
          <ABLoopControls />
        </section>

        {/* SECTION 3: Main Audio Playback Engine & Transport Controls */}
        <section aria-label="Playback Engine Controls">
          <PlaybackEngineControls
            onOpenLockScreenPreview={() => setIsLockScreenModalOpen(true)}
            onOpenSleepTimerModal={() => setIsSleepModalOpen(true)}
          />
        </section>

        {/* SECTION 4: Library Management & Storage Scanner */}
        <section aria-label="Audio Library & Scanner">
          <LibraryManager />
        </section>
      </main>

      {/* Modals */}
      <SleepTimerModal
        isOpen={isSleepModalOpen}
        onClose={() => setIsSleepModalOpen(false)}
      />

      <LockScreenModal
        isOpen={isLockScreenModalOpen}
        onClose={() => setIsLockScreenModalOpen(false)}
      />

      {/* Footer */}
      <footer className="border-t border-zinc-900 bg-zinc-950 py-4 px-4 text-center text-xs text-zinc-500 font-mono">
        <span>A-B Loop & Precise Segment Repeater • Web Audio API with MediaSession</span>
      </footer>
    </div>
  );
};

export default function App() {
  return (
    <AudioProvider>
      <MainPlayerView />
    </AudioProvider>
  );
}
