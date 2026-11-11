package com.manchorati.www

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch

enum class MoodType(
    val id: String,
    val title: String,
    val emoji: String,
    val bannerPhrase: String,
    val keywords: List<String>,
    val categoryIds: List<Int>,
    val defaultTag: String
) {
    ALL("all", "الكل", "", "", emptyList(), emptyList(), ""),
    CHILL("chill", "رايق وهادئ", "", "كوب قهوة، وهدوء يملأ روحك.. اقرأ بروية وسلام",
        listOf("قهوة", "مساء", "صباح", "هدوء", "راحة", "سكينة", "أمل", "سلام", "روقان", "رايق", "شاي", "عزلة", "بحر", "مطر", "ورد", "تأمل", "كتاب", "فنجان", "نسيم"),
        listOf(13, 14, 20), "قهوة ومساء"),
    MOTIVATED("motivated", "محتاج تحفيز", "", "انهض! لا شيء مستحيل أمام إرادتك وعزيمتك الجبارة",
        listOf("تحفيز", "تطوير", "نجاح", "أمل", "قوة", "إصرار", "حلم", "هدف", "طموح", "شغف", "عزيمة", "تحدي", "إرادة", "صبر", "مستقبل", "إنجاز", "كفاح", "طاقة", "تفاؤل"),
        listOf(11, 18), "تحفيز وتطوير"),
    SAD("sad", "مهموم وحزين", "", "﴿لَا تَحْزَنْ إِنَّ اللَّهَ مَعَنَا﴾ — كل مرٍّ سيمر، وهذه الكلمات لتُطبطب على قلبك",
        listOf("خواطر", "حزن", "صبر", "فراق", "ألم", "دعاء", "طبطبة", "فرج", "عسر", "مهموم", "وجع", "ضيق", "تعب", "هم", "دموع", "يارب", "فقد", "مواساة", "خيبة"),
        listOf(15, 4, 12), "خواطر"),
    HAPPY("happy", "متفائل وسعيد", "", "ابتسم، فالحياة أجمل وأوسع عندما نراها بعين الرضا والبهجة",
        listOf("ابتسامة", "سعادة", "فرح", "ضحك", "فكاهة", "تفاؤل", "جمال", "فرفشة", "نكت", "وناسة", "سرور", "بهجة", "عيد", "أمل", "طاقة"),
        listOf(8, 6, 9, 7), "فكاهة وضحك"),
    LOVE("love", "حب وامتنان", "", "الحب والامتنان سر السعادة.. شارك من تحب كلمة طيبة وأثراً جميلاً",
        listOf("حب", "عشق", "امتنان", "شوق", "وفاء", "صداقة", "أهل", "أمي", "أبي", "غرام", "مودة", "حبيبي", "صديق", "عتاب", "قلب", "مشاعر", "روحي", "تقدير"),
        listOf(2, 3, 21, 23), "شعر وأدب"),
    PEACE("peace", "أدعية وسكينة", "", "ألا بذكر الله تطمئن القلوب.. سكينة ونور ودعوات مستجابة",
        listOf("أدعية", "أذكار", "دعاء", "طمأنينة", "ذكر", "قرآن", "استغفار", "الله", "يارب", "اللهم", "الحمدلله", "سبحان", "سكينة", "إيمان", "صلاة", "رسول", "نبي", "تسبيح", "رحمة", "مغفرة", "جنة", "نور", "أجر"),
        listOf(1, 17, 10), "أدعية وأذكار")
}

fun Post.toCuratedCommunityPost(mood: MoodType, context: Context): CommunityPost {
    val bgList = listOf("#1E293B", "#0F172A", "#18181B", "#1E1B4B", "#14532D", "#701A75", "#0C4A6E", "#831843")
    val fontList = listOf("amiri", "tajawal", "cairo", "kufi")
    val selectedBg = bgList[Math.abs(id.hashCode()) % bgList.size]
    val selectedFont = fontList[Math.abs(id.hashCode()) % fontList.size]
    val randomLikes = (35 + (Math.abs(id.hashCode()) % 150)).toLong()
    val isLiked = LocalLikesManager.isLiked(context, "curated_$id")
    val currentUid = AuthManager.currentUser?.uid ?: "local_guest"
    val likedByList = if (isLiked) listOf(currentUid) else emptyList()

    // استخدام أفاتار كرتوني حصري من أفاتار التطبيق فقط (avatar_1 .. avatar_14) لمنع أي استخدام لصور حسابات المستخدمين
    val avatarIdx = (Math.abs(id.hashCode()) % 14) + 1
    val avatarResourceName = "avatar_$avatarIdx"

    return CommunityPost(
        id = "curated_$id",
        authorId = "manchorati_curated",
        authorName = "منشوراتي",
        authorUsername = "manchorati",
        authorPhotoUrl = avatarResourceName,
        content = content,
        bgId = selectedBg,
        fontId = selectedFont,
        textColor = "#FFFFFF",
        tag = mood.title,
        likesCount = if (isLiked) randomLikes + 1 else randomLikes,
        commentsCount = 0L,
        reportCount = 0L,
        likedBy = likedByList,
        authorBadge = "official",
        timestamp = System.currentTimeMillis() - (Math.abs(id.hashCode()) % 86400000L)
    )
}

class CommunityFragment : Fragment() {

    private lateinit var rvCommunity: RecyclerView
    private lateinit var fabAddPost: FloatingActionButton
    private lateinit var communityAdapter: CommunityAdapter
    private lateinit var swipeRefreshCommunity: androidx.swiperefreshlayout.widget.SwipeRefreshLayout
    private lateinit var layoutEmptyCommunity: View
    private lateinit var tvEmptyTitle: TextView
    private lateinit var tvEmptySubtitle: TextView

    private lateinit var btnSortContainer: View
    private lateinit var tvActiveSortLabel: TextView
    private lateinit var btnTagSelector: View
    private lateinit var tvActiveTagLabel: TextView
    private lateinit var ivTagChevron: ImageView
    private lateinit var btnClearTag: ImageView

    private var currentTagFilter: String = "الكل"
    private var isTrendingSort: Boolean = false
    private var sortMode: Int = 0 // 0 = latest, 1 = trending, 2 = following
    private var selectedMood: MoodType = MoodType.ALL
    private var followingUserIds: Set<String> = emptySet()
    private var followingListener: ListenerRegistration? = null

    private var allPostsList: List<CommunityPost> = emptyList()
    private var selectMoodAction: ((MoodType) -> Unit)? = null
    private var deviceId: String = ""
    private var loginDialog: BottomSheetDialog? = null
    private var postsListener: ListenerRegistration? = null

    private var isInitialLoad = true
    private var isRefreshingFeed = false
    private var isLoadingMore = false

    private fun getActiveUserId(): String {
        return AuthManager.currentUser?.uid ?: ""
    }

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                if (account != null) {
                    AuthManager.firebaseAuthWithGoogle(
                        account = account,
                        onSuccess = { isNewUser ->
                            val currentUid = getActiveUserId()
                            val currentUser = FirebaseAuth.getInstance().currentUser

                            if (currentUser != null) {
                                val db = FirebaseFirestore.getInstance()
                                db.collection("users").document(currentUid).get()
                                    .addOnSuccessListener { doc ->
                                        val existingUsername = doc.getString("username")

                                        if (existingUsername.isNullOrEmpty()) {
                                            // توليد اسم مستخدم عشوائي تلقائياً
                                            val baseName = AuthManager.generateBaseUsername(
                                                currentUser.displayName, 
                                                currentUser.email
                                            )

                                            AuthManager.generateUniqueUsername(baseName) { autoUsername ->
                                                val displayName = currentUser.displayName ?: autoUsername
                                                val email = currentUser.email ?: ""

                                                // حفظ الحساب فوراً باليوزرنيم التلقائي لضمان عدم تركه فارغاً إذا خرج
                                                AuthManager.registerUsername(
                                                    currentUid, 
                                                    autoUsername, 
                                                    displayName, 
                                                    email
                                                ) { ok, _ ->
                                                    communityAdapter.updateCurrentUserId(currentUid)
                                                    // عرض النافذة مع وضع الاسم التلقائي داخل الحقل ليتيح له تغييره
                                                    showUsernameSetupInDialog(autoUsername)
                                                }
                                            }
                                        } else {
                                            // المستخدم يملك اسم مستخدم بالفعل
                                            loginDialog?.dismiss()
                                            communityAdapter.updateCurrentUserId(currentUid)
                                            Toast.makeText(requireContext(), "تم تسجيل الدخول بنجاح", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    .addOnFailureListener {
                                        loginDialog?.dismiss()
                                        communityAdapter.updateCurrentUserId(currentUid)
                                    }
                            }
                        },
                        onFailure = { error ->
                            Toast.makeText(requireContext(), "فشل الدخول: $error", Toast.LENGTH_LONG).show()
                        }
                    )
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "خطأ جوجل: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_community, container, false)

        deviceId = Settings.Secure.getString(requireContext().contentResolver, Settings.Secure.ANDROID_ID)

        rvCommunity = view.findViewById(R.id.rvCommunity)
        fabAddPost = view.findViewById(R.id.fabAddPost)
        swipeRefreshCommunity = view.findViewById(R.id.swipeRefreshCommunity)
        layoutEmptyCommunity = view.findViewById(R.id.layoutEmptyCommunity)
        tvEmptyTitle = view.findViewById(R.id.tvEmptyTitle)
        tvEmptySubtitle = view.findViewById(R.id.tvEmptySubtitle)
        swipeRefreshCommunity.setColorSchemeColors(Color.parseColor("#0284C7"))
        swipeRefreshCommunity.setOnRefreshListener {
            isRefreshingFeed = true
            VirtualCommunityManager.getCuratedFeedPosts(requireContext(), forceRefresh = true)
            VirtualCommunityManager.maybeSeedDailyFirestorePost(requireContext())
            startListeningToPosts()
        }

        setupSortControls(view)
        setupTagSelector(view)
        setupMoodSelector(view)

        val btnExploreWriters = view.findViewById<View>(R.id.btnExploreWritersCommunity)
        btnExploreWriters?.setOnClickListener {
            val intent = Intent(requireContext(), WritersActivity::class.java)
            startActivity(intent)
        }

        rvCommunity.layoutManager = LinearLayoutManager(requireContext())

        communityAdapter = CommunityAdapter(
            posts = emptyList(),
            currentUserId = getActiveUserId(),
            onLikeClicked = { post, isLiked ->
                if (post.id.startsWith("curated_") || post.id.startsWith("vpost_")) {
                    LocalLikesManager.toggleLike(requireContext(), post.id)
                    return@CommunityAdapter
                }
                val currentUser = AuthManager.currentUser ?: com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                if (currentUser == null) {
                    Toast.makeText(requireContext(), getString(R.string.login_required_like), Toast.LENGTH_SHORT).show()
                    showLoginDialog()
                } else {
                    val uid = currentUser.uid
                    FirestoreManager.toggleLike(post.id, uid, isLiked) {
                        if (!isLiked && post.authorId != uid) {
                            val senderName = currentUser.displayName ?: getString(R.string.default_user_name)
                            FirestoreManager.sendNotification(
                                recipientId = post.authorId,
                                senderId = uid,
                                senderName = senderName,
                                type = "LIKE",
                                postId = post.id,
                                message = getString(R.string.notif_liked_post, senderName)
                            )
                        }
                    }
                }
            },
            onCommentClicked = { post ->
                val currentUser = AuthManager.currentUser
                if (currentUser == null) {
                    Toast.makeText(requireContext(), getString(R.string.login_required_comment), Toast.LENGTH_SHORT).show()
                    showLoginDialog()
                } else {
                    showCommentsDialog(post)
                }
            },
            onAuthorClicked = { authorId, authorName ->
                val intent = Intent(requireContext(), UserProfileActivity::class.java).apply {
                    putExtra("USER_ID", authorId)
                    putExtra("USER_NAME", authorName)
                }
                startActivity(intent)
            },
            onEditClicked = { post -> showEditDialog(post) },
            onDeleteClicked = { post -> confirmDelete(post.id) },
            onReportClicked = { post -> showReportDialog(post.id) },
            onTagClicked = { tag -> filterByHashtag(tag) }
        )
        rvCommunity.adapter = communityAdapter

        rvCommunity.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return
                val firstVisible = layoutManager.findFirstVisibleItemPosition()
                val lastVisible = layoutManager.findLastVisibleItemPosition()

                if (firstVisible != RecyclerView.NO_POSITION && lastVisible != RecyclerView.NO_POSITION && firstVisible <= lastVisible) {
                    val posts = communityAdapter.getPosts()
                    for (i in firstVisible..lastVisible) {
                        val post = posts.getOrNull(i)
                        if (post != null) {
                            CommunityViewTracker.markPostSeen(recyclerView.context, post.id)
                        }
                    }
                }

                val totalItemCount = layoutManager.itemCount
                if (dy > 0 && !isLoadingMore && totalItemCount > 0 && lastVisible >= totalItemCount - 4) {
                    loadMorePosts()
                }
            }
        })

        val btnEmptyAction = view.findViewById<View>(R.id.btnEmptyAction)
        btnEmptyAction?.setOnClickListener {
            val tag = if (selectedMood != MoodType.ALL) selectedMood.defaultTag else if (currentTagFilter != "الكل") currentTagFilter else ""
            checkUserAndOpenPostDialog(initialTag = tag)
        }

        fabAddPost.setOnClickListener {
            showCreationChoiceBottomSheet()
        }

        return view
    }

    private fun showCreationChoiceBottomSheet() {
        val dialog = BottomSheetDialog(requireContext())
        val sheetView = layoutInflater.inflate(R.layout.dialog_create_choice, null)
        dialog.setContentView(sheetView)

        val tag = if (selectedMood != MoodType.ALL) selectedMood.defaultTag else if (currentTagFilter != "الكل") currentTagFilter else ""

        sheetView.findViewById<View>(R.id.btnChoiceWritePost)?.setOnClickListener {
            dialog.dismiss()
            checkUserAndOpenPostDialog(initialTag = tag)
        }

        sheetView.findViewById<View>(R.id.btnChoiceCreateReel)?.setOnClickListener {
            dialog.dismiss()
            val intent = Intent(requireContext(), VideoStudioActivity::class.java)
            startActivity(intent)
        }

        sheetView.findViewById<View>(R.id.btnChoiceAiWriter)?.setOnClickListener {
            dialog.dismiss()
            val intent = Intent(requireContext(), AiChatActivity::class.java).apply {
                putExtra("EXTRA_INITIAL_TOPIC", tag)
            }
            startActivity(intent)
        }

        dialog.show()
    }

    private fun setupSortControls(view: View) {
        btnSortContainer = view.findViewById(R.id.btnSortContainer)
        tvActiveSortLabel = view.findViewById(R.id.tvActiveSortLabel)

        fun updateSortUI() {
            tvActiveSortLabel.text = when (sortMode) {
                1 -> getString(R.string.sort_trending)
                2 -> "أتابعهم"
                else -> getString(R.string.sort_latest)
            }
        }
        updateSortUI()

        btnSortContainer.setOnClickListener {
            val popup = androidx.appcompat.widget.PopupMenu(requireContext(), btnSortContainer)
            popup.menu.add(0, 1, 0, getString(R.string.sort_latest)).apply {
                isCheckable = true
                isChecked = (sortMode == 0)
            }
            popup.menu.add(0, 2, 1, getString(R.string.sort_trending)).apply {
                isCheckable = true
                isChecked = (sortMode == 1)
            }
            popup.menu.add(0, 3, 2, getString(R.string.following_feed_menu)).apply {
                isCheckable = true
                isChecked = (sortMode == 2)
            }
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    1 -> {
                        sortMode = 0
                        isTrendingSort = false
                        updateSortUI()
                        applyFilterAndSort()
                        true
                    }
                    2 -> {
                        sortMode = 1
                        isTrendingSort = true
                        updateSortUI()
                        applyFilterAndSort()
                        true
                    }
                    3 -> {
                        val user = FirebaseAuth.getInstance().currentUser
                        if (user == null) {
                            Toast.makeText(requireContext(), getString(R.string.login_required_following), Toast.LENGTH_SHORT).show()
                            showLoginDialog()
                        } else {
                            sortMode = 2
                            isTrendingSort = false
                            updateSortUI()
                            applyFilterAndSort()
                        }
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }
    }

    private fun setupMoodSelector(view: View) {
        val chipMoodAll = view.findViewById<TextView>(R.id.chipMoodAll)
        val chipMoodChill = view.findViewById<TextView>(R.id.chipMoodChill)
        val chipMoodMotivated = view.findViewById<TextView>(R.id.chipMoodMotivated)
        val chipMoodSad = view.findViewById<TextView>(R.id.chipMoodSad)
        val chipMoodHappy = view.findViewById<TextView>(R.id.chipMoodHappy)
        val chipMoodLove = view.findViewById<TextView>(R.id.chipMoodLove)
        val chipMoodPeace = view.findViewById<TextView>(R.id.chipMoodPeace)

        val layoutMoodBanner = view.findViewById<View>(R.id.layoutMoodBanner)
        val tvMoodBannerEmoji = view.findViewById<TextView>(R.id.tvMoodBannerEmoji)
        val tvMoodBannerText = view.findViewById<TextView>(R.id.tvMoodBannerText)
        val tvActiveMoodSubtitle = view.findViewById<TextView>(R.id.tvActiveMoodSubtitle)
        val btnMoodWritePost = view.findViewById<View>(R.id.btnMoodWritePost)
        val btnDismissMoodBanner = view.findViewById<View>(R.id.btnDismissMoodBanner)

        btnMoodWritePost?.setOnClickListener {
            btnMoodWritePost.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            checkUserAndOpenPostDialog(initialTag = selectedMood.defaultTag)
        }

        btnDismissMoodBanner?.setOnClickListener {
            btnDismissMoodBanner.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            layoutMoodBanner.visibility = View.GONE
        }

        val chips = listOf(
            Pair(chipMoodAll, MoodType.ALL),
            Pair(chipMoodChill, MoodType.CHILL),
            Pair(chipMoodMotivated, MoodType.MOTIVATED),
            Pair(chipMoodSad, MoodType.SAD),
            Pair(chipMoodHappy, MoodType.HAPPY),
            Pair(chipMoodLove, MoodType.LOVE),
            Pair(chipMoodPeace, MoodType.PEACE)
        )

        fun selectMood(mood: MoodType) {
            selectedMood = mood
            for ((chip, m) in chips) {
                if (m == mood) {
                    chip.setBackgroundResource(R.drawable.bg_mood_chip_active)
                    chip.setTextColor(Color.WHITE)
                    chip.setTypeface(null, Typeface.BOLD)
                } else {
                    chip.setBackgroundResource(R.drawable.bg_mood_chip_idle)
                    chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
                    chip.setTypeface(null, Typeface.NORMAL)
                }
            }

            if (mood == MoodType.ALL) {
                layoutMoodBanner.visibility = View.GONE
                tvActiveMoodSubtitle.text = getString(R.string.mood_selector_subtitle)
            } else {
                layoutMoodBanner.visibility = View.VISIBLE
                tvMoodBannerEmoji.visibility = View.GONE
                tvMoodBannerText.text = mood.bannerPhrase
                tvActiveMoodSubtitle.text = getString(R.string.mood_active_display, mood.title)
            }

            applyFilterAndSort()
        }

        for ((chip, mood) in chips) {
            chip.setOnClickListener {
                chip.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                selectMood(mood)
            }
        }
        selectMoodAction = { selectMood(it) }
    }

    private fun setupTagSelector(view: View) {
        btnTagSelector = view.findViewById(R.id.btnTagSelector)
        tvActiveTagLabel = view.findViewById(R.id.tvActiveTagLabel)
        ivTagChevron = view.findViewById(R.id.ivTagChevron)
        btnClearTag = view.findViewById(R.id.btnClearTag)

        updateTagSelectorUI()

        btnTagSelector.setOnClickListener {
            showCategorySelectionDialog()
        }

        btnClearTag.setOnClickListener {
            currentTagFilter = "الكل"
            updateTagSelectorUI()
            applyFilterAndSort()
        }
    }

    private fun getLocalizedTagLabel(tag: String): String {
        return when (tag) {
            "الكل" -> getString(R.string.tag_all)
            "المحفوظات" -> getString(R.string.saved_posts)
            "حكم وأقوال" -> getString(R.string.tag_wisdom)
            "خواطر" -> getString(R.string.tag_thoughts)
            "أدعية وأذكار" -> getString(R.string.tag_dua)
            "تحفيز وتطوير" -> getString(R.string.tag_motivation)
            "فكاهة وضحك" -> getString(R.string.tag_humor)
            "شعر وأدب" -> getString(R.string.tag_poetry)
            "قهوة ومساء" -> getString(R.string.tag_coffee)
            "عام" -> getString(R.string.tag_general)
            else -> tag
        }
    }

    private fun updateTagSelectorUI() {
        val isAll = currentTagFilter == "الكل" || currentTagFilter.isEmpty()
        if (isAll) {
            btnTagSelector.setBackgroundResource(R.drawable.bg_tool_chip_idle)
            tvActiveTagLabel.text = getString(R.string.tag_all)
            tvActiveTagLabel.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.text_secondary))
            tvActiveTagLabel.typeface = android.graphics.Typeface.DEFAULT
            ivTagChevron.visibility = View.VISIBLE
            btnClearTag.visibility = View.GONE
        } else {
            btnTagSelector.setBackgroundResource(R.drawable.bg_tool_chip_active)
            tvActiveTagLabel.text = getLocalizedTagLabel(currentTagFilter)
            tvActiveTagLabel.setTextColor(Color.WHITE)
            tvActiveTagLabel.typeface = android.graphics.Typeface.DEFAULT_BOLD
            ivTagChevron.visibility = View.GONE
            btnClearTag.visibility = View.VISIBLE
        }
    }

    private fun showCategorySelectionDialog() {
        val dialog = BottomSheetDialog(requireContext())
        val dialogView = layoutInflater.inflate(R.layout.dialog_select_category, null)
        dialog.setContentView(dialogView)

        val btnClose = dialogView.findViewById<ImageView>(R.id.btnCloseCategoryDialog)
        val layoutCategoryList = dialogView.findViewById<LinearLayout>(R.id.layoutCategoryList)

        btnClose.setOnClickListener { dialog.dismiss() }

        val categories = listOf(
            "الكل" to getString(R.string.tag_all),
            "المحفوظات" to getString(R.string.saved_posts),
            "حكم وأقوال" to getString(R.string.tag_wisdom),
            "خواطر" to getString(R.string.tag_thoughts),
            "أدعية وأذكار" to getString(R.string.tag_dua),
            "تحفيز وتطوير" to getString(R.string.tag_motivation),
            "فكاهة وضحك" to getString(R.string.tag_humor),
            "شعر وأدب" to getString(R.string.tag_poetry),
            "قهوة ومساء" to getString(R.string.tag_coffee),
            "عام" to getString(R.string.tag_general)
        )

        val density = resources.displayMetrics.density

        for ((tagValue, tagLabel) in categories) {
            val isSelected = if (tagValue == "الكل") (currentTagFilter == "الكل" || currentTagFilter.isEmpty()) else currentTagFilter == tagValue

            val row = LinearLayout(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    (48 * density).toInt()
                )
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding((12 * density).toInt(), 0, (12 * density).toInt(), 0)
                background = androidx.core.content.ContextCompat.getDrawable(
                    requireContext(),
                    if (isSelected) R.drawable.bg_category_chip_idle else android.R.color.transparent
                )
                isClickable = true
                isFocusable = true
            }

            val tvTitle = TextView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                text = tagLabel
                textSize = 14f
                setTextColor(
                    if (isSelected) Color.parseColor("#0284C7")
                    else androidx.core.content.ContextCompat.getColor(requireContext(), R.color.text_primary)
                )
                typeface = if (isSelected) android.graphics.Typeface.DEFAULT_BOLD else android.graphics.Typeface.DEFAULT
            }
            row.addView(tvTitle)

            if (isSelected) {
                val ivCheck = ImageView(requireContext()).apply {
                    layoutParams = LinearLayout.LayoutParams((18 * density).toInt(), (18 * density).toInt())
                    setImageResource(R.drawable.ic_check)
                    setColorFilter(Color.parseColor("#0284C7"))
                }
                row.addView(ivCheck)
            }

            row.setOnClickListener {
                currentTagFilter = tagValue
                updateTagSelectorUI()
                applyFilterAndSort()
                dialog.dismiss()
            }

            layoutCategoryList.addView(row)
        }

        dialog.show()
    }

    private fun isRealUserPost(post: CommunityPost): Boolean {
        if (post.id.startsWith("vpost_") || post.id.startsWith("curated_")) return false
        if (post.authorId.startsWith("persona_") || post.authorId == "manchorati_curated") return false
        return true
    }

    private fun getPostPriorityRank(post: CommunityPost): Int {
        val isFollowing = followingUserIds.contains(post.authorId)
        val isReal = isRealUserPost(post)
        val isSeen = isAdded && CommunityViewTracker.isPostSeen(requireContext(), post.id)

        return when {
            // أولوية 1: الأشخاص الذين أتابعهم (جديد غير مشاهد) -> في الصدارة دائماً
            isFollowing && !isSeen -> 0
            // أولوية 2: الأشخاص الذين أتابعهم (تمت مشاهدته سابقاً)
            isFollowing && isSeen -> 1
            // أولوية 3: مستخدمين حقيقيين (جديد غير مشاهد)
            isReal && !isSeen -> 2
            // أولوية 4: مستخدمين حقيقيين (مشاهد سابقاً)
            isReal && isSeen -> 3
            // أولوية 5: اقتباسات وأشخاص افتراضيين
            else -> 4
        }
    }

    private fun applyFilterAndSort() {
        var list = allPostsList

        // 1. تصفية حسب الوسم المختار (Tag filter)
        if (currentTagFilter == "المحفوظات") {
            val savedIds = BookmarkManager.getBookmarkedPostIds(requireContext())
            list = list.filter { savedIds.contains(it.id) }
        } else if (currentTagFilter != "الكل") {
            val cleanCurrent = currentTagFilter.replace(Regex("[^\\p{L}\\p{Nd}\\s]"), "").trim()
            list = list.filter { post ->
                val cleanPostTag = (post.tag ?: "").replace(Regex("[^\\p{L}\\p{Nd}\\s]"), "").trim()
                cleanPostTag.contains(cleanCurrent) || cleanCurrent.contains(cleanPostTag)
            }
        }

        // 2. تصفية حسب المزاج المختار أو الدمج الهجين في التغذية العامة
        if (selectedMood != MoodType.ALL) {
            val moodKeywords = selectedMood.keywords
            val userPostsMatching = list.filter { post ->
                val content = post.content.lowercase()
                val tag = (post.tag ?: "").trim()
                if (tag.isNotEmpty() && tag != "خواطر" && tag != "عام") {
                    tag == selectedMood.title || tag == selectedMood.defaultTag
                } else {
                    post.tag == selectedMood.title || post.tag == selectedMood.defaultTag || moodKeywords.any { kw -> content.contains(kw) }
                }
            }

            if (isAdded) {
                try {
                    val dbHelper = DatabaseHelper(requireContext())
                    val curatedRaw = dbHelper.getCuratedPostsForCategoryIds(selectedMood.categoryIds, limit = 25)
                    val curatedPosts = curatedRaw.mapIndexed { idx, p ->
                        VirtualCommunityManager.toVirtualCommunityPost(
                            p,
                            requireContext(),
                            targetTag = selectedMood.title,
                            staggerMinutesAgo = 10L + idx * 25L
                        )
                    }
                    list = userPostsMatching + curatedPosts
                } catch (e: Exception) {
                    list = userPostsMatching
                }
            } else {
                list = userPostsMatching
            }
        } else if (sortMode == 2) {
            // أتابعهم: مخصص فقط للكتّاب والمستخدمين الذين يتابعهم
            list = list.filter { followingUserIds.contains(it.authorId) }
        } else {
            // التغذية العامة (ALL): دمج ذكي مع اقتباسات المجتمع لضمان فيد حي ولا نهائي
            if (isAdded && currentTagFilter != "المحفوظات") {
                val curatedFeed = VirtualCommunityManager.getCuratedFeedPosts(requireContext(), forceRefresh = isRefreshingFeed)
                val filteredCurated = if (currentTagFilter != "الكل") {
                    val cleanCurrent = currentTagFilter.replace(Regex("[^\\p{L}\\p{Nd}\\s]"), "").trim()
                    curatedFeed.filter { post ->
                        val cleanPostTag = (post.tag ?: "").replace(Regex("[^\\p{L}\\p{Nd}\\s]"), "").trim()
                        cleanPostTag.contains(cleanCurrent) || cleanCurrent.contains(cleanPostTag)
                    }
                } else {
                    curatedFeed
                }
                list = list + filteredCurated
            }
        }

        // 3. ترتيب وتصفية التبويب مع احترام الأولويات الصارمة: (المتابعون غير المشاهدين -> المتابعون المشاهدين -> المستخدمون الحقيقيون -> البقية)
        list = when (sortMode) {
            1 -> { // الأكثر تفاعلاً
                list.sortedWith(
                    compareBy<CommunityPost> { getPostPriorityRank(it) }
                        .thenByDescending { it.likesCount }
                        .thenByDescending { it.getTimeMillis() }
                )
            }
            2 -> { // أتابعهم
                list.sortedWith(
                    compareBy<CommunityPost> { getPostPriorityRank(it) }
                        .thenByDescending { it.getTimeMillis() }
                )
            }
            else -> { // الأحدث / الرئيسية
                list.sortedWith(
                    compareBy<CommunityPost> { getPostPriorityRank(it) }
                        .thenByDescending { it.getTimeMillis() }
                )
            }
        }

        communityAdapter.updateData(list)

        // 4. معالجة الحالة الفارغة (Empty State UI)
        if (list.isEmpty()) {
            layoutEmptyCommunity.visibility = View.VISIBLE
            if (sortMode == 2) {
                tvEmptyTitle.text = getString(R.string.following_feed_empty_title)
                tvEmptySubtitle.text = getString(R.string.following_feed_empty_subtitle)
            } else if (currentTagFilter == "المحفوظات") {
                tvEmptyTitle.text = getString(R.string.saved_posts)
                tvEmptySubtitle.text = getString(R.string.no_posts_yet)
            } else if (selectedMood != MoodType.ALL) {
                tvEmptyTitle.text = getString(R.string.mood_empty_title)
                tvEmptySubtitle.text = getString(R.string.mood_empty_subtitle, selectedMood.title)
            } else if (currentTagFilter != "الكل") {
                tvEmptyTitle.text = "${getString(R.string.no_posts_yet)} (${getLocalizedTagLabel(currentTagFilter)})"
                tvEmptySubtitle.text = getString(R.string.share_first_post)
            } else {
                tvEmptyTitle.text = getString(R.string.no_posts_yet)
                tvEmptySubtitle.text = getString(R.string.share_first_post)
            }
        } else {
            layoutEmptyCommunity.visibility = View.GONE
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val appBar = view.findViewById<com.google.android.material.appbar.AppBarLayout>(R.id.appBarCommunity)
        appBar?.addOnOffsetChangedListener { _, verticalOffset ->
            swipeRefreshCommunity.isEnabled = (verticalOffset == 0)
        }
        startListeningToPosts()
        listenToFollowingList()
        observeAuthState()
    }

    private fun listenToFollowingList() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        followingListener?.remove()
        followingListener = FirebaseFirestore.getInstance().collection("users").document(uid)
            .addSnapshotListener { doc, _ ->
                val list = (doc?.get("following") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
                val newSet = list.toSet()
                if (newSet != followingUserIds) {
                    followingUserIds = newSet
                    applyFilterAndSort()
                }
            }
    }

    private fun observeAuthState() {
        viewLifecycleOwner.lifecycleScope.launch {
            AuthManager.authStateFlow.collect { user ->
                val uid = user?.uid ?: ""
                communityAdapter.updateCurrentUserId(uid)
            }
        }
    }

    private var pendingNotificationPostId: String? = null
    private var pendingNotificationType: String? = null

    private fun startListeningToPosts() {
        postsListener?.remove()
        postsListener = FirestoreManager.listenToCommunityPosts(
            onUpdate = { list ->
                activity?.runOnUiThread {
                    swipeRefreshCommunity.isRefreshing = false
                    val previousFirestorePosts = allPostsList
                    allPostsList = list
                    for (p in list) {
                        if (p.authorId.isNotEmpty()) {
                            BadgeHelper.fetchAndCacheAuthorBadge(p.authorId)
                        }
                    }

                    if (isInitialLoad || isRefreshingFeed) {
                        isInitialLoad = false
                        isRefreshingFeed = false
                        applyFilterAndSort()
                    } else {
                        // المستخدم يتصفح حالياً: تحديث الإحصائيات في مكانها دون أي قفز أو إعادة ترتيب
                        val currentAdapterPosts = communityAdapter.getPosts()
                        val currentAdapterIds = currentAdapterPosts.map { it.id }.toSet()
                        for (p in list) {
                            if (currentAdapterIds.contains(p.id)) {
                                communityAdapter.updatePostStats(p)
                            }
                        }

                        // حذف المنشور إذا تم حذفه من قبل المستخدم
                        val newFirestoreIds = list.map { it.id }.toSet()
                        for (oldP in previousFirestorePosts) {
                            if (!newFirestoreIds.contains(oldP.id) && currentAdapterIds.contains(oldP.id)) {
                                communityAdapter.removePost(oldP.id)
                            }
                        }

                        // في حال نشر منشور جديد وهو في أعلى الصفحة
                        val previousIds = previousFirestorePosts.map { it.id }.toSet()
                        val newlyAdded = list.filterNot { previousIds.contains(it.id) }
                        if (newlyAdded.isNotEmpty()) {
                            val layoutMgr = rvCommunity.layoutManager as? LinearLayoutManager
                            val firstVisible = layoutMgr?.findFirstVisibleItemPosition() ?: 0
                            if (firstVisible <= 1) {
                                for (np in newlyAdded.reversed()) {
                                    communityAdapter.insertPostAtTop(np)
                                }
                                rvCommunity.scrollToPosition(0)
                            }
                        }
                    }

                    if (pendingNotificationPostId != null) {
                        val pId = pendingNotificationPostId!!
                        val pType = pendingNotificationType ?: ""
                        pendingNotificationPostId = null
                        pendingNotificationType = null
                        rvCommunity.postDelayed({
                            handleNotificationClick(pId, pType)
                        }, 250)
                    }
                }
            },
            onError = {
                activity?.runOnUiThread {
                    swipeRefreshCommunity.isRefreshing = false
                    isRefreshingFeed = false
                    if (isInitialLoad) {
                        isInitialLoad = false
                        applyFilterAndSort()
                    }
                }
            }
        )
    }

    private fun loadMorePosts() {
        if (isLoadingMore) return
        isLoadingMore = true

        viewLifecycleOwner.lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val dbHelper = DatabaseHelper(requireContext())
                val categoryIds = if (selectedMood != MoodType.ALL) selectedMood.categoryIds else emptyList()
                val rawQuotes = if (categoryIds.isNotEmpty()) {
                    dbHelper.getCuratedPostsForCategoryIds(categoryIds, limit = 20)
                } else {
                    dbHelper.getRandomCuratedPosts(limit = 20)
                }

                if (rawQuotes.isNotEmpty() && isAdded) {
                    val currentCount = communityAdapter.itemCount
                    val newPosts = rawQuotes.mapIndexed { index, post ->
                        val uniqueId = "vpost_${post.id}_${System.currentTimeMillis() % 100000}_$index"
                        val persona = VirtualCommunityManager.getPersonaForId(post.id + index + currentCount)
                        val bgList = listOf("card_luxury", "card_crimson_brand", "card_ocean_breeze", "card_emerald", "card_sunset", "card_glass_royal", "card_neon_cyber", "card_classic_paper")
                        val fontList = listOf("tajawal", "cairoplay", "kufam", "arefruqaa", "marhey")
                        val selectedBg = bgList[Math.abs((post.id + index + currentCount).hashCode()) % bgList.size]
                        val selectedFont = fontList[Math.abs((post.id + index + currentCount).hashCode() / 3) % fontList.size]
                        val baseLikes = (25 + (Math.abs((post.id + index).hashCode()) % 80)).toLong()

                        CommunityPost(
                            id = uniqueId,
                            authorId = persona.id,
                            authorName = persona.name,
                            authorUsername = persona.username,
                            authorPhotoUrl = persona.avatarResource,
                            content = post.content,
                            bgId = selectedBg,
                            fontId = selectedFont,
                            textColor = "",
                            tag = if (selectedMood != MoodType.ALL) selectedMood.title else (post.categoryName.ifEmpty { "خواطر" }),
                            likesCount = baseLikes,
                            commentsCount = (1 + (Math.abs(post.id.hashCode()) % 3)).toLong(),
                            reportCount = 0L,
                            likedBy = emptyList(),
                            authorBadge = persona.badge,
                            timestamp = System.currentTimeMillis() - (currentCount + index) * 600000L
                        )
                    }

                    activity?.runOnUiThread {
                        if (isAdded) {
                            communityAdapter.appendPosts(newPosts)
                            isLoadingMore = false
                        }
                    }
                } else {
                    isLoadingMore = false
                }
            } catch (e: Exception) {
                isLoadingMore = false
            }
        }
    }

    private fun stopListeningToPosts() {
        postsListener?.remove()
        postsListener = null
        allPostsList = emptyList()
        applyFilterAndSort()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopListeningToPosts()
        followingListener?.remove()
        followingListener = null
    }

    override fun onResume() {
        super.onResume()
        // تحديث معرّف المستخدم في الـ Adapter لضمان تلوين القلوب حسب الحساب المتصل حالياً أو تفريغها عند الخروج
        communityAdapter.updateCurrentUserId(getActiveUserId())
        if (currentTagFilter == "المحفوظات") {
            applyFilterAndSort()
        }
    }

    fun handleNotificationClick(postId: String, type: String) {
        if (postId.isEmpty()) return

        // 1. Reset filters so post is not hidden in custom tabs
        if (selectedMood != MoodType.ALL || (currentTagFilter != "الكل" && currentTagFilter.isNotEmpty())) {
            selectedMood = MoodType.ALL
            currentTagFilter = "الكل"
            applyFilterAndSort()
        }

        fun onPostReady(post: CommunityPost, index: Int) {
            (rvCommunity.layoutManager as? LinearLayoutManager)?.scrollToPositionWithOffset(index, 20)
            communityAdapter.highlightPost(post.id)

            if (type.equals("COMMENT", ignoreCase = true) || type.equals("REPLY", ignoreCase = true)) {
                rvCommunity.postDelayed({
                    val currentUser = FirebaseAuth.getInstance().currentUser
                    if (currentUser == null) {
                        showLoginDialog()
                    } else {
                        showCommentsDialog(post)
                    }
                }, 350)
            } else if (type.equals("LIKE", ignoreCase = true)) {
                Toast.makeText(requireContext(), "أعجب أحدهم بهذا المنشور", Toast.LENGTH_SHORT).show()
            }
        }

        val currentPosts = communityAdapter.getPosts()
        val targetIndex = currentPosts.indexOfFirst { it.id == postId }
        if (targetIndex != -1) {
            onPostReady(currentPosts[targetIndex], targetIndex)
        } else {
            // Fetch post from Firestore if not in current loaded batch
            FirebaseFirestore.getInstance().collection("community_posts").document(postId).get()
                .addOnSuccessListener { doc ->
                    if (!isAdded || !doc.exists()) return@addOnSuccessListener
                    val post = doc.toObject(CommunityPost::class.java)?.apply { id = doc.id }
                    if (post != null) {
                        val updated = communityAdapter.getPosts().toMutableList()
                        updated.add(0, post)
                        communityAdapter.updateData(updated)
                        onPostReady(post, 0)
                    }
                }
        }
    }

    fun openPostDialogWithContent(content: String, tag: String = "") {
        checkUserAndOpenPostDialog(initialTag = tag, initialContent = content)
    }

    private fun checkUserAndOpenPostDialog(initialTag: String = "", initialContent: String = "") {
        val user = AuthManager.currentUser
        if (user == null) {
            Toast.makeText(requireContext(), getString(R.string.login_required_post), Toast.LENGTH_SHORT).show()
            showLoginDialog()
            return
        }

        val cachedBlocked = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
            .getBoolean("is_blocked", false)
        if (cachedBlocked) {
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("حسابك محظور")
                .setMessage("تم حظر حسابك من النشر في المجتمع لمخالفته شروط وقواعد الاستخدام.")
                .setPositiveButton("حسناً", null)
                .show()
            return
        }

        val baseUsername = AuthManager.generateBaseUsername(user.displayName, user.email)
        val defaultDisplayName = user.displayName ?: getString(R.string.default_user_name)

        FirebaseFirestore.getInstance().collection("users").document(user.uid).get()
            .addOnSuccessListener { doc ->
                if (!isAdded) return@addOnSuccessListener
                val isBlocked = doc.getBoolean("isBlocked") ?: false
                requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                    .edit()
                    .putBoolean("is_blocked", isBlocked)
                    .apply()

                if (isBlocked) {
                    androidx.appcompat.app.AlertDialog.Builder(requireContext())
                        .setTitle("حسابك محظور")
                        .setMessage("تم حظر حسابك من النشر في المجتمع لمخالفته شروط وقواعد الاستخدام.")
                        .setPositiveButton("حسناً", null)
                        .show()
                    return@addOnSuccessListener
                }

                val username = doc.getString("username")
                if (doc.exists() && !username.isNullOrEmpty()) {
                    val displayName = doc.getString("displayName")
                        ?.takeIf { it.isNotBlank() }
                        ?: defaultDisplayName
                    showPostBottomSheet(user.uid, displayName, username, null, initialTag, initialContent)
                } else {
                    showPostBottomSheet(user.uid, defaultDisplayName, baseUsername, null, initialTag, initialContent)
                }
            }
            .addOnFailureListener {
                if (!isAdded) return@addOnFailureListener
                showPostBottomSheet(user.uid, defaultDisplayName, baseUsername, null, initialTag, initialContent)
            }
    }

    fun showLoginDialog(showUsernameOnly: Boolean = false) {
        val dialog = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.dialog_google_login, null)
        dialog.setContentView(view)
        loginDialog = dialog

        val btnGoogle = view.findViewById<Button>(R.id.btnGoogleSignIn)
        val layoutUsername = view.findViewById<LinearLayout>(R.id.layoutSetUsername)
        val etUsername = view.findViewById<EditText>(R.id.etUsernameInput)
        val btnConfirm = view.findViewById<Button>(R.id.btnConfirmUsername)

        val layoutAuthDivider = view.findViewById<View>(R.id.layoutAuthDivider)
        val layoutEmailAuth = view.findViewById<View>(R.id.layoutEmailAuth)
        val etAuthDisplayName = view.findViewById<EditText>(R.id.etAuthDisplayName)
        val etAuthEmail = view.findViewById<EditText>(R.id.etAuthEmail)
        val etAuthPassword = view.findViewById<EditText>(R.id.etAuthPassword)
        val tvAuthError = view.findViewById<TextView>(R.id.tvAuthError)
        val btnSubmitEmailAuth = view.findViewById<Button>(R.id.btnSubmitEmailAuth)
        val tvToggleAuthMode = view.findViewById<TextView>(R.id.tvToggleAuthMode)
        val tvForgotPassword = view.findViewById<TextView>(R.id.tvForgotPassword)

        var isSignUpMode = false

        fun updateAuthUIMode() {
            tvAuthError.visibility = View.GONE
            if (isSignUpMode) {
                etAuthDisplayName.visibility = View.VISIBLE
                btnSubmitEmailAuth.text = getString(R.string.sign_up_btn)
                tvToggleAuthMode.text = getString(R.string.already_have_account)
                tvForgotPassword.visibility = View.GONE
            } else {
                etAuthDisplayName.visibility = View.GONE
                btnSubmitEmailAuth.text = getString(R.string.sign_in_btn)
                tvToggleAuthMode.text = getString(R.string.dont_have_account)
                tvForgotPassword.visibility = View.VISIBLE
            }
        }
        updateAuthUIMode()

        tvToggleAuthMode.setOnClickListener {
            isSignUpMode = !isSignUpMode
            updateAuthUIMode()
        }

        tvForgotPassword.setOnClickListener {
            val email = etAuthEmail.text.toString().trim()
            if (email.isEmpty()) {
                Toast.makeText(requireContext(), getString(R.string.enter_email_to_reset), Toast.LENGTH_LONG).show()
                etAuthEmail.requestFocus()
                return@setOnClickListener
            }
            AuthManager.sendPasswordReset(email,
                onSuccess = {
                    Toast.makeText(requireContext(), getString(R.string.reset_link_sent), Toast.LENGTH_LONG).show()
                },
                onFailure = { err ->
                    tvAuthError.text = err
                    tvAuthError.visibility = View.VISIBLE
                }
            )
        }

        btnSubmitEmailAuth.setOnClickListener {
            val email = etAuthEmail.text.toString().trim()
            val password = etAuthPassword.text.toString().trim()
            val name = etAuthDisplayName.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                tvAuthError.text = getString(R.string.auth_field_empty)
                tvAuthError.visibility = View.VISIBLE
                return@setOnClickListener
            }

            tvAuthError.visibility = View.GONE
            btnSubmitEmailAuth.isEnabled = false

            if (isSignUpMode) {
                AuthManager.signUpWithEmail(email, password, name,
                    onSuccess = { isNewUser ->
                        btnSubmitEmailAuth.isEnabled = true
                        Toast.makeText(requireContext(), getString(R.string.auth_success), Toast.LENGTH_SHORT).show()
                        if (isNewUser) {
                            showUsernameSetupInDialog(suggestedUsername = name)
                        } else {
                            dialog.dismiss()
                            startListeningToPosts()
                        }
                    },
                    onFailure = { err ->
                        btnSubmitEmailAuth.isEnabled = true
                        tvAuthError.text = err
                        tvAuthError.visibility = View.VISIBLE
                    }
                )
            } else {
                AuthManager.signInWithEmail(email, password,
                    onSuccess = { isNewUser ->
                        btnSubmitEmailAuth.isEnabled = true
                        Toast.makeText(requireContext(), getString(R.string.auth_success), Toast.LENGTH_SHORT).show()
                        if (isNewUser) {
                            showUsernameSetupInDialog(suggestedUsername = email.substringBefore("@"))
                        } else {
                            dialog.dismiss()
                            startListeningToPosts()
                        }
                    },
                    onFailure = { err ->
                        btnSubmitEmailAuth.isEnabled = true
                        tvAuthError.text = err
                        tvAuthError.visibility = View.VISIBLE
                    }
                )
            }
        }

        if (showUsernameOnly) {
            btnGoogle.visibility = View.GONE
            layoutAuthDivider.visibility = View.GONE
            layoutEmailAuth.visibility = View.GONE
            layoutUsername.visibility = View.VISIBLE
        }

        btnGoogle.setOnClickListener {
            val client = AuthManager.getGoogleClient(requireContext())
            googleSignInLauncher.launch(client.signInIntent)
        }

        btnConfirm.setOnClickListener {
            val rawName = etUsername.text.toString().trim()
            if (rawName.isEmpty()) {
                etUsername.error = getString(R.string.username_example_hint)
                return@setOnClickListener
            }

            val username = rawName.lowercase().replace(" ", "_")
            val user = AuthManager.currentUser
            if (user == null) {
                Toast.makeText(requireContext(), getString(R.string.save_error), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnConfirm.isEnabled = false
            AuthManager.isUsernameAvailable(username) { available ->
                if (!available) {
                    btnConfirm.isEnabled = true
                    etUsername.error = getString(R.string.username_change_failed)
                    return@isUsernameAvailable
                }

                val displayName = user.displayName ?: username
                val email = user.email ?: ""

                AuthManager.registerUsername(user.uid, username, displayName, email) { ok, err ->
                    btnConfirm.isEnabled = true
                    if (ok) {
                        Toast.makeText(requireContext(), getString(R.string.photo_updated_success), Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                        showPostBottomSheet(user.uid, displayName, username, null)
                    } else {
                        Toast.makeText(requireContext(), err ?: getString(R.string.save_error), Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        dialog.show()
    }

    private fun showUsernameSetupInDialog(suggestedUsername: String = "") {
        loginDialog?.let { dialog ->
            dialog.findViewById<Button>(R.id.btnGoogleSignIn)?.visibility = View.GONE
            val layoutUsername = dialog.findViewById<LinearLayout>(R.id.layoutSetUsername)
            val etUsername = dialog.findViewById<EditText>(R.id.etUsernameInput)
            
            layoutUsername?.visibility = View.VISIBLE
            if (suggestedUsername.isNotEmpty()) {
                etUsername?.setText(suggestedUsername)
                etUsername?.setSelection(suggestedUsername.length)
            }
        }
    }
    fun filterByHashtag(tag: String) {
        if (!isAdded) return
        val rawTag = tag.trim()
        if (rawTag.isEmpty()) return
        selectMoodAction?.invoke(MoodType.ALL)
        currentTagFilter = rawTag
        updateTagSelectorUI()
        applyFilterAndSort()
        rvCommunity.scrollToPosition(0)
        val cleanTag = rawTag.replace(Regex("[^\\p{L}\\p{Nd}\\s]"), "").trim()
        val displayTag = if (cleanTag.isNotEmpty()) "#" + cleanTag.replace("\\s+".toRegex(), "_") else "#$rawTag"
        Toast.makeText(requireContext(), "تصفية حسب الهاشتاغ: $displayTag", Toast.LENGTH_SHORT).show()
    }

    fun onSearchQueryChanged(query: String) {
        val trimmed = query.trim().removePrefix("#").trim()
        if (trimmed.isEmpty()) {
            communityAdapter.updateData(allPostsList)
        } else {
            val filtered = allPostsList.filter { post ->
                post.content.contains(trimmed, ignoreCase = true) ||
                post.authorName.contains(trimmed, ignoreCase = true) ||
                post.authorUsername.contains(trimmed, ignoreCase = true) ||
                (post.tag ?: "").contains(trimmed, ignoreCase = true)
            }
            communityAdapter.updateData(filtered)
        }
    }

    // private fun showCommentsDialog(post: CommunityPost) {
    //     val dialog = BottomSheetDialog(requireContext())
    //     val view = layoutInflater.inflate(R.layout.dialog_comments, null)
    //     dialog.setContentView(view)

    //     dialog.setCanceledOnTouchOutside(false)

    //     dialog.setOnShowListener {
    //         val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
    //         bottomSheet?.let { sheet ->
    //             val behavior = BottomSheetBehavior.from(sheet)
    //             behavior.state = BottomSheetBehavior.STATE_EXPANDED
    //             behavior.skipCollapsed = true
    //             behavior.isHideable = true
    //         }
    //     }

    //     view.findViewById<TextView>(R.id.tvCommentPostAuthor)?.text = post.authorName
    //     view.findViewById<TextView>(R.id.tvCommentPostContent)?.text = post.content

    //     val rvComments = view.findViewById<RecyclerView>(R.id.rvComments)
    //     val etInput = view.findViewById<EditText>(R.id.etCommentInput)
    //     val btnSend = view.findViewById<ImageView>(R.id.btnSendComment)

    //     val user = FirebaseAuth.getInstance().currentUser
    //     val authorId = user?.uid ?: return
    //     val authorName = user.displayName ?: "فاعل خير"

    //     rvComments.layoutManager = LinearLayoutManager(requireContext())
    //     val commentsAdapter = CommentsAdapter(
    //         comments = emptyList(),
    //         currentUserId = authorId,
    //         postAuthorId = post.authorId,
    //         onUserClicked = { userId, userName ->
    //             dialog.dismiss()
    //             val intent = Intent(requireContext(), UserProfileActivity::class.java).apply {
    //                 putExtra("USER_ID", userId)
    //                 putExtra("USER_NAME", userName)
    //             }
    //             startActivity(intent)
    //         },
    //         onDeleteClicked = { comment ->
    //             FirestoreManager.deleteComment(post.id, comment.id) {}
    //         }
    //     )
    //     rvComments.adapter = commentsAdapter

    //     FirestoreManager.listenToComments(post.id) { list ->
    //         activity?.runOnUiThread {
    //             commentsAdapter.updateData(list)
    //             if (list.isNotEmpty()) {
    //                 rvComments.post {
    //                     rvComments.scrollToPosition(list.size - 1)
    //                 }
    //             }
    //         }
    //     }

    //     btnSend.setOnClickListener {
    //         val text = etInput.text?.toString()?.trim() ?: ""
    //         if (text.isEmpty()) return@setOnClickListener

    //         if (BadWordsFilter.containsBadWords(text)) {
    //             Toast.makeText(requireContext(), "التعليق يحتوي على كلمات غير لائقة", Toast.LENGTH_SHORT).show()
    //             return@setOnClickListener
    //         }

    //         btnSend.isEnabled = false
    //         val authorPhotoUrl = user.photoUrl?.toString() ?: ""

    //         FirestoreManager.addComment(post.id, authorId, authorName, authorPhotoUrl, text) { success ->
    //             btnSend.isEnabled = true
    //             if (success) {
    //                 etInput.setText("")
    //                 if (post.authorId != authorId) {
    //                     FirestoreManager.sendNotification(
    //                         recipientId = post.authorId,
    //                         senderId = authorId,
    //                         senderName = authorName,
    //                         type = "COMMENT",
    //                         postId = post.id,
    //                         message = "علّق $authorName على منشورك: \"${text.take(30)}...\""
    //                     )
    //                 }
    //             }
    //         }
    //     }

    //     dialog.show()
    // }
    private fun showCommentsDialog(post: CommunityPost) {
        val dialog = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.dialog_comments, null)
        dialog.setContentView(view)

        dialog.setCanceledOnTouchOutside(false)

        dialog.setOnShowListener {
            val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let { sheet ->
                val behavior = BottomSheetBehavior.from(sheet)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.skipCollapsed = true
                behavior.isHideable = true
            }
        }

        view.findViewById<TextView>(R.id.tvCommentPostAuthor)?.text = post.authorName
        view.findViewById<TextView>(R.id.tvCommentPostContent)?.text = post.content

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
        val authorId = user?.uid ?: return
        val fallbackAuthorName = user.displayName ?: "فاعل خير"

        // تحميل صورة المستخدم في شريط التعليق
        val prefs = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val currentPhotoUrl = prefs.getString("user_photo", "")?.takeIf { it.isNotEmpty() }
            ?: user.photoUrl?.toString() ?: ""
        ivCurrentUserAvatar?.loadUserAvatar(currentPhotoUrl)

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

        // تفاعل زر الإرسال مع الكتابة
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

        rvComments.layoutManager = LinearLayoutManager(requireContext())
        val commentsAdapter = CommentsAdapter(
            comments = emptyList(),
            currentUserId = authorId,
            postAuthorId = post.authorId,
            onUserClicked = { userId, userName ->
                dialog.dismiss()
                val intent = Intent(requireContext(), UserProfileActivity::class.java).apply {
                    putExtra("USER_ID", userId)
                    putExtra("USER_NAME", userName)
                }
                startActivity(intent)
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
                val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
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
                val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
                imm?.showSoftInput(etInput, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
            },
            onDeleteClicked = { comment ->
                AlertDialog.Builder(requireContext())
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
            activity?.runOnUiThread {
                val displayList = if (list.isEmpty() && (post.id.startsWith("vpost_") || post.id.startsWith("curated_"))) {
                    val p1 = VirtualCommunityManager.getPersonaForId(post.id.hashCode() + 1)
                    val p2 = VirtualCommunityManager.getPersonaForId(post.id.hashCode() + 3)
                    listOf(
                        Comment(
                            id = "seed_c1_${post.id}",
                            authorId = p1.id,
                            authorName = p1.name,
                            authorPhotoUrl = p1.avatarResource,
                            content = VirtualCommunityManager.getContextualComment(post.content, post.tag),
                            timestamp = post.getTimeMillis() + 180000L
                        ),
                        Comment(
                            id = "seed_c2_${post.id}",
                            authorId = p2.id,
                            authorName = p2.name,
                            authorPhotoUrl = p2.avatarResource,
                            content = "ما شاء الله، ذوق راقٍ واختيار مميز",
                            timestamp = post.getTimeMillis() + 450000L
                        )
                    )
                } else {
                    list
                }
                commentsAdapter.updateData(displayList)
                if (displayList.isEmpty()) {
                    layoutEmptyComments?.visibility = View.VISIBLE
                    tvCommentsCountHeader?.text = String.format(getString(R.string.comments_count_format), 0)
                } else {
                    layoutEmptyComments?.visibility = View.GONE
                    tvCommentsCountHeader?.text = String.format(getString(R.string.comments_count_format), displayList.size)
                    rvComments.post {
                        rvComments.scrollToPosition(displayList.size - 1)
                    }
                }
            }
        }

        btnSend.setOnClickListener {
            val text = etInput.text?.toString()?.trim() ?: ""
            if (text.isEmpty()) return@setOnClickListener

            if (BadWordsFilter.containsBadWords(text)) {
                Toast.makeText(requireContext(), getString(R.string.comment_bad_words), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnSend.isEnabled = false

            val currentUser = AuthManager.currentUser
            if (currentUser == null) {
                btnSend.isEnabled = true
                Toast.makeText(requireContext(), getString(R.string.login_required_to_comment), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // حالة تعديل التعليق
            val targetEditing = editingComment
            if (targetEditing != null) {
                FirestoreManager.updateComment(post.id, targetEditing.id, text) { success ->
                    btnSend.isEnabled = true
                    if (success) {
                        etInput.setText("")
                        resetBannerState()
                        Toast.makeText(requireContext(), getString(R.string.comment_edited_success), Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(), getString(R.string.comment_edit_error), Toast.LENGTH_SHORT).show()
                    }
                }
                return@setOnClickListener
            }

            // حالة إضافة تعليق جديد (أو رد)
            val currentAuthorId = currentUser.uid
            val currentAuthorName = currentUser.displayName ?: fallbackAuthorName
            val currentPhoto = prefs.getString("user_photo", "")?.takeIf { it.isNotEmpty() }
                ?: currentUser.photoUrl?.toString() ?: ""

            val replyAuthorName = replyingToComment?.authorName ?: ""
            val replyCommentId = replyingToComment?.id ?: ""
            val targetRepliedComment = replyingToComment

            FirestoreManager.addComment(
                postId = post.id,
                authorId = currentAuthorId,
                authorName = currentAuthorName,
                authorPhotoUrl = currentPhoto,
                content = text,
                replyToAuthorName = replyAuthorName,
                replyToCommentId = replyCommentId
            ) { success ->
                btnSend.isEnabled = true
                if (success) {
                    etInput.setText("")
                    resetBannerState()

                    // إشعار صاحب التعليق المردود عليه
                    if (targetRepliedComment != null && targetRepliedComment.authorId != currentAuthorId) {
                        FirestoreManager.sendNotification(
                            recipientId = targetRepliedComment.authorId,
                            senderId = currentAuthorId,
                            senderName = currentAuthorName,
                            type = "COMMENT",
                            postId = post.id,
                            message = "رد $currentAuthorName على تعليقك: \"${text.take(30)}...\""
                        )
                    }

                    // إشعار كاتب المنشور إذا كان شخصاً آخر
                    if (post.authorId != currentAuthorId && post.authorId != targetRepliedComment?.authorId) {
                        FirestoreManager.sendNotification(
                            recipientId = post.authorId,
                            senderId = currentAuthorId,
                            senderName = currentAuthorName,
                            type = "COMMENT",
                            postId = post.id,
                            message = "علّق $currentAuthorName على منشورك: \"${text.take(30)}...\""
                        )
                    }

                    if (post.id.startsWith("vpost_") || post.id.startsWith("curated_")) {
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            if (isAdded) {
                                val replyText = listOf(
                                    "شكراً جزيلاً على ذوقك وكلماتك الطيبة",
                                    "تسلم، أسعدني تعليقك الراقي جداً",
                                    "شكراً لك، دمت بألف خير وسعادة",
                                    "مرورك العطر زاد المنشور جمالاً، شكراً لك"
                                ).random()
                                FirestoreManager.addComment(
                                    postId = post.id,
                                    authorId = post.authorId,
                                    authorName = post.authorName,
                                    authorPhotoUrl = post.authorPhotoUrl,
                                    content = replyText,
                                    replyToAuthorName = currentAuthorName
                                ) {}
                            }
                        }, 9000L)
                    }
                } else {
                    Toast.makeText(requireContext(), getString(R.string.comment_error), Toast.LENGTH_SHORT).show()
                }
            }
        }

        dialog.show()
    }
    private fun showEditDialog(post: CommunityPost) {
        viewLifecycleOwner.lifecycleScope.launch {
            val user = AuthManager.getConfirmedUser() ?: return@launch
            val authorId = user.uid
            val authorName = user.displayName ?: post.authorName
            showPostBottomSheet(authorId, authorName, post.authorUsername, post)
        }
    }

    private fun confirmDelete(postId: String) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.post_action_delete)
            .setMessage(R.string.confirm_delete_post)
            .setPositiveButton(R.string.post_action_delete) { _, _ ->
                FirestoreManager.deleteCommunityPost(postId) { success ->
                    val msg = if (success) getString(R.string.delete_button) else getString(R.string.save_error)
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(R.string.no, null)
            .show()
    }

    private fun showReportDialog(postId: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            val user = AuthManager.getConfirmedUser(timeoutMs = 500)
            if (user == null) {
                Toast.makeText(requireContext(), getString(R.string.login_required_report), Toast.LENGTH_SHORT).show()
                showLoginDialog()
                return@launch
            }

            val reporterId = user.uid

        val reasons = arrayOf(
            getString(R.string.report_reason_inappropriate),
            getString(R.string.report_reason_profanity),
            getString(R.string.report_reason_religion),
            getString(R.string.report_reason_hate),
            getString(R.string.report_reason_spam),
            getString(R.string.report_reason_other)
        )

        var selectedReasonIndex = 0

        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.report_post_title))
            .setSingleChoiceItems(reasons, selectedReasonIndex) { _, which ->
                selectedReasonIndex = which
            }
            .setPositiveButton(getString(R.string.send_report)) { _, _ ->
                val chosenReason = reasons[selectedReasonIndex]
                FirestoreManager.reportCommunityPost(postId, reporterId, chosenReason) { success ->
                    val msg = if (success) {
                        getString(R.string.report_received_success)
                    } else {
                        getString(R.string.report_error)
                    }
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
        }
    }
    private fun showPostBottomSheet(
        authorId: String,
        authorName: String,
        authorUsername: String,
        editingPost: CommunityPost?,
        initialTag: String = "",
        initialContent: String = ""
    ) {
        CommunityPostDialogHelper.show(
            context = requireContext(),
            authorId = authorId,
            authorName = authorName,
            authorUsername = authorUsername,
            editingPost = editingPost,
            initialTag = initialTag,
            initialContent = initialContent,
            onAuthRequired = { showLoginDialog() }
        )
    }
}