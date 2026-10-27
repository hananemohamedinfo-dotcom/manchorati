package com.manchorati.www

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import coil.load
import coil.transform.CircleCropTransformation
import kotlinx.coroutines.launch
import java.util.Locale

class UserProfileActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(LocaleHelper.applyLocale(newBase))
    }

    private lateinit var rvPosts: RecyclerView
    private lateinit var adapter: CommunityAdapter
    private lateinit var tvName: TextView
    private lateinit var tvProfileBadge: TextView
    private lateinit var tvProfileUsername: TextView
    private lateinit var btnProfileMenu: ImageView
    private lateinit var tvBio: TextView
    private lateinit var tvPostsCount: TextView
    private lateinit var tvFollowersCount: TextView
    private lateinit var tvFollowingCount: TextView
    private lateinit var layoutFollowersContainer: LinearLayout
    private lateinit var layoutFollowingContainer: LinearLayout
    private lateinit var btnBack: ImageView
    private lateinit var btnProfileAction: TextView
    private lateinit var ivProfileAvatar: ImageView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var layoutEmpty: View

    private var targetUserId: String = ""
    private var targetUserName: String = ""
    private var activeUserId: String = ""
    private var currentUsername: String = ""
    private var isFollowing = false
    private var currentFollowersCount: Long = 0L
    private var currentBadge: UserBadge = BadgeHelper.BADGE_BEGINNER
    private var fabAddPostProfile: View? = null
    private var postsListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LocaleHelper.applyLayoutDirection(this)
        setContentView(R.layout.activity_user_profile)

        val passedId = intent.getStringExtra("USER_ID") ?: ""
        targetUserId = passedId
        targetUserName = intent.getStringExtra("USER_NAME") ?: "فاعل خير"
        activeUserId = AuthManager.currentUser?.uid ?: ""

        initViews()
        setupRecyclerView()
        observeAuthState()
    }

    private fun observeAuthState() {
        lifecycleScope.launch {
            AuthManager.authStateFlow.collect { user ->
                if (user != null) {
                    activeUserId = user.uid
                    val isMyProfile = targetUserId.isEmpty() || targetUserId == user.uid
                    if (isMyProfile) {
                        targetUserId = user.uid
                        val prefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                        val localName = prefs.getString("user_name", user.displayName ?: "مستخدم") ?: (user.displayName ?: "مستخدم")
                        val localPhoto = prefs.getString("user_photo", user.photoUrl?.toString() ?: "") ?: (user.photoUrl?.toString() ?: "")
                        targetUserName = localName
                        tvName.text = targetUserName

                        if (currentUsername.isEmpty()) {
                            currentUsername = AuthManager.generateBaseUsername(user.displayName, user.email)
                            tvProfileUsername.visibility = View.VISIBLE
                            tvProfileUsername.text = "@$currentUsername"
                        }
                        if (tvBio.text.isNullOrEmpty() || tvBio.text == "فاعل خير") {
                            tvBio.text = getString(R.string.no_bio_yet)
                        }
                        ivProfileAvatar.loadUserAvatar(localPhoto)
                    }

                    adapter.updateCurrentUserId(activeUserId)
                    updateProfileUI()

                    // Execute Firestore queries AFTER currentUser is confirmed not null
                    loadProfileData()
                    listenToPosts()
                } else {
                    activeUserId = ""
                    adapter.updateCurrentUserId("")
                    if (targetUserId.isEmpty()) {
                        Toast.makeText(this@UserProfileActivity, getString(R.string.login_required_profile), Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        updateProfileUI()
                    }
                }
            }
        }
    }

    private fun setupRecyclerView() {
        rvPosts.layoutManager = LinearLayoutManager(this)
        adapter = CommunityAdapter(
            posts = emptyList(),
            currentUserId = activeUserId,
            onLikeClicked = { post, isLiked ->
                val uid = if (activeUserId.isNotEmpty()) activeUserId else (AuthManager.currentUser?.uid ?: "")
                if (uid.isNotEmpty()) {
                    FirestoreManager.toggleLike(post.id, uid, isLiked) {}
                } else {
                    Toast.makeText(this, getString(R.string.login_required_like), Toast.LENGTH_SHORT).show()
                }
            },
            onCommentClicked = { post -> showCommentsDialog(post) },
            onAuthorClicked = { _, _ -> },
            onEditClicked = { post -> showEditPostDialog(post) },
            onDeleteClicked = { post ->
                AlertDialog.Builder(this)
                    .setTitle(getString(R.string.delete_post_title))
                    .setMessage(getString(R.string.delete_post_confirm))
                    .setPositiveButton(getString(R.string.delete_button)) { _, _ ->
                        FirestoreManager.deleteCommunityPost(post.id) { success ->
                            if (success) {
                                Toast.makeText(this, getString(R.string.post_deleted_success), Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
            },
            onReportClicked = { post ->
                FirestoreManager.reportPost(post.id, "Profile report") {
                    Toast.makeText(this, getString(R.string.report_received_success), Toast.LENGTH_SHORT).show()
                }
            }
        )
        rvPosts.adapter = adapter
    }

    private fun showEditPostDialog(post: CommunityPost) {
        val user = AuthManager.currentUser ?: AuthManager.auth.currentUser ?: return
        CommunityPostDialogHelper.show(
            context = this,
            authorId = user.uid,
            authorName = post.authorName,
            authorUsername = post.authorUsername,
            editingPost = post
        )
    }

    private fun initViews() {
        tvName = findViewById(R.id.tvProfileName)
        tvProfileBadge = findViewById(R.id.tvProfileBadge)
        tvProfileUsername = findViewById(R.id.tvProfileUsername)
        btnProfileMenu = findViewById(R.id.btnProfileMenu)
        tvBio = findViewById(R.id.tvProfileBio)
        tvPostsCount = findViewById(R.id.tvPostsCount)
        tvFollowersCount = findViewById(R.id.tvFollowersCount)
        tvFollowingCount = findViewById(R.id.tvFollowingCount)
        layoutFollowersContainer = findViewById(R.id.layoutFollowersContainer)
        layoutFollowingContainer = findViewById(R.id.layoutFollowingContainer)
        btnProfileAction = findViewById(R.id.btnProfileAction)
        btnBack = findViewById(R.id.btnBack)
        rvPosts = findViewById(R.id.rvUserPosts)
        ivProfileAvatar = findViewById(R.id.ivProfileAvatar)
        fabAddPostProfile = findViewById(R.id.fabAddPostProfile)
        swipeRefresh = findViewById<SwipeRefreshLayout>(R.id.swipeRefreshUserPosts)
        layoutEmpty = findViewById<View>(R.id.layoutEmptyUserPosts)

        layoutFollowersContainer.setOnClickListener {
            showFollowersOrFollowingDialog(isFollowers = true)
        }
        layoutFollowingContainer.setOnClickListener {
            showFollowersOrFollowingDialog(isFollowers = false)
        }
        tvProfileBadge.setOnClickListener {
            showWriterChallengesBottomSheet(currentBadge, currentFollowersCount)
        }

        swipeRefresh.setColorSchemeColors(Color.parseColor("#0284C7"))
        swipeRefresh.setOnRefreshListener {
            loadProfileData()
            listenToPosts()
        }

        fabAddPostProfile?.setOnClickListener {
            openAddPostDialog()
        }

        tvName.text = targetUserName
        btnBack.setOnClickListener { finish() }

        updateProfileUI()
    }

    private fun updateProfileUI() {
        val isMyProfile = (targetUserId.isNotEmpty() && targetUserId == activeUserId) || targetUserId.isEmpty()
        fabAddPostProfile?.visibility = if (isMyProfile && activeUserId.isNotEmpty()) View.VISIBLE else View.GONE

        if (isMyProfile) {
            ivProfileAvatar.setOnClickListener {
                showEditProfileDialog()
            }

            btnProfileAction.visibility = View.GONE
            btnProfileMenu.visibility = View.VISIBLE
            btnProfileMenu.setOnClickListener { showProfileOptionsMenu(it) }
        } else {
            ivProfileAvatar.setOnClickListener(null)
            btnProfileMenu.visibility = View.GONE
            btnProfileAction.visibility = View.VISIBLE
            updateFollowButtonUI()

            btnProfileAction.setOnClickListener {
                if (activeUserId.isEmpty()) {
                    Toast.makeText(this, getString(R.string.login_required_follow), Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                btnProfileAction.isEnabled = false
                FirestoreManager.toggleFollow(activeUserId, targetUserId, isFollowing) { success ->
                    btnProfileAction.isEnabled = true
                    if (success) {
                        isFollowing = !isFollowing
                        updateFollowButtonUI()
                        if (isFollowing) {
                            currentFollowersCount++
                        } else {
                            currentFollowersCount = maxOf(0L, currentFollowersCount - 1)
                        }
                        tvFollowersCount.text = currentFollowersCount.formatCount()
                    }
                }
            }
        }
    }

    private fun loadProfileData() {
        if (targetUserId.isEmpty()) return

        FirestoreManager.getUserProfile(targetUserId) { data ->
            runOnUiThread {
                if (data != null) {
                    val bio = data["bio"] as? String ?: getString(R.string.no_bio_yet)
                    val name = data["displayName"] as? String 
                        ?: data["name"] as? String 
                        ?: targetUserName

                    val usernameFromDoc = data["username"] as? String ?: ""
                    if (usernameFromDoc.isNotEmpty()) {
                        currentUsername = usernameFromDoc
                    }
                    
                    val photoUrl = data["photoUrl"] as? String ?: ""
                    if (photoUrl.isNotEmpty()) {
                        ivProfileAvatar.loadUserAvatar(photoUrl)
                    }

                    val followersRaw = data["followers"]
                    val followersList = when (followersRaw) {
                        is List<*> -> followersRaw.mapNotNull { it?.toString() }
                        else -> emptyList()
                    }

                    currentFollowersCount = (data["followersCount"] as? Number)?.toLong() ?: followersList.size.toLong()
                    val followingRaw = data["following"]
                    val followingCount = (data["followingCount"] as? Number)?.toLong()
                        ?: when (followingRaw) {
                            is List<*> -> followingRaw.size.toLong()
                            else -> 0L
                        }

                    tvName.text = name

                    if (currentUsername.isNotEmpty()) {
                        tvProfileUsername.visibility = View.VISIBLE
                        tvProfileUsername.text = "@$currentUsername"
                    }

                    tvBio.text = bio
                    tvFollowersCount.text = currentFollowersCount.formatCount()
                    tvFollowingCount.text = followingCount.formatCount()
                    swipeRefresh.isRefreshing = false

                    // شارة ورتبة المستخدم والتحديات
                    val explicitBadge = data["badge"] as? String
                    val email = data["email"] as? String ?: (if (targetUserId == AuthManager.currentUser?.uid) AuthManager.currentUser?.email else null)
                    val totalLikes = (data["totalLikes"] as? Number)?.toLong() ?: 0L
                    currentBadge = BadgeHelper.getBadgeForProfile(explicitBadge, email, totalLikes, currentFollowersCount)
                    BadgeHelper.setCachedBadge(targetUserId, currentBadge)
                    tvProfileBadge.visibility = View.VISIBLE
                    tvProfileBadge.text = "${currentBadge.iconEmoji} ${currentBadge.title}"
                    tvProfileBadge.setBackgroundResource(currentBadge.bgResId)
                    tvProfileBadge.setTextColor(Color.parseColor(currentBadge.textColorHex))

                    if (targetUserId != activeUserId) {
                        isFollowing = followersList.contains(activeUserId)
                        updateFollowButtonUI()
                    }
                } else {
                    val currentUser = AuthManager.currentUser
                    if (currentUser != null && targetUserId == currentUser.uid) {
                        if (currentUsername.isEmpty()) {
                            currentUsername = AuthManager.generateBaseUsername(currentUser.displayName, currentUser.email)
                        }
                        tvProfileUsername.visibility = View.VISIBLE
                        tvProfileUsername.text = "@$currentUsername"
                        if (tvBio.text.isNullOrEmpty()) {
                            tvBio.text = getString(R.string.no_bio_yet)
                        }
                        currentBadge = BadgeHelper.getBadgeForProfile(null, currentUser.email, 0L, currentFollowersCount)
                        BadgeHelper.setCachedBadge(targetUserId, currentBadge)
                        tvProfileBadge.visibility = View.VISIBLE
                        tvProfileBadge.text = "${currentBadge.iconEmoji} ${currentBadge.title}"
                        tvProfileBadge.setBackgroundResource(currentBadge.bgResId)
                        tvProfileBadge.setTextColor(Color.parseColor(currentBadge.textColorHex))
                    }
                }
            }
        }
    }

    private fun showWriterChallengesBottomSheet(badge: UserBadge, followersCount: Long) {
        val dialog = BottomSheetDialog(this)
        val dialogView = layoutInflater.inflate(R.layout.dialog_writer_challenges, null)
        dialog.setContentView(dialogView)

        dialog.setOnShowListener {
            val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let { sheet ->
                sheet.setBackgroundResource(android.R.color.transparent)
                val behavior = BottomSheetBehavior.from(sheet)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.skipCollapsed = true
            }
        }

        val btnClose = dialogView.findViewById<ImageView>(R.id.btnCloseChallenges)
        val btnDone = dialogView.findViewById<Button>(R.id.btnDoneChallenges)
        val tvRankPill = dialogView.findViewById<TextView>(R.id.tvCurrentRankPill)
        val tvProgressText = dialogView.findViewById<TextView>(R.id.tvNextTierProgressText)
        val pbProgress = dialogView.findViewById<ProgressBar>(R.id.pbTierProgress)
        val tvRemaining = dialogView.findViewById<TextView>(R.id.tvRemainingFollowersText)
        val layoutRanks = dialogView.findViewById<LinearLayout>(R.id.layoutRanksGuide)
        val layoutMilestones = dialogView.findViewById<LinearLayout>(R.id.layoutMilestonesContainer)

        btnClose.setOnClickListener { dialog.dismiss() }
        btnDone.setOnClickListener { dialog.dismiss() }

        // 1. الرتبة الحالية
        tvRankPill.text = "${badge.iconEmoji} ${badge.title}"
        tvRankPill.setBackgroundResource(badge.bgResId)
        tvRankPill.setTextColor(Color.parseColor(badge.textColorHex))

        // 2. حساب التقدم نحو الرتبة التالية
        val nextTier = BadgeHelper.getNextTier(badge)
        if (nextTier != null) {
            val min = badge.minFollowers
            val max = nextTier.minFollowers
            val currentInTier = (followersCount - min).coerceAtLeast(0L)
            val range = (max - min).coerceAtLeast(1L)
            val pct = ((currentInTier.toFloat() / range.toFloat()) * 100).toInt().coerceIn(0, 100)

            pbProgress.progress = pct
            tvProgressText.text = "${followersCount} / ${nextTier.minFollowers} متابع للترقية إلى ${nextTier.title} ($pct%)"
            val remaining = (nextTier.minFollowers - followersCount).coerceAtLeast(0L)
            tvRemaining.text = "متبقي $remaining متابع للترقية 🚀"
            tvRemaining.visibility = View.VISIBLE
        } else {
            pbProgress.progress = 100
            tvProgressText.text = getString(R.string.max_tier_reached)
            tvRemaining.visibility = View.GONE
        }

        // 3. عرض دليل الرتب مع تمييز الرتبة الحالية
        layoutRanks.removeAllViews()
        for (tier in BadgeHelper.ALL_TIERS) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(24, 16, 24, 16)
                gravity = android.view.Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = 8 }

                if (tier.id == badge.id) {
                    setBackgroundResource(R.drawable.bg_milestone_card)
                } else {
                    setBackgroundColor(Color.parseColor("#080F1D"))
                }
            }

            val tvTierBadge = TextView(this).apply {
                text = "${tier.iconEmoji} ${tier.title}"
                textSize = 12f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setTextColor(Color.parseColor(tier.textColorHex))
                setBackgroundResource(tier.bgResId)
                setPadding(16, 6, 16, 6)
            }

            val layoutDetails = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginStart = 16
                }
            }

            val tvFollowersReq = TextView(this).apply {
                text = if (tier.minFollowers == 0L) "لجميع الأعضاء الجدد" else "${tier.minFollowers}+ متابع"
                textSize = 11f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setTextColor(Color.parseColor("#E2E8F0"))
            }

            val tvDesc = TextView(this).apply {
                text = tier.description
                textSize = 10f
                setTextColor(Color.parseColor("#94A3B8"))
            }

            layoutDetails.addView(tvFollowersReq)
            layoutDetails.addView(tvDesc)

            row.addView(tvTierBadge)
            row.addView(layoutDetails)

            if (tier.id == badge.id) {
                val tvActiveBadge = TextView(this).apply {
                    text = "رتبتك الحالية"
                    textSize = 10f
                    setTextColor(Color.parseColor("#10B981"))
                    setTypeface(null, android.graphics.Typeface.BOLD)
                }
                row.addView(tvActiveBadge)
            }

            layoutRanks.addView(row)
        }

        // 4. عرض أوسمة الإنجازات والتحديات
        layoutMilestones.removeAllViews()
        for (ms in BadgeHelper.ALL_MILESTONES) {
            val isUnlocked = ms.isUnlocked(followersCount)
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(24, 16, 24, 16)
                gravity = android.view.Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = 8 }
                setBackgroundResource(R.drawable.bg_milestone_card)
            }

            val tvEmoji = TextView(this).apply {
                text = ms.emoji
                textSize = 22f
                setPadding(0, 0, 16, 0)
            }

            val layoutText = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val tvTitle = TextView(this).apply {
                text = ms.title
                textSize = 12f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setTextColor(if (isUnlocked) Color.parseColor("#F59E0B") else Color.parseColor("#94A3B8"))
            }

            val tvSub = TextView(this).apply {
                text = ms.description
                textSize = 10f
                setTextColor(Color.parseColor("#64748B"))
            }

            layoutText.addView(tvTitle)
            layoutText.addView(tvSub)

            val tvStatus = TextView(this).apply {
                if (isUnlocked) {
                    text = getString(R.string.badge_unlocked_label)
                    setTextColor(Color.parseColor("#10B981"))
                } else {
                    val rem = (ms.targetFollowers - followersCount).coerceAtLeast(0L)
                    text = getString(R.string.badge_locked_label, rem)
                    setTextColor(Color.parseColor("#64748B"))
                }
                textSize = 11f
                setTypeface(null, android.graphics.Typeface.BOLD)
            }

            card.addView(tvEmoji)
            card.addView(layoutText)
            card.addView(tvStatus)

            layoutMilestones.addView(card)
        }

        dialog.show()
    }

    private fun showFollowersOrFollowingDialog(isFollowers: Boolean) {
        if (targetUserId.isEmpty()) return

        val dialog = BottomSheetDialog(this)
        val dialogView = layoutInflater.inflate(R.layout.dialog_user_list, null)
        dialog.setContentView(dialogView)

        dialog.setOnShowListener {
            val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let { sheet ->
                sheet.setBackgroundResource(android.R.color.transparent)
                val behavior = BottomSheetBehavior.from(sheet)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.skipCollapsed = true
            }
        }

        val tvTitle = dialogView.findViewById<TextView>(R.id.tvUserListTitle)
        val btnClose = dialogView.findViewById<ImageView>(R.id.btnCloseUserList)
        val pbLoading = dialogView.findViewById<ProgressBar>(R.id.pbUserListLoading)
        val layoutEmptyView = dialogView.findViewById<View>(R.id.layoutUserListEmpty)
        val tvEmptyText = dialogView.findViewById<TextView>(R.id.tvUserListEmptyText)
        val rvUsers = dialogView.findViewById<RecyclerView>(R.id.rvUserList)

        tvTitle.text = if (isFollowers) getString(R.string.followers_label) else getString(R.string.following_label)
        tvEmptyText.text = if (isFollowers) getString(R.string.no_users_found) else getString(R.string.no_users_found)

        btnClose.setOnClickListener { dialog.dismiss() }

        rvUsers.layoutManager = LinearLayoutManager(this)
        var userFollowAdapter: UserFollowAdapter? = null

        userFollowAdapter = UserFollowAdapter(
            users = emptyList(),
            currentUserId = activeUserId,
            onUserClicked = { userId, userName ->
                dialog.dismiss()
                if (userId != targetUserId) {
                    val intent = Intent(this, UserProfileActivity::class.java).apply {
                        putExtra("USER_ID", userId)
                        putExtra("USER_NAME", userName)
                    }
                    startActivity(intent)
                }
            },
            onFollowToggle = { userItem, pos ->
                if (activeUserId.isEmpty()) {
                    Toast.makeText(this, getString(R.string.login_required_follow), Toast.LENGTH_SHORT).show()
                    return@UserFollowAdapter
                }
                val currentlyFollowing = userItem.isFollowing
                val willFollow = !currentlyFollowing
                userItem.isFollowing = willFollow
                userFollowAdapter?.notifyItemChanged(pos)

                FirestoreManager.toggleFollow(activeUserId, userItem.userId, currentlyFollowing) { success ->
                    if (!success) {
                        userItem.isFollowing = currentlyFollowing
                        userFollowAdapter?.notifyItemChanged(pos)
                        Toast.makeText(this, getString(R.string.save_error), Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
        rvUsers.adapter = userFollowAdapter

        dialog.show()

        val db = FirebaseFirestore.getInstance()
        db.collection("users").document(targetUserId).get()
            .addOnSuccessListener { doc ->
                val fieldKey = if (isFollowers) "followers" else "following"
                val rawIds = doc.get(fieldKey)
                val userIds = when (rawIds) {
                    is List<*> -> rawIds.mapNotNull { it?.toString() }
                    else -> emptyList()
                }

                if (userIds.isEmpty()) {
                    pbLoading.visibility = View.GONE
                    layoutEmptyView.visibility = View.VISIBLE
                    return@addOnSuccessListener
                }

                tvTitle.text = "${if (isFollowers) getString(R.string.followers_title) else getString(R.string.following_title)} (${userIds.size})"

                val myFollowingSet = mutableSetOf<String>()
                fun loadUsersDetails() {
                    val usersList = mutableListOf<UserFollowItem>()
                    var fetchedCount = 0

                    for (uid in userIds) {
                        db.collection("users").document(uid).get()
                            .addOnSuccessListener { uDoc ->
                                val dName = uDoc.getString("displayName") ?: uDoc.getString("name") ?: "مستخدم"
                                val uName = uDoc.getString("username") ?: ""
                                val pUrl = uDoc.getString("photoUrl") ?: ""
                                val isF = myFollowingSet.contains(uid)
                                usersList.add(
                                    UserFollowItem(
                                        userId = uid,
                                        displayName = dName,
                                        username = uName,
                                        photoUrl = pUrl,
                                        isFollowing = isF
                                    )
                                )
                                fetchedCount++
                                if (fetchedCount == userIds.size) {
                                    pbLoading.visibility = View.GONE
                                    if (usersList.isEmpty()) {
                                        layoutEmptyView.visibility = View.VISIBLE
                                    } else {
                                        userFollowAdapter?.updateData(usersList)
                                    }
                                }
                            }
                            .addOnFailureListener {
                                fetchedCount++
                                if (fetchedCount == userIds.size) {
                                    pbLoading.visibility = View.GONE
                                    if (usersList.isEmpty()) {
                                        layoutEmptyView.visibility = View.VISIBLE
                                    } else {
                                        userFollowAdapter?.updateData(usersList)
                                    }
                                }
                            }
                    }
                }

                if (activeUserId.isNotEmpty()) {
                    db.collection("users").document(activeUserId).get()
                        .addOnSuccessListener { myDoc ->
                            val myF = myDoc.get("following")
                            if (myF is List<*>) {
                                myFollowingSet.addAll(myF.mapNotNull { it?.toString() })
                            }
                            loadUsersDetails()
                        }
                        .addOnFailureListener {
                            loadUsersDetails()
                        }
                } else {
                    loadUsersDetails()
                }
            }
            .addOnFailureListener {
                pbLoading.visibility = View.GONE
                layoutEmptyView.visibility = View.VISIBLE
            }
    }

    private fun showSelectAvatarDialog(onAvatarSelected: ((String) -> Unit)? = null) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_select_avatar, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        val ivGooglePhoto = dialogView.findViewById<ImageView>(R.id.ivGooglePhoto)
        
        val googlePhotoUrl = AuthManager.currentUser?.photoUrl?.toString() ?: ""
        if (googlePhotoUrl.isNotEmpty()) {
            ivGooglePhoto.load(googlePhotoUrl) {
                crossfade(true)
                transformations(CircleCropTransformation())
            }
        } else {
            ivGooglePhoto.setImageResource(R.drawable.ic_profile)
        }

        ivGooglePhoto.setOnClickListener {
            if (onAvatarSelected != null) {
                onAvatarSelected(googlePhotoUrl)
            } else {
                updateUserProfilePhoto(googlePhotoUrl)
            }
            dialog.dismiss()
        }

        val avatars = listOf(
            R.id.ivAvatar1 to "avatar_1",
            R.id.ivAvatar2 to "avatar_2",
            R.id.ivAvatar3 to "avatar_3",
            R.id.ivAvatar4 to "avatar_4",
            R.id.ivAvatar5 to "avatar_5",
            R.id.ivAvatar6 to "avatar_6",
            R.id.ivAvatar7 to "avatar_7",
            R.id.ivAvatar8 to "avatar_8",
            R.id.ivAvatar9 to "avatar_9",
            R.id.ivAvatar10 to "avatar_10",
            R.id.ivAvatar11 to "avatar_11",
            R.id.ivAvatar12 to "avatar_12",
            R.id.ivAvatar13 to "avatar_13",
            R.id.ivAvatar14 to "avatar_14"
        )

        for ((viewId, avatarName) in avatars) {
            dialogView.findViewById<ImageView>(viewId)?.apply {
                val resId = resources.getIdentifier(avatarName, "drawable", packageName)
                if (resId != 0) {
                    setImageResource(resId)
                }
                
                setOnClickListener {
                    if (onAvatarSelected != null) {
                        onAvatarSelected(avatarName)
                    } else {
                        updateUserProfilePhoto(avatarName)
                    }
                    dialog.dismiss()
                }
            }
        }

        dialog.show()
    }

    private fun updateUserProfilePhoto(photoUrl: String) {
        FirestoreManager.updateUserProfilePhoto(this, activeUserId, photoUrl) { success ->
            if (success) {
                Toast.makeText(this, getString(R.string.profile_picture_updated), Toast.LENGTH_SHORT).show()
                updatePhotoInOldPosts(photoUrl)
                loadProfileData()
            } else {
                Toast.makeText(this, getString(R.string.profile_picture_update_failed), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateFollowButtonUI() {
        val isMyProfile = (targetUserId.isNotEmpty() && targetUserId == activeUserId) || targetUserId.isEmpty()
        if (isMyProfile) {
            btnProfileAction.visibility = View.GONE
            return
        }
        btnProfileAction.visibility = View.VISIBLE
        if (isFollowing) {
            btnProfileAction.text = getString(R.string.following)
            btnProfileAction.setBackgroundResource(R.drawable.bg_tool_chip_idle)
            btnProfileAction.setTextColor(Color.parseColor("#10B981"))
        } else {
            btnProfileAction.text = getString(R.string.follow)
            btnProfileAction.setBackgroundResource(R.drawable.bg_tool_chip_active)
            btnProfileAction.setTextColor(Color.WHITE)
        }
    }

    private fun listenToPosts() {
        if (targetUserId.isEmpty()) return

        postsListener?.remove()
        postsListener = FirestoreManager.listenToUserPosts(targetUserId) { posts ->
            runOnUiThread {
                adapter.updateData(posts)
                tvPostsCount.text = posts.size.formatCount()
                layoutEmpty.visibility = if (posts.isEmpty()) View.VISIBLE else View.GONE
                swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun openAddPostDialog() {
        val user = AuthManager.currentUser ?: AuthManager.auth.currentUser
        if (user == null) {
            Toast.makeText(this, getString(R.string.login_required_post), Toast.LENGTH_SHORT).show()
            return
        }

        val authorName = user.displayName ?: (if (targetUserName.isNotEmpty() && targetUserName != "فاعل خير") targetUserName else "مستخدم")
        val authorUsername = if (currentUsername.isNotEmpty()) currentUsername else AuthManager.generateBaseUsername(user.displayName, user.email)

        showAddPostBottomSheet(user.uid, authorName, authorUsername)
    }

    private fun showAddPostBottomSheet(
        authorId: String,
        authorName: String,
        authorUsername: String
    ) {
        CommunityPostDialogHelper.show(
            context = this,
            authorId = authorId,
            authorName = authorName,
            authorUsername = authorUsername,
            editingPost = null
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        postsListener?.remove()
        postsListener = null
    }

    private fun showEditProfileDialog() {
        val dialog = BottomSheetDialog(this)
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_profile, null)
        dialog.setContentView(dialogView)

        dialog.setOnShowListener {
            val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let { sheet ->
                val behavior = BottomSheetBehavior.from(sheet)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.skipCollapsed = true
            }
        }

        val btnClose = dialogView.findViewById<ImageView>(R.id.btnCloseEditProfile)
        val btnChangeAvatar = dialogView.findViewById<View>(R.id.btnChangeAvatar)
        val btnChangeAvatarText = dialogView.findViewById<View>(R.id.btnChangeAvatarText)
        val ivEditAvatarPreview = dialogView.findViewById<ImageView>(R.id.ivEditAvatarPreview)
        val etName = dialogView.findViewById<EditText>(R.id.etEditName)
        val etUsername = dialogView.findViewById<EditText>(R.id.etEditUsername)
        val ivUsernameStatus = dialogView.findViewById<ImageView>(R.id.ivUsernameStatus)
        val pbUsernameChecking = dialogView.findViewById<ProgressBar>(R.id.pbUsernameChecking)
        val tvUsernameStatus = dialogView.findViewById<TextView>(R.id.tvUsernameStatus)
        val etBio = dialogView.findViewById<EditText>(R.id.etEditBio)
        val tvBioCounter = dialogView.findViewById<TextView>(R.id.tvBioCounter)
        val btnSave = dialogView.findViewById<TextView>(R.id.btnSaveProfile)
        val pbSave = dialogView.findViewById<ProgressBar>(R.id.pbSaveProfile)

        btnClose.setOnClickListener { dialog.dismiss() }

        // تحميل صورة المستخدم الحالية
        val prefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        var selectedPhotoUrl = prefs.getString("user_photo", "")?.takeIf { it.isNotEmpty() }
            ?: (AuthManager.currentUser?.photoUrl?.toString() ?: "")
        ivEditAvatarPreview.loadUserAvatar(selectedPhotoUrl)

        val openAvatarPicker = {
            showSelectAvatarDialog { chosenAvatar ->
                selectedPhotoUrl = chosenAvatar
                ivEditAvatarPreview.loadUserAvatar(chosenAvatar)
            }
        }
        btnChangeAvatar.setOnClickListener { openAvatarPicker() }
        btnChangeAvatarText.setOnClickListener { openAvatarPicker() }

        // الاسم والنبذة
        etName.setText(tvName.text.toString())
        val initialBio = if (tvBio.text.toString() == "لا توجد نبذة شخصية بعد." || tvBio.text.toString() == "فاعل خير") "" else tvBio.text.toString()
        etBio.setText(initialBio)
        tvBioCounter.text = "${initialBio.length}/150"

        etBio.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                tvBioCounter.text = "${s?.length ?: 0}/150"
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        // اسم المستخدم الفريد (@)
        val initialUsername = currentUsername.removePrefix("@").trim()
        etUsername.setText(initialUsername)
        var isUsernameValid = true

        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        var checkRunnable: Runnable? = null

        etUsername.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                checkRunnable?.let { handler.removeCallbacks(it) }

                val inputVal = s?.toString()?.trim()?.lowercase() ?: ""

                if (inputVal.isEmpty()) {
                    isUsernameValid = false
                    pbUsernameChecking.visibility = View.GONE
                    ivUsernameStatus.visibility = View.GONE
                    tvUsernameStatus.visibility = View.GONE
                    return
                }

                if (inputVal == initialUsername.lowercase()) {
                    isUsernameValid = true
                    pbUsernameChecking.visibility = View.GONE
                    ivUsernameStatus.visibility = View.VISIBLE
                    ivUsernameStatus.setImageResource(R.drawable.ic_check)
                    ivUsernameStatus.imageTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#0284C7"))
                    tvUsernameStatus.visibility = View.VISIBLE
                    tvUsernameStatus.text = getString(R.string.username_current)
                    tvUsernameStatus.setTextColor(Color.parseColor("#0284C7"))
                    return
                }

                if (inputVal.length < 3) {
                    isUsernameValid = false
                    pbUsernameChecking.visibility = View.GONE
                    ivUsernameStatus.visibility = View.VISIBLE
                    ivUsernameStatus.setImageResource(R.drawable.ic_close_small)
                    ivUsernameStatus.imageTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#EF4444"))
                    tvUsernameStatus.visibility = View.VISIBLE
                    tvUsernameStatus.text = getString(R.string.username_too_short)
                    tvUsernameStatus.setTextColor(Color.parseColor("#EF4444"))
                    return
                }

                val validRegex = "^[a-z0-9_.]+$".toRegex()
                if (!inputVal.matches(validRegex)) {
                    isUsernameValid = false
                    pbUsernameChecking.visibility = View.GONE
                    ivUsernameStatus.visibility = View.VISIBLE
                    ivUsernameStatus.setImageResource(R.drawable.ic_close_small)
                    ivUsernameStatus.imageTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#EF4444"))
                    tvUsernameStatus.visibility = View.VISIBLE
                    tvUsernameStatus.text = getString(R.string.username_invalid_chars)
                    tvUsernameStatus.setTextColor(Color.parseColor("#EF4444"))
                    return
                }

                // جاري الفحص مع تأخير 400ms لتفادي ضغط الشبكة
                pbUsernameChecking.visibility = View.VISIBLE
                ivUsernameStatus.visibility = View.GONE
                tvUsernameStatus.visibility = View.VISIBLE
                tvUsernameStatus.text = getString(R.string.username_checking)
                tvUsernameStatus.setTextColor(Color.parseColor("#64748B"))

                checkRunnable = Runnable {
                    AuthManager.isUsernameAvailable(inputVal) { available ->
                        runOnUiThread {
                            if (etUsername.text.toString().trim().lowercase() != inputVal) return@runOnUiThread
                            pbUsernameChecking.visibility = View.GONE
                            ivUsernameStatus.visibility = View.VISIBLE
                            if (available) {
                                isUsernameValid = true
                                ivUsernameStatus.setImageResource(R.drawable.ic_check)
                                ivUsernameStatus.imageTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#10B981"))
                                tvUsernameStatus.text = getString(R.string.username_available)
                                tvUsernameStatus.setTextColor(Color.parseColor("#10B981"))
                            } else {
                                isUsernameValid = false
                                ivUsernameStatus.setImageResource(R.drawable.ic_close_small)
                                ivUsernameStatus.imageTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#EF4444"))
                                tvUsernameStatus.text = getString(R.string.username_taken)
                                tvUsernameStatus.setTextColor(Color.parseColor("#EF4444"))
                            }
                        }
                    }
                }
                handler.postDelayed(checkRunnable!!, 400)
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        // عند النقر على حفظ التغييرات
        btnSave.setOnClickListener {
            val newName = etName.text.toString().trim().ifEmpty { "فاعل خير" }
            val newBio = etBio.text.toString().trim()
            val newUsername = etUsername.text.toString().trim().lowercase()

            if (!isUsernameValid) {
                Toast.makeText(this, R.string.username_invalid_chars, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnSave.text = ""
            pbSave.visibility = View.VISIBLE
            btnSave.isClickable = false

            val completeProfileSave = {
                FirestoreManager.updateUserProfile(activeUserId, newName, newBio) { success ->
                    if (success) {
                        tvName.text = newName
                        tvBio.text = newBio.ifEmpty { getString(R.string.bio_default) }
                        prefs.edit().putString("user_name", newName).apply()
                        updateNameInCommunityPosts(newName)

                        // تحديث الصورة الشخصية إذا تم اختيار صورة جديدة
                        val oldPhoto = prefs.getString("user_photo", "") ?: ""
                        if (selectedPhotoUrl.isNotEmpty() && selectedPhotoUrl != oldPhoto) {
                            updateUserProfilePhoto(selectedPhotoUrl)
                        } else {
                            ivProfileAvatar.loadUserAvatar(selectedPhotoUrl)
                        }

                        runOnUiThread {
                            btnSave.text = getString(R.string.save_changes)
                            pbSave.visibility = View.GONE
                            btnSave.isClickable = true
                            dialog.dismiss()
                            Toast.makeText(this@UserProfileActivity, R.string.profile_saved_success, Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        runOnUiThread {
                            btnSave.text = getString(R.string.save_changes)
                            pbSave.visibility = View.GONE
                            btnSave.isClickable = true
                            Toast.makeText(this@UserProfileActivity, R.string.save_error, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }

            // إذا تغير اسم المستخدم، نحدثه أولاً عبر المعاملة الذرية في AuthManager
            if (newUsername.isNotEmpty() && newUsername != initialUsername.lowercase()) {
                AuthManager.changeUsername(
                    userId = activeUserId,
                    oldUsername = currentUsername,
                    newUsername = newUsername
                ) { success, errorMsg ->
                    if (success) {
                        currentUsername = newUsername
                        runOnUiThread {
                            tvProfileUsername.visibility = View.VISIBLE
                            tvProfileUsername.text = "@$currentUsername"
                        }
                        completeProfileSave()
                    } else {
                        runOnUiThread {
                            btnSave.text = getString(R.string.save_changes)
                            pbSave.visibility = View.GONE
                            btnSave.isClickable = true
                            Toast.makeText(this@UserProfileActivity, errorMsg ?: getString(R.string.username_change_failed), Toast.LENGTH_LONG).show()
                        }
                    }
                }
            } else {
                completeProfileSave()
            }
        }

        dialog.show()
    }

    // private fun showCommentsDialog(post: CommunityPost) {
    //     val dialog = BottomSheetDialog(this)
    //     val view = layoutInflater.inflate(R.layout.dialog_comments, null)
    //     dialog.setContentView(view)

    //     view.findViewById<TextView?>(R.id.tvCommentPostAuthor)?.text = post.authorName
    //     view.findViewById<TextView?>(R.id.tvCommentPostContent)?.text = post.content

    //     val rvComments = view.findViewById<RecyclerView>(R.id.rvComments)
    //     val etInput = view.findViewById<EditText>(R.id.etCommentInput)
    //     val btnSend = view.findViewById<ImageView>(R.id.btnSendComment)

    //     val user = FirebaseAuth.getInstance().currentUser
    //     val currentUserName = user?.displayName 
    //         ?: getSharedPreferences("user_prefs", Context.MODE_PRIVATE).getString("user_name", "فاعل خير") 
    //         ?: "فاعل خير"

    //     rvComments.layoutManager = LinearLayoutManager(this)
    //     val commentsAdapter = CommentsAdapter(
    //         comments = emptyList(),
    //         currentUserId = activeUserId,
    //         postAuthorId = post.authorId,
    //         onDeleteClicked = { comment ->
    //             FirestoreManager.deleteComment(post.id, comment.id) {}
    //         }
    //     )
    //     rvComments.adapter = commentsAdapter

    //     FirestoreManager.listenToComments(post.id) { list ->
    //         runOnUiThread {
    //             commentsAdapter.updateData(list)
    //             if (list.isNotEmpty()) rvComments.scrollToPosition(list.size - 1)
    //         }
    //     }

    //     btnSend.setOnClickListener {
    //         val text = etInput.text.toString().trim()
    //         if (text.isEmpty()) return@setOnClickListener

    //         if (BadWordsFilter.containsBadWords(text)) {
    //             Toast.makeText(this, "التعليق يحتوي على كلمات غير لائقة", Toast.LENGTH_SHORT).show()
    //             return@setOnClickListener
    //         }

    //         btnSend.isEnabled = false
            
    //         // الحل النهائي: جلب أحدث صورة واسم مباشرة من قاعدة البيانات قبل إرسال التعليق
    //         val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
    //         db.collection("users").document(activeUserId).get()
    //             .addOnSuccessListener { document ->
    //                 // جلب الصورة الحالية (وإذا لم تكن موجودة نعطيه مساراً فارغاً)
    //                 val actualPhotoUrl = document.getString("photoUrl") ?: ""
    //                 // جلب الاسم الحالي لتفادي أي أخطاء في الاسم أيضاً
    //                 val actualName = document.getString("displayName") 
    //                     ?: document.getString("name") 
    //                     ?: currentUserName

    //                 // إضافة التعليق بالبيانات الجديدة
    //                 FirestoreManager.addComment(
    //                     postId = post.id,
    //                     authorId = activeUserId,
    //                     authorName = actualName,
    //                     authorPhotoUrl = actualPhotoUrl,
    //                     content = text
    //                 ) { success ->
    //                     btnSend.isEnabled = true
    //                     if (success) {
    //                         etInput.setText("")
    //                         if (post.authorId != activeUserId) {
    //                             FirestoreManager.sendNotification(
    //                                 recipientId = post.authorId,
    //                                 senderId = activeUserId,
    //                                 senderName = actualName,
    //                                 type = "COMMENT",
    //                                 postId = post.id,
    //                                 message = "علّق $actualName على منشورك: \"${text.take(30)}...\""
    //                             )
    //                         }
    //                     }
    //                 }
    //             }
    //             .addOnFailureListener {
    //                 btnSend.isEnabled = true
    //                 Toast.makeText(this, "حدث خطأ، يرجى المحاولة لاحقاً", Toast.LENGTH_SHORT).show()
    //             }
    //     }

    //     dialog.show()
    // }
    private fun showCommentsDialog(post: CommunityPost) {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.dialog_comments, null)
        dialog.setContentView(view)

        view.findViewById<TextView?>(R.id.tvCommentPostAuthor)?.text = post.authorName
        view.findViewById<TextView?>(R.id.tvCommentPostContent)?.text = post.content

        val rvComments = view.findViewById<RecyclerView>(R.id.rvComments)
        val etInput = view.findViewById<EditText>(R.id.etCommentInput)
        val btnSend = view.findViewById<ImageView>(R.id.btnSendComment)
        val ivCurrentUserAvatar = view.findViewById<ImageView>(R.id.ivCurrentUserCommentAvatar)
        val layoutEmptyComments = view.findViewById<View>(R.id.layoutEmptyComments)
        val tvCommentsCountHeader = view.findViewById<TextView>(R.id.tvCommentsCountHeader)

        // شريط الرد / التعديل
        val layoutReplyBanner = view.findViewById<View>(R.id.layoutReplyBanner)
        val tvReplyBannerText = view.findViewById<TextView>(R.id.tvReplyBannerText)
        val tvActionBannerSubtitle = view.findViewById<TextView>(R.id.tvActionBannerSubtitle)
        val btnCancelReply = view.findViewById<ImageView>(R.id.btnCancelReply)

        var replyingToComment: Comment? = null
        var editingComment: Comment? = null

        val user = AuthManager.currentUser
        val prefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val currentUserName = user?.displayName 
            ?: prefs.getString("user_name", "فاعل خير") 
            ?: "فاعل خير"

        // تحميل صورة المستخدم في شريط التعليق
        val currentPhoto = prefs.getString("user_photo", "")?.takeIf { it.isNotEmpty() }
            ?: user?.photoUrl?.toString() ?: ""
        ivCurrentUserAvatar?.loadUserAvatar(currentPhoto)

        val resetBannerState = {
            replyingToComment = null
            editingComment = null
            layoutReplyBanner.visibility = View.GONE
            tvActionBannerSubtitle?.visibility = View.GONE
            etInput.hint = getString(R.string.comment_hint)
            btnSend.setImageResource(R.drawable.ic_send)
        }

        btnCancelReply.setOnClickListener {
            resetBannerState()
            etInput.setText("")
        }

        val updateSendButtonState = {
            val hasText = etInput.text?.toString()?.trim()?.isNotEmpty() == true
            btnSend.alpha = if (hasText) 1.0f else 0.4f
            btnSend.isClickable = hasText
        }
        updateSendButtonState()

        etInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateSendButtonState()
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        rvComments.layoutManager = LinearLayoutManager(this)
        
        val commentsAdapter = CommentsAdapter(
            comments = emptyList(),
            currentUserId = activeUserId,
            postAuthorId = post.authorId,
            onUserClicked = { clickedUserId, clickedUserName ->
                val intent = android.content.Intent(this, UserProfileActivity::class.java).apply {
                    putExtra("USER_ID", clickedUserId)
                    putExtra("USER_NAME", clickedUserName)
                }
                startActivity(intent)
                dialog.dismiss()
            },
            onReplyClicked = { comment ->
                replyingToComment = comment
                editingComment = null
                layoutReplyBanner.visibility = View.VISIBLE
                tvReplyBannerText.text = getString(R.string.reply_to_user, comment.authorName)
                tvActionBannerSubtitle?.text = comment.content
                tvActionBannerSubtitle?.visibility = View.VISIBLE
                etInput.hint = getString(R.string.reply_to_user, comment.authorName)
                btnSend.setImageResource(R.drawable.ic_send)
                etInput.requestFocus()
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
                imm?.showSoftInput(etInput, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
            },
            onEditClicked = { comment ->
                editingComment = comment
                replyingToComment = null
                layoutReplyBanner.visibility = View.VISIBLE
                tvReplyBannerText.text = getString(R.string.edit_comment_title)
                tvActionBannerSubtitle?.text = comment.content
                tvActionBannerSubtitle?.visibility = View.VISIBLE
                etInput.setText(comment.content)
                etInput.setSelection(comment.content.length)
                etInput.hint = getString(R.string.edit_comment_hint)
                btnSend.setImageResource(R.drawable.ic_check)
                etInput.requestFocus()
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
                imm?.showSoftInput(etInput, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
            },
            onDeleteClicked = { comment ->
                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle(R.string.delete_button)
                    .setMessage(R.string.confirm_delete_comment)
                    .setPositiveButton(R.string.delete_button) { _, _ ->
                        FirestoreManager.deleteComment(post.id, comment.id) {}
                    }
                    .setNegativeButton(R.string.no, null)
                    .show()
            }
        )
        rvComments.adapter = commentsAdapter

        FirestoreManager.listenToComments(post.id) { list ->
            runOnUiThread {
                commentsAdapter.updateData(list)
                if (list.isEmpty()) {
                    layoutEmptyComments?.visibility = View.VISIBLE
                    tvCommentsCountHeader?.text = getString(R.string.comments_count_zero)
                } else {
                    layoutEmptyComments?.visibility = View.GONE
                    tvCommentsCountHeader?.text = getString(R.string.comments_count_format, list.size)
                    rvComments.scrollToPosition(list.size - 1)
                }
            }
        }

        btnSend.setOnClickListener {
            val text = etInput.text.toString().trim()
            if (text.isEmpty()) return@setOnClickListener

            if (BadWordsFilter.containsBadWords(text)) {
                Toast.makeText(this, getString(R.string.comment_inappropriate_words), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnSend.isEnabled = false

            val currentUser = AuthManager.currentUser
            if (currentUser == null) {
                btnSend.isEnabled = true
                Toast.makeText(this@UserProfileActivity, getString(R.string.login_required_comment), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // تعديل التعليق
            val targetEditing = editingComment
            if (targetEditing != null) {
                FirestoreManager.updateComment(post.id, targetEditing.id, text) { success ->
                    btnSend.isEnabled = true
                    if (success) {
                        etInput.setText("")
                        resetBannerState()
                        Toast.makeText(this@UserProfileActivity, getString(R.string.comment_edited_success), Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@UserProfileActivity, getString(R.string.comment_edit_error), Toast.LENGTH_SHORT).show()
                    }
                }
                return@setOnClickListener
            }

            val currentActiveId = currentUser.uid
            val currentAuthorName = currentUser.displayName ?: currentUserName
            val currentPhotoUrl = prefs.getString("user_photo", "")?.takeIf { it.isNotEmpty() }
                ?: currentUser.photoUrl?.toString() ?: ""

            val replyAuthorName = replyingToComment?.authorName ?: ""
            val replyCommentId = replyingToComment?.id ?: ""
            val targetRepliedComment = replyingToComment

            FirestoreManager.addComment(
                postId = post.id,
                authorId = currentActiveId,
                authorName = currentAuthorName,
                authorPhotoUrl = currentPhotoUrl,
                content = text,
                replyToAuthorName = replyAuthorName,
                replyToCommentId = replyCommentId
            ) { success ->
                btnSend.isEnabled = true
                if (success) {
                    etInput.setText("")
                    resetBannerState()

                    // إشعار صاحب التعليق المردود عليه
                    if (targetRepliedComment != null && targetRepliedComment.authorId != currentActiveId) {
                        FirestoreManager.sendNotification(
                            recipientId = targetRepliedComment.authorId,
                            senderId = currentActiveId,
                            senderName = currentAuthorName,
                            type = "COMMENT",
                            postId = post.id,
                            message = "رد $currentAuthorName على تعليقك: \"${text.take(30)}...\""
                        )
                    }

                    if (post.authorId != currentActiveId && post.authorId != targetRepliedComment?.authorId) {
                        FirestoreManager.sendNotification(
                            recipientId = post.authorId,
                            senderId = currentActiveId,
                            senderName = currentAuthorName,
                            type = "COMMENT",
                            postId = post.id,
                            message = "علّق $currentAuthorName على منشورك: \"${text.take(30)}...\""
                        )
                    }
                } else {
                    Toast.makeText(this@UserProfileActivity, getString(R.string.comment_error), Toast.LENGTH_SHORT).show()
                }
            }
        }

        dialog.show()
    }
    private fun updateNameInCommunityPosts(newName: String) {
        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        if (activeUserId.isEmpty()) return

        db.collection("community_posts")
            .whereEqualTo("authorId", activeUserId)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val batch = db.batch()
                for (document in querySnapshot.documents) {
                    batch.update(document.reference, "authorName", newName)
                }
                batch.commit()
            }
    }

    private fun updatePhotoInOldPosts(newPhotoUrl: String) {
        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        if (activeUserId.isEmpty()) return

        // 1. تحديث الصورة في جميع المنشورات القديمة
        db.collection("community_posts")
            .whereEqualTo("authorId", activeUserId)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val batch = db.batch()
                for (document in querySnapshot.documents) {
                    batch.update(document.reference, "authorPhotoUrl", newPhotoUrl)
                }
                batch.commit()
            }

        // 2. تحديث الصورة في التعليقات باستخدام collectionGroup
        db.collectionGroup("comments")
            .whereEqualTo("authorId", activeUserId)
            .get()
            .addOnSuccessListener { querySnapshot ->
                
                // رسالة تأكيد لمعرفة عدد التعليقات التي تم العثور عليها
                // Toast.makeText(this, "تم العثور على ${querySnapshot.size()} تعليق قديم لتحديث الصورة", Toast.LENGTH_LONG).show()

                if (querySnapshot.isEmpty) return@addOnSuccessListener

                val batch = db.batch()
                for (document in querySnapshot.documents) {
                    batch.update(document.reference, "authorPhotoUrl", newPhotoUrl)
                }
                
                batch.commit()
                    .addOnSuccessListener {
                        // Toast.makeText(this, "تم تحديث صور التعليقات بنجاح", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        // Toast.makeText(this, "فشل حفظ صور التعليقات: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "خطأ في  تحديث صور التعليقات: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun showProfileOptionsMenu(anchor: View) {
        val popup = androidx.appcompat.widget.PopupMenu(this, anchor)
        popup.menu.add(0, 1, 0, getString(R.string.menu_edit_profile))
        popup.menu.add(0, 2, 1, getString(R.string.logout))
        popup.menu.add(0, 3, 2, getString(R.string.delete_account))

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> showEditProfileDialog()
                2 -> showLogoutConfirmDialog()
                3 -> showDeleteAccountDialog()
            }
            true
        }
        popup.show()
    }

    private fun showLogoutConfirmDialog() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(R.string.logout)
            .setMessage(R.string.confirm_logout)
            .setPositiveButton(R.string.logout) { _, _ ->
                AuthManager.signOut(this) {
                    Toast.makeText(this, R.string.confirm_logout, Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .setNegativeButton(R.string.no, null)
            .show()
    }

    private fun showDeleteAccountDialog() {
        val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        if (user == null) {
            android.widget.Toast.makeText(this, "يرجى تسجيل الدخول أولاً", android.widget.Toast.LENGTH_SHORT).show()
            return
        }

        var countDownTimer: android.os.CountDownTimer? = null
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(R.string.delete_account_confirm_title)
            .setMessage(R.string.delete_account_warning)
            .setPositiveButton(getString(R.string.delete_account_confirm_btn) + " (5)") { _, _ ->
                performAccountDeletion(user)
            }
            .setNegativeButton(R.string.no) { d, _ ->
                countDownTimer?.cancel()
                d.dismiss()
            }
            .setCancelable(false)
            .create()

        dialog.setOnShowListener {
            val positiveBtn = dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
            positiveBtn.isEnabled = false
            positiveBtn.setTextColor(android.graphics.Color.GRAY)

            countDownTimer = object : android.os.CountDownTimer(5000, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    val sec = (millisUntilFinished / 1000) + 1
                    positiveBtn.text = getString(R.string.delete_account_confirm_btn) + " ($sec)"
                }

                override fun onFinish() {
                    positiveBtn.isEnabled = true
                    positiveBtn.text = getString(R.string.delete_account_confirm_btn)
                    positiveBtn.setTextColor(android.graphics.Color.RED)
                }
            }.start()
        }

        dialog.show()
    }

    private fun performAccountDeletion(user: com.google.firebase.auth.FirebaseUser) {
        val uid = user.uid
        val progressDialog = android.app.ProgressDialog(this).apply {
            setMessage(getString(R.string.delete_account_progress))
            setCancelable(false)
            show()
        }

        FirestoreManager.deleteUserCompleteData(uid) { _ ->
            user.delete().addOnCompleteListener { _ ->
                try {
                    progressDialog.dismiss()
                } catch (e: Exception) {
                }
                getSharedPreferences("user_prefs", MODE_PRIVATE).edit().clear().apply()
                AuthManager.signOut(this) {
                    android.widget.Toast.makeText(this, R.string.delete_account_success, android.widget.Toast.LENGTH_LONG).show()
                    val intent = android.content.Intent(this, MainActivity::class.java).apply {
                        flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(intent)
                    finish()
                }
            }
        }
    }
}
