import React, { createContext, useContext, useEffect, useRef, useState, useCallback } from 'react';
import { ABLoopState, JumpInterval, SavedLoop, SleepTimerState, Track } from '../types';
import { generateDemoTracks } from '../utils/audioGenerator';

interface AudioContextType {
  // Audio state
  currentTrack: Track | null;
  isPlaying: boolean;
  currentTime: number;
  duration: number;
  volume: number;
  isMuted: boolean;
  playbackRate: number;
  jumpInterval: JumpInterval;
  isPitchPreserved: boolean;
  
  // Track list & library
  playlist: Track[];
  currentTrackIndex: number;
  
  // A-B Loop state
  loopState: ABLoopState;
  setPointA: (time?: number) => void;
  setPointB: (time?: number) => void;
  resetLoop: () => void;
  toggleLoopActive: () => void;
  setLoopTargetCount: (count: number) => void;
  fineTuneA: (deltaSeconds: number) => void;
  fineTuneB: (deltaSeconds: number) => void;
  jumpToPointA: () => void;
  jumpToPointB: () => void;
  setManualLoopTimes: (a: number | null, b: number | null) => void;
  saveCurrentLoop: (label: string, tag?: SavedLoop['tag'], color?: string) => void;
  applySavedLoop: (loop: SavedLoop) => void;
  deleteSavedLoop: (loopId: string) => void;
  
  // Sleep timer
  sleepTimer: SleepTimerState;
  startSleepTimer: (minutes: number, fadeDuration?: number) => void;
  setEndOfTrackTimer: (fadeDuration?: number) => void;
  cancelSleepTimer: () => void;
  
  // Controls
  playTrack: (track: Track, startTime?: number) => void;
  togglePlayPause: () => void;
  seekTo: (seconds: number) => void;
  skipForward: () => void;
  skipBackward: () => void;
  setVolume: (val: number) => void;
  toggleMute: () => void;
  setPlaybackRate: (rate: number) => void;
  setJumpInterval: (interval: JumpInterval) => void;
  playNextTrack: () => void;
  playPreviousTrack: () => void;
  addTracksToLibrary: (tracks: Track[], replaceAll?: boolean) => void;
  removeTrackFromLibrary: (trackId: string) => void;
  
  // Analyser node for live visualizer
  getLiveAudioData: () => { freqData: Uint8Array; timeData: Uint8Array } | null;
  bluetoothDisconnectedNotice: boolean;
  dismissBluetoothNotice: () => void;
}

const AudioContext = createContext<AudioContextType | null>(null);

export const AudioProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [playlist, setPlaylist] = useState<Track[]>([]);
  const [currentTrackIndex, setCurrentTrackIndex] = useState<number>(-1);
  const [isPlaying, setIsPlaying] = useState<boolean>(false);
  const [currentTime, setCurrentTime] = useState<number>(0);
  const [duration, setDuration] = useState<number>(0);
  const [volume, setVolumeState] = useState<number>(0.85);
  const [isMuted, setIsMuted] = useState<boolean>(false);
  const [playbackRate, setPlaybackRateState] = useState<number>(1.0);
  const [jumpInterval, setJumpInterval] = useState<JumpInterval>(10);
  const [isPitchPreserved, setIsPitchPreserved] = useState<boolean>(true);
  const [bluetoothDisconnectedNotice, setBluetoothDisconnectedNotice] = useState<boolean>(false);

  // A-B Loop State
  const [loopState, setLoopState] = useState<ABLoopState>({
    isActive: false,
    pointA: null,
    pointB: null,
    targetCount: 0, // 0 = infinite
    completedCount: 0,
  });

  // Sleep Timer State
  const [sleepTimer, setSleepTimer] = useState<SleepTimerState>({
    active: false,
    mode: 'countdown',
    totalDurationSeconds: 0,
    remainingSeconds: 0,
    fadeDuration: 30,
    isFading: false,
  });

  // Audio nodes & references
  const audioRef = useRef<HTMLAudioElement | null>(null);
  const audioCtxRef = useRef<AudioContext | null>(null);
  const gainNodeRef = useRef<GainNode | null>(null);
  const fadeGainNodeRef = useRef<GainNode | null>(null);
  const analyserRef = useRef<AnalyserNode | null>(null);
  const sourceNodeRef = useRef<MediaElementAudioSourceNode | null>(null);
  const loopStateRef = useRef(loopState);
  loopStateRef.current = loopState;
  const sleepTimerRef = useRef(sleepTimer);
  sleepTimerRef.current = sleepTimer;

  const currentTrack = currentTrackIndex >= 0 && playlist[currentTrackIndex] ? playlist[currentTrackIndex] : null;

  // Initialize Demo Tracks on first mount
  useEffect(() => {
    let isMounted = true;
    generateDemoTracks().then(demos => {
      if (isMounted) {
        setPlaylist(demos);
        if (demos.length > 0) {
          setCurrentTrackIndex(0);
          setDuration(demos[0].duration);
          // Pre-load default loop if present
          if (demos[0].savedLoops && demos[0].savedLoops.length > 0) {
            const first = demos[0].savedLoops[0];
            setLoopState({
              isActive: true,
              pointA: first.pointA,
              pointB: first.pointB,
              targetCount: first.loopCount,
              completedCount: 0,
            });
          }
        }
      }
    });
    return () => { isMounted = false; };
  }, []);

  // Initialize Web Audio graph
  const initWebAudio = useCallback(() => {
    if (!audioRef.current) return;

    if (!audioCtxRef.current) {
      const AudioCtx = window.AudioContext || (window as unknown as { webkitAudioContext: typeof AudioContext }).webkitAudioContext;
      const ctx = new AudioCtx();
      audioCtxRef.current = ctx;

      const gain = ctx.createGain();
      gain.gain.value = isMuted ? 0 : volume;
      gainNodeRef.current = gain;

      const fadeGain = ctx.createGain();
      fadeGain.gain.value = 1.0;
      fadeGainNodeRef.current = fadeGain;

      const analyser = ctx.createAnalyser();
      analyser.fftSize = 256;
      analyserRef.current = analyser;

      try {
        const source = ctx.createMediaElementSource(audioRef.current);
        sourceNodeRef.current = source;
        // Graph: Source -> Analyser -> Gain -> FadeGain -> Destination
        source.connect(analyser);
        analyser.connect(gain);
        gain.connect(fadeGain);
        fadeGain.connect(ctx.destination);
      } catch (e) {
        console.warn('Web Audio source connection notice:', e);
      }
    }

    if (audioCtxRef.current.state === 'suspended') {
      audioCtxRef.current.resume();
    }
  }, [volume, isMuted]);

  // Handle single HTMLAudioElement lifecycle
  useEffect(() => {
    const audio = new Audio();
    audio.preload = 'auto';
    audio.crossOrigin = 'anonymous';
    // Enable pitch preservation
    (audio as unknown as { preservesPitch: boolean }).preservesPitch = true;
    (audio as unknown as { mozPreservesPitch: boolean }).mozPreservesPitch = true;
    (audio as unknown as { webkitPreservesPitch: boolean }).webkitPreservesPitch = true;
    audioRef.current = audio;

    const onPlay = () => setIsPlaying(true);
    const onPause = () => setIsPlaying(false);
    const onLoadedMetadata = () => {
      if (audio.duration && !isNaN(audio.duration)) {
        setDuration(audio.duration);
      }
    };
    const onEnded = () => {
      // Check if End-of-track sleep timer is active
      if (sleepTimerRef.current.active && sleepTimerRef.current.mode === 'end_of_track') {
        setIsPlaying(false);
        setSleepTimer(prev => ({ ...prev, active: false }));
        return;
      }
      // Otherwise proceed to next track
      playNextTrack();
    };

    audio.addEventListener('play', onPlay);
    audio.addEventListener('pause', onPause);
    audio.addEventListener('loadedmetadata', onLoadedMetadata);
    audio.addEventListener('ended', onEnded);

    return () => {
      audio.removeEventListener('play', onPlay);
      audio.removeEventListener('pause', onPause);
      audio.removeEventListener('loadedmetadata', onLoadedMetadata);
      audio.removeEventListener('ended', onEnded);
      audio.pause();
    };
  }, []);

  // Update MediaSession API for lockscreen & background controls
  useEffect(() => {
    if (!('mediaSession' in navigator) || !currentTrack) return;

    try {
      navigator.mediaSession.metadata = new MediaMetadata({
        title: currentTrack.title,
        artist: currentTrack.artist,
        album: currentTrack.album,
        artwork: [
          { src: 'https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=512&auto=format&fit=crop&q=80', sizes: '512x512', type: 'image/jpeg' },
        ],
      });

      navigator.mediaSession.setActionHandler('play', () => {
        if (audioRef.current) {
          initWebAudio();
          audioRef.current.play();
        }
      });
      navigator.mediaSession.setActionHandler('pause', () => {
        if (audioRef.current) audioRef.current.pause();
      });
      navigator.mediaSession.setActionHandler('seekbackward', (details) => {
        const skip = details.seekOffset || jumpInterval;
        if (audioRef.current) {
          audioRef.current.currentTime = Math.max(0, audioRef.current.currentTime - skip);
        }
      });
      navigator.mediaSession.setActionHandler('seekforward', (details) => {
        const skip = details.seekOffset || jumpInterval;
        if (audioRef.current) {
          audioRef.current.currentTime = Math.min(duration, audioRef.current.currentTime + skip);
        }
      });
      navigator.mediaSession.setActionHandler('seekto', (details) => {
        if (audioRef.current && details.seekTime !== undefined) {
          audioRef.current.currentTime = details.seekTime;
        }
      });
      navigator.mediaSession.setActionHandler('previoustrack', () => {
        playPreviousTrack();
      });
      navigator.mediaSession.setActionHandler('nexttrack', () => {
        playNextTrack();
      });
    } catch (e) {
      console.warn('MediaSession configuration warning:', e);
    }
  }, [currentTrack, duration, jumpInterval]);

  // Device Change (Bluetooth / Headset Disconnect) listener
  useEffect(() => {
    if (!navigator.mediaDevices || !navigator.mediaDevices.addEventListener) return;

    const handleDeviceChange = () => {
      // Auto-pause on audio output device change (e.g. bluetooth disconnect)
      if (audioRef.current && !audioRef.current.paused) {
        audioRef.current.pause();
        setBluetoothDisconnectedNotice(true);
      }
    };

    navigator.mediaDevices.addEventListener('devicechange', handleDeviceChange);
    return () => {
      navigator.mediaDevices.removeEventListener('devicechange', handleDeviceChange);
    };
  }, []);

  // Millisecond-Accurate RAF Loop for Playhead Position & A-B Looping
  useEffect(() => {
    let animFrameId: number;

    const checkLoopAndSync = () => {
      const audio = audioRef.current;
      if (audio) {
        const cur = audio.currentTime;
        setCurrentTime(cur);

        // Update MediaSession position state when possible
        if ('mediaSession' in navigator && navigator.mediaSession.setPositionState && !isNaN(audio.duration) && audio.duration > 0) {
          try {
            navigator.mediaSession.setPositionState({
              duration: audio.duration,
              playbackRate: audio.playbackRate,
              position: Math.min(audio.currentTime, audio.duration),
            });
          } catch {
            // Ignore benign MediaSession synchronization exceptions
          }
        }

        // Check A-B Loop boundaries
        const ls = loopStateRef.current;
        if (ls.isActive && ls.pointA !== null && ls.pointB !== null && ls.pointB > ls.pointA) {
          if (cur >= ls.pointB) {
            // Reached Point B!
            const newCount = ls.completedCount + 1;
            if (ls.targetCount > 0 && newCount >= ls.targetCount) {
              // Completed all target loops
              audio.currentTime = ls.pointA;
              audio.pause();
              setLoopState(prev => ({ ...prev, completedCount: newCount, isActive: false }));
            } else {
              // Loop back to Point A smoothly
              audio.currentTime = ls.pointA;
              setLoopState(prev => ({ ...prev, completedCount: newCount }));
            }
          }
        }

        // Sleep Timer gradual fade logic for 'end_of_track' mode
        const st = sleepTimerRef.current;
        if (st.active && st.mode === 'end_of_track' && audio.duration > 0) {
          const timeLeftInTrack = audio.duration - cur;
          if (timeLeftInTrack <= st.fadeDuration && fadeGainNodeRef.current) {
            const fadeFraction = Math.max(0, timeLeftInTrack / st.fadeDuration);
            fadeGainNodeRef.current.gain.value = fadeFraction;
            if (!st.isFading) {
              setSleepTimer(prev => ({ ...prev, isFading: true }));
            }
          }
        }
      }

      animFrameId = requestAnimationFrame(checkLoopAndSync);
    };

    animFrameId = requestAnimationFrame(checkLoopAndSync);
    return () => cancelAnimationFrame(animFrameId);
  }, []);

  // Sleep Timer Countdown Interval (Every 1 second)
  useEffect(() => {
    if (!sleepTimer.active || sleepTimer.mode !== 'countdown') return;

    const interval = setInterval(() => {
      setSleepTimer(prev => {
        if (!prev.active || prev.mode !== 'countdown') return prev;

        const nextRemaining = prev.remainingSeconds - 1;

        if (nextRemaining <= 0) {
          // Timer expired! Stop audio
          if (audioRef.current) {
            audioRef.current.pause();
          }
          if (fadeGainNodeRef.current) {
            fadeGainNodeRef.current.gain.value = 1.0;
          }
          return {
            ...prev,
            active: false,
            remainingSeconds: 0,
            isFading: false,
          };
        }

        // Check if within fadeDuration (e.g. last 30 or 60s)
        const isFading = nextRemaining <= prev.fadeDuration;
        if (isFading && fadeGainNodeRef.current) {
          const fadeFraction = Math.max(0, nextRemaining / prev.fadeDuration);
          fadeGainNodeRef.current.gain.value = fadeFraction;
        }

        return {
          ...prev,
          remainingSeconds: nextRemaining,
          isFading,
        };
      });
    }, 1000);

    return () => clearInterval(interval);
  }, [sleepTimer.active, sleepTimer.mode]);

  // Play a specific track
  const playTrack = useCallback((track: Track, startTime?: number) => {
    const idx = playlist.findIndex(t => t.id === track.id);
    if (idx !== -1) {
      setCurrentTrackIndex(idx);
    }
    const audio = audioRef.current;
    if (!audio) return;

    initWebAudio();

    if (audio.src !== track.url) {
      audio.src = track.url;
      audio.load();
    }

    if (track.duration) {
      setDuration(track.duration);
    }

    // Restore or initialize loop state if the track has saved loops
    if (track.savedLoops && track.savedLoops.length > 0) {
      const topLoop = track.savedLoops[0];
      setLoopState({
        isActive: true,
        pointA: topLoop.pointA,
        pointB: topLoop.pointB,
        targetCount: topLoop.loopCount,
        completedCount: 0,
      });
    } else {
      setLoopState({
        isActive: false,
        pointA: null,
        pointB: null,
        targetCount: 0,
        completedCount: 0,
      });
    }

    const targetTime = startTime !== undefined ? startTime : (track.savedLoops?.[0]?.pointA || 0);
    audio.currentTime = targetTime;
    setCurrentTime(targetTime);

    audio.play().catch(err => {
      console.warn('Audio play request notice:', err);
    });
  }, [playlist, initWebAudio]);

  // Toggle Play / Pause
  const togglePlayPause = useCallback(() => {
    const audio = audioRef.current;
    if (!audio) return;

    initWebAudio();

    if (audio.paused) {
      if (!audio.src && currentTrack) {
        audio.src = currentTrack.url;
      }
      audio.play().catch(console.warn);
    } else {
      audio.pause();
    }
  }, [currentTrack, initWebAudio]);

  // Seek to position
  const seekTo = useCallback((seconds: number) => {
    const audio = audioRef.current;
    if (!audio) return;
    const clamped = Math.max(0, Math.min(duration || 1000, seconds));
    audio.currentTime = clamped;
    setCurrentTime(clamped);
  }, [duration]);

  // Skip buttons
  const skipForward = useCallback(() => {
    if (audioRef.current) {
      seekTo(audioRef.current.currentTime + jumpInterval);
    }
  }, [jumpInterval, seekTo]);

  const skipBackward = useCallback(() => {
    if (audioRef.current) {
      seekTo(audioRef.current.currentTime - jumpInterval);
    }
  }, [jumpInterval, seekTo]);

  // Volume & Mute
  const setVolume = useCallback((val: number) => {
    const clamped = Math.max(0, Math.min(1, val));
    setVolumeState(clamped);
    if (gainNodeRef.current) {
      gainNodeRef.current.gain.value = isMuted ? 0 : clamped;
    }
    if (audioRef.current) {
      audioRef.current.volume = clamped;
    }
  }, [isMuted]);

  const toggleMute = useCallback(() => {
    setIsMuted(prev => {
      const next = !prev;
      if (gainNodeRef.current) {
        gainNodeRef.current.gain.value = next ? 0 : volume;
      }
      if (audioRef.current) {
        audioRef.current.muted = next;
      }
      return next;
    });
  }, [volume]);

  // Playback Rate
  const setPlaybackRate = useCallback((rate: number) => {
    setPlaybackRateState(rate);
    if (audioRef.current) {
      audioRef.current.playbackRate = rate;
      (audioRef.current as unknown as { preservesPitch: boolean }).preservesPitch = isPitchPreserved;
    }
  }, [isPitchPreserved]);

  // Track navigation
  const playNextTrack = useCallback(() => {
    if (playlist.length === 0) return;
    const nextIdx = (currentTrackIndex + 1) % playlist.length;
    playTrack(playlist[nextIdx]);
  }, [playlist, currentTrackIndex, playTrack]);

  const playPreviousTrack = useCallback(() => {
    if (playlist.length === 0) return;
    const prevIdx = (currentTrackIndex - 1 + playlist.length) % playlist.length;
    playTrack(playlist[prevIdx]);
  }, [playlist, currentTrackIndex, playTrack]);

  // A-B Loop Operations
  const setPointA = useCallback((customTime?: number) => {
    const cur = customTime !== undefined ? customTime : (audioRef.current?.currentTime || 0);
    setLoopState(prev => {
      let b = prev.pointB;
      // If B exists and is before A, push B after A
      if (b !== null && b <= cur) {
        b = Math.min(duration || 1000, cur + 5);
      }
      return {
        ...prev,
        pointA: Math.max(0, cur),
        pointB: b,
        isActive: b !== null,
        completedCount: 0,
      };
    });
  }, [duration]);

  const setPointB = useCallback((customTime?: number) => {
    const cur = customTime !== undefined ? customTime : (audioRef.current?.currentTime || 0);
    setLoopState(prev => {
      let a = prev.pointA;
      // If A doesn't exist or is after B, set A before B
      if (a === null || a >= cur) {
        a = Math.max(0, cur - 5);
      }
      return {
        ...prev,
        pointA: a,
        pointB: cur,
        isActive: true,
        completedCount: 0,
      };
    });
  }, []);

  const resetLoop = useCallback(() => {
    setLoopState({
      isActive: false,
      pointA: null,
      pointB: null,
      targetCount: 0,
      completedCount: 0,
    });
  }, []);

  const toggleLoopActive = useCallback(() => {
    setLoopState(prev => {
      if (prev.pointA === null || prev.pointB === null) return prev;
      return {
        ...prev,
        isActive: !prev.isActive,
        completedCount: 0,
      };
    });
  }, []);

  const setLoopTargetCount = useCallback((count: number) => {
    setLoopState(prev => ({
      ...prev,
      targetCount: Math.max(0, count),
      completedCount: 0,
    }));
  }, []);

  const fineTuneA = useCallback((delta: number) => {
    setLoopState(prev => {
      if (prev.pointA === null) return prev;
      const newA = Math.max(0, prev.pointA + delta);
      // Ensure A stays before B
      if (prev.pointB !== null && newA >= prev.pointB) {
        return prev;
      }
      return {
        ...prev,
        pointA: newA,
        completedCount: 0,
      };
    });
  }, []);

  const fineTuneB = useCallback((delta: number) => {
    setLoopState(prev => {
      if (prev.pointB === null) return prev;
      const newB = Math.min(duration || 1000, prev.pointB + delta);
      // Ensure B stays after A
      if (prev.pointA !== null && newB <= prev.pointA) {
        return prev;
      }
      return {
        ...prev,
        pointB: newB,
        completedCount: 0,
      };
    });
  }, [duration]);

  const jumpToPointA = useCallback(() => {
    if (loopState.pointA !== null) {
      seekTo(loopState.pointA);
    }
  }, [loopState.pointA, seekTo]);

  const jumpToPointB = useCallback(() => {
    if (loopState.pointB !== null) {
      seekTo(loopState.pointB);
    }
  }, [loopState.pointB, seekTo]);

  const setManualLoopTimes = useCallback((a: number | null, b: number | null) => {
    setLoopState(prev => ({
      ...prev,
      pointA: a,
      pointB: b,
      isActive: a !== null && b !== null && b > a,
      completedCount: 0,
    }));
  }, []);

  // Save loop to current track
  const saveCurrentLoop = useCallback((label: string, tag: SavedLoop['tag'] = 'Music', color: string = '#06b6d4') => {
    if (!currentTrack || loopState.pointA === null || loopState.pointB === null) return;
    const newLoop: SavedLoop = {
      id: 'loop-' + Date.now() + '-' + Math.random().toString(36).substring(2, 6),
      label: label.trim() || `Loop ${formatTimeSimple(loopState.pointA)} - ${formatTimeSimple(loopState.pointB)}`,
      tag,
      color,
      pointA: loopState.pointA,
      pointB: loopState.pointB,
      loopCount: loopState.targetCount,
      createdAt: Date.now(),
    };

    setPlaylist(prev => prev.map(t => {
      if (t.id === currentTrack.id) {
        return {
          ...t,
          savedLoops: [newLoop, ...(t.savedLoops || [])],
        };
      }
      return t;
    }));
  }, [currentTrack, loopState]);

  const applySavedLoop = useCallback((loop: SavedLoop) => {
    setLoopState({
      isActive: true,
      pointA: loop.pointA,
      pointB: loop.pointB,
      targetCount: loop.loopCount,
      completedCount: 0,
    });
    seekTo(loop.pointA);
  }, [seekTo]);

  const deleteSavedLoop = useCallback((loopId: string) => {
    if (!currentTrack) return;
    setPlaylist(prev => prev.map(t => {
      if (t.id === currentTrack.id) {
        return {
          ...t,
          savedLoops: t.savedLoops.filter(l => l.id !== loopId),
        };
      }
      return t;
    }));
  }, [currentTrack]);

  // Sleep Timer controls
  const startSleepTimer = useCallback((minutes: number, fadeDuration: number = 30) => {
    const totalSeconds = minutes * 60;
    if (fadeGainNodeRef.current) {
      fadeGainNodeRef.current.gain.value = 1.0;
    }
    setSleepTimer({
      active: true,
      mode: 'countdown',
      totalDurationSeconds: totalSeconds,
      remainingSeconds: totalSeconds,
      fadeDuration,
      isFading: false,
    });
  }, []);

  const setEndOfTrackTimer = useCallback((fadeDuration: number = 30) => {
    if (fadeGainNodeRef.current) {
      fadeGainNodeRef.current.gain.value = 1.0;
    }
    setSleepTimer({
      active: true,
      mode: 'end_of_track',
      totalDurationSeconds: 0,
      remainingSeconds: 0,
      fadeDuration,
      isFading: false,
    });
  }, []);

  const cancelSleepTimer = useCallback(() => {
    if (fadeGainNodeRef.current) {
      fadeGainNodeRef.current.gain.value = 1.0;
    }
    setSleepTimer({
      active: false,
      mode: 'countdown',
      totalDurationSeconds: 0,
      remainingSeconds: 0,
      fadeDuration: 30,
      isFading: false,
    });
  }, []);

  // Library updates
  const addTracksToLibrary = useCallback((tracks: Track[], replaceAll: boolean = false) => {
    setPlaylist(prev => {
      if (replaceAll) return tracks;
      return [...prev, ...tracks];
    });
    if (tracks.length > 0 && (replaceAll || playlist.length === 0)) {
      playTrack(tracks[0]);
    }
  }, [playlist.length, playTrack]);

  const removeTrackFromLibrary = useCallback((trackId: string) => {
    setPlaylist(prev => {
      const next = prev.filter(t => t.id !== trackId);
      return next;
    });
  }, []);

  // Analyser live audio data for oscilloscope/bars
  const getLiveAudioData = useCallback(() => {
    if (!analyserRef.current) return null;
    const bufferLength = analyserRef.current.frequencyBinCount;
    const freqData = new Uint8Array(bufferLength);
    const timeData = new Uint8Array(bufferLength);
    analyserRef.current.getByteFrequencyData(freqData);
    analyserRef.current.getByteTimeDomainData(timeData);
    return { freqData, timeData };
  }, []);

  const dismissBluetoothNotice = useCallback(() => {
    setBluetoothDisconnectedNotice(false);
  }, []);

  return (
    <AudioContext.Provider
      value={{
        currentTrack,
        isPlaying,
        currentTime,
        duration,
        volume,
        isMuted,
        playbackRate,
        jumpInterval,
        isPitchPreserved,
        playlist,
        currentTrackIndex,
        loopState,
        setPointA,
        setPointB,
        resetLoop,
        toggleLoopActive,
        setLoopTargetCount,
        fineTuneA,
        fineTuneB,
        jumpToPointA,
        jumpToPointB,
        setManualLoopTimes,
        saveCurrentLoop,
        applySavedLoop,
        deleteSavedLoop,
        sleepTimer,
        startSleepTimer,
        setEndOfTrackTimer,
        cancelSleepTimer,
        playTrack,
        togglePlayPause,
        seekTo,
        skipForward,
        skipBackward,
        setVolume,
        toggleMute,
        setPlaybackRate,
        setJumpInterval,
        playNextTrack,
        playPreviousTrack,
        addTracksToLibrary,
        removeTrackFromLibrary,
        getLiveAudioData,
        bluetoothDisconnectedNotice,
        dismissBluetoothNotice,
      }}
    >
      {children}
    </AudioContext.Provider>
  );
};

export function useAudio() {
  const context = useContext(AudioContext);
  if (!context) {
    throw new Error('useAudio must be used within an AudioProvider');
  }
  return context;
}

function formatTimeSimple(sec: number): string {
  const m = Math.floor(sec / 60);
  const s = Math.floor(sec % 60);
  return `${m}:${s < 10 ? '0' : ''}${s}`;
}
