package com.manchorati.www

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import java.util.Calendar

class WritersActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.applyLocale(newBase))
    }

    private lateinit var btnBack: ImageView
    private lateinit var etWriterSearch: EditText
    private lateinit var btnWriterSearchClear: ImageView
    private lateinit var chipWeekly: TextView
    private lateinit var chipFollowers: TextView
    private lateinit var chipAll: TextView
    private lateinit var swipeRefreshWriters: SwipeRefreshLayout
    private lateinit var cardWriterOfTheWeek: CardView
    private lateinit var tvChallengeCountdown: TextView
    private lateinit var ivWriterOfWeekAvatar: ImageView
    private lateinit var tvWriterOfWeekName: TextView
    private lateinit var tvWriterOfWeekBadge: TextView
    private lateinit var tvWriterOfWeekUsername: TextView
    private lateinit var tvWriterOfWeekStats: TextView
    private lateinit var btnWriterOfWeekProfile: TextView
    private lateinit var btnWriterOfWeekFollow: TextView
    private lateinit var tvLeaderboardCongrats: TextView
    private lateinit var tvLeaderboardSectionTitle: TextView
    private lateinit var rvWriters: RecyclerView
    private lateinit var layoutEmptyWriters: LinearLayout
    private lateinit var tvEmptyWriters: TextView
    private lateinit var pbWritersLoading: ProgressBar

    private lateinit var writersAdapter: WritersAdapter
    private var allWritersList = mutableListOf<WriterItem>()
    private var currentFilteredList = mutableListOf<WriterItem>()
    private var activeTabMode = WritersTabMode.WEEKLY
    private var currentQuery = ""
    private var activeUserId: String = ""
    private var myFollowingSet = mutableSetOf<String>()
    private var writerOfTheWeek: WriterItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LocaleHelper.applyLayoutDirection(this)
        setContentView(R.layout.activity_writers)

        val rootLayout = findViewById<View>(R.id.layoutWritersRoot)
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(rootLayout) { v, insets ->
            val statusBar = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.statusBars())
            v.setPadding(0, statusBar.top, 0, 0)
            insets
        }

        activeUserId = AuthManager.currentUser?.uid ?: ""

        initViews()
        setupRecyclerView()
        setupListeners()
        loadData()
        observeAuthState()
    }

    override fun onResume() {
        super.onResume()
        if (activeUserId.isNotEmpty()) {
            FirebaseFirestore.getInstance().collection("users").document(activeUserId).get()
                .addOnSuccessListener { myDoc ->
                    if (isFinishing || isDestroyed) return@addOnSuccessListener
                    val rawFollowing = myDoc.get("following")
                    val updatedSet = when (rawFollowing) {
                        is List<*> -> rawFollowing.mapNotNull { it?.toString() }.toMutableSet()
                        else -> mutableSetOf()
                    }
                    if (updatedSet != myFollowingSet) {
                        myFollowingSet = updatedSet
                        allWritersList.forEach { writer ->
                            writer.isFollowing = myFollowingSet.contains(writer.userId)
                        }
                        writersAdapter.notifyDataSetChanged()
                        updateWriterOfWeekFollowUI()
                    }
                }
        }
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        etWriterSearch = findViewById(R.id.etWriterSearch)
        btnWriterSearchClear = findViewById(R.id.btnWriterSearchClear)
        chipWeekly = findViewById(R.id.chipWeekly)
        chipFollowers = findViewById(R.id.chipFollowers)
        chipAll = findViewById(R.id.chipAll)
        swipeRefreshWriters = findViewById(R.id.swipeRefreshWriters)
        cardWriterOfTheWeek = findViewById(R.id.cardWriterOfTheWeek)
        tvChallengeCountdown = findViewById(R.id.tvChallengeCountdown)
        ivWriterOfWeekAvatar = findViewById(R.id.ivWriterOfWeekAvatar)
        tvWriterOfWeekName = findViewById(R.id.tvWriterOfWeekName)
        tvWriterOfWeekBadge = findViewById(R.id.tvWriterOfWeekBadge)
        tvWriterOfWeekUsername = findViewById(R.id.tvWriterOfWeekUsername)
        tvWriterOfWeekStats = findViewById(R.id.tvWriterOfWeekStats)
        btnWriterOfWeekProfile = findViewById(R.id.btnWriterOfWeekProfile)
        btnWriterOfWeekFollow = findViewById(R.id.btnWriterOfWeekFollow)
        tvLeaderboardCongrats = findViewById(R.id.tvLeaderboardCongrats)
        tvLeaderboardSectionTitle = findViewById(R.id.tvLeaderboardSectionTitle)
        rvWriters = findViewById(R.id.rvWriters)
        layoutEmptyWriters = findViewById(R.id.layoutEmptyWriters)
        tvEmptyWriters = findViewById(R.id.tvEmptyWriters)
        pbWritersLoading = findViewById(R.id.pbWritersLoading)

        tvChallengeCountdown.text = getChallengeCountdownText()
    }

    private fun setupRecyclerView() {
        rvWriters.layoutManager = LinearLayoutManager(this)
        writersAdapter = WritersAdapter(
            writers = emptyList(),
            currentUserId = activeUserId,
            tabMode = activeTabMode,
            onUserClicked = { userId, userName ->
                openUserProfile(userId, userName)
            },
            onFollowToggle = { writer, pos ->
                handleFollowToggle(writer, pos)
            }
        )
        rvWriters.adapter = writersAdapter
    }

    private fun setupListeners() {
        btnBack.setOnClickListener { finish() }

        // التبويبات الثلاثة
        chipWeekly.setOnClickListener { switchTab(WritersTabMode.WEEKLY) }
        chipFollowers.setOnClickListener { switchTab(WritersTabMode.FOLLOWERS) }
        chipAll.setOnClickListener { switchTab(WritersTabMode.ALL) }

        // البحث الفوري الحي
        etWriterSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                currentQuery = s?.toString()?.trim() ?: ""
                btnWriterSearchClear.visibility = if (currentQuery.isNotEmpty()) View.VISIBLE else View.GONE
                applyFilterAndSorting()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        btnWriterSearchClear.setOnClickListener {
            etWriterSearch.text.clear()
        }

        // التحديث بالسحب
        swipeRefreshWriters.setOnRefreshListener {
            loadData()
        }

        // بطاقة كاتب الأسبوع
        btnWriterOfWeekProfile.setOnClickListener {
            writerOfTheWeek?.let {
                openUserProfile(it.userId, it.displayName)
            }
        }

        btnWriterOfWeekFollow.setOnClickListener {
            writerOfTheWeek?.let { writer ->
                handleFollowToggle(writer, -1)
            }
        }
    }

    private fun observeAuthState() {
        lifecycleScope.launch {
            AuthManager.authStateFlow.collect { user ->
                activeUserId = user?.uid ?: ""
                writersAdapter.updateCurrentUserId(activeUserId)
                updateWriterOfWeekFollowUI()
            }
        }
    }

    private fun switchTab(newMode: WritersTabMode) {
        if (activeTabMode == newMode) return
        activeTabMode = newMode

        // تحديث مظهر الأزرار
        val activeBg = R.drawable.bg_tool_chip_active
        val idleBg = R.drawable.bg_tool_chip_idle
        val activeText = Color.WHITE
        val idleText = ContextCompat.getColor(this, R.color.text_primary)

        chipWeekly.setBackgroundResource(if (newMode == WritersTabMode.WEEKLY) activeBg else idleBg)
        chipWeekly.setTextColor(if (newMode == WritersTabMode.WEEKLY) activeText else idleText)

        chipFollowers.setBackgroundResource(if (newMode == WritersTabMode.FOLLOWERS) activeBg else idleBg)
        chipFollowers.setTextColor(if (newMode == WritersTabMode.FOLLOWERS) activeText else idleText)

        chipAll.setBackgroundResource(if (newMode == WritersTabMode.ALL) activeBg else idleBg)
        chipAll.setTextColor(if (newMode == WritersTabMode.ALL) activeText else idleText)

        applyFilterAndSorting()
    }

    private fun loadData() {
        if (!swipeRefreshWriters.isRefreshing) {
            pbWritersLoading.visibility = View.VISIBLE
        }
        val db = FirebaseFirestore.getInstance()
        val startOfWeek = getStartOfWeek()

        // 1. استرجاع قائمة من أتابعهم لمعرفة حالة المتابعة
        if (activeUserId.isNotEmpty()) {
            db.collection("users").document(activeUserId).get()
                .addOnSuccessListener { myDoc ->
                    val rawFollowing = myDoc.get("following")
                    myFollowingSet = when (rawFollowing) {
                        is List<*> -> rawFollowing.mapNotNull { it?.toString() }.toMutableSet()
                        else -> mutableSetOf()
                    }
                    fetchPostsAndUsers(db, startOfWeek)
                }
                .addOnFailureListener {
                    fetchPostsAndUsers(db, startOfWeek)
                }
        } else {
            myFollowingSet.clear()
            fetchPostsAndUsers(db, startOfWeek)
        }
    }

    private fun fetchPostsAndUsers(db: FirebaseFirestore, startOfWeek: Long) {
        // استرجاع منشورات المجتمع مرتبة حسب الأحدث لحساب تفاعل الأسبوع بدقة
        db.collection("community_posts")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(500)
            .get()
            .addOnSuccessListener { postsSnapshot ->
                val weeklyLikesMap = mutableMapOf<String, Long>()
                val totalLikesMap = mutableMapOf<String, Long>()
                val postsCountMap = mutableMapOf<String, Int>()
                val authorsFromPosts = mutableMapOf<String, CommunityPost>()

                for (doc in postsSnapshot.documents) {
                    val post = doc.toObject(CommunityPost::class.java) ?: continue
                    post.id = doc.id
                    val authorId = post.authorId
                    if (authorId.isEmpty()) continue

                    val likes = post.likesCount
                    totalLikesMap[authorId] = (totalLikesMap[authorId] ?: 0L) + likes
                    postsCountMap[authorId] = (postsCountMap[authorId] ?: 0) + 1
                    if (!authorsFromPosts.containsKey(authorId)) {
                        authorsFromPosts[authorId] = post
                    }

                    if (post.getTimeMillis() >= startOfWeek) {
                        weeklyLikesMap[authorId] = (weeklyLikesMap[authorId] ?: 0L) + likes
                    }
                }

                // استرجاع وثائق المستخدمين المسجلين في جدول users
                db.collection("users")
                    .limit(200)
                    .get()
                    .addOnSuccessListener { usersSnapshot ->
                        val writersMap = mutableMapOf<String, WriterItem>()

                        for (doc in usersSnapshot.documents) {
                            val uid = doc.id
                            val displayName = doc.getString("displayName") ?: doc.getString("name") ?: ""
                            val username = doc.getString("username") ?: ""
                            val photoUrl = doc.getString("photoUrl") ?: ""
                            val bio = doc.getString("bio") ?: ""
                            val badge = doc.getString("badge") ?: ""
                            val rawFollowers = doc.get("followers")
                            val followersCount = when (rawFollowers) {
                                is List<*> -> rawFollowers.size.toLong()
                                is Number -> rawFollowers.toLong()
                                else -> 0L
                            }
                            val rawTotalLikes = doc.get("totalLikes")
                            val docTotalLikes = when (rawTotalLikes) {
                                is Number -> rawTotalLikes.toLong()
                                else -> 0L
                            }
                            val rawPostsCount = doc.get("postsCount")
                            val docPostsCount = when (rawPostsCount) {
                                is Number -> rawPostsCount.toInt()
                                else -> 0
                            }

                            val finalTotalLikes = maxOf(docTotalLikes, totalLikesMap[uid] ?: 0L)
                            val finalPostsCount = maxOf(docPostsCount, postsCountMap[uid] ?: 0)

                            writersMap[uid] = WriterItem(
                                userId = uid,
                                displayName = displayName.ifEmpty { getString(R.string.anonymous_author) },
                                username = username,
                                photoUrl = photoUrl,
                                bio = bio,
                                badge = badge,
                                followersCount = followersCount,
                                weeklyLikes = weeklyLikesMap[uid] ?: 0L,
                                totalLikes = finalTotalLikes,
                                postsCount = finalPostsCount,
                                isFollowing = myFollowingSet.contains(uid)
                            )
                        }

                        // إضافة أي كتّاب نشروا ولكن ليس لديهم وثيقة منفصلة بعد
                        for ((authorId, post) in authorsFromPosts) {
                            if (authorId == "manchorati_curated") continue
                            if (!writersMap.containsKey(authorId)) {
                                writersMap[authorId] = WriterItem(
                                    userId = authorId,
                                    displayName = post.authorName.ifEmpty { getString(R.string.anonymous_author) },
                                    username = post.authorUsername,
                                    photoUrl = post.authorPhotoUrl,
                                    bio = "",
                                    badge = post.authorBadge,
                                    followersCount = 0L,
                                    weeklyLikes = weeklyLikesMap[authorId] ?: 0L,
                                    totalLikes = totalLikesMap[authorId] ?: 0L,
                                    postsCount = postsCountMap[authorId] ?: 0,
                                    isFollowing = myFollowingSet.contains(authorId)
                                )
                            }
                        }

                        allWritersList = writersMap.values.toMutableList()
                        finishDataLoading()
                    }
                    .addOnFailureListener {
                        finishDataLoading()
                    }
            }
            .addOnFailureListener {
                finishDataLoading()
            }
    }

    private fun finishDataLoading() {
        pbWritersLoading.visibility = View.GONE
        swipeRefreshWriters.isRefreshing = false
        applyFilterAndSorting()
    }

    private fun applyFilterAndSorting() {
        val cleanQuery = currentQuery.removePrefix("@").lowercase()

        // 1. فرز القائمة بحسب التبويب النشط
        val sortedList = when (activeTabMode) {
            WritersTabMode.WEEKLY -> {
                allWritersList.sortedWith(
                    compareByDescending<WriterItem> { it.weeklyLikes }
                        .thenByDescending { it.totalLikes }
                        .thenByDescending { it.followersCount }
                )
            }
            WritersTabMode.FOLLOWERS -> {
                allWritersList.sortedWith(
                    compareByDescending<WriterItem> { it.followersCount }
                        .thenByDescending { it.totalLikes }
                        .thenByDescending { it.weeklyLikes }
                )
            }
            WritersTabMode.ALL -> {
                allWritersList.sortedWith(
                    compareByDescending<WriterItem> { it.postsCount }
                        .thenByDescending { it.followersCount }
                )
            }
        }

        // تحديد كاتب الأسبوع (دائماً متصدر قائمة الأسبوع بغض النظر عن التبويب الحالي)
        val weeklyChampion = allWritersList
            .filter { it.userId != "manchorati_curated" }
            .maxWithOrNull(
                compareBy<WriterItem> { it.weeklyLikes }
                    .thenBy { it.totalLikes }
                    .thenBy { it.followersCount }
            )
        writerOfTheWeek = weeklyChampion
        updateWriterOfTheWeekCard()

        // 2. إسناد الرتب للمستخدمين (1, 2, 3...)
        sortedList.forEachIndexed { index, writerItem ->
            writerItem.rank = index + 1
        }

        // 3. تطبيق فلتر البحث بالاسم أو @username أو النبذة التعريفية bio
        currentFilteredList = if (cleanQuery.isEmpty()) {
            sortedList.toMutableList()
        } else {
            sortedList.filter {
                it.username.lowercase().contains(cleanQuery) ||
                it.displayName.lowercase().contains(cleanQuery) ||
                it.bio.lowercase().contains(cleanQuery)
            }.toMutableList()
        }

        // في وضع الأسبوع وبدون بحث، نخفي المركز الأول من القائمة لأنه معروض بالفعل في بطاقة كاتب الأسبوع الكبيرة
        val displayList = if (activeTabMode == WritersTabMode.WEEKLY && cleanQuery.isEmpty() && writerOfTheWeek != null) {
            currentFilteredList.filter { it.userId != writerOfTheWeek?.userId }
        } else {
            currentFilteredList
        }

        writersAdapter.updateData(displayList, activeTabMode)

        // إظهار أو إخفاء الحالة الفارغة
        val isChampionCardVisible = (activeTabMode == WritersTabMode.WEEKLY && cleanQuery.isEmpty() && writerOfTheWeek != null)
        if (displayList.isEmpty()) {
            if (isChampionCardVisible) {
                // الكاتب المتصدر وحده موجود ومعروض بالفعل في البطاقة الذهبية العلوية
                layoutEmptyWriters.visibility = View.GONE
                tvLeaderboardSectionTitle.visibility = View.GONE
            } else {
                layoutEmptyWriters.visibility = View.VISIBLE
                tvEmptyWriters.text = if (cleanQuery.isNotEmpty()) getString(R.string.no_writers_found) else getString(R.string.no_weekly_posts_yet)
                tvLeaderboardSectionTitle.visibility = View.GONE
            }
        } else {
            layoutEmptyWriters.visibility = View.GONE
            tvLeaderboardSectionTitle.visibility = View.VISIBLE
        }
    }

    private fun updateWriterOfTheWeekCard() {
        val champion = writerOfTheWeek
        // تظهر البطاقة فقط في وضع كُتّاب الأسبوع وطالما لا يوجد بحث جاري يخفيها
        if (activeTabMode == WritersTabMode.WEEKLY && currentQuery.isEmpty() && champion != null) {
            cardWriterOfTheWeek.visibility = View.VISIBLE
            tvLeaderboardCongrats.visibility = View.VISIBLE

            ivWriterOfWeekAvatar.loadUserAvatar(champion.photoUrl)
            tvWriterOfWeekName.text = champion.displayName
            val cleanHandle = champion.username.removePrefix("@").trim()
            tvWriterOfWeekUsername.text = if (cleanHandle.isNotEmpty()) "@$cleanHandle" else getString(R.string.user_default_handle)

            // شارة الرتبة (بدون تكرار الإيموجي)
            val badge = BadgeHelper.getBadgeForProfile(
                explicitBadge = champion.badge,
                email = null,
                totalLikes = champion.totalLikes,
                followersCount = champion.followersCount
            )
            if (champion.userId != "manchorati_curated" &&
                badge.id != "official" &&
                badge.id != BadgeHelper.BADGE_BEGINNER.id) {
                tvWriterOfWeekBadge.visibility = View.VISIBLE
                tvWriterOfWeekBadge.text = badge.getLocalizedTitle(this)
            } else {
                tvWriterOfWeekBadge.visibility = View.GONE
            }

            // النص التحفيزي
            if (champion.weeklyLikes > 0L) {
                tvLeaderboardCongrats.text = getString(R.string.weekly_champion_congrats)
            } else {
                tvLeaderboardCongrats.text = getString(R.string.no_weekly_posts_yet)
            }

            updateWriterOfWeekFollowUI()
        } else {
            cardWriterOfTheWeek.visibility = View.GONE
            tvLeaderboardCongrats.visibility = View.GONE
        }
    }

    private fun updateWriterOfWeekFollowUI() {
        val champion = writerOfTheWeek ?: return
        // تحديث إحصائيات كاتب الأسبوع (الإعجابات + المتابعون المتزامنون)
        val likesFormatted = champion.weeklyLikes
        val followersFormatted = champion.followersCount.formatCount()
        tvWriterOfWeekStats.text = "❤️ " + getString(R.string.writer_weekly_likes, likesFormatted) + " • 👥 " + getString(R.string.writer_followers_count, followersFormatted)

        if (activeUserId.isNotEmpty() && champion.userId == activeUserId) {
            btnWriterOfWeekFollow.visibility = View.GONE
        } else {
            btnWriterOfWeekFollow.visibility = View.VISIBLE
            if (champion.isFollowing) {
                btnWriterOfWeekFollow.text = getString(R.string.following)
                btnWriterOfWeekFollow.setBackgroundResource(R.drawable.bg_tool_chip_idle)
                btnWriterOfWeekFollow.setTextColor(ContextCompat.getColor(this, R.color.text_primary))
            } else {
                btnWriterOfWeekFollow.text = getString(R.string.follow)
                btnWriterOfWeekFollow.setBackgroundResource(R.drawable.bg_gold_crown_badge)
                btnWriterOfWeekFollow.setTextColor(Color.WHITE)
            }
        }
    }

    private fun handleFollowToggle(writer: WriterItem, position: Int) {
        if (activeUserId.isEmpty()) {
            Toast.makeText(this, getString(R.string.login_required_follow), Toast.LENGTH_SHORT).show()
            return
        }

        val wasFollowing = writer.isFollowing
        val willFollow = !wasFollowing
        writer.isFollowing = willFollow

        if (willFollow) {
            myFollowingSet.add(writer.userId)
            writer.followersCount += 1
        } else {
            myFollowingSet.remove(writer.userId)
            if (writer.followersCount > 0) writer.followersCount -= 1
        }

        // تحديث العنصر في القائمة
        if (position >= 0) {
            writersAdapter.notifyItemChanged(position)
        }
        // إن كان هو نفسه كاتب الأسبوع
        if (writer.userId == writerOfTheWeek?.userId) {
            updateWriterOfWeekFollowUI()
        }

        FirestoreManager.toggleFollow(activeUserId, writer.userId, wasFollowing) { success ->
            if (!success) {
                // تراجع عند الفشل
                writer.isFollowing = wasFollowing
                if (wasFollowing) {
                    myFollowingSet.add(writer.userId)
                    writer.followersCount += 1
                } else {
                    myFollowingSet.remove(writer.userId)
                    if (writer.followersCount > 0) writer.followersCount -= 1
                }
                if (position >= 0) {
                    writersAdapter.notifyItemChanged(position)
                }
                if (writer.userId == writerOfTheWeek?.userId) {
                    updateWriterOfWeekFollowUI()
                }
                Toast.makeText(this, getString(R.string.save_error), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openUserProfile(userId: String, userName: String) {
        val intent = Intent(this, UserProfileActivity::class.java).apply {
            putExtra("USER_ID", userId)
            putExtra("USER_NAME", userName)
        }
        startActivity(intent)
    }

    private fun getStartOfWeek(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // 1=Sunday, 2=Monday, ..., 7=Saturday
        val daysSinceMonday = if (dayOfWeek == Calendar.SUNDAY) 6 else (dayOfWeek - Calendar.MONDAY)
        cal.add(Calendar.DAY_OF_YEAR, -daysSinceMonday)
        return cal.timeInMillis
    }

    private fun getChallengeCountdownText(): String {
        val cal = Calendar.getInstance()
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // 1=Sunday, 2=Monday, ..., 7=Saturday
        val daysRemaining = if (dayOfWeek == Calendar.SUNDAY) 0 else (8 - dayOfWeek)
        return when (daysRemaining) {
            0 -> getString(R.string.challenge_ends_today)
            1 -> getString(R.string.challenge_ends_tomorrow)
            else -> getString(R.string.challenge_ends_in_days, daysRemaining)
        }
    }
}
