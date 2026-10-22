package com.manchorati.www

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.widget.ImageView
import androidx.lifecycle.lifecycleScope
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch

class CommunityFragment : Fragment() {

    private lateinit var rvCommunity: RecyclerView
    private lateinit var fabAddPost: FloatingActionButton
    private lateinit var communityAdapter: CommunityAdapter
    private lateinit var swipeRefreshCommunity: androidx.swiperefreshlayout.widget.SwipeRefreshLayout
    private lateinit var layoutEmptyCommunity: View
    private lateinit var tvEmptyTitle: TextView
    private lateinit var tvEmptySubtitle: TextView

    private lateinit var btnSortLatest: TextView
    private lateinit var btnSortTrending: TextView

    private var currentTagFilter: String = "🌟 الكل"
    private var isTrendingSort: Boolean = false
    private val tagChips = mutableListOf<TextView>()

    private var allPostsList: List<CommunityPost> = emptyList()
    private var deviceId: String = ""
    private var loginDialog: BottomSheetDialog? = null
    private var postsListener: ListenerRegistration? = null

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
        btnSortLatest = view.findViewById(R.id.btnSortLatest)
        btnSortTrending = view.findViewById(R.id.btnSortTrending)

        swipeRefreshCommunity.setColorSchemeColors(Color.parseColor("#0284C7"))
        swipeRefreshCommunity.setOnRefreshListener {
            startListeningToPosts()
        }

        setupSortButtons()
        setupTagChips(view)

        rvCommunity.layoutManager = LinearLayoutManager(requireContext())

        communityAdapter = CommunityAdapter(
            posts = emptyList(),
            currentUserId = getActiveUserId(),
            onLikeClicked = { post, isLiked ->
                val currentUser = AuthManager.currentUser ?: com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                if (currentUser == null) {
                    Toast.makeText(requireContext(), "يرجى تسجيل الدخول أولاً للإعجاب بالمنشور", Toast.LENGTH_SHORT).show()
                    showLoginDialog()
                } else {
                    val uid = currentUser.uid
                    FirestoreManager.toggleLike(post.id, uid, isLiked) {
                        if (!isLiked && post.authorId != uid) {
                            val senderName = currentUser.displayName ?: "فاعل خير"
                            FirestoreManager.sendNotification(
                                recipientId = post.authorId,
                                senderId = uid,
                                senderName = senderName,
                                type = "LIKE",
                                postId = post.id,
                                message = "أعجب $senderName بمنشورك"
                            )
                        }
                    }
                }
            },
            onCommentClicked = { post ->
                val currentUser = AuthManager.currentUser
                if (currentUser == null) {
                    Toast.makeText(requireContext(), "يرجى تسجيل الدخول أولاً للتعليق على المنشور", Toast.LENGTH_SHORT).show()
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
            onReportClicked = { post -> showReportDialog(post.id) }
        )
        rvCommunity.adapter = communityAdapter

        fabAddPost.setOnClickListener {
            checkUserAndOpenPostDialog()
        }

        return view
    }

    private fun setupSortButtons() {
        btnSortLatest.setOnClickListener {
            if (!isTrendingSort) return@setOnClickListener
            isTrendingSort = false
            btnSortLatest.setBackgroundResource(R.drawable.bg_segment_btn_active)
            btnSortLatest.setTextColor(Color.WHITE)
            btnSortTrending.setBackgroundColor(Color.TRANSPARENT)
            btnSortTrending.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.text_secondary))
            applyFilterAndSort()
        }

        btnSortTrending.setOnClickListener {
            if (isTrendingSort) return@setOnClickListener
            isTrendingSort = true
            btnSortTrending.setBackgroundResource(R.drawable.bg_segment_btn_active)
            btnSortTrending.setTextColor(Color.WHITE)
            btnSortLatest.setBackgroundColor(Color.TRANSPARENT)
            btnSortLatest.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.text_secondary))
            applyFilterAndSort()
        }
    }

    private fun setupTagChips(view: View) {
        val hsvTags = view.findViewById<android.widget.HorizontalScrollView>(R.id.hsvTags)
        hsvTags?.setOnTouchListener { v, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN, android.view.MotionEvent.ACTION_MOVE -> {
                    v.parent?.requestDisallowInterceptTouchEvent(true)
                }
                android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                    v.parent?.requestDisallowInterceptTouchEvent(false)
                }
            }
            false
        }

        val chipIds = listOf(
            R.id.chipTagAll to "🌟 الكل",
            R.id.chipTagWisdom to "💭 حكم وأقوال",
            R.id.chipTagThoughts to "❤️ خواطر",
            R.id.chipTagDua to "🌿 أدعية وأذكار",
            R.id.chipTagMotivation to "💡 تحفيز وتطوير",
            R.id.chipTagHumor to "🎭 فكاهة وضحك",
            R.id.chipTagPoetry to "📜 شعر وأدب",
            R.id.chipTagCoffee to "☕ قهوة ومساء",
            R.id.chipTagGeneral to "🌸 عام",
            R.id.chipTagSaved to "🔖 المحفوظات"
        )

        tagChips.clear()
        for ((chipId, tagLabel) in chipIds) {
            val chip = view.findViewById<TextView>(chipId) ?: continue
            tagChips.add(chip)
            chip.setOnClickListener {
                currentTagFilter = tagLabel
                updateTagChipsUI()
                applyFilterAndSort()
            }
        }
        updateTagChipsUI()
    }

    private fun updateTagChipsUI() {
        for (chip in tagChips) {
            val isSelected = chip.text.toString() == currentTagFilter
            if (isSelected) {
                chip.setBackgroundResource(R.drawable.bg_category_chip_active)
                chip.setTextColor(Color.WHITE)
                chip.setTypeface(null, android.graphics.Typeface.BOLD)
            } else {
                chip.setBackgroundResource(R.drawable.bg_category_chip_idle)
                chip.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.text_secondary))
                chip.setTypeface(null, android.graphics.Typeface.NORMAL)
            }
        }
    }

    private fun applyFilterAndSort() {
        var list = allPostsList

        // Tag filter
        if (currentTagFilter == "🔖 المحفوظات") {
            val savedIds = BookmarkManager.getBookmarkedPostIds(requireContext())
            list = list.filter { savedIds.contains(it.id) }
        } else if (currentTagFilter != "🌟 الكل") {
            list = list.filter { it.tag == currentTagFilter }
        }

        // Sort
        list = if (isTrendingSort) {
            list.sortedWith(compareByDescending<CommunityPost> { it.likesCount }.thenByDescending { it.getTimeMillis() })
        } else {
            list.sortedByDescending { it.getTimeMillis() }
        }

        communityAdapter.updateData(list)

        // Empty state UI
        if (list.isEmpty()) {
            layoutEmptyCommunity.visibility = View.VISIBLE
            if (currentTagFilter == "🔖 المحفوظات") {
                tvEmptyTitle.text = "لا توجد منشورات محفوظة"
                tvEmptySubtitle.text = "احفظ المنشورات التي تنال إعجابك بالضغط على أيقونة 🔖 للرجوع إليها في أي وقت!"
            } else if (currentTagFilter != "🌟 الكل") {
                tvEmptyTitle.text = "لا توجد منشورات في قسم ($currentTagFilter)"
                tvEmptySubtitle.text = "كن أول من ينشر في هذا القسم وشارك أفكارك مع الجميع!"
            } else {
                tvEmptyTitle.text = "لا توجد منشورات حالياً"
                tvEmptySubtitle.text = "كن أول من ينشر في المجتمع وتفاعل مع الآخرين!"
            }
        } else {
            layoutEmptyCommunity.visibility = View.GONE
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        startListeningToPosts()
        observeAuthState()
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
                    allPostsList = list
                    applyFilterAndSort()

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
                }
            }
        )
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
    }

    override fun onResume() {
        super.onResume()
        // تحديث معرّف المستخدم في الـ Adapter لضمان تلوين القلوب حسب الحساب المتصل حالياً أو تفريغها عند الخروج
        communityAdapter.updateCurrentUserId(getActiveUserId())
        if (currentTagFilter == "🔖 المحفوظات") {
            applyFilterAndSort()
        }
    }

    fun handleNotificationClick(postId: String, type: String) {
        if (postId.isEmpty()) return

        val targetIndex = allPostsList.indexOfFirst { it.id == postId }
        if (targetIndex != -1) {
            (rvCommunity.layoutManager as? LinearLayoutManager)?.scrollToPositionWithOffset(targetIndex, 20)

            val targetPost = allPostsList[targetIndex]
            if (type == "COMMENT") {
                rvCommunity.postDelayed({
                    val currentUser = FirebaseAuth.getInstance().currentUser
                    if (currentUser == null) {
                        showLoginDialog()
                    } else {
                        showCommentsDialog(targetPost)
                    }
                }, 150)
            }
        } else {
            pendingNotificationPostId = postId
            pendingNotificationType = type
        }
    }

    private fun checkUserAndOpenPostDialog() {
        val user = AuthManager.currentUser
        if (user == null) {
            Toast.makeText(requireContext(), "يرجى تسجيل الدخول أولاً لإضافة منشور", Toast.LENGTH_SHORT).show()
            showLoginDialog()
            return
        }

        val baseUsername = AuthManager.generateBaseUsername(user.displayName, user.email)
        val defaultDisplayName = user.displayName ?: "مستخدم"

        FirebaseFirestore.getInstance().collection("users").document(user.uid).get()
            .addOnSuccessListener { doc ->
                if (!isAdded) return@addOnSuccessListener
                val username = doc.getString("username")
                if (doc.exists() && !username.isNullOrEmpty()) {
                    val displayName = doc.getString("displayName")
                        ?.takeIf { it.isNotBlank() }
                        ?: defaultDisplayName
                    showPostBottomSheet(user.uid, displayName, username, null)
                } else {
                    showPostBottomSheet(user.uid, defaultDisplayName, baseUsername, null)
                }
            }
            .addOnFailureListener {
                if (!isAdded) return@addOnFailureListener
                showPostBottomSheet(user.uid, defaultDisplayName, baseUsername, null)
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

        if (showUsernameOnly) {
            btnGoogle.visibility = View.GONE
            layoutUsername.visibility = View.VISIBLE
        }

        btnGoogle.setOnClickListener {
            val client = AuthManager.getGoogleClient(requireContext())
            googleSignInLauncher.launch(client.signInIntent)
        }

        btnConfirm.setOnClickListener {
            val rawName = etUsername.text.toString().trim()
            if (rawName.isEmpty()) {
                etUsername.error = "الرجاء كتابة اسم المستخدم"
                return@setOnClickListener
            }

            val username = rawName.lowercase().replace(" ", "_")
            val user = AuthManager.currentUser
            if (user == null) {
                Toast.makeText(requireContext(), "خطأ: لم يتم تسجيل الدخول", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnConfirm.isEnabled = false
            AuthManager.isUsernameAvailable(username) { available ->
                if (!available) {
                    btnConfirm.isEnabled = true
                    etUsername.error = "اسم المستخدم مأخوذ بالفعل، جرب غيره"
                    return@isUsernameAvailable
                }

                val displayName = user.displayName ?: username
                val email = user.email ?: ""

                AuthManager.registerUsername(user.uid, username, displayName, email) { ok, err ->
                    btnConfirm.isEnabled = true
                    if (ok) {
                        Toast.makeText(requireContext(), "تم إعداد الحساب بنجاح!", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                        showPostBottomSheet(user.uid, displayName, username, null)
                    } else {
                        Toast.makeText(requireContext(), err ?: "فشل حفظ الاسم", Toast.LENGTH_LONG).show()
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
    fun onSearchQueryChanged(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            communityAdapter.updateData(allPostsList)
        } else {
            val filtered = allPostsList.filter { post ->
                post.content.contains(trimmed, ignoreCase = true) ||
                post.authorName.contains(trimmed, ignoreCase = true) ||
                post.authorUsername.contains(trimmed, ignoreCase = true)
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

        // شريط الرد على تعليق محدد
        val layoutReplyBanner = view.findViewById<View>(R.id.layoutReplyBanner)
        val tvReplyBannerText = view.findViewById<TextView>(R.id.tvReplyBannerText)
        val btnCancelReply = view.findViewById<ImageView>(R.id.btnCancelReply)

        var replyingToComment: Comment? = null

        btnCancelReply.setOnClickListener {
            replyingToComment = null
            layoutReplyBanner.visibility = View.GONE
            etInput.hint = "اكتب تعليقاً..."
        }

        val user = AuthManager.currentUser
        val authorId = user?.uid ?: return
        
        // احتفظنا بهذا كاسم احتياطي في حال فشل الاتصال بقاعدة البيانات
        val fallbackAuthorName = user.displayName ?: "فاعل خير"

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
                layoutReplyBanner.visibility = View.VISIBLE
                tvReplyBannerText.text = "↩️ الرد على ${comment.authorName}"
                etInput.hint = "اكتب ردك على ${comment.authorName}..."
                etInput.requestFocus()
                val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
                imm?.showSoftInput(etInput, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
            },
            onDeleteClicked = { comment ->
                FirestoreManager.deleteComment(post.id, comment.id) {}
            }
        )
        rvComments.adapter = commentsAdapter

        FirestoreManager.listenToComments(post.id) { list ->
            activity?.runOnUiThread {
                commentsAdapter.updateData(list)
                if (list.isNotEmpty()) {
                    rvComments.post {
                        rvComments.scrollToPosition(list.size - 1)
                    }
                }
            }
        }

        btnSend.setOnClickListener {
            val text = etInput.text?.toString()?.trim() ?: ""
            if (text.isEmpty()) return@setOnClickListener

            if (BadWordsFilter.containsBadWords(text)) {
                Toast.makeText(requireContext(), "التعليق يحتوي على كلمات غير لائقة", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnSend.isEnabled = false

            val user = AuthManager.currentUser
            if (user == null) {
                btnSend.isEnabled = true
                Toast.makeText(requireContext(), "يرجى تسجيل الدخول أولاً للتعليق", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val currentAuthorId = user.uid
            val currentAuthorName = user.displayName ?: fallbackAuthorName
            val currentPhotoUrl = user.photoUrl?.toString() ?: ""

            val replyAuthorName = replyingToComment?.authorName ?: ""
            val replyCommentId = replyingToComment?.id ?: ""
            val targetRepliedComment = replyingToComment

            FirestoreManager.addComment(
                postId = post.id,
                authorId = currentAuthorId,
                authorName = currentAuthorName,
                authorPhotoUrl = currentPhotoUrl,
                content = text,
                replyToAuthorName = replyAuthorName,
                replyToCommentId = replyCommentId
            ) { success ->
                btnSend.isEnabled = true
                if (success) {
                    etInput.setText("")
                    replyingToComment = null
                    layoutReplyBanner.visibility = View.GONE
                    etInput.hint = "اكتب تعليقاً..."

                    // إشعار صاحب التعليق المردود عليه
                    if (targetRepliedComment != null && targetRepliedComment.authorId != currentAuthorId) {
                        FirestoreManager.sendNotification(
                            recipientId = targetRepliedComment.authorId,
                            senderId = currentAuthorId,
                            senderName = currentAuthorName,
                            type = "COMMENT",
                            postId = post.id,
                            message = "رد $currentAuthorName على تعليقك: \"${text.take(30)}...\" 💬"
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
                } else {
                    Toast.makeText(requireContext(), "تعذر إرسال التعليق، يرجى المحاولة لاحقاً", Toast.LENGTH_SHORT).show()
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
            .setTitle("حذف المنشور")
            .setMessage("هل أنت متأكد من حذف هذا المنشور؟")
            .setPositiveButton("حذف") { _, _ ->
                FirestoreManager.deleteCommunityPost(postId) { success ->
                    val msg = if (success) "تم الحذف بنجاح" else "فشل حذف المنشور"
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }

    private fun showReportDialog(postId: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            val user = AuthManager.getConfirmedUser(timeoutMs = 500)
            if (user == null) {
                Toast.makeText(requireContext(), "يرجى تسجيل الدخول أولاً للإبلاغ", Toast.LENGTH_SHORT).show()
                showLoginDialog()
                return@launch
            }

            val reporterId = user.uid

        val reasons = arrayOf(
            "محتوى غير لائق أو إباحي",
            "ألفاظ نابية وشتائم",
            "إساءة دينية أو ازدراء أديان",
            "خطاب كراهية وتنمر",
            "نشر محتوى مضلل أو احتيال",
            "محتوى مزعج (سبام)",
            "أخرى"
        )

        var selectedReasonIndex = 0

        AlertDialog.Builder(requireContext())
            .setTitle("سبب الإبلاغ عن هذا المنشور")
            .setSingleChoiceItems(reasons, selectedReasonIndex) { _, which ->
                selectedReasonIndex = which
            }
            .setPositiveButton("إرسال البلاغ") { _, _ ->
                val chosenReason = reasons[selectedReasonIndex]
                FirestoreManager.reportCommunityPost(postId, reporterId, chosenReason) { success ->
                    val msg = if (success) "تم استلام البلاغ، شكراً لحرصك على أمان المجتمع" else "حدث خطأ أثناء الإبلاغ"
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("إلغاء", null)
            .show()
        }
    }
    private fun showPostBottomSheet(
        authorId: String,
        authorName: String,
        authorUsername: String,
        editingPost: CommunityPost?
    ) {
        CommunityPostDialogHelper.show(
            context = requireContext(),
            authorId = authorId,
            authorName = authorName,
            authorUsername = authorUsername,
            editingPost = editingPost,
            onAuthRequired = { showLoginDialog() }
        )
    }
}