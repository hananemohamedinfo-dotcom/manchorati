import React, { useState } from 'react';
import { useAudio } from '../context/AudioContext';
import { formatTime, parseTimeToSeconds } from '../utils/formatters';
import { SavedLoop } from '../types';
import {
  RotateCcw,
  Repeat,
  BookmarkPlus,
  Play,
  Trash2,
  Tag,
  Clock,
  Sparkles,
  ChevronRight,
  Sliders,
  Check,
} from 'lucide-react';

export const ABLoopControls: React.FC = () => {
  const {
    currentTrack,
    currentTime,
    duration,
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
    seekTo,
  } = useAudio();

  // Dialog / form state for saving loop
  const [isSavingModalOpen, setIsSavingModalOpen] = useState(false);
  const [saveLabel, setSaveLabel] = useState('');
  const [saveTag, setSaveTag] = useState<SavedLoop['tag']>('Practice');
  const [saveColor, setSaveColor] = useState('#06b6d4');
  const [customRepeatInput, setCustomRepeatInput] = useState<string>('0');

  // Manual numeric inputs
  const [manualA, setManualA] = useState('');
  const [manualB, setManualB] = useState('');
  const [isEditingManual, setIsEditingManual] = useState(false);

  const loopDuration =
    loopState.pointA !== null && loopState.pointB !== null
      ? Math.max(0, loopState.pointB - loopState.pointA)
      : 0;

  const openSaveDialog = () => {
    if (loopState.pointA === null || loopState.pointB === null) return;
    const defaultLabel = `${currentTrack?.title ? currentTrack.title.substring(0, 18) : 'Segment'} [${formatTime(loopState.pointA, false)}-${formatTime(loopState.pointB, false)}]`;
    setSaveLabel(defaultLabel);
    setSaveColor(saveTag === 'Lecture' ? '#3b82f6' : saveTag === 'Language' ? '#14b8a6' : '#06b6d4');
    setIsSavingModalOpen(true);
  };

  const handleSaveConfirm = (e: React.FormEvent) => {
    e.preventDefault();
    if (!saveLabel.trim()) return;
    saveCurrentLoop(saveLabel, saveTag, saveColor);
    setIsSavingModalOpen(false);
  };

  const handleStartManualEdit = () => {
    setManualA(loopState.pointA !== null ? formatTime(loopState.pointA, true) : '');
    setManualB(loopState.pointB !== null ? formatTime(loopState.pointB, true) : '');
    setIsEditingManual(true);
  };

  const handleApplyManual = () => {
    const secA = parseTimeToSeconds(manualA);
    const secB = parseTimeToSeconds(manualB);
    if (secA !== null && secB !== null && secB > secA) {
      setManualLoopTimes(secA, secB);
      setIsEditingManual(false);
    } else if (secA !== null && !manualB) {
      setManualLoopTimes(secA, null);
      setIsEditingManual(false);
    }
  };

  const colorOptions = [
    { label: 'Cyan', hex: '#06b6d4' },
    { label: 'Amber', hex: '#f59e0b' },
    { label: 'Emerald', hex: '#10b981' },
    { label: 'Purple', hex: '#8b5cf6' },
    { label: 'Rose', hex: '#f43f5e' },
    { label: 'Blue', hex: '#3b82f6' },
  ];

  return (
    <div id="ab-loop-controls-panel" className="w-full bg-zinc-900/90 rounded-2xl p-4 sm:p-5 border border-zinc-800 shadow-xl space-y-4">
      {/* Top Banner: A-B Loop Master Status & Repeat Count */}
      <div className="flex flex-wrap items-center justify-between gap-3 pb-3 border-b border-zinc-800">
        <div className="flex items-center space-x-3">
          <button
            id="toggle-loop-active-btn"
            onClick={toggleLoopActive}
            disabled={loopState.pointA === null || loopState.pointB === null}
            className={`flex items-center space-x-2 px-3.5 py-1.5 rounded-xl font-semibold text-sm transition-all duration-200 border ${
              loopState.isActive
                ? 'bg-cyan-500/20 text-cyan-300 border-cyan-500/50 shadow-[0_0_15px_rgba(6,182,212,0.3)]'
                : 'bg-zinc-800/80 text-zinc-400 border-zinc-700/60 hover:text-zinc-200 disabled:opacity-40'
            }`}
          >
            <Repeat className={`w-4 h-4 ${loopState.isActive ? 'animate-spin-slow text-cyan-400' : ''}`} />
            <span>{loopState.isActive ? 'A-B LOOP ACTIVE' : 'A-B LOOP OFF'}</span>
          </button>

          {/* Loop Duration pill */}
          {loopDuration > 0 && (
            <div className="hidden sm:flex items-center space-x-1.5 px-3 py-1 rounded-lg bg-zinc-800 text-zinc-300 text-xs font-mono">
              <Clock className="w-3.5 h-3.5 text-zinc-400" />
              <span>Segment: {loopDuration.toFixed(3)}s</span>
            </div>
          )}
        </div>

        {/* Repeat Count Selector (Infinite or 1x, 2x, 3x, 5x, 10x) */}
        <div className="flex items-center space-x-2 text-xs">
          <span className="text-zinc-400 font-medium">Repeats:</span>
          <div className="flex items-center bg-zinc-950 p-1 rounded-lg border border-zinc-800">
            <button
              id="loop-count-inf-btn"
              onClick={() => setLoopTargetCount(0)}
              className={`px-2.5 py-1 rounded font-medium text-xs transition-colors ${
                loopState.targetCount === 0
                  ? 'bg-cyan-500 text-black font-bold'
                  : 'text-zinc-400 hover:text-zinc-200'
              }`}
              title="Infinite loop repeats"
            >
              ∞
            </button>
            {[1, 3, 5, 10].map(count => (
              <button
                key={count}
                id={`loop-count-${count}-btn`}
                onClick={() => setLoopTargetCount(count)}
                className={`px-2 py-1 rounded font-medium text-xs transition-colors ${
                  loopState.targetCount === count
                    ? 'bg-cyan-500 text-black font-bold'
                    : 'text-zinc-400 hover:text-zinc-200'
                }`}
              >
                {count}x
              </button>
            ))}
          </div>

          {/* Live loop completion counter */}
          {loopState.isActive && (
            <span className="px-2 py-1 bg-cyan-950/80 border border-cyan-800 text-cyan-300 rounded font-mono text-xs">
              {loopState.targetCount > 0
                ? `${loopState.completedCount} / ${loopState.targetCount}`
                : `#${loopState.completedCount}`}
            </span>
          )}
        </div>
      </div>

      {/* Primary Action Buttons: Set A, Set B, Reset, Jump to A/B */}
      <div className="grid grid-cols-2 sm:grid-cols-5 gap-2.5">
        <button
          id="set-point-a-btn"
          onClick={() => setPointA()}
          className="flex items-center justify-center space-x-2 py-2.5 px-3 bg-cyan-600 hover:bg-cyan-500 text-zinc-950 font-bold text-sm rounded-xl shadow-md transition-all active:scale-95"
        >
          <span className="w-5 h-5 rounded-full bg-zinc-950 text-cyan-400 flex items-center justify-center text-xs">A</span>
          <span>Set Point A</span>
        </button>

        <button
          id="set-point-b-btn"
          onClick={() => setPointB()}
          className="flex items-center justify-center space-x-2 py-2.5 px-3 bg-amber-500 hover:bg-amber-400 text-zinc-950 font-bold text-sm rounded-xl shadow-md transition-all active:scale-95"
        >
          <span className="w-5 h-5 rounded-full bg-zinc-950 text-amber-400 flex items-center justify-center text-xs">B</span>
          <span>Set Point B</span>
        </button>

        <button
          id="jump-to-a-btn"
          onClick={jumpToPointA}
          disabled={loopState.pointA === null}
          className="flex items-center justify-center space-x-1.5 py-2.5 px-3 bg-zinc-800 hover:bg-zinc-700 text-zinc-200 font-medium text-xs rounded-xl border border-zinc-700 disabled:opacity-30 transition-colors"
        >
          <span>Jump to A</span>
        </button>

        <button
          id="jump-to-b-btn"
          onClick={jumpToPointB}
          disabled={loopState.pointB === null}
          className="flex items-center justify-center space-x-1.5 py-2.5 px-3 bg-zinc-800 hover:bg-zinc-700 text-zinc-200 font-medium text-xs rounded-xl border border-zinc-700 disabled:opacity-30 transition-colors"
        >
          <span>Jump to B</span>
        </button>

        <button
          id="reset-loop-btn"
          onClick={resetLoop}
          className="col-span-2 sm:col-span-1 flex items-center justify-center space-x-1.5 py-2.5 px-3 bg-zinc-800/80 hover:bg-red-950/40 text-zinc-400 hover:text-red-400 font-medium text-xs rounded-xl border border-zinc-700/80 transition-colors"
        >
          <RotateCcw className="w-3.5 h-3.5" />
          <span>Reset Loop</span>
        </button>
      </div>

      {/* Point A & Point B Fine-Tuning Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-3.5">
        {/* Point A Fine-Tuning Box */}
        <div className="bg-zinc-950/70 border border-cyan-950/80 rounded-xl p-3 space-y-2.5">
          <div className="flex items-center justify-between">
            <div className="flex items-center space-x-2">
              <span className="px-2 py-0.5 rounded bg-cyan-500/20 text-cyan-400 font-bold text-xs border border-cyan-500/40">
                POINT A (START)
              </span>
              <span className="font-mono text-sm font-semibold text-zinc-200">
                {loopState.pointA !== null ? formatTime(loopState.pointA, true) : '--:--.---'}
              </span>
            </div>
            <button
              onClick={() => fineTuneA(0.1)}
              disabled={loopState.pointA === null}
              className="text-[11px] text-cyan-400 hover:underline disabled:opacity-40"
            >
              +0.1s Nudge
            </button>
          </div>

          {/* Fine tune buttons row */}
          <div className="flex items-center justify-between gap-1">
            <div className="flex items-center space-x-1">
              <button
                id="fine-tune-a-minus-5"
                onClick={() => fineTuneA(-5)}
                disabled={loopState.pointA === null}
                className="px-2 py-1 bg-zinc-900 hover:bg-zinc-800 text-zinc-300 font-mono text-xs rounded border border-zinc-800 disabled:opacity-30"
              >
                -5s
              </button>
              <button
                id="fine-tune-a-minus-1"
                onClick={() => fineTuneA(-1)}
                disabled={loopState.pointA === null}
                className="px-2 py-1 bg-zinc-900 hover:bg-zinc-800 text-zinc-300 font-mono text-xs rounded border border-zinc-800 disabled:opacity-30"
              >
                -1s
              </button>
              <button
                id="fine-tune-a-minus-01"
                onClick={() => fineTuneA(-0.1)}
                disabled={loopState.pointA === null}
                className="px-2 py-1 bg-zinc-900 hover:bg-zinc-800 text-cyan-300 font-mono text-xs rounded border border-zinc-800 disabled:opacity-30"
              >
                -0.1s
              </button>
            </div>
            <div className="flex items-center space-x-1">
              <button
                id="fine-tune-a-plus-01"
                onClick={() => fineTuneA(0.1)}
                disabled={loopState.pointA === null}
                className="px-2 py-1 bg-zinc-900 hover:bg-zinc-800 text-cyan-300 font-mono text-xs rounded border border-zinc-800 disabled:opacity-30"
              >
                +0.1s
              </button>
              <button
                id="fine-tune-a-plus-1"
                onClick={() => fineTuneA(1)}
                disabled={loopState.pointA === null}
                className="px-2 py-1 bg-zinc-900 hover:bg-zinc-800 text-zinc-300 font-mono text-xs rounded border border-zinc-800 disabled:opacity-30"
              >
                +1s
              </button>
              <button
                id="fine-tune-a-plus-5"
                onClick={() => fineTuneA(5)}
                disabled={loopState.pointA === null}
                className="px-2 py-1 bg-zinc-900 hover:bg-zinc-800 text-zinc-300 font-mono text-xs rounded border border-zinc-800 disabled:opacity-30"
              >
                +5s
              </button>
            </div>
          </div>
        </div>

        {/* Point B Fine-Tuning Box */}
        <div className="bg-zinc-950/70 border border-amber-950/80 rounded-xl p-3 space-y-2.5">
          <div className="flex items-center justify-between">
            <div className="flex items-center space-x-2">
              <span className="px-2 py-0.5 rounded bg-amber-500/20 text-amber-400 font-bold text-xs border border-amber-500/40">
                POINT B (END)
              </span>
              <span className="font-mono text-sm font-semibold text-zinc-200">
                {loopState.pointB !== null ? formatTime(loopState.pointB, true) : '--:--.---'}
              </span>
            </div>
            <button
              onClick={() => fineTuneB(0.1)}
              disabled={loopState.pointB === null}
              className="text-[11px] text-amber-400 hover:underline disabled:opacity-40"
            >
              +0.1s Nudge
            </button>
          </div>

          {/* Fine tune buttons row */}
          <div className="flex items-center justify-between gap-1">
            <div className="flex items-center space-x-1">
              <button
                id="fine-tune-b-minus-5"
                onClick={() => fineTuneB(-5)}
                disabled={loopState.pointB === null}
                className="px-2 py-1 bg-zinc-900 hover:bg-zinc-800 text-zinc-300 font-mono text-xs rounded border border-zinc-800 disabled:opacity-30"
              >
                -5s
              </button>
              <button
                id="fine-tune-b-minus-1"
                onClick={() => fineTuneB(-1)}
                disabled={loopState.pointB === null}
                className="px-2 py-1 bg-zinc-900 hover:bg-zinc-800 text-zinc-300 font-mono text-xs rounded border border-zinc-800 disabled:opacity-30"
              >
                -1s
              </button>
              <button
                id="fine-tune-b-minus-01"
                onClick={() => fineTuneB(-0.1)}
                disabled={loopState.pointB === null}
                className="px-2 py-1 bg-zinc-900 hover:bg-zinc-800 text-amber-300 font-mono text-xs rounded border border-zinc-800 disabled:opacity-30"
              >
                -0.1s
              </button>
            </div>
            <div className="flex items-center space-x-1">
              <button
                id="fine-tune-b-plus-01"
                onClick={() => fineTuneB(0.1)}
                disabled={loopState.pointB === null}
                className="px-2 py-1 bg-zinc-900 hover:bg-zinc-800 text-amber-300 font-mono text-xs rounded border border-zinc-800 disabled:opacity-30"
              >
                +0.1s
              </button>
              <button
                id="fine-tune-b-plus-1"
                onClick={() => fineTuneB(1)}
                disabled={loopState.pointB === null}
                className="px-2 py-1 bg-zinc-900 hover:bg-zinc-800 text-zinc-300 font-mono text-xs rounded border border-zinc-800 disabled:opacity-30"
              >
                +1s
              </button>
              <button
                id="fine-tune-b-plus-5"
                onClick={() => fineTuneB(5)}
                disabled={loopState.pointB === null}
                className="px-2 py-1 bg-zinc-900 hover:bg-zinc-800 text-zinc-300 font-mono text-xs rounded border border-zinc-800 disabled:opacity-30"
              >
                +5s
              </button>
            </div>
          </div>
        </div>
      </div>

      {/* Manual Time Input Toggle & Save Loop Section */}
      <div className="flex flex-wrap items-center justify-between gap-2 pt-1">
        <div className="flex items-center space-x-2">
          {!isEditingManual ? (
            <button
              id="toggle-manual-input-btn"
              onClick={handleStartManualEdit}
              className="flex items-center space-x-1 px-3 py-1.5 rounded-lg bg-zinc-800/80 hover:bg-zinc-700 text-zinc-300 text-xs transition-colors"
            >
              <Sliders className="w-3.5 h-3.5 text-zinc-400" />
              <span>Type Exact Timestamps</span>
            </button>
          ) : (
            <div className="flex items-center space-x-2 bg-zinc-950 p-1.5 rounded-xl border border-zinc-700">
              <input
                id="manual-a-input"
                type="text"
                placeholder="A: mm:ss.ms"
                value={manualA}
                onChange={e => setManualA(e.target.value)}
                className="w-24 px-2 py-1 bg-zinc-900 text-xs font-mono text-cyan-300 rounded border border-zinc-700 focus:outline-none focus:border-cyan-500"
              />
              <span className="text-zinc-500 text-xs">to</span>
              <input
                id="manual-b-input"
                type="text"
                placeholder="B: mm:ss.ms"
                value={manualB}
                onChange={e => setManualB(e.target.value)}
                className="w-24 px-2 py-1 bg-zinc-900 text-xs font-mono text-amber-300 rounded border border-zinc-700 focus:outline-none focus:border-amber-500"
              />
              <button
                id="apply-manual-times-btn"
                onClick={handleApplyManual}
                className="px-2.5 py-1 bg-cyan-600 hover:bg-cyan-500 text-black font-semibold text-xs rounded"
              >
                Apply
              </button>
              <button
                id="cancel-manual-times-btn"
                onClick={() => setIsEditingManual(false)}
                className="px-2 py-1 text-zinc-400 hover:text-zinc-200 text-xs"
              >
                Cancel
              </button>
            </div>
          )}
        </div>

        {/* Save Segment Button */}
        <button
          id="open-save-loop-dialog-btn"
          onClick={openSaveDialog}
          disabled={loopState.pointA === null || loopState.pointB === null}
          className="flex items-center space-x-1.5 px-3.5 py-1.5 rounded-xl bg-gradient-to-r from-cyan-500 to-blue-600 hover:from-cyan-400 hover:to-blue-500 text-zinc-950 font-semibold text-xs shadow-md disabled:opacity-40 transition-all"
        >
          <BookmarkPlus className="w-4 h-4" />
          <span>Save & Label This Loop</span>
        </button>
      </div>

      {/* Saved Loops for Current Track List */}
      <div className="pt-2 border-t border-zinc-800">
        <div className="flex items-center justify-between pb-2">
          <div className="flex items-center space-x-2">
            <Tag className="w-3.5 h-3.5 text-cyan-400" />
            <h3 className="text-xs font-semibold uppercase tracking-wider text-zinc-300">
              Saved Segment Loops ({currentTrack?.savedLoops?.length || 0})
            </h3>
          </div>
          <span className="text-[11px] text-zinc-500">Click to instantly replay loop</span>
        </div>

        {currentTrack?.savedLoops && currentTrack.savedLoops.length > 0 ? (
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-2 max-h-48 overflow-y-auto pr-1">
            {currentTrack.savedLoops.map(loop => {
              const isCurrent =
                loopState.pointA !== null &&
                loopState.pointB !== null &&
                Math.abs(loopState.pointA - loop.pointA) < 0.05 &&
                Math.abs(loopState.pointB - loop.pointB) < 0.05;

              return (
                <div
                  key={loop.id}
                  className={`group relative flex items-center justify-between p-2.5 rounded-xl border transition-all ${
                    isCurrent
                      ? 'bg-cyan-950/40 border-cyan-500/70 shadow-[0_0_10px_rgba(6,182,212,0.15)]'
                      : 'bg-zinc-950/60 border-zinc-800/80 hover:border-zinc-700 hover:bg-zinc-900/60'
                  }`}
                >
                  <div
                    className="flex-1 cursor-pointer min-w-0 pr-2"
                    onClick={() => applySavedLoop(loop)}
                  >
                    <div className="flex items-center space-x-1.5">
                      <span
                        className="w-2 h-2 rounded-full flex-shrink-0"
                        style={{ backgroundColor: loop.color || '#06b6d4' }}
                      />
                      <span className="font-semibold text-xs text-zinc-200 truncate">
                        {loop.label}
                      </span>
                    </div>

                    <div className="flex items-center space-x-2 mt-1 text-[10px] font-mono text-zinc-400">
                      <span>
                        {formatTime(loop.pointA, false)} → {formatTime(loop.pointB, false)}
                      </span>
                      <span className="text-zinc-600">•</span>
                      <span>{(loop.pointB - loop.pointA).toFixed(1)}s</span>
                      {loop.loopCount > 0 ? (
                        <span className="text-cyan-400 font-semibold">({loop.loopCount}x)</span>
                      ) : (
                        <span className="text-zinc-500">(∞)</span>
                      )}
                    </div>
                  </div>

                  <div className="flex items-center space-x-1">
                    <button
                      onClick={() => applySavedLoop(loop)}
                      title="Activate Loop"
                      className="p-1 rounded-lg bg-zinc-800/80 hover:bg-cyan-500 hover:text-black text-zinc-300 transition-colors"
                    >
                      <Play className="w-3.5 h-3.5 fill-current" />
                    </button>
                    <button
                      onClick={() => deleteSavedLoop(loop.id)}
                      title="Delete Loop"
                      className="p-1 rounded-lg bg-zinc-800/40 hover:bg-rose-950/60 hover:text-rose-400 text-zinc-500 transition-colors opacity-0 group-hover:opacity-100"
                    >
                      <Trash2 className="w-3.5 h-3.5" />
                    </button>
                  </div>
                </div>
              );
            })}
          </div>
        ) : (
          <div className="p-4 rounded-xl bg-zinc-950/40 border border-dashed border-zinc-800 text-center text-xs text-zinc-500">
            No saved segments for this track yet. Mark Point A & Point B and click "Save & Label This Loop" to bookmark rehearsal bars, lecture insights, or language drills!
          </div>
        )}
      </div>

      {/* Save Loop Modal */}
      {isSavingModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 backdrop-blur-sm p-4 animate-in fade-in duration-150">
          <div className="bg-zinc-900 border border-zinc-700 rounded-2xl p-5 max-w-md w-full shadow-2xl space-y-4">
            <div className="flex items-center justify-between pb-2 border-b border-zinc-800">
              <h3 className="font-bold text-base text-zinc-100 flex items-center space-x-2">
                <BookmarkPlus className="w-5 h-5 text-cyan-400" />
                <span>Save Segment Bookmark</span>
              </h3>
              <button
                onClick={() => setIsSavingModalOpen(false)}
                className="text-zinc-400 hover:text-zinc-200 text-sm"
              >
                ✕
              </button>
            </div>

            <form onSubmit={handleSaveConfirm} className="space-y-3.5">
              <div>
                <label className="block text-xs font-medium text-zinc-400 mb-1">
                  Segment Label / Notes
                </label>
                <input
                  id="save-loop-label-input"
                  type="text"
                  required
                  placeholder="e.g. Guitar Solo Rehearsal, Chapter 2 Formula, Pronunciation"
                  value={saveLabel}
                  onChange={e => setSaveLabel(e.target.value)}
                  className="w-full px-3 py-2 bg-zinc-950 border border-zinc-700 rounded-xl text-sm text-zinc-100 focus:outline-none focus:border-cyan-500"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-medium text-zinc-400 mb-1">
                    Tag / Category
                  </label>
                  <select
                    id="save-loop-tag-select"
                    value={saveTag}
                    onChange={e => setSaveTag(e.target.value as SavedLoop['tag'])}
                    className="w-full px-3 py-2 bg-zinc-950 border border-zinc-700 rounded-xl text-xs text-zinc-200 focus:outline-none focus:border-cyan-500"
                  >
                    <option value="Practice">Practice</option>
                    <option value="Music">Music</option>
                    <option value="Lecture">Lecture</option>
                    <option value="Language">Language</option>
                    <option value="Speech">Speech</option>
                    <option value="Custom">Custom</option>
                  </select>
                </div>

                <div>
                  <label className="block text-xs font-medium text-zinc-400 mb-1">
                    Marker Color
                  </label>
                  <div className="flex items-center space-x-1.5 pt-1">
                    {colorOptions.map(c => (
                      <button
                        type="button"
                        key={c.hex}
                        onClick={() => setSaveColor(c.hex)}
                        className={`w-6 h-6 rounded-full border-2 transition-transform ${
                          saveColor === c.hex ? 'border-white scale-110' : 'border-transparent'
                        }`}
                        style={{ backgroundColor: c.hex }}
                      />
                    ))}
                  </div>
                </div>
              </div>

              <div className="p-3 bg-zinc-950 rounded-xl border border-zinc-800 text-xs font-mono text-zinc-400 space-y-1">
                <div className="flex justify-between">
                  <span>Start (A):</span>
                  <span className="text-cyan-400 font-bold">{formatTime(loopState.pointA || 0, true)}</span>
                </div>
                <div className="flex justify-between">
                  <span>End (B):</span>
                  <span className="text-amber-400 font-bold">{formatTime(loopState.pointB || 0, true)}</span>
                </div>
                <div className="flex justify-between">
                  <span>Segment Length:</span>
                  <span className="text-zinc-200 font-bold">{loopDuration.toFixed(3)} seconds</span>
                </div>
              </div>

              <div className="flex items-center justify-end space-x-2 pt-2">
                <button
                  type="button"
                  onClick={() => setIsSavingModalOpen(false)}
                  className="px-4 py-2 bg-zinc-800 hover:bg-zinc-700 text-zinc-300 text-xs font-medium rounded-xl"
                >
                  Cancel
                </button>
                <button
                  id="save-loop-submit-btn"
                  type="submit"
                  className="px-5 py-2 bg-cyan-500 hover:bg-cyan-400 text-black text-xs font-bold rounded-xl shadow-lg"
                >
                  Save Loop
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
