import { extractWaveformPeaks } from './audioGenerator';
import { ScanFilters, Track } from '../types';

let audioContext: AudioContext | null = null;
function getAudioContext(): AudioContext {
  if (!audioContext) {
    const AudioCtx = window.AudioContext || (window as unknown as { webkitAudioContext: typeof AudioContext }).webkitAudioContext;
    audioContext = new AudioCtx();
  }
  if (audioContext.state === 'suspended') {
    audioContext.resume();
  }
  return audioContext;
}

export async function processAudioFile(file: File): Promise<Track> {
  const url = URL.createObjectURL(file);
  const path = (file as unknown as { webkitRelativePath?: string }).webkitRelativePath || file.name;
  
  // Extract folder from webkitRelativePath if available
  let folder = '/Root';
  if (path.includes('/')) {
    const segments = path.split('/');
    segments.pop(); // remove file name
    folder = '/' + segments.join('/');
  }

  // Clean title from file name
  const nameWithoutExt = file.name.replace(/\.[^/.]+$/, '');
  let artist = 'Local Audio';
  let title = nameWithoutExt;
  let album = folder === '/Root' ? 'Unsorted' : folder.split('/').pop() || 'Unknown Album';

  // If filename looks like "Artist - Title"
  if (nameWithoutExt.includes(' - ')) {
    const parts = nameWithoutExt.split(' - ');
    artist = parts[0].trim();
    title = parts.slice(1).join(' - ').trim();
  }

  // Decode audio to extract exact duration and waveform peaks
  let duration = 0;
  let peaks: number[] = [];

  try {
    const arrayBuffer = await file.arrayBuffer();
    const ctx = getAudioContext();
    const audioBuffer = await ctx.decodeAudioData(arrayBuffer);
    duration = audioBuffer.duration;
    peaks = extractWaveformPeaks(audioBuffer, 200);
  } catch (err) {
    console.warn('Could not decode audio buffer with Web Audio, falling back to HTMLAudioElement:', err);
    // Fallback: load in audio element to at least get duration
    duration = await getAudioDuration(url);
    // Generate synthetic plausible peaks
    peaks = generateSyntheticPeaks(200);
  }

  return {
    id: 'track-' + Math.random().toString(36).substring(2, 9) + '-' + Date.now(),
    title,
    artist,
    album,
    folder,
    duration,
    url,
    fileSize: file.size,
    waveformPeaks: peaks,
    savedLoops: [],
    createdAt: Date.now(),
  };
}

function getAudioDuration(url: string): Promise<number> {
  return new Promise((resolve) => {
    const tempAudio = new Audio(url);
    tempAudio.addEventListener('loadedmetadata', () => {
      resolve(tempAudio.duration || 0);
    });
    tempAudio.addEventListener('error', () => {
      resolve(0);
    });
  });
}

function generateSyntheticPeaks(numPeaks: number): number[] {
  const peaks: number[] = [];
  for (let i = 0; i < numPeaks; i++) {
    const r = Math.sin(i * 0.15) * 0.4 + Math.cos(i * 0.35) * 0.3 + 0.4 + Math.random() * 0.2;
    peaks.push(Math.max(0.08, Math.min(1.0, r)));
  }
  return peaks;
}

// Filter files against scan filters
export function shouldIncludeFile(file: File, filters: ScanFilters): boolean {
  const fileName = file.name.toLowerCase();
  const path = ((file as unknown as { webkitRelativePath?: string }).webkitRelativePath || file.name).toLowerCase();

  // 1. Extension check
  const hasExt = filters.supportedExtensions.some(ext => fileName.endsWith(ext.toLowerCase()));
  if (!hasExt) return false;

  // 2. Exclude patterns (e.g. voice_note, sfx, game, ringtone)
  for (const pattern of filters.excludePatterns) {
    if (pattern && (fileName.includes(pattern.toLowerCase()) || path.includes(pattern.toLowerCase()))) {
      return false;
    }
  }

  return true;
}
