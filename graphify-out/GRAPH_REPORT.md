# Graph Report - manchorati-main  (2026-11-08)

## Corpus Check
- cluster-only mode — file stats not available

## Summary
- 1252 nodes · 2732 edges · 63 communities (15 shown, 48 thin omitted)
- Extraction: 99% EXTRACTED · 1% INFERRED · 0% AMBIGUOUS · INFERRED: 33 edges (avg confidence: 0.85)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `75b02344`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- Community 0
- Community 1
- Community 2
- Community 3
- Community 4
- Community 5
- Community 6
- Community 7
- Community 8
- Community 9
- Community 10
- Community 11
- Community 12
- Community 13
- Community 14
- Community 15
- Community 16
- Community 17
- Community 19
- Community 21
- Community 22
- Community 23
- Community 24
- Community 25
- Community 26
- Community 27
- Community 28
- Community 29
- Community 30
- Community 32
- Community 33
- Community 34
- Community 35
- Community 36
- Community 37
- Community 39
- Community 42
- Community 44
- Community 45
- Community 47
- Community 48
- Community 49
- Community 50
- Community 52
- Community 53
- Community 55
- Community 56
- Community 57

## God Nodes (most connected - your core abstractions)
1. `MainActivity` - 51 edges
2. `VideoStudioActivity` - 50 edges
3. `CommunityFragment` - 49 edges
4. `DatabaseHelper` - 40 edges
5. `CommunityPost` - 38 edges
6. `UserProfileActivity` - 36 edges
7. `VideoMotionView` - 30 edges
8. `WritersActivity` - 30 edges
9. `FirestoreManager` - 27 edges
10. `CommunityAdapter` - 22 edges

## Surprising Connections (you probably didn't know these)
- `PostAdapter` --calls--> `DatabaseHelper`  [INFERRED]
  app/src/main/java/com/manchorati/www/PostAdapter.kt → app/src/main/java/com/manchorati/www/DatabaseHelper.kt
- `VideoStudioActivity` --references--> `VideoMotionView`  [EXTRACTED]
  app/src/main/java/com/manchorati/www/VideoStudioActivity.kt → app/src/main/java/com/manchorati/www/VideoMotionView.kt
- `UserProfileActivity` --references--> `CommunityAdapter`  [EXTRACTED]
  app/src/main/java/com/manchorati/www/UserProfileActivity.kt → app/src/main/java/com/manchorati/www/CommunityAdapter.kt
- `MainActivity` --references--> `AppNotification`  [EXTRACTED]
  app/src/main/java/com/manchorati/www/MainActivity.kt → app/src/main/java/com/manchorati/www/AppNotification.kt
- `ReelPlayerActivity` --references--> `VideoMotionView`  [EXTRACTED]
  app/src/main/java/com/manchorati/www/ReelPlayerActivity.kt → app/src/main/java/com/manchorati/www/VideoMotionView.kt

## Import Cycles
- None detected.

## Communities (63 total, 48 thin omitted)

### Community 0 - "Community 0"
Cohesion: 0.05
Nodes (12): OnlineAudio, OnlineBackground, StudioAssetManager, Callback, Callback, VideoStudioActivity, OnSeekBarChangeListener, OnSeekBarChangeListener (+4 more)

### Community 1 - "Community 1"
Cohesion: 0.05
Nodes (8): BadgeHelper, MilestoneBadge, UserBadge, UserProfileActivity, TextWatcher, CountDownTimer, TextWatcher, TextWatcher

### Community 2 - "Community 2"
Cohesion: 0.06
Nodes (8): BookmarkManager, CommunityAdapter, PostDiffCallback, PostViewHolder, CommunityPost, StoryShareHelper, CardTemplate, ThemeHelper

### Community 3 - "Community 3"
Cohesion: 0.06
Nodes (9): WriterItem, WritersActivity, TextWatcher, WritersAdapter, WritersTabMode, ALL, FOLLOWERS, WEEKLY (+1 more)

### Community 4 - "Community 4"
Cohesion: 0.06
Nodes (4): ContactActivity, LocaleHelper, NotificationSettingsActivity, PrivacyPolicyHelper

### Community 5 - "Community 5"
Cohesion: 0.09
Nodes (28): add_quote(), delete_community_post(), get_categories(), get_community_posts(), get_sqlite_conn(), get_stats(), get_users(), index() (+20 more)

### Community 6 - "Community 6"
Cohesion: 0.06
Nodes (4): AppFirebaseMessagingService, AutoEngagementWorker, NotificationSyncWorker, SyncManager

### Community 7 - "Community 7"
Cohesion: 0.08
Nodes (4): CategoriesFragment, DailyNotificationHelper, DailyNotificationWorker, NotificationReceiver

### Community 8 - "Community 8"
Cohesion: 0.09
Nodes (4): CopyActivity, PostAdapter, PostViewHolder, QuoteWidgetProvider

### Community 9 - "Community 9"
Cohesion: 0.09
Nodes (5): Category, CategoryAdapter, CategoryViewHolder, DatabaseHelper, Post

### Community 10 - "Community 10"
Cohesion: 0.09
Nodes (6): AppNotification, NotificationsAdapter, NotificationViewHolder, UserFollowAdapter, UserFollowItem, UserViewHolder

### Community 12 - "Community 12"
Cohesion: 0.10
Nodes (3): CategoryPostsActivity, FavoritesFragment, MainPagerAdapter

### Community 16 - "Community 16"
Cohesion: 0.09
Nodes (10): MoodType, ALL, CHILL, HAPPY, LOVE, MOTIVATED, PEACE, SAD (+2 more)

### Community 19 - "Community 19"
Cohesion: 0.15
Nodes (4): FontItem, ImageUtils, OnAmbilWarnaListener, OnAmbilWarnaListener

### Community 22 - "Community 22"
Cohesion: 0.18
Nodes (3): Comment, CommentsAdapter, CommentViewHolder

## Knowledge Gaps
- **10 isolated node(s):** `ALL`, `CHILL`, `HAPPY`, `LOVE`, `MOTIVATED` (+5 more)
  These have ≤1 connection - possible missing edges. (Counts symbols only; 217 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **48 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `VideoStudioActivity` connect `Community 0` to `Community 4`, `Community 38`, `Community 11`, `Community 20`, `Community 26`?**
  _High betweenness centrality (0.103) - this node is a cross-community bridge._
- **Are the 11 inferred relationships involving `DatabaseHelper` (e.g. with `.applyFilterAndSort()` and `.loadMorePosts()`) actually correct?**
  _`DatabaseHelper` has 11 INFERRED edges - model-reasoned connections that need verification._
- **What connects `ALL`, `CHILL`, `HAPPY` to the rest of the system?**
  _10 weakly-connected nodes found - possible missing edges._
- **Should `Community 0` be split into smaller, more focused modules?**
  _Cohesion score 0.05030643513789581 - nodes in this community are weakly interconnected._
- **Why does `MainActivity` connect `Community 21` to `Community 34`, `Community 35`, `Community 6`, `Community 40`, `Community 9`, `Community 10`, `Community 45`, `Community 60`, `Community 46`, `Community 19`, `Community 58`, `Community 28`, `Community 30`?**
  _High betweenness centrality (0.095) - this node is a cross-community bridge._
- **Should `Community 1` be split into smaller, more focused modules?**
  _Cohesion score 0.052313883299798795 - nodes in this community are weakly interconnected._
- **Why does `UserProfileActivity` connect `Community 1` to `Community 2`, `Community 27`, `Community 29`?**
  _High betweenness centrality (0.081) - this node is a cross-community bridge._