# Graph Report - manchorati-main  (2026-11-10)

## Corpus Check
- 178 files · ~231,929 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 470 file(s) not represented in the graph (top: .xml 421, .ttf 19, (none) 7)
- WARNING: 470 of 795 files seen (59%) have no supported extractor and are missing from this graph entirely. It may look complete while representing only part of the codebase.

## Summary
- 2529 nodes · 5149 edges · 201 communities (36 shown, 165 thin omitted)
- Extraction: 99% EXTRACTED · 1% INFERRED · 0% AMBIGUOUS · INFERRED: 47 edges (avg confidence: 0.85)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `a6b38d9c`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- VideoStudioActivity
- FirebaseFirestore
- CommunityAdapter
- WritersActivity
- NotificationSettingsActivity
- app.py
- app/src/main/java/com/manchorati/www/AiWriterManager.kt
- build
- PostAdapter
- MainActivity
- NotificationsAdapter
- VideoMotionView
- PostAdapter
- app/src/main/java/com/manchorati/www/AiWriterBottomSheetDialog.kt
- AudioTimelineTrimmerView
- CommunityFragment
- VirtualCommunityManager
- AuthManager
- ImageUtils
- app/src/main/java/com/manchorati/www/CategoriesFragment.kt
- CommentsAdapter
- CodecInputSurface
- ReelViewHolder
- CommunityFragment
- UserProfileActivity
- FirestoreManager
- app/src/main/java/com/manchorati/www/LocaleHelper.kt
- BadWordsFilter
- StreakManager
- ReelsFragment
- app/src/main/java/com/manchorati/www/SplashActivity.kt
- VideoStudioActivity
- ReelPlayerActivity
- Canvas
- FirestoreManager
- DatabaseHelper
- package.json
- app/src/main/java/com/manchorati/www/PrivacyPolicyHelper.kt
- app/src/androidTest/java/com/manchorati/www/ExampleInstrumentedTest.kt
- WritersActivity
- UserBadge
- manchorati-main/app/src/main/java/com/manchorati/www/FavoritesFragment.kt
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
- toast
- .show
- WritersAdapter
- StudioAssetManager
- with
- manchorati-main/app/src/main/java/com/manchorati/www/CategoriesFragment.kt
- CommentsAdapter
- manchorati-main/app/src/main/java/com/manchorati/www/MainActivity.kt
- ReelPlayerActivity
- .show
- UserFollowAdapter
- compilerOptions
- AiWriterManager
- UserFollowAdapter
- AiChatActivity.kt
- VideoMotionRenderer
- CategoryAdapter
- StreakManager
- AudioContext.tsx
- .generatePostBitmap
- StudioAssetManager
- app/src/main/java/com/manchorati/www/ThemeHelper.kt
- app/src/main/java/com/manchorati/www/TimelineFilmstripView.kt
- .generatePostBitmap
- manchorati-main/app/src/main/java/com/manchorati/www/ThemeHelper.kt
- manchorati-main/app/src/main/java/com/manchorati/www/TimelineFilmstripView.kt
- .loadReels
- .createFragment
- SimpleOnGestureListener
- .getPrefs
- manchorati-main/app/src/main/java/com/manchorati/www/AiWriterBottomSheetDialog.kt
- .getPrefs
- manchorati-main/app/src/main/java/com/manchorati/www/SplashActivity.kt
- DatabaseHelper
- UserProfileActivity
- MainPagerAdapter
- scripts
- manchorati-main/app/src/main/java/com/manchorati/www/CommunityPost.kt
- TextWatcher
- manchorati-main/app/src/test/java/com/manchorati/www/ExampleUnitTest.kt
- BadWordsFilter
- manchorati-main/app/src/main/java/com/manchorati/www/FirestorePost.kt
- app/src/main/java/com/manchorati/www/Category.kt
- app/src/main/java/com/manchorati/www/Post.kt
- app/src/main/java/com/manchorati/www/WriterItem.kt
- manchorati-main/app/src/main/java/com/manchorati/www/Category.kt
- manchorati-main/app/src/main/java/com/manchorati/www/Post.kt
- manchorati-main/app/src/main/java/com/manchorati/www/WriterItem.kt
- app/src/main/java/com/manchorati/www/Extensions.kt
- .showDeleteAccountDialog

## God Nodes (most connected - your core abstractions)
1. `VideoStudioActivity` - 51 edges
2. `MainActivity` - 51 edges
3. `MainActivity` - 46 edges
4. `CommunityFragment` - 45 edges
5. `VideoStudioActivity` - 42 edges
6. `CommunityFragment` - 41 edges
7. `UserProfileActivity` - 38 edges
8. `UserProfileActivity` - 36 edges
9. `DatabaseHelper` - 33 edges
10. `WritersActivity` - 30 edges

## Surprising Connections (you probably didn't know these)
- `PostAdapter` --calls--> `DatabaseHelper`  [INFERRED]
  app/src/main/java/com/manchorati/www/PostAdapter.kt → app/src/main/java/com/manchorati/www/DatabaseHelper.kt
- `PostAdapter` --calls--> `DatabaseHelper`  [INFERRED]
  Desktop/apps/manchorati-main/app/src/main/java/com/manchorati/www/PostAdapter.kt → Desktop/apps/manchorati-main/app/src/main/java/com/manchorati/www/DatabaseHelper.kt
- `ReelsFragment` --references--> `ReelsAdapter`  [EXTRACTED]
  app/src/main/java/com/manchorati/www/ReelsFragment.kt → app/src/main/java/com/manchorati/www/ReelsAdapter.kt
- `UserProfileActivity` --references--> `CommunityAdapter`  [EXTRACTED]
  app/src/main/java/com/manchorati/www/UserProfileActivity.kt → app/src/main/java/com/manchorati/www/CommunityAdapter.kt
- `App()` --calls--> `AudioProvider()`  [EXTRACTED]
  Desktop/apps py me/Modernappmusic-main/src/App.tsx → Desktop/apps py me/Modernappmusic-main/src/context/AudioContext.tsx

## Import Cycles
- None detected.

## Communities (201 total, 165 thin omitted)

### Community 0 - "VideoStudioActivity"
Cohesion: 0.07
Nodes (6): VideoStudioActivity, OnSeekBarChangeListener, OnSeekBarChangeListener, OnSeekBarChangeListener, Runnable, VideoTheme

### Community 1 - "FirebaseFirestore"
Cohesion: 0.16
Nodes (3): AutoEngagementWorker, NotificationSyncWorker, AutoEngagementWorker

### Community 2 - "CommunityAdapter"
Cohesion: 0.09
Nodes (3): CommunityAdapter, PostDiffCallback, PostViewHolder

### Community 3 - "WritersActivity"
Cohesion: 0.06
Nodes (9): WriterOfTheWeekHelper, WritersActivity, TextWatcher, WritersAdapter, WritersTabMode, ALL, FOLLOWERS, WEEKLY (+1 more)

### Community 4 - "NotificationSettingsActivity"
Cohesion: 0.06
Nodes (4): ContactActivity, NotificationSettingsActivity, ContactActivity, NotificationSettingsActivity

### Community 5 - "app.py"
Cohesion: 0.07
Nodes (42): add_quote(), delete_community_post(), get_categories(), get_community_posts(), get_sqlite_conn(), get_stats(), get_users(), index() (+34 more)

### Community 7 - "build"
Cohesion: 0.06
Nodes (9): AppFirebaseMessagingService, DailyNotificationHelper, DailyNotificationWorker, NotificationHelper, NotificationReceiver, AiWriterManager, DailyNotificationHelper, DailyNotificationWorker (+1 more)

### Community 8 - "PostAdapter"
Cohesion: 0.05
Nodes (5): CategoryPostsActivity, FavoritesFragment, PostAdapter, PostViewHolder, CategoryPostsActivity

### Community 9 - "MainActivity"
Cohesion: 0.05
Nodes (5): MainActivity, OnBackPressedCallback, TextWatcher, OnPageChangeCallback, SimpleCallback

### Community 10 - "NotificationsAdapter"
Cohesion: 0.09
Nodes (4): NotificationsAdapter, NotificationViewHolder, NotificationsAdapter, NotificationViewHolder

### Community 15 - "CommunityFragment"
Cohesion: 0.05
Nodes (14): CommunityFragment, OnScrollListener, TextWatcher, MoodType, ALL, CHILL, HAPPY, LOVE (+6 more)

### Community 19 - "ImageUtils"
Cohesion: 0.15
Nodes (4): FontItem, ImageUtils, OnAmbilWarnaListener, OnAmbilWarnaListener

### Community 25 - "CommunityFragment"
Cohesion: 0.07
Nodes (11): CommunityFragment, TextWatcher, MoodType, ALL, CHILL, HAPPY, LOVE, MOTIVATED (+3 more)

### Community 27 - "FirestoreManager"
Cohesion: 0.07
Nodes (4): AppNotification, Comment, FirestoreManager, QuoteWidgetProvider

### Community 33 - "VideoStudioActivity"
Cohesion: 0.07
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
Cohesion: 0.20
Nodes (3): BadgeHelper, MilestoneBadge, UserBadge

### Community 58 - "App.tsx"
Cohesion: 0.25
Nodes (16): App(), MainPlayerView(), ABLoopControls(), LockScreenModal(), LockScreenModalProps, PlaybackEngineControls(), PlaybackEngineControlsProps, SleepTimerModal() (+8 more)

### Community 60 - "ImageUtils"
Cohesion: 0.15
Nodes (4): FontItem, ImageUtils, OnAmbilWarnaListener, OnAmbilWarnaListener

### Community 63 - "LibraryManager.tsx"
Cohesion: 0.17
Nodes (18): LibraryManager(), LibraryCategory, Playlist, ScanFilters, generateSyntheticPeaks(), getAudioContext(), getAudioDuration(), processAudioFile() (+10 more)

### Community 68 - "WritersAdapter"
Cohesion: 0.15
Nodes (6): WritersAdapter, WritersTabMode, ALL, FOLLOWERS, WEEKLY, WriterViewHolder

### Community 69 - "StudioAssetManager"
Cohesion: 0.16
Nodes (5): OnlineAudio, OnlineBackground, StudioAssetManager, Callback, Callback

### Community 70 - "with"
Cohesion: 0.11
Nodes (18): runs-on, steps, jobs, build, name, on, push, workflow_dispatch (+10 more)

### Community 73 - "manchorati-main/app/src/main/java/com/manchorati/www/MainActivity.kt"
Cohesion: 0.05
Nodes (5): MainActivity, OnBackPressedCallback, TextWatcher, OnPageChangeCallback, SimpleCallback

### Community 76 - "UserFollowAdapter"
Cohesion: 0.19
Nodes (3): UserFollowAdapter, UserFollowItem, UserViewHolder

### Community 77 - "compilerOptions"
Cohesion: 0.12
Nodes (15): compilerOptions, allowImportingTsExtensions, allowJs, experimentalDecorators, isolatedModules, jsx, lib, module (+7 more)

### Community 78 - "AiWriterManager"
Cohesion: 0.10
Nodes (5): AiWriterManager, Callback, Callback, LocalLikesManager, LocalLikesManager

### Community 79 - "UserFollowAdapter"
Cohesion: 0.17
Nodes (3): UserFollowAdapter, UserFollowItem, UserViewHolder

### Community 81 - "AiChatActivity.kt"
Cohesion: 0.07
Nodes (11): AiChatActivity, AiChatAdapter, BotViewHolder, LoadingViewHolder, UserViewHolder, AiChatMessage, Bot, Loading (+3 more)

### Community 87 - "AudioContext.tsx"
Cohesion: 0.38
Nodes (9): AudioContext, AudioContextType, AudioProvider(), formatTimeSimple(), ABLoopState, JumpInterval, SavedLoop, SleepTimerState (+1 more)

### Community 89 - "StudioAssetManager"
Cohesion: 0.15
Nodes (5): OnlineAudio, OnlineBackground, StudioAssetManager, Callback, Callback

### Community 106 - "UserProfileActivity"
Cohesion: 0.05
Nodes (8): BadgeHelper, MilestoneBadge, UserBadge, UserProfileActivity, TextWatcher, CountDownTimer, TextWatcher, TextWatcher

### Community 109 - "scripts"
Cohesion: 0.33
Nodes (6): scripts, build, clean, dev, lint, preview

### Community 114 - "TextWatcher"
Cohesion: 0.15
Nodes (3): TextWatcher, TextWatcher, TextWatcher

## Knowledge Gaps
- **23 isolated node(s):** `Bot`, `Loading`, `User`, `ALL`, `CHILL` (+18 more)
  These have ≤1 connection - possible missing edges. (Counts symbols only; 487 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **165 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `VideoStudioActivity` connect `VideoStudioActivity` to `app/src/main/java/com/manchorati/www/ImageUtils.kt`?**
  _High betweenness centrality (0.041) - this node is a cross-community bridge._
- **What connects `Bot`, `Loading`, `User` to the rest of the system?**
  _23 weakly-connected nodes found - possible missing edges._
- **Should `VideoStudioActivity` be split into smaller, more focused modules?**
  _Cohesion score 0.07155399473222125 - nodes in this community are weakly interconnected._
- **Why does `FirestoreManager` connect `FirestoreManager` to `FirebaseFirestore`?**
  _High betweenness centrality (0.037) - this node is a cross-community bridge._
- **Should `CommunityAdapter` be split into smaller, more focused modules?**
  _Cohesion score 0.0945945945945946 - nodes in this community are weakly interconnected._
- **Should `WritersActivity` be split into smaller, more focused modules?**
  _Cohesion score 0.05608322026232474 - nodes in this community are weakly interconnected._
- **Should `NotificationSettingsActivity` be split into smaller, more focused modules?**
  _Cohesion score 0.05928614640048397 - nodes in this community are weakly interconnected._