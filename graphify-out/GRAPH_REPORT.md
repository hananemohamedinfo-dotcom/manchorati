# Graph Report - manchorati-main  (2026-11-08)

## Corpus Check
- 174 files · ~228,908 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 454 file(s) not represented in the graph (top: .xml 405, .ttf 19, (none) 7)
- WARNING: 454 of 775 files seen (59%) have no supported extractor and are missing from this graph entirely. It may look complete while representing only part of the codebase.

## Summary
- 2441 nodes · 4958 edges · 176 communities (37 shown, 139 thin omitted)
- Extraction: 99% EXTRACTED · 1% INFERRED · 0% AMBIGUOUS · INFERRED: 50 edges (avg confidence: 0.85)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `12bb877b`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- VideoStudioActivity
- UserProfileActivity
- CommunityAdapter
- WritersActivity
- NotificationSettingsActivity
- app.py
- app/src/main/java/com/manchorati/www/SyncManager.kt
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
- app/src/main/java/com/manchorati/www/LocaleHelper.kt
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
- StudioAssetManager
- with
- app/src/main/java/com/manchorati/www/CategoriesFragment.kt
- CommentsAdapter
- MainActivity
- ReelPlayerActivity
- UserFollowAdapter
- compilerOptions
- AiWriterManager
- UserFollowAdapter
- context
- CategoryAdapter
- VirtualCommunityManager
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
- TextWatcher
- .show
- .onCreate
- SimpleCallback
- .getPrefs
- manchorati-main/app/src/main/java/com/manchorati/www/AiWriterBottomSheetDialog.kt
- .getPrefs
- manchorati-main/app/src/main/java/com/manchorati/www/SplashActivity.kt
- .setupSearch
- .showDeleteAccountDialog
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
9. `DatabaseHelper` - 33 edges
10. `WritersActivity` - 30 edges

## Surprising Connections (you probably didn't know these)
- `PostAdapter` --calls--> `DatabaseHelper`  [INFERRED]
  Desktop/apps/manchorati-main/app/src/main/java/com/manchorati/www/PostAdapter.kt → Desktop/apps/manchorati-main/app/src/main/java/com/manchorati/www/DatabaseHelper.kt
- `PostAdapter` --calls--> `DatabaseHelper`  [INFERRED]
  app/src/main/java/com/manchorati/www/PostAdapter.kt → app/src/main/java/com/manchorati/www/DatabaseHelper.kt
- `CommunityFragment` --references--> `CommunityAdapter`  [EXTRACTED]
  app/src/main/java/com/manchorati/www/CommunityFragment.kt → app/src/main/java/com/manchorati/www/CommunityAdapter.kt
- `UserProfileActivity` --references--> `CommunityAdapter`  [EXTRACTED]
  app/src/main/java/com/manchorati/www/UserProfileActivity.kt → app/src/main/java/com/manchorati/www/CommunityAdapter.kt
- `UserProfileActivity` --references--> `UserBadge`  [EXTRACTED]
  app/src/main/java/com/manchorati/www/UserProfileActivity.kt → app/src/main/java/com/manchorati/www/UserBadge.kt

## Import Cycles
- None detected.

## Communities (176 total, 139 thin omitted)

### Community 0 - "VideoStudioActivity"
Cohesion: 0.06
Nodes (7): VideoStudioActivity, OnSeekBarChangeListener, OnSeekBarChangeListener, OnSeekBarChangeListener, Runnable, TextWatcher, VideoTheme

### Community 1 - "UserProfileActivity"
Cohesion: 0.06
Nodes (7): formatCount(), UserProfileActivity, TextWatcher, CountDownTimer, TextWatcher, TextWatcher, formatCount()

### Community 2 - "CommunityAdapter"
Cohesion: 0.09
Nodes (3): CommunityAdapter, PostDiffCallback, PostViewHolder

### Community 3 - "WritersActivity"
Cohesion: 0.06
Nodes (8): WritersActivity, TextWatcher, WritersAdapter, WritersTabMode, ALL, FOLLOWERS, WEEKLY, WriterViewHolder

### Community 5 - "app.py"
Cohesion: 0.07
Nodes (42): add_quote(), delete_community_post(), get_categories(), get_community_posts(), get_sqlite_conn(), get_stats(), get_users(), index() (+34 more)

### Community 7 - "build"
Cohesion: 0.07
Nodes (7): DailyNotificationHelper, DailyNotificationWorker, NotificationReceiver, AiWriterManager, DailyNotificationHelper, DailyNotificationWorker, NotificationReceiver

### Community 8 - "PostAdapter"
Cohesion: 0.05
Nodes (5): CategoryPostsActivity, FavoritesFragment, PostAdapter, PostViewHolder, CategoryPostsActivity

### Community 10 - "NotificationsAdapter"
Cohesion: 0.09
Nodes (4): NotificationsAdapter, NotificationViewHolder, NotificationsAdapter, NotificationViewHolder

### Community 12 - "PostAdapter"
Cohesion: 0.11
Nodes (3): FavoritesFragment, PostAdapter, PostViewHolder

### Community 14 - "locale"
Cohesion: 0.07
Nodes (3): AudioTimelineTrimmerView, TimelineFilmstripView, TimelineFilmstripView

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

### Community 27 - "FirestoreManager"
Cohesion: 0.06
Nodes (4): AppNotification, Comment, FirestoreManager, QuoteWidgetProvider

### Community 33 - "VideoStudioActivity"
Cohesion: 0.07
Nodes (4): VideoStudioActivity, OnSeekBarChangeListener, OnSeekBarChangeListener, TextWatcher

### Community 37 - "VideoMotionRenderer"
Cohesion: 0.14
Nodes (4): VideoConfig, VideoMotionRenderer, VideoConfig, VideoMotionRenderer

### Community 39 - "FirestoreManager"
Cohesion: 0.07
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

### Community 68 - "WritersAdapter"
Cohesion: 0.13
Nodes (6): WritersAdapter, WritersTabMode, ALL, FOLLOWERS, WEEKLY, WriterViewHolder

### Community 69 - "StudioAssetManager"
Cohesion: 0.16
Nodes (5): OnlineAudio, OnlineBackground, StudioAssetManager, Callback, Callback

### Community 70 - "with"
Cohesion: 0.11
Nodes (18): runs-on, steps, jobs, build, name, on, push, workflow_dispatch (+10 more)

### Community 71 - "app/src/main/java/com/manchorati/www/CategoriesFragment.kt"
Cohesion: 0.07
Nodes (4): CategoriesFragment, MainPagerAdapter, CategoriesFragment, MainPagerAdapter

### Community 76 - "UserFollowAdapter"
Cohesion: 0.19
Nodes (3): UserFollowAdapter, UserFollowItem, UserViewHolder

### Community 77 - "compilerOptions"
Cohesion: 0.12
Nodes (15): compilerOptions, allowImportingTsExtensions, allowJs, experimentalDecorators, isolatedModules, jsx, lib, module (+7 more)

### Community 79 - "UserFollowAdapter"
Cohesion: 0.17
Nodes (3): UserFollowAdapter, UserFollowItem, UserViewHolder

### Community 80 - "context"
Cohesion: 0.18
Nodes (3): AutoEngagementWorker, NotificationSyncWorker, AutoEngagementWorker

### Community 81 - "CategoryAdapter"
Cohesion: 0.18
Nodes (3): CategoryAdapter, CategoryViewHolder, CategoryVisual

### Community 87 - "AudioContext.tsx"
Cohesion: 0.38
Nodes (9): AudioContext, AudioContextType, AudioProvider(), formatTimeSimple(), ABLoopState, JumpInterval, SavedLoop, SleepTimerState (+1 more)

### Community 89 - "StudioAssetManager"
Cohesion: 0.16
Nodes (5): OnlineAudio, OnlineBackground, StudioAssetManager, Callback, Callback

### Community 94 - "TextWatcher"
Cohesion: 0.15
Nodes (3): TextWatcher, TextWatcher, TextWatcher

### Community 109 - "scripts"
Cohesion: 0.33
Nodes (6): scripts, build, clean, dev, lint, preview

## Knowledge Gaps
- **20 isolated node(s):** `ALL`, `CHILL`, `MOTIVATED`, `SAD`, `HAPPY` (+15 more)
  These have ≤1 connection - possible missing edges. (Counts symbols only; 448 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **139 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `DatabaseHelper` connect `DatabaseHelper` to `NotificationSettingsActivity`, `FirestoreManager`, `build`, `.onCreate`, `PostAdapter`, `CommunityFragment`?**
  _High betweenness centrality (0.059) - this node is a cross-community bridge._
- **What connects `ALL`, `CHILL`, `MOTIVATED` to the rest of the system?**
  _20 weakly-connected nodes found - possible missing edges._
- **Should `VideoStudioActivity` be split into smaller, more focused modules?**
  _Cohesion score 0.05727605727605728 - nodes in this community are weakly interconnected._
- **Why does `UserProfileActivity` connect `UserProfileActivity` to `.showCommentsDialog`, `.onCreate`, `.showDeleteAccountDialog`, `.showEditProfileDialog`, `UserFollowAdapter`, `TextWatcher`?**
  _High betweenness centrality (0.057) - this node is a cross-community bridge._
- **Should `UserProfileActivity` be split into smaller, more focused modules?**
  _Cohesion score 0.05817028027498678 - nodes in this community are weakly interconnected._
- **Why does `CommunityFragment` connect `CommunityFragment` to `app/src/main/java/com/manchorati/www/CategoriesFragment.kt`?**
  _High betweenness centrality (0.043) - this node is a cross-community bridge._
- **Should `CommunityAdapter` be split into smaller, more focused modules?**
  _Cohesion score 0.08771929824561403 - nodes in this community are weakly interconnected._