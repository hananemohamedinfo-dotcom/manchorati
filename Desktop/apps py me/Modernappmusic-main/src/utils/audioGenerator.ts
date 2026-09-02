import { SavedLoop, Track } from '../types';

// Convert an AudioBuffer to a WAV blob URL
export function audioBufferToWavBlob(buffer: AudioBuffer): Blob {
  const numOfChan = buffer.numberOfChannels;
  const length = buffer.length * numOfChan * 2 + 44;
  const out = new DataView(new ArrayBuffer(length));
  const channels: Float32Array[] = [];
  let sample: number = 0;
  let offset = 0;
  let pos = 0;

  function setUint16(data: number) {
    out.setUint16(pos, data, true);
    pos += 2;
  }
  function setUint32(data: number) {
    out.setUint32(pos, data, true);
    pos += 4;
  }

  // RIFF chunk descriptor
  setUint32(0x46464952); // "RIFF"
  setUint32(length - 8); // file length - 8
  setUint32(0x45564157); // "WAVE"

  // FMT sub-chunk
  setUint32(0x20746d66); // "fmt "
  setUint32(16); // Subchunk1Size (16 for PCM)
  setUint16(1); // AudioFormat (1 for PCM)
  setUint16(numOfChan);
  setUint32(buffer.sampleRate);
  setUint32(buffer.sampleRate * 2 * numOfChan); // ByteRate
  setUint16(numOfChan * 2); // BlockAlign
  setUint16(16); // BitsPerSample

  // data sub-chunk
  setUint32(0x61746164); // "data"
  setUint32(length - pos - 4);

  for (let i = 0; i < buffer.numberOfChannels; i++) {
    channels.push(buffer.getChannelData(i));
  }

  while (offset < buffer.length) {
    for (let i = 0; i < numOfChan; i++) {
      sample = Math.max(-1, Math.min(1, channels[i][offset]));
      sample = (0.5 + sample < 0 ? sample * 32768 : sample * 32767) | 0;
      out.setInt16(pos, sample, true);
      pos += 2;
    }
    offset++;
  }

  return new Blob([out.buffer], { type: 'audio/wav' });
}

// Compute peaks from an AudioBuffer for fast waveform rendering
export function extractWaveformPeaks(buffer: AudioBuffer, numPeaks: number = 180): number[] {
  const channelData = buffer.getChannelData(0);
  const step = Math.floor(channelData.length / numPeaks);
  const peaks: number[] = [];

  for (let i = 0; i < numPeaks; i++) {
    const start = i * step;
    let max = 0;
    for (let j = 0; j < step; j++) {
      const val = Math.abs(channelData[start + j] || 0);
      if (val > max) max = val;
    }
    peaks.push(Math.min(1, Math.max(0.05, max)));
  }
  return peaks;
}

// Generate realistic music/speech demo tracks using Web Audio synthesis
export async function generateDemoTracks(): Promise<Track[]> {
  const sampleRate = 44100;

  // 1. Cosmic Pulse (Synthwave / Electro) - 36 seconds
  const synthwaveBuffer = await generateSynthwaveBuffer(sampleRate, 36);
  const synthwaveBlob = audioBufferToWavBlob(synthwaveBuffer);
  const synthwavePeaks = extractWaveformPeaks(synthwaveBuffer);

  const synthLoops: SavedLoop[] = [
    {
      id: 'loop-synth-1',
      label: 'Main Synth Arp Hook',
      tag: 'Practice',
      pointA: 4.0,
      pointB: 12.0,
      color: '#06b6d4', // cyan
      loopCount: 0, // infinite
      createdAt: Date.now() - 50000,
    },
    {
      id: 'loop-synth-2',
      label: 'Bassline Drop & Groove',
      tag: 'Music',
      pointA: 12.0,
      pointB: 24.0,
      color: '#8b5cf6', // purple
      loopCount: 5,
      createdAt: Date.now() - 40000,
    },
    {
      id: 'loop-synth-3',
      label: 'Outro Melody Rehearsal',
      tag: 'Practice',
      pointA: 24.0,
      pointB: 32.0,
      color: '#10b981', // emerald
      loopCount: 3,
      createdAt: Date.now() - 30000,
    },
  ];

  // 2. Classical Acoustic Fingerstyle - 32 seconds
  const acousticBuffer = await generateAcousticBuffer(sampleRate, 32);
  const acousticBlob = audioBufferToWavBlob(acousticBuffer);
  const acousticPeaks = extractWaveformPeaks(acousticBuffer);

  const acousticLoops: SavedLoop[] = [
    {
      id: 'loop-ac-1',
      label: 'Intro Chord Progression',
      tag: 'Practice',
      pointA: 0.0,
      pointB: 8.0,
      color: '#f59e0b', // amber
      loopCount: 0,
      createdAt: Date.now() - 20000,
    },
    {
      id: 'loop-ac-2',
      label: 'Fast Hammer-on Riff',
      tag: 'Practice',
      pointA: 8.0,
      pointB: 16.0,
      color: '#ec4899', // pink
      loopCount: 10,
      createdAt: Date.now() - 10000,
    },
  ];

  // 3. Audio Lecture: Cognitive Neuroscience - 40 seconds
  const lectureBuffer = await generateLectureBuffer(sampleRate, 40);
  const lectureBlob = audioBufferToWavBlob(lectureBuffer);
  const lecturePeaks = extractWaveformPeaks(lectureBuffer);

  const lectureLoops: SavedLoop[] = [
    {
      id: 'loop-lec-1',
      label: 'Key Concept: Synaptic Plasticity',
      tag: 'Lecture',
      pointA: 5.5,
      pointB: 15.2,
      color: '#3b82f6', // blue
      loopCount: 3,
      createdAt: Date.now() - 8000,
    },
    {
      id: 'loop-lec-2',
      label: 'Exam Review: Hippocampus Role',
      tag: 'Lecture',
      pointA: 20.0,
      pointB: 32.5,
      color: '#6366f1', // indigo
      loopCount: 0,
      createdAt: Date.now() - 4000,
    },
  ];

  // 4. Spanish Pronunciation & Verb Drill - 30 seconds
  const languageBuffer = await generateLanguageDrillBuffer(sampleRate, 30);
  const languageBlob = audioBufferToWavBlob(languageBuffer);
  const languagePeaks = extractWaveformPeaks(languageBuffer);

  const languageLoops: SavedLoop[] = [
    {
      id: 'loop-lang-1',
      label: 'Rolling R Drill ("Ferrocarril")',
      tag: 'Language',
      pointA: 3.0,
      pointB: 9.0,
      color: '#14b8a6', // teal
      loopCount: 5,
      createdAt: Date.now() - 2000,
    },
    {
      id: 'loop-lang-2',
      label: 'Subjunctive Clause Cadence',
      tag: 'Language',
      pointA: 12.0,
      pointB: 22.0,
      color: '#f97316', // orange
      loopCount: 0,
      createdAt: Date.now() - 1000,
    },
  ];

  return [
    {
      id: 'demo-track-1',
      title: 'Neon Odyssey (Synthwave Master)',
      artist: 'Aura Studio',
      album: 'Synthesized Horizons',
      folder: '/Music/Rehearsals',
      duration: 36,
      url: URL.createObjectURL(synthwaveBlob),
      fileSize: synthwaveBlob.size,
      waveformPeaks: synthwavePeaks,
      savedLoops: synthLoops,
      createdAt: Date.now() - 100000,
      isDemo: true,
    },
    {
      id: 'demo-track-2',
      title: 'Acoustic Fingerstyle Study in D-Minor',
      artist: 'Julian Reyes',
      album: 'Solo Guitar Practice Vol. 1',
      folder: '/Music/Rehearsals',
      duration: 32,
      url: URL.createObjectURL(acousticBlob),
      fileSize: acousticBlob.size,
      waveformPeaks: acousticPeaks,
      savedLoops: acousticLoops,
      createdAt: Date.now() - 90000,
      isDemo: true,
    },
    {
      id: 'demo-track-3',
      title: 'Lecture 04: Synaptic Plasticity & Memory',
      artist: 'Prof. S. Vance',
      album: 'Neuroscience 201',
      folder: '/Lectures/Science',
      duration: 40,
      url: URL.createObjectURL(lectureBlob),
      fileSize: lectureBlob.size,
      waveformPeaks: lecturePeaks,
      savedLoops: lectureLoops,
      createdAt: Date.now() - 80000,
      isDemo: true,
    },
    {
      id: 'demo-track-4',
      title: 'Conversational Drill: Spanish Cadence',
      artist: 'Linguistics Lab',
      album: 'Accent & Fluency Series',
      folder: '/Language Practice',
      duration: 30,
      url: URL.createObjectURL(languageBlob),
      fileSize: languageBlob.size,
      waveformPeaks: languagePeaks,
      savedLoops: languageLoops,
      createdAt: Date.now() - 70000,
      isDemo: true,
    },
  ];
}

// 1. Synthwave Generator
async function generateSynthwaveBuffer(sampleRate: number, duration: number): Promise<AudioBuffer> {
  const ctx = new OfflineAudioContext(2, sampleRate * duration, sampleRate);
  const bpm = 120;
  const beatSec = 60 / bpm;

  // Bass chords progression: Am - F - C - G
  const chordNotes = [
    [220, 261.63, 329.63], // A4, C5, E5
    [174.61, 220, 261.63], // F4, A4, C5
    [130.81, 164.81, 196.0], // C4, E4, G4
    [196.0, 246.94, 293.66], // G4, B4, D5
  ];

  const totalBeats = Math.floor(duration / beatSec);

  // Kick & snare drum loop
  for (let b = 0; b < totalBeats; b++) {
    const t = b * beatSec;
    // Kick on 1 and 3
    if (b % 4 === 0 || b % 4 === 2) {
      const osc = ctx.createOscillator();
      const gain = ctx.createGain();
      osc.frequency.setValueAtTime(140, t);
      osc.frequency.exponentialRampToValueAtTime(38, t + 0.15);
      gain.gain.setValueAtTime(0.7, t);
      gain.gain.exponentialRampToValueAtTime(0.001, t + 0.25);
      osc.connect(gain);
      gain.connect(ctx.destination);
      osc.start(t);
      osc.stop(t + 0.25);
    }
    // Snare on 2 and 4
    if (b % 4 === 1 || b % 4 === 3) {
      const noiseNode = createNoiseNode(ctx, t, 0.18, 0.4);
      noiseNode.connect(ctx.destination);
    }
    // Hi-hat every 8th note
    for (let sub = 0; sub < 2; sub++) {
      const subT = t + sub * (beatSec / 2);
      const hat = createNoiseNode(ctx, subT, 0.05, 0.15, 6000);
      hat.connect(ctx.destination);
    }
  }

  // Synth Bass Arpeggio
  const bassNotes = [55, 65.41, 73.42, 82.41, 98, 110];
  const sixteenthSec = beatSec / 4;
  const total16ths = Math.floor(duration / sixteenthSec);

  for (let s = 0; s < total16ths; s++) {
    const t = s * sixteenthSec;
    const noteIdx = s % bassNotes.length;
    const freq = bassNotes[noteIdx];

    const osc = ctx.createOscillator();
    const filter = ctx.createBiquadFilter();
    const gain = ctx.createGain();

    osc.type = 'sawtooth';
    osc.frequency.setValueAtTime(freq, t);

    filter.type = 'lowpass';
    filter.frequency.setValueAtTime(800 + Math.sin(t * 0.5) * 500, t);

    gain.gain.setValueAtTime(0.18, t);
    gain.gain.exponentialRampToValueAtTime(0.001, t + sixteenthSec * 0.9);

    osc.connect(filter);
    filter.connect(gain);
    gain.connect(ctx.destination);

    osc.start(t);
    osc.stop(t + sixteenthSec * 0.9);
  }

  // Lead Pad chords
  const measureSec = beatSec * 4;
  const totalMeasures = Math.floor(duration / measureSec);

  for (let m = 0; m < totalMeasures; m++) {
    const chord = chordNotes[m % chordNotes.length];
    const t = m * measureSec;

    chord.forEach((freq) => {
      const osc = ctx.createOscillator();
      const gain = ctx.createGain();
      osc.type = 'sine';
      osc.frequency.setValueAtTime(freq, t);
      // Gentle vibrato
      gain.gain.setValueAtTime(0.001, t);
      gain.gain.linearRampToValueAtTime(0.08, t + 0.5);
      gain.gain.setValueAtTime(0.08, t + measureSec - 0.4);
      gain.gain.linearRampToValueAtTime(0.001, t + measureSec);

      osc.connect(gain);
      gain.connect(ctx.destination);
      osc.start(t);
      osc.stop(t + measureSec);
    });
  }

  return await ctx.startRendering();
}

// 2. Acoustic Fingerstyle Generator
async function generateAcousticBuffer(sampleRate: number, duration: number): Promise<AudioBuffer> {
  const ctx = new OfflineAudioContext(2, sampleRate * duration, sampleRate);
  const patterns = [
    [146.83, 220.0, 261.63, 293.66, 349.23, 440.0], // D minor arpeggio
    [130.81, 196.0, 261.63, 329.63, 392.0, 523.25], // C major arpeggio
    [116.54, 174.61, 233.08, 293.66, 349.23, 466.16], // Bb major arpeggio
    [110.0, 164.81, 220.0, 277.18, 329.63, 440.0], // A7 arpeggio
  ];

  const noteDuration = 0.28;
  const totalNotes = Math.floor(duration / noteDuration);

  for (let i = 0; i < totalNotes; i++) {
    const t = i * noteDuration;
    const chordIdx = Math.floor(i / 12) % patterns.length;
    const chord = patterns[chordIdx];
    const noteIdx = i % chord.length;
    const freq = chord[noteIdx];

    // Simulate plucked acoustic string (triangle + sine + decay)
    const osc = ctx.createOscillator();
    const gain = ctx.createGain();

    osc.type = noteIdx === 0 ? 'sine' : 'triangle';
    osc.frequency.setValueAtTime(freq, t);

    // Initial pluck attack
    gain.gain.setValueAtTime(0.001, t);
    gain.gain.linearRampToValueAtTime(0.24, t + 0.015);
    gain.gain.exponentialRampToValueAtTime(0.001, t + 0.9);

    osc.connect(gain);
    gain.connect(ctx.destination);

    osc.start(t);
    osc.stop(t + 0.9);
  }

  return await ctx.startRendering();
}

// 3. Audio Lecture Generator (Simulated acoustic resonance & human speech formants)
async function generateLectureBuffer(sampleRate: number, duration: number): Promise<AudioBuffer> {
  const ctx = new OfflineAudioContext(2, sampleRate * duration, sampleRate);

  // Formant frequencies for simulated voice syllables
  const vowels = [
    [700, 1200, 2800], // /a/
    [300, 2300, 3000], // /i/
    [500, 1000, 2500], // /o/
    [400, 2000, 2700], // /e/
  ];

  const phraseLength = 2.8;
  const totalPhrases = Math.floor(duration / phraseLength);

  for (let p = 0; p < totalPhrases; p++) {
    const phraseStart = p * phraseLength + 0.3;
    const syllables = 7;
    const sylDuration = 0.32;

    for (let s = 0; s < syllables; s++) {
      const t = phraseStart + s * sylDuration;
      const f0 = 125 + Math.sin(s * 1.2) * 18; // Vocal pitch contour

      const osc = ctx.createOscillator();
      osc.type = 'sawtooth';
      osc.frequency.setValueAtTime(f0, t);

      const f1 = ctx.createBiquadFilter();
      f1.type = 'bandpass';
      f1.frequency.setValueAtTime(vowels[s % vowels.length][0], t);
      f1.Q.setValueAtTime(4.0, t);

      const gain = ctx.createGain();
      gain.gain.setValueAtTime(0.001, t);
      gain.gain.linearRampToValueAtTime(0.18, t + 0.04);
      gain.gain.exponentialRampToValueAtTime(0.001, t + sylDuration * 0.85);

      osc.connect(f1);
      f1.connect(gain);
      gain.connect(ctx.destination);

      osc.start(t);
      osc.stop(t + sylDuration * 0.85);
    }
  }

  return await ctx.startRendering();
}

// 4. Language Drill Generator (Cadence drills with metronome & spoken prompts)
async function generateLanguageDrillBuffer(sampleRate: number, duration: number): Promise<AudioBuffer> {
  const ctx = new OfflineAudioContext(2, sampleRate * duration, sampleRate);
  const step = 0.5;
  const totalSteps = Math.floor(duration / step);

  for (let i = 0; i < totalSteps; i++) {
    const t = i * step;

    // Metronome tick
    const click = ctx.createOscillator();
    const clickGain = ctx.createGain();
    click.type = 'sine';
    click.frequency.setValueAtTime(i % 4 === 0 ? 880 : 440, t);
    clickGain.gain.setValueAtTime(0.08, t);
    clickGain.gain.exponentialRampToValueAtTime(0.0001, t + 0.04);
    click.connect(clickGain);
    clickGain.connect(ctx.destination);
    click.start(t);
    click.stop(t + 0.05);

    // Harmonic response tone
    if (i % 2 === 0) {
      const tone = ctx.createOscillator();
      const toneGain = ctx.createGain();
      tone.type = 'triangle';
      tone.frequency.setValueAtTime(261.63 + (i % 8) * 30, t + 0.1);
      toneGain.gain.setValueAtTime(0.001, t + 0.1);
      toneGain.gain.linearRampToValueAtTime(0.12, t + 0.15);
      toneGain.gain.exponentialRampToValueAtTime(0.001, t + 0.4);
      tone.connect(toneGain);
      toneGain.connect(ctx.destination);
      tone.start(t + 0.1);
      tone.stop(t + 0.45);
    }
  }

  return await ctx.startRendering();
}

// Helper: Synthesize noise burst for drums/hi-hats
function createNoiseNode(
  ctx: BaseAudioContext,
  time: number,
  duration: number,
  gainLevel: number,
  highpassFreq = 1000
) {
  const bufferSize = ctx.sampleRate * duration;
  const buffer = ctx.createBuffer(1, bufferSize, ctx.sampleRate);
  const data = buffer.getChannelData(0);
  for (let i = 0; i < bufferSize; i++) {
    data[i] = Math.random() * 2 - 1;
  }

  const noise = ctx.createBufferSource();
  noise.buffer = buffer;

  const filter = ctx.createBiquadFilter();
  filter.type = 'highpass';
  filter.frequency.setValueAtTime(highpassFreq, time);

  const gain = ctx.createGain();
  gain.gain.setValueAtTime(gainLevel, time);
  gain.gain.exponentialRampToValueAtTime(0.0001, time + duration);

  noise.connect(filter);
  filter.connect(gain);
  noise.start(time);
  noise.stop(time + duration);

  return gain;
}
