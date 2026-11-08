# Graph Report - manchorati-main  (2026-11-08)

## Corpus Check
- 174 files · ~228,940 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 448 file(s) not represented in the graph (top: .xml 399, .ttf 19, (none) 7)
- WARNING: 448 of 769 files seen (58%) have no supported extractor and are missing from this graph entirely. It may look complete while representing only part of the codebase.

## Summary
- 2410 nodes · 4966 edges · 130 communities (35 shown, 95 thin omitted)
- Extraction: 99% EXTRACTED · 1% INFERRED · 0% AMBIGUOUS · INFERRED: 62 edges (avg confidence: 0.85)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `cbd92e01`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- VideoStudioActivity
- UserProfileActivity
- CommunityAdapter
- WritersActivity
- NotificationSettingsActivity
- app.py
- firebasefirestore
- build
- PostAdapter
- DatabaseHelper
- NotificationsAdapter
- VideoMotionView
- PostAdapter
- app/src/main/java/com/manchorati/www/AiWriterBottomSheetDialog.kt
- locale
- CommunityFragment
- app/src/main/java/com/manchorati/www/LocalLikesManager.kt
- AuthManager
- ImageUtils
- app/src/main/java/com/manchorati/www/CommunityPostDialogHelper.kt
- MainActivity
- CommentsAdapter
- CodecInputSurface
- ReelViewHolder
- CommunityFragment
- UserProfileActivity
- FirestoreManager
- .onMessageReceived
- BadWordsFilter
- StreakManager
- app/src/main/java/com/manchorati/www/SplashActivity.kt
- VideoStudioActivity
- SimpleCallback
- .setupSearch
- ReelPlayerActivity
- VideoMotionRenderer
- FirestoreManager
- .onCreate
- DatabaseHelper
- package.json
- PrivacyPolicyHelper
- app/src/androidTest/java/com/manchorati/www/ExampleInstrumentedTest.kt
- TextWatcher
- WritersActivity
- UserBadge
- .show
- app/src/main/java/com/manchorati/www/Extensions.kt
- CodecInputSurface
- PostViewHolder
- app/src/test/java/com/manchorati/www/ExampleUnitTest.kt
- quality-gate.mjs
- app/src/main/java/com/manchorati/www/FirestorePost.kt
- idea-refine.sh
- App.tsx
- ImageUtils
- LibraryManager.tsx
- VideoMotionView
- intent
- firebaseauth
- WritersAdapter
- WritersAdapter
- StudioAssetManager
- with
- manchorati-main/app/src/main/java/com/manchorati/www/CategoriesFragment.kt
- CommentsAdapter
- MainActivity
- ReelPlayerActivity
- UserFollowAdapter
- compilerOptions
- TextWatcher
- UserFollowAdapter
- app/src/main/java/com/manchorati/www/CategoriesFragment.kt
- CategoryAdapter
- app/src/main/java/com/manchorati/www/FavoritesFragment.kt
- TextWatcher
- CategoryAdapter
- StreakManager
- AudioContext.tsx
- .generatePostBitmap
- StudioAssetManager
- app/src/main/java/com/manchorati/www/ThemeHelper.kt
- PrivacyPolicyHelper
- .generatePostBitmap
- manchorati-main/app/src/main/java/com/manchorati/www/ThemeHelper.kt
- VideoMotionRenderer
- Call
- SimpleCallback
- .getPrefs
- manchorati-main/app/src/main/java/com/manchorati/www/AiWriterBottomSheetDialog.kt
- .getPrefs
- manchorati-main/app/src/main/java/com/manchorati/www/SplashActivity.kt
- MainPagerAdapter
- .showDeleteAccountDialog
- .setupSearch
- MainPagerAdapter
- scripts
- manchorati-main/app/src/main/java/com/manchorati/www/CommunityPost.kt
- manchorati-main/app/src/test/java/com/manchorati/www/ExampleUnitTest.kt
- BadWordsFilter
- manchorati-main/app/src/main/java/com/manchorati/www/FirestorePost.kt
- app/src/main/java/com/manchorati/www/Category.kt
- app/src/main/java/com/manchorati/www/Post.kt
- app/src/main/java/com/manchorati/www/WriterItem.kt
- manchorati-main/app/src/main/java/com/manchorati/www/Category.kt
- manchorati-main/app/src/main/java/com/manchorati/www/Post.kt
- manchorati-main/app/src/main/java/com/manchorati/www/WriterItem.kt

## God Nodes (most connected - your core abstractions)
1. `MainActivity` - 50 edges
2. `VideoStudioActivity` - 50 edges
3. `MainActivity` - 46 edges
4. `CommunityFragment` - 45 edges
5. `VideoStudioActivity` - 42 edges
6. `CommunityFragment` - 41 edges
7. `UserProfileActivity` - 36 edges
8. `UserProfileActivity` - 36 edges
9. `DatabaseHelper` - 35 edges
10. `DatabaseHelper` - 33 edges

## Surprising Connections (you probably didn't know these)
- `PostAdapter` --calls--> `DatabaseHelper`  [INFERRED]
  Desktop/apps/manchorati-main/app/src/main/java/com/manchorati/www/PostAdapter.kt → Desktop/apps/manchorati-main/app/src/main/java/com/manchorati/www/DatabaseHelper.kt
- `PostAdapter` --calls--> `DatabaseHelper`  [INFERRED]
  app/src/main/java/com/manchorati/www/PostAdapter.kt → app/src/main/java/com/manchorati/www/DatabaseHelper.kt
- `MainPlayerView()` --calls--> `LibraryManager()`  [EXTRACTED]
  Desktop/apps py me/Modernappmusic-main/src/App.tsx → Desktop/apps py me/Modernappmusic-main/src/components/LibraryManager.tsx
- `App()` --calls--> `AudioProvider()`  [EXTRACTED]
  Desktop/apps py me/Modernappmusic-main/src/App.tsx → Desktop/apps py me/Modernappmusic-main/src/context/AudioContext.tsx
- `LibraryManager()` --calls--> `useAudio()`  [EXTRACTED]
  Desktop/apps py me/Modernappmusic-main/src/components/LibraryManager.tsx → Desktop/apps py me/Modernappmusic-main/src/context/AudioContext.tsx

## Import Cycles
- None detected.

## Communities (130 total, 95 thin omitted)

### Community 0 - "VideoStudioActivity"
Cohesion: 0.06
Nodes (7): VideoStudioActivity, OnSeekBarChangeListener, OnSeekBarChangeListener, OnSeekBarChangeListener, Runnable, TextWatcher, VideoTheme

### Community 2 - "CommunityAdapter"
Cohesion: 0.08
Nodes (3): CommunityAdapter, PostDiffCallback, PostViewHolder

### Community 5 - "app.py"
Cohesion: 0.07
Nodes (42): add_quote(), delete_community_post(), get_categories(), get_community_posts(), get_sqlite_conn(), get_stats(), get_users(), index() (+34 more)

### Community 6 - "firebasefirestore"
Cohesion: 0.06
Nodes (6): AiWriterManager, AutoEngagementWorker, NotificationSyncWorker, SyncManager, AutoEngagementWorker, SyncManager

### Community 7 - "build"
Cohesion: 0.07
Nodes (7): DailyNotificationHelper, DailyNotificationWorker, NotificationReceiver, AiWriterManager, DailyNotificationHelper, DailyNotificationWorker, NotificationReceiver

### Community 9 - "DatabaseHelper"
Cohesion: 0.09
Nodes (3): DatabaseHelper, VirtualCommunityManager, VirtualPersona

### Community 10 - "NotificationsAdapter"
Cohesion: 0.09
Nodes (4): NotificationsAdapter, NotificationViewHolder, NotificationsAdapter, NotificationViewHolder

### Community 12 - "PostAdapter"
Cohesion: 0.05
Nodes (5): CategoryPostsActivity, CategoryPostsActivity, FavoritesFragment, PostAdapter, PostViewHolder

### Community 14 - "locale"
Cohesion: 0.05
Nodes (5): AudioTimelineTrimmerView, LocaleHelper, TimelineFilmstripView, LocaleHelper, TimelineFilmstripView

### Community 15 - "CommunityFragment"
Cohesion: 0.06
Nodes (13): CommunityFragment, OnScrollListener, TextWatcher, MoodType, ALL, CHILL, HAPPY, LOVE (+5 more)

### Community 19 - "ImageUtils"
Cohesion: 0.15
Nodes (4): FontItem, ImageUtils, OnAmbilWarnaListener, OnAmbilWarnaListener

### Community 24 - "ReelViewHolder"
Cohesion: 0.06
Nodes (6): CommunityPost, ReelsAdapter, ReelViewHolder, SimpleOnGestureListener, ReelsFragment, OnPageChangeCallback

### Community 25 - "CommunityFragment"
Cohesion: 0.07
Nodes (11): CommunityFragment, TextWatcher, MoodType, ALL, CHILL, HAPPY, LOVE, MOTIVATED (+3 more)

### Community 26 - "UserProfileActivity"
Cohesion: 0.06
Nodes (5): UserProfileActivity, TextWatcher, CountDownTimer, TextWatcher, TextWatcher

### Community 27 - "FirestoreManager"
Cohesion: 0.06
Nodes (4): AppNotification, Comment, FirestoreManager, QuoteWidgetProvider

### Community 33 - "VideoStudioActivity"
Cohesion: 0.08
Nodes (4): VideoStudioActivity, OnSeekBarChangeListener, OnSeekBarChangeListener, TextWatcher

### Community 39 - "FirestoreManager"
Cohesion: 0.06
Nodes (4): AppNotification, Comment, FirestoreManager, QuoteWidgetProvider

### Community 41 - "DatabaseHelper"
Cohesion: 0.09
Nodes (3): DatabaseHelper, VirtualCommunityManager, VirtualPersona

### Community 42 - "package.json"
Cohesion: 0.07
Nodes (39): dependencies, dotenv@^17.2.3, express@^4.21.2, @google/genai@^2.4.0, lucide-react@^0.546.0, motion@^12.23.24, react@^19.0.1, react-dom@^19.0.1 (+31 more)

### Community 48 - "UserBadge"
Cohesion: 0.10
Nodes (6): BadgeHelper, MilestoneBadge, UserBadge, BadgeHelper, MilestoneBadge, UserBadge

### Community 58 - "App.tsx"
Cohesion: 0.25
Nodes (16): App(), MainPlayerView(), ABLoopControls(), LockScreenModal(), LockScreenModalProps, PlaybackEngineControls(), PlaybackEngineControlsProps, SleepTimerModal() (+8 more)

### Community 60 - "ImageUtils"
Cohesion: 0.15
Nodes (4): FontItem, ImageUtils, OnAmbilWarnaListener, OnAmbilWarnaListener

### Community 63 - "LibraryManager.tsx"
Cohesion: 0.17
Nodes (18): LibraryManager(), LibraryCategory, Playlist, ScanFilters, generateSyntheticPeaks(), getAudioContext(), getAudioDuration(), processAudioFile() (+10 more)

### Community 67 - "WritersAdapter"
Cohesion: 0.13
Nodes (6): WritersAdapter, WritersTabMode, ALL, FOLLOWERS, WEEKLY, WriterViewHolder

### Community 68 - "WritersAdapter"
Cohesion: 0.13
Nodes (6): WritersAdapter, WritersTabMode, ALL, FOLLOWERS, WEEKLY, WriterViewHolder

### Community 69 - "StudioAssetManager"
Cohesion: 0.18
Nodes (5): OnlineAudio, OnlineBackground, StudioAssetManager, Callback, Callback

### Community 70 - "with"
Cohesion: 0.11
Nodes (18): runs-on, steps, jobs, build, name, on, push, workflow_dispatch (+10 more)

### Community 76 - "UserFollowAdapter"
Cohesion: 0.17
Nodes (3): UserFollowAdapter, UserFollowItem, UserViewHolder

### Community 77 - "compilerOptions"
Cohesion: 0.12
Nodes (15): compilerOptions, allowImportingTsExtensions, allowJs, experimentalDecorators, isolatedModules, jsx, lib, module (+7 more)

### Community 78 - "TextWatcher"
Cohesion: 0.15
Nodes (3): TextWatcher, TextWatcher, TextWatcher

### Community 79 - "UserFollowAdapter"
Cohesion: 0.19
Nodes (3): UserFollowAdapter, UserFollowItem, UserViewHolder

### Community 81 - "CategoryAdapter"
Cohesion: 0.20
Nodes (3): CategoryAdapter, CategoryViewHolder, CategoryVisual

### Community 87 - "AudioContext.tsx"
Cohesion: 0.38
Nodes (9): AudioContext, AudioContextType, AudioProvider(), formatTimeSimple(), ABLoopState, JumpInterval, SavedLoop, SleepTimerState (+1 more)

### Community 89 - "StudioAssetManager"
Cohesion: 0.36
Nodes (3): OnlineAudio, OnlineBackground, StudioAssetManager

### Community 109 - "scripts"
Cohesion: 0.33
Nodes (6): scripts, build, clean, dev, lint, preview

## Knowledge Gaps
- **20 isolated node(s):** `ALL`, `CHILL`, `MOTIVATED`, `SAD`, `HAPPY` (+15 more)
  These have ≤1 connection - possible missing edges. (Counts symbols only; 410 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **95 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `DatabaseHelper` connect `DatabaseHelper` to `.onCreate`, `NotificationSettingsActivity`, `build`, `PostAdapter`, `CommunityFragment`, `ReelViewHolder`, `FirestoreManager`?**
  _High betweenness centrality (0.044) - this node is a cross-community bridge._
- **What connects `ALL`, `CHILL`, `MOTIVATED` to the rest of the system?**
  _20 weakly-connected nodes found - possible missing edges._
- **Should `VideoStudioActivity` be split into smaller, more focused modules?**
  _Cohesion score 0.05964912280701754 - nodes in this community are weakly interconnected._
- **Why does `MainActivity` connect `MainActivity` to `.setupSearch`, `.onNewIntent`, `.onCreate`, `TextWatcher`, `.setupDrawerNavigation`?**
  _High betweenness centrality (0.040) - this node is a cross-community bridge._
- **Should `CommunityAdapter` be split into smaller, more focused modules?**
  _Cohesion score 0.08170731707317073 - nodes in this community are weakly interconnected._
- **Should `WritersActivity` be split into smaller, more focused modules?**
  _Cohesion score 0.0951219512195122 - nodes in this community are weakly interconnected._
- **Should `NotificationSettingsActivity` be split into smaller, more focused modules?**
  _Cohesion score 0.09024390243902439 - nodes in this community are weakly interconnected._