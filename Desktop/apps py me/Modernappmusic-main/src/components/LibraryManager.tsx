import React, { useState, useRef, useMemo } from 'react';
import { useAudio } from '../context/AudioContext';
import { LibraryCategory, Playlist, ScanFilters, Track } from '../types';
import { processAudioFile, shouldIncludeFile } from '../utils/audioDecoder';
import { formatTime, formatFileSize } from '../utils/formatters';
import { generateDemoTracks } from '../utils/audioGenerator';
import {
  Search,
  Folder,
  Music,
  Disc3,
  User,
  ListMusic,
  FolderSearch,
  Upload,
  Filter,
  Plus,
  Trash2,
  Play,
  Bookmark,
  Sparkles,
  Check,
  FolderOpen,
  Sliders,
  ChevronRight,
  FolderPlus,
} from 'lucide-react';

export const LibraryManager: React.FC = () => {
  const {
    playlist,
    currentTrack,
    playTrack,
    addTracksToLibrary,
    removeTrackFromLibrary,
  } = useAudio();

  // Active Category tab
  const [activeCategory, setActiveCategory] = useState<LibraryCategory>('tracks');
  const [searchQuery, setSearchQuery] = useState('');

  // Scanner Filters
  const [isFilterModalOpen, setIsFilterModalOpen] = useState(false);
  const [filters, setFilters] = useState<ScanFilters>({
    minDurationSeconds: 10, // ignore < 10s
    excludePatterns: ['voice_note', 'sfx', 'ringtone', 'ui_', 'game'],
    supportedExtensions: ['.mp3', '.wav', '.m4a', '.ogg', '.flac', '.aac', '.webm'],
  });
  const [newExcludePattern, setNewExcludePattern] = useState('');

  // Scanning progress state
  const [isScanning, setIsScanning] = useState(false);
  const [scanStatusMessage, setScanStatusMessage] = useState('');

  // Custom Playlists State
  const [customPlaylists, setCustomPlaylists] = useState<Playlist[]>([
    {
      id: 'pl-rehearsal',
      name: 'Rehearsal Loops',
      trackIds: ['demo-track-1', 'demo-track-2'],
      createdAt: Date.now() - 100000,
    },
    {
      id: 'pl-study',
      name: 'Study & Retention',
      trackIds: ['demo-track-3', 'demo-track-4'],
      createdAt: Date.now() - 50000,
    },
  ]);
  const [newPlaylistName, setNewPlaylistName] = useState('');
  const [isCreatingPlaylist, setIsCreatingPlaylist] = useState(false);
  const [selectedPlaylistId, setSelectedPlaylistId] = useState<string | null>(null);

  // File Inputs
  const fileInputRef = useRef<HTMLInputElement | null>(null);
  const folderInputRef = useRef<HTMLInputElement | null>(null);

  // Process files from scan
  const handleFilesSelected = async (files: FileList | null) => {
    if (!files || files.length === 0) return;
    setIsScanning(true);
    setScanStatusMessage(`Scanning ${files.length} candidate files...`);

    const newTracks: Track[] = [];
    let ignoredCount = 0;

    for (let i = 0; i < files.length; i++) {
      const file = files[i];
      if (shouldIncludeFile(file, filters)) {
        try {
          const track = await processAudioFile(file);
          // Check min duration filter
          if (track.duration > 0 && track.duration < filters.minDurationSeconds) {
            ignoredCount++;
            continue;
          }
          newTracks.push(track);
          setScanStatusMessage(`Processed ${newTracks.length} tracks...`);
        } catch (err) {
          console.warn('Error processing audio file:', file.name, err);
        }
      } else {
        ignoredCount++;
      }
    }

    if (newTracks.length > 0) {
      addTracksToLibrary(newTracks, false);
      setScanStatusMessage(`Added ${newTracks.length} tracks! (${ignoredCount} filtered out)`);
    } else {
      setScanStatusMessage(`No eligible audio files found (${ignoredCount} filtered by rules).`);
    }

    setTimeout(() => {
      setIsScanning(false);
      setScanStatusMessage('');
    }, 2500);
  };

  // Reset to demo tracks
  const handleReloadDemoTracks = async () => {
    setIsScanning(true);
    setScanStatusMessage('Generating mastered studio sample tracks...');
    const demos = await generateDemoTracks();
    addTracksToLibrary(demos, true);
    setIsScanning(false);
    setScanStatusMessage('');
  };

  // Filtered tracks based on search
  const filteredTracks = useMemo(() => {
    if (!searchQuery.trim()) return playlist;
    const q = searchQuery.toLowerCase().trim();
    return playlist.filter(
      t =>
        t.title.toLowerCase().includes(q) ||
        t.artist.toLowerCase().includes(q) ||
        t.album.toLowerCase().includes(q) ||
        t.folder.toLowerCase().includes(q) ||
        t.savedLoops.some(l => l.label.toLowerCase().includes(q))
    );
  }, [playlist, searchQuery]);

  // Grouped by Folders
  const folderGroups = useMemo(() => {
    const groups: { [folder: string]: Track[] } = {};
    filteredTracks.forEach(t => {
      const f = t.folder || '/Unsorted';
      if (!groups[f]) groups[f] = [];
      groups[f].push(t);
    });
    return groups;
  }, [filteredTracks]);

  // Grouped by Albums
  const albumGroups = useMemo(() => {
    const groups: { [album: string]: Track[] } = {};
    filteredTracks.forEach(t => {
      const a = t.album || 'Single Tracks';
      if (!groups[a]) groups[a] = [];
      groups[a].push(t);
    });
    return groups;
  }, [filteredTracks]);

  // Grouped by Artists
  const artistGroups = useMemo(() => {
    const groups: { [artist: string]: Track[] } = {};
    filteredTracks.forEach(t => {
      const ar = t.artist || 'Unknown Artist';
      if (!groups[ar]) groups[ar] = [];
      groups[ar].push(t);
    });
    return groups;
  }, [filteredTracks]);

  // Playlist management
  const handleCreatePlaylist = (e: React.FormEvent) => {
    e.preventDefault();
    if (!newPlaylistName.trim()) return;
    const pl: Playlist = {
      id: 'pl-' + Date.now(),
      name: newPlaylistName.trim(),
      trackIds: currentTrack ? [currentTrack.id] : [],
      createdAt: Date.now(),
    };
    setCustomPlaylists(prev => [pl, ...prev]);
    setNewPlaylistName('');
    setIsCreatingPlaylist(false);
    setSelectedPlaylistId(pl.id);
  };

  const addTrackToPlaylist = (playlistId: string, trackId: string) => {
    setCustomPlaylists(prev =>
      prev.map(p => {
        if (p.id === playlistId && !p.trackIds.includes(trackId)) {
          return { ...p, trackIds: [...p.trackIds, trackId] };
        }
        return p;
      })
    );
  };

  const removeTrackFromPlaylist = (playlistId: string, trackId: string) => {
    setCustomPlaylists(prev =>
      prev.map(p => {
        if (p.id === playlistId) {
          return { ...p, trackIds: p.trackIds.filter(id => id !== trackId) };
        }
        return p;
      })
    );
  };

  const deletePlaylist = (playlistId: string) => {
    setCustomPlaylists(prev => prev.filter(p => p.id !== playlistId));
    if (selectedPlaylistId === playlistId) {
      setSelectedPlaylistId(null);
    }
  };

  // Add exclude pattern
  const handleAddExcludePattern = () => {
    if (!newExcludePattern.trim()) return;
    setFilters(prev => ({
      ...prev,
      excludePatterns: [...prev.excludePatterns, newExcludePattern.trim().toLowerCase()],
    }));
    setNewExcludePattern('');
  };

  const handleRemoveExcludePattern = (pattern: string) => {
    setFilters(prev => ({
      ...prev,
      excludePatterns: prev.excludePatterns.filter(p => p !== pattern),
    }));
  };

  return (
    <div id="library-manager-panel" className="w-full bg-zinc-900/90 rounded-2xl border border-zinc-800 p-4 sm:p-5 shadow-xl space-y-4">
      {/* Hidden File Inputs for Scanning */}
      <input
        ref={fileInputRef}
        type="file"
        multiple
        accept="audio/*"
        onChange={e => handleFilesSelected(e.target.files)}
        className="hidden"
      />
      <input
        ref={folderInputRef}
        type="file"
        multiple
        {...({ webkitdirectory: '', directory: '' } as React.InputHTMLAttributes<HTMLInputElement>)}
        onChange={e => handleFilesSelected(e.target.files)}
        className="hidden"
      />

      {/* Top Header: Search & Scanner Actions */}
      <div className="flex flex-col lg:flex-row items-stretch lg:items-center justify-between gap-3">
        {/* Search Bar */}
        <div className="relative flex-1">
          <Search className="w-4 h-4 text-zinc-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
          <input
            id="library-search-input"
            type="text"
            placeholder="Search by title, artist, folder, album, or saved loop label..."
            value={searchQuery}
            onChange={e => setSearchQuery(e.target.value)}
            className="w-full pl-10 pr-4 py-2 bg-zinc-950 border border-zinc-700/80 rounded-xl text-xs sm:text-sm text-zinc-100 placeholder-zinc-500 focus:outline-none focus:border-cyan-500"
          />
          {searchQuery && (
            <button
              onClick={() => setSearchQuery('')}
              className="absolute right-3 top-1/2 -translate-y-1/2 text-xs text-zinc-400 hover:text-zinc-200"
            >
              ✕
            </button>
          )}
        </div>

        {/* Scan Buttons & Filter Controls */}
        <div className="flex flex-wrap items-center gap-2">
          {/* Scan Files */}
          <button
            id="scan-files-btn"
            onClick={() => fileInputRef.current?.click()}
            className="flex items-center space-x-1.5 px-3 py-2 bg-zinc-800 hover:bg-zinc-700 text-zinc-200 rounded-xl text-xs font-medium border border-zinc-700 transition-colors"
          >
            <Upload className="w-3.5 h-3.5 text-cyan-400" />
            <span>Add Audio Files</span>
          </button>

          {/* Scan Folder */}
          <button
            id="scan-folder-btn"
            onClick={() => folderInputRef.current?.click()}
            className="flex items-center space-x-1.5 px-3 py-2 bg-zinc-800 hover:bg-zinc-700 text-zinc-200 rounded-xl text-xs font-medium border border-zinc-700 transition-colors"
          >
            <FolderSearch className="w-3.5 h-3.5 text-amber-400" />
            <span>Scan Device Folder</span>
          </button>

          {/* Scanner Filter Settings */}
          <button
            id="scan-filters-btn"
            onClick={() => setIsFilterModalOpen(true)}
            className="flex items-center space-x-1 px-2.5 py-2 bg-zinc-850 hover:bg-zinc-800 text-zinc-400 hover:text-zinc-200 rounded-xl text-xs border border-zinc-800 transition-colors"
            title="Configure folder exclude/include filters"
          >
            <Filter className="w-3.5 h-3.5" />
            <span className="hidden sm:inline">Filters</span>
          </button>

          {/* Reload Demos */}
          <button
            id="reload-demos-btn"
            onClick={handleReloadDemoTracks}
            className="flex items-center space-x-1 px-2.5 py-2 bg-zinc-850 hover:bg-zinc-800 text-zinc-400 hover:text-zinc-200 rounded-xl text-xs border border-zinc-800 transition-colors"
            title="Restore Built-in Sample Audio Pack"
          >
            <Sparkles className="w-3.5 h-3.5 text-cyan-400" />
            <span className="hidden sm:inline">Demos</span>
          </button>
        </div>
      </div>

      {/* Scanning status ticker if active */}
      {isScanning && (
        <div className="p-2.5 bg-cyan-950/40 border border-cyan-500/40 rounded-xl flex items-center space-x-2 text-xs text-cyan-300 animate-pulse font-mono">
          <FolderSearch className="w-4 h-4 animate-spin" />
          <span>{scanStatusMessage}</span>
        </div>
      )}

      {/* Categories Navigation Tabs */}
      <div className="flex items-center space-x-1 border-b border-zinc-800 overflow-x-auto pb-1">
        {[
          { id: 'tracks', label: 'Tracks', count: playlist.length, icon: Music },
          { id: 'folders', label: 'Folders', count: Object.keys(folderGroups).length, icon: Folder },
          { id: 'albums', label: 'Albums', count: Object.keys(albumGroups).length, icon: Disc3 },
          { id: 'artists', label: 'Artists', count: Object.keys(artistGroups).length, icon: User },
          { id: 'playlists', label: 'Playlists', count: customPlaylists.length, icon: ListMusic },
        ].map(cat => {
          const Icon = cat.icon;
          const isActive = activeCategory === cat.id;
          return (
            <button
              key={cat.id}
              id={`cat-tab-${cat.id}`}
              onClick={() => {
                setActiveCategory(cat.id as LibraryCategory);
                if (cat.id !== 'playlists') setSelectedPlaylistId(null);
              }}
              className={`flex items-center space-x-1.5 px-3 py-2 text-xs font-semibold rounded-t-xl transition-colors border-b-2 whitespace-nowrap ${
                isActive
                  ? 'border-cyan-500 text-cyan-300 bg-zinc-850/50'
                  : 'border-transparent text-zinc-400 hover:text-zinc-200'
              }`}
            >
              <Icon className="w-3.5 h-3.5" />
              <span>{cat.label}</span>
              <span className="ml-1 px-1.5 py-0.2 rounded-full text-[10px] bg-zinc-800 text-zinc-400">
                {cat.count}
              </span>
            </button>
          );
        })}
      </div>

      {/* Categorized Content Views */}

      {/* 1. TRACKS VIEW */}
      {activeCategory === 'tracks' && (
        <div className="space-y-1.5 max-h-96 overflow-y-auto pr-1">
          {filteredTracks.length > 0 ? (
            filteredTracks.map((track, idx) => {
              const isCurrent = currentTrack?.id === track.id;
              return (
                <div
                  key={track.id}
                  id={`track-row-${track.id}`}
                  className={`group flex items-center justify-between p-3 rounded-xl border transition-all ${
                    isCurrent
                      ? 'bg-cyan-950/30 border-cyan-500/60 shadow-[0_0_12px_rgba(6,182,212,0.1)]'
                      : 'bg-zinc-950/40 border-zinc-800/80 hover:bg-zinc-850/50 hover:border-zinc-700'
                  }`}
                >
                  <div
                    className="flex items-center space-x-3 flex-1 min-w-0 cursor-pointer"
                    onClick={() => playTrack(track)}
                  >
                    <div className="w-7 h-7 rounded-lg bg-zinc-850 flex items-center justify-center flex-shrink-0 text-xs font-mono text-zinc-400 group-hover:text-cyan-400">
                      {isCurrent ? (
                        <Play className="w-3.5 h-3.5 text-cyan-400 fill-current" />
                      ) : (
                        idx + 1
                      )}
                    </div>

                    <div className="min-w-0 flex-1">
                      <div className="flex items-center space-x-2">
                        <span className="font-bold text-xs sm:text-sm text-zinc-200 truncate group-hover:text-cyan-300">
                          {track.title}
                        </span>
                        {track.isDemo && (
                          <span className="px-1.5 py-0.2 rounded bg-zinc-800 text-[10px] text-zinc-400 font-mono">
                            DEMO
                          </span>
                        )}
                      </div>
                      <div className="flex items-center space-x-2 text-[11px] text-zinc-400">
                        <span>{track.artist}</span>
                        <span className="text-zinc-600">•</span>
                        <span className="text-zinc-500 truncate max-w-[120px]">{track.folder}</span>
                      </div>
                    </div>
                  </div>

                  <div className="flex items-center space-x-2 sm:space-x-3 text-xs font-mono text-zinc-400">
                    {/* Saved Loops Count Chip */}
                    {track.savedLoops && track.savedLoops.length > 0 && (
                      <div className="flex items-center space-x-1 px-2 py-0.5 rounded-full bg-cyan-950/60 border border-cyan-800/60 text-cyan-300 text-[11px]">
                        <Bookmark className="w-3 h-3" />
                        <span>{track.savedLoops.length} loops</span>
                      </div>
                    )}

                    <span>{formatTime(track.duration, false)}</span>

                    {/* Delete track button */}
                    {!track.isDemo && (
                      <button
                        onClick={() => removeTrackFromLibrary(track.id)}
                        title="Remove from library"
                        className="p-1 rounded-lg text-zinc-500 hover:text-rose-400 opacity-0 group-hover:opacity-100 transition-opacity"
                      >
                        <Trash2 className="w-3.5 h-3.5" />
                      </button>
                    )}
                  </div>
                </div>
              );
            })
          ) : (
            <div className="p-8 text-center text-zinc-500 text-xs rounded-xl bg-zinc-950/30 border border-zinc-800/80">
              No tracks match "{searchQuery}".
            </div>
          )}
        </div>
      )}

      {/* 2. FOLDERS VIEW */}
      {activeCategory === 'folders' && (
        <div className="space-y-3 max-h-96 overflow-y-auto pr-1">
          {(Object.entries(folderGroups) as [string, Track[]][]).map(([folderPath, tracks]) => (
            <div key={folderPath} className="bg-zinc-950/50 border border-zinc-800 rounded-xl p-3 space-y-2">
              <div className="flex items-center justify-between">
                <div className="flex items-center space-x-2">
                  <FolderOpen className="w-4 h-4 text-amber-400" />
                  <span className="font-bold text-xs text-zinc-200 font-mono">{folderPath}</span>
                  <span className="text-[11px] text-zinc-500">({tracks.length} tracks)</span>
                </div>
                <button
                  onClick={() => tracks[0] && playTrack(tracks[0])}
                  className="flex items-center space-x-1 text-[11px] text-cyan-400 hover:underline"
                >
                  <Play className="w-3 h-3 fill-current" />
                  <span>Play Folder</span>
                </button>
              </div>

              <div className="space-y-1 pl-4 border-l border-zinc-800">
                {tracks.map(t => (
                  <div
                    key={t.id}
                    onClick={() => playTrack(t)}
                    className="flex items-center justify-between p-1.5 rounded-lg hover:bg-zinc-850 cursor-pointer text-xs transition-colors"
                  >
                    <span className="text-zinc-300 truncate">{t.title}</span>
                    <span className="text-zinc-500 font-mono text-[11px]">{formatTime(t.duration, false)}</span>
                  </div>
                ))}
              </div>
            </div>
          ))}
        </div>
      )}

      {/* 3. ALBUMS VIEW */}
      {activeCategory === 'albums' && (
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 max-h-96 overflow-y-auto pr-1">
          {(Object.entries(albumGroups) as [string, Track[]][]).map(([albumName, tracks]) => (
            <div
              key={albumName}
              className="bg-zinc-950/50 border border-zinc-800 rounded-xl p-3 space-y-2 hover:border-zinc-700 transition-colors"
            >
              <div className="flex items-center justify-between">
                <div className="flex items-center space-x-2">
                  <Disc3 className="w-4 h-4 text-cyan-400" />
                  <span className="font-bold text-xs text-zinc-200 truncate">{albumName}</span>
                </div>
                <span className="text-[11px] text-zinc-500">{tracks.length} tracks</span>
              </div>
              <div className="text-[11px] text-zinc-400 font-mono">
                Artist: {tracks[0]?.artist || 'Various'}
              </div>
              <button
                onClick={() => tracks[0] && playTrack(tracks[0])}
                className="w-full py-1.5 rounded-lg bg-zinc-900 hover:bg-cyan-500 hover:text-black text-zinc-300 text-xs font-semibold transition-colors flex items-center justify-center space-x-1"
              >
                <Play className="w-3 h-3 fill-current" />
                <span>Play Album</span>
              </button>
            </div>
          ))}
        </div>
      )}

      {/* 4. ARTISTS VIEW */}
      {activeCategory === 'artists' && (
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 max-h-96 overflow-y-auto pr-1">
          {(Object.entries(artistGroups) as [string, Track[]][]).map(([artistName, tracks]) => (
            <div
              key={artistName}
              className="bg-zinc-950/50 border border-zinc-800 rounded-xl p-3 space-y-2 hover:border-zinc-700 transition-colors"
            >
              <div className="flex items-center justify-between">
                <div className="flex items-center space-x-2">
                  <User className="w-4 h-4 text-emerald-400" />
                  <span className="font-bold text-xs text-zinc-200 truncate">{artistName}</span>
                </div>
                <span className="text-[11px] text-zinc-500">{tracks.length} tracks</span>
              </div>
              <button
                onClick={() => tracks[0] && playTrack(tracks[0])}
                className="w-full py-1.5 rounded-lg bg-zinc-900 hover:bg-emerald-500 hover:text-black text-zinc-300 text-xs font-semibold transition-colors flex items-center justify-center space-x-1"
              >
                <Play className="w-3 h-3 fill-current" />
                <span>Play Tracks</span>
              </button>
            </div>
          ))}
        </div>
      )}

      {/* 5. CUSTOM PLAYLISTS VIEW */}
      {activeCategory === 'playlists' && (
        <div className="space-y-3">
          <div className="flex items-center justify-between pb-2 border-b border-zinc-800">
            <h3 className="text-xs font-bold text-zinc-300 uppercase tracking-wider">
              Custom Playlists ({customPlaylists.length})
            </h3>
            <button
              onClick={() => setIsCreatingPlaylist(true)}
              className="flex items-center space-x-1 px-3 py-1 bg-cyan-600 hover:bg-cyan-500 text-black font-bold text-xs rounded-lg"
            >
              <Plus className="w-3.5 h-3.5" />
              <span>New Playlist</span>
            </button>
          </div>

          {/* New Playlist Form */}
          {isCreatingPlaylist && (
            <form onSubmit={handleCreatePlaylist} className="p-3 bg-zinc-950 border border-zinc-700 rounded-xl space-y-2">
              <input
                type="text"
                required
                placeholder="Playlist name (e.g. Guitar Solos, Physics Review)..."
                value={newPlaylistName}
                onChange={e => setNewPlaylistName(e.target.value)}
                className="w-full px-3 py-1.5 bg-zinc-900 border border-zinc-700 rounded-lg text-xs text-zinc-100 focus:outline-none focus:border-cyan-500"
              />
              <div className="flex justify-end space-x-2">
                <button
                  type="button"
                  onClick={() => setIsCreatingPlaylist(false)}
                  className="px-3 py-1 text-xs text-zinc-400 hover:text-zinc-200"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-4 py-1 bg-cyan-500 text-black font-bold text-xs rounded-lg"
                >
                  Create
                </button>
              </div>
            </form>
          )}

          {/* Playlists grid */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            {customPlaylists.map(pl => {
              const plTracks = playlist.filter(t => pl.trackIds.includes(t.id));
              return (
                <div
                  key={pl.id}
                  className="bg-zinc-950/60 border border-zinc-800 rounded-xl p-3 space-y-2 hover:border-zinc-700"
                >
                  <div className="flex items-center justify-between">
                    <div className="flex items-center space-x-2">
                      <ListMusic className="w-4 h-4 text-cyan-400" />
                      <span className="font-bold text-xs text-zinc-200 truncate">{pl.name}</span>
                    </div>
                    <button
                      onClick={() => deletePlaylist(pl.id)}
                      className="text-zinc-500 hover:text-rose-400 text-xs"
                      title="Delete playlist"
                    >
                      <Trash2 className="w-3.5 h-3.5" />
                    </button>
                  </div>

                  <div className="text-[11px] text-zinc-400">
                    {plTracks.length} tracks included
                  </div>

                  {/* Play Playlist button */}
                  {plTracks.length > 0 && (
                    <button
                      onClick={() => playTrack(plTracks[0])}
                      className="w-full py-1 bg-zinc-900 hover:bg-cyan-500 hover:text-black text-zinc-300 text-xs font-semibold rounded-lg transition-colors flex items-center justify-center space-x-1"
                    >
                      <Play className="w-3 h-3 fill-current" />
                      <span>Play Playlist</span>
                    </button>
                  )}
                </div>
              );
            })}
          </div>
        </div>
      )}

      {/* Filter Settings Modal */}
      {isFilterModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 backdrop-blur-sm p-4 animate-in fade-in">
          <div className="bg-zinc-900 border border-zinc-700 rounded-2xl max-w-md w-full p-5 shadow-2xl space-y-4">
            <div className="flex items-center justify-between pb-2 border-b border-zinc-800">
              <div className="flex items-center space-x-2">
                <Filter className="w-5 h-5 text-amber-400" />
                <h3 className="font-bold text-base text-zinc-100">Storage Scanner Filters</h3>
              </div>
              <button
                onClick={() => setIsFilterModalOpen(false)}
                className="text-zinc-400 hover:text-zinc-200 text-xs"
              >
                ✕
              </button>
            </div>

            <p className="text-xs text-zinc-400">
              Configure automatic folder include/exclude rules to filter out short voice memos, game sounds, and ringtones when scanning local folders.
            </p>

            {/* Minimum duration */}
            <div className="space-y-1">
              <label className="block text-xs font-semibold text-zinc-300 uppercase tracking-wider">
                Minimum Track Duration
              </label>
              <div className="flex items-center space-x-2">
                <input
                  type="number"
                  min="0"
                  max="300"
                  value={filters.minDurationSeconds}
                  onChange={e =>
                    setFilters(prev => ({
                      ...prev,
                      minDurationSeconds: Math.max(0, parseInt(e.target.value) || 0),
                    }))
                  }
                  className="w-24 px-3 py-1.5 bg-zinc-950 border border-zinc-700 rounded-xl text-xs font-mono text-zinc-100"
                />
                <span className="text-xs text-zinc-400">
                  seconds (Tracks under {filters.minDurationSeconds}s will be ignored)
                </span>
              </div>
            </div>

            {/* Exclude name/path patterns */}
            <div className="space-y-2">
              <label className="block text-xs font-semibold text-zinc-300 uppercase tracking-wider">
                Excluded Folder & Name Keywords
              </label>
              <div className="flex flex-wrap gap-1.5">
                {filters.excludePatterns.map(pattern => (
                  <span
                    key={pattern}
                    className="inline-flex items-center space-x-1 px-2.5 py-1 rounded-lg bg-zinc-950 border border-zinc-700 text-xs text-rose-300"
                  >
                    <span>*{pattern}*</span>
                    <button
                      type="button"
                      onClick={() => handleRemoveExcludePattern(pattern)}
                      className="text-zinc-500 hover:text-rose-400 ml-1"
                    >
                      ✕
                    </button>
                  </span>
                ))}
              </div>

              <div className="flex items-center space-x-2 pt-1">
                <input
                  type="text"
                  placeholder="e.g. voice_note, sfx, game, alert..."
                  value={newExcludePattern}
                  onChange={e => setNewExcludePattern(e.target.value)}
                  className="flex-1 px-3 py-1.5 bg-zinc-950 border border-zinc-700 rounded-xl text-xs text-zinc-100 focus:outline-none focus:border-cyan-500"
                />
                <button
                  type="button"
                  onClick={handleAddExcludePattern}
                  className="px-3 py-1.5 bg-zinc-800 hover:bg-zinc-700 text-zinc-200 text-xs rounded-xl font-medium"
                >
                  Add Filter
                </button>
              </div>
            </div>

            <div className="flex items-center justify-end pt-3 border-t border-zinc-800">
              <button
                onClick={() => setIsFilterModalOpen(false)}
                className="px-5 py-2 bg-cyan-500 hover:bg-cyan-400 text-black font-bold text-xs rounded-xl"
              >
                Done
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
