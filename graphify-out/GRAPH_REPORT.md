# Graph Report - manchorati-main  (2026-11-09)

## Corpus Check
- 175 files · ~229,256 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 460 file(s) not represented in the graph (top: .xml 411, .ttf 19, (none) 7)
- WARNING: 460 of 782 files seen (59%) have no supported extractor and are missing from this graph entirely. It may look complete while representing only part of the codebase.

## Summary
- 2458 nodes · 4984 edges · 187 communities (38 shown, 149 thin omitted)
- Extraction: 99% EXTRACTED · 1% INFERRED · 0% AMBIGUOUS · INFERRED: 50 edges (avg confidence: 0.85)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `2a8b75fc`
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
- app/src/main/java/com/manchorati/www/CommunityPostDialogHelper.kt
- MainActivity
- CommentsAdapter
- CodecInputSurface
- ReelViewHolder
- CommunityFragment
- UserProfileActivity
- FirestoreManager
- LocaleHelper
- BadWordsFilter
- StreakManager
- LocaleHelper
- app/src/main/java/com/manchorati/www/SplashActivity.kt
- VideoStudioActivity
- ReelPlayerActivity
- VideoMotionRenderer
- FirestoreManager
- DatabaseHelper
- package.json
- PrivacyPolicyHelper
- app/src/androidTest/java/com/manchorati/www/ExampleInstrumentedTest.kt
- WritersActivity
- UserBadge
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
- VideoMotionRenderer
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
- app/src/main/java/com/manchorati/www/AutoEngagementWorker.kt
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
- app/src/main/java/com/manchorati/www/Extensions.kt
- .onCreate
- SimpleCallback
- .getPrefs
- manchorati-main/app/src/main/java/com/manchorati/www/AiWriterBottomSheetDialog.kt
- .getPrefs
- manchorati-main/app/src/main/java/com/manchorati/www/SplashActivity.kt
- FirebaseFirestore
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
- app/src/main/java/com/manchorati/www/CopyActivity.kt
- manchorati-main/app/src/main/java/com/manchorati/www/AutoEngagementWorker.kt
- manchorati-main/app/src/main/java/com/manchorati/www/CopyActivity.kt

## God Nodes (most connected - your core abstractions)
1. `VideoStudioActivity` - 50 edges
2. `MainActivity` - 50 edges
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
  Desktop/apps/manchorati-main/app/src/main/java/com/manchorati/www/PostAdapter.kt → Desktop/apps/manchorati-main/app/src/main/java/com/manchorati/www/DatabaseHelper.kt
- `PostAdapter` --calls--> `DatabaseHelper`  [INFERRED]
  app/src/main/java/com/manchorati/www/PostAdapter.kt → app/src/main/java/com/manchorati/www/DatabaseHelper.kt
- `CommunityFragment` --references--> `CommunityAdapter`  [EXTRACTED]
  app/src/main/java/com/manchorati/www/CommunityFragment.kt → app/src/main/java/com/manchorati/www/CommunityAdapter.kt
- `UserProfileActivity` --references--> `CommunityAdapter`  [EXTRACTED]
  app/src/main/java/com/manchorati/www/UserProfileActivity.kt → app/src/main/java/com/manchorati/www/CommunityAdapter.kt
- `ReelsFragment` --references--> `ReelsAdapter`  [EXTRACTED]
  app/src/main/java/com/manchorati/www/ReelsFragment.kt → app/src/main/java/com/manchorati/www/ReelsAdapter.kt

## Import Cycles
- None detected.

## Communities (187 total, 149 thin omitted)

### Community 0 - "VideoStudioActivity"
Cohesion: 0.06
Nodes (7): VideoStudioActivity, OnSeekBarChangeListener, OnSeekBarChangeListener, OnSeekBarChangeListener, Runnable, TextWatcher, VideoTheme

### Community 1 - "UserProfileActivity"
Cohesion: 0.05
Nodes (8): BadgeHelper, MilestoneBadge, UserBadge, UserProfileActivity, TextWatcher, CountDownTimer, TextWatcher, TextWatcher

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
Cohesion: 0.05
Nodes (9): AppFirebaseMessagingService, DailyNotificationHelper, DailyNotificationWorker, NotificationHelper, NotificationReceiver, AiWriterManager, DailyNotificationHelper, DailyNotificationWorker (+1 more)

### Community 8 - "PostAdapter"
Cohesion: 0.11
Nodes (3): FavoritesFragment, PostAdapter, PostViewHolder

### Community 9 - "DatabaseHelper"
Cohesion: 0.07
Nodes (4): CommunityPost, DatabaseHelper, ReelsFragment, OnPageChangeCallback

### Community 10 - "NotificationsAdapter"
Cohesion: 0.09
Nodes (4): NotificationsAdapter, NotificationViewHolder, NotificationsAdapter, NotificationViewHolder

### Community 12 - "PostAdapter"
Cohesion: 0.05
Nodes (5): CategoryPostsActivity, CategoryPostsActivity, FavoritesFragment, PostAdapter, PostViewHolder

### Community 14 - "locale"
Cohesion: 0.07
Nodes (3): AudioTimelineTrimmerView, TimelineFilmstripView, TimelineFilmstripView

### Community 15 - "CommunityFragment"
Cohesion: 0.06
Nodes (13): CommunityFragment, OnScrollListener, TextWatcher, MoodType, ALL, CHILL, HAPPY, LOVE (+5 more)

### Community 19 - "ImageUtils"
Cohesion: 0.15
Nodes (4): FontItem, ImageUtils, OnAmbilWarnaListener, OnAmbilWarnaListener

### Community 20 - "app/src/main/java/com/manchorati/www/CommunityPostDialogHelper.kt"
Cohesion: 0.09
Nodes (4): CommunityPostDialogHelper, TextWatcher, CommunityPostDialogHelper, TextWatcher

### Community 21 - "MainActivity"
Cohesion: 0.05
Nodes (5): MainActivity, OnBackPressedCallback, TextWatcher, OnPageChangeCallback, SimpleCallback

### Community 24 - "ReelViewHolder"
Cohesion: 0.11
Nodes (3): ReelsAdapter, ReelViewHolder, SimpleOnGestureListener

### Community 25 - "CommunityFragment"
Cohesion: 0.07
Nodes (11): CommunityFragment, TextWatcher, MoodType, ALL, CHILL, HAPPY, LOVE, MOTIVATED (+3 more)

### Community 27 - "FirestoreManager"
Cohesion: 0.06
Nodes (4): AppNotification, Comment, FirestoreManager, QuoteWidgetProvider

### Community 33 - "VideoStudioActivity"
Cohesion: 0.07
Nodes (4): VideoStudioActivity, OnSeekBarChangeListener, OnSeekBarChangeListener, TextWatcher

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
Cohesion: 0.22
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
- **20 isolated node(s):** `WEEKLY`, `FOLLOWERS`, `ALL`, `ALL`, `CHILL` (+15 more)
  These have ≤1 connection - possible missing edges. (Counts symbols only; 462 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **149 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `UserProfileActivity` connect `UserProfileActivity` to `CommunityAdapter`, `WritersActivity`?**
  _High betweenness centrality (0.061) - this node is a cross-community bridge._
- **What connects `WEEKLY`, `FOLLOWERS`, `ALL` to the rest of the system?**
  _20 weakly-connected nodes found - possible missing edges._
- **Should `VideoStudioActivity` be split into smaller, more focused modules?**
  _Cohesion score 0.05727605727605728 - nodes in this community are weakly interconnected._
- **Why does `VideoStudioActivity` connect `VideoStudioActivity` to `app/src/main/java/com/manchorati/www/CommunityPostDialogHelper.kt`, `app/src/main/java/com/manchorati/www/VideoStudioActivity.kt`?**
  _High betweenness centrality (0.056) - this node is a cross-community bridge._
- **Should `UserProfileActivity` be split into smaller, more focused modules?**
  _Cohesion score 0.05271629778672032 - nodes in this community are weakly interconnected._
- **Why does `VideoStudioActivity` connect `VideoStudioActivity` to `app/src/main/java/com/manchorati/www/VideoStudioActivity.kt`?**
  _High betweenness centrality (0.042) - this node is a cross-community bridge._
- **Should `CommunityAdapter` be split into smaller, more focused modules?**
  _Cohesion score 0.08771929824561403 - nodes in this community are weakly interconnected._