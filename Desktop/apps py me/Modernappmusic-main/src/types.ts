export interface SavedLoop {
  id: string;
  label: string;
  tag?: 'Music' | 'Lecture' | 'Practice' | 'Language' | 'Speech' | 'Custom';
  pointA: number; // in seconds (float with millisecond precision)
  pointB: number; // in seconds (float with millisecond precision)
  color: string;
  loopCount: number; // 0 for infinite, >0 for finite count
  createdAt: number;
}

export interface Track {
  id: string;
  title: string;
  artist: string;
  album: string;
  folder: string;
  duration: number; // in seconds
  url: string; // Blob URL or audio data URL
  fileSize?: number; // bytes
  waveformPeaks?: number[]; // normalized 0..1 peak points
  savedLoops: SavedLoop[];
  createdAt: number;
  isDemo?: boolean;
}

export interface ABLoopState {
  isActive: boolean;
  pointA: number | null;
  pointB: number | null;
  targetCount: number; // 0 = infinite (∞), >0 = specific repeats
  completedCount: number;
}

export interface SleepTimerState {
  active: boolean;
  mode: 'countdown' | 'end_of_track';
  totalDurationSeconds: number;
  remainingSeconds: number;
  fadeDuration: number; // e.g. 30 or 60 seconds
  isFading: boolean;
}

export type JumpInterval = 5 | 10 | 30;

export type LibraryCategory = 'tracks' | 'folders' | 'albums' | 'artists' | 'playlists';

export interface Playlist {
  id: string;
  name: string;
  trackIds: string[];
  createdAt: number;
}

export interface ScanFilters {
  minDurationSeconds: number;
  excludePatterns: string[];
  supportedExtensions: string[];
}
