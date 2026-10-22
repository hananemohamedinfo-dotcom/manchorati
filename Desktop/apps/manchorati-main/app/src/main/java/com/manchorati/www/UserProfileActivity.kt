package com.manchorati.www

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
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
import com.google.firebase.firestore.ListenerRegistration
import coil.load
import coil.transform.CircleCropTransformation
import kotlinx.coroutines.launch
import java.util.Locale

class UserProfileActivity : AppCompatActivity() {

    private lateinit var rvPosts: RecyclerView
    private lateinit var adapter: CommunityAdapter
    private lateinit var tvName: TextView
    private lateinit var tvProfileUsername: TextView
    private lateinit var btnLogoutTop: TextView
    private lateinit var tvBio: TextView
    private lateinit var tvPostsCount: TextView
    private lateinit var tvFollowersCount: TextView
    private lateinit var tvFollowingCount: TextView
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
    private var fabAddPostProfile: View? = null
    private var postsListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_profile)

        val passedId = intent.getStringExtra("USER_ID") ?: ""
        targetUserId = passedId
        targetUserName = intent.getStringExtra("USER_NAME") ?: "فاعل خير"

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
                            tvBio.text = "لا توجد نبذة شخصية بعد."
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
                        Toast.makeText(this@UserProfileActivity, "يرجى تسجيل الدخول أولاً", Toast.LENGTH_SHORT).show()
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
                    Toast.makeText(this, "يرجى تسجيل الدخول أولاً للإعجاب", Toast.LENGTH_SHORT).show()
                }
            },
            onCommentClicked = { post -> showCommentsDialog(post) },
            onAuthorClicked = { _, _ -> },
            onEditClicked = { post -> showEditPostDialog(post) },
            onDeleteClicked = { post ->
                AlertDialog.Builder(this)
                    .setTitle("حذف المنشور")
                    .setMessage("هل أنت متأكد من رغبتك في حذف هذا المنشور نهائياً؟")
                    .setPositiveButton("حذف") { _, _ ->
                        FirestoreManager.deleteCommunityPost(post.id) { success ->
                            if (success) {
                                Toast.makeText(this, "تم حذف المنشور بنجاح", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    .setNegativeButton("إلغاء", null)
                    .show()
            },
            onReportClicked = { post ->
                FirestoreManager.reportPost(post.id, "إبلاغ من صفحة الملف الشخصي") {
                    Toast.makeText(this, "تم إرسال البلاغ للإدارة", Toast.LENGTH_SHORT).show()
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
        tvProfileUsername = findViewById(R.id.tvProfileUsername)
        btnLogoutTop = findViewById(R.id.btnLogoutTop)
        tvBio = findViewById(R.id.tvProfileBio)
        tvPostsCount = findViewById(R.id.tvPostsCount)
        tvFollowersCount = findViewById(R.id.tvFollowersCount)
        tvFollowingCount = findViewById(R.id.tvFollowingCount)
        btnProfileAction = findViewById(R.id.btnProfileAction)
        btnBack = findViewById(R.id.btnBack)
        rvPosts = findViewById(R.id.rvUserPosts)
        ivProfileAvatar = findViewById(R.id.ivProfileAvatar)
        fabAddPostProfile = findViewById(R.id.fabAddPostProfile)
        swipeRefresh = findViewById<SwipeRefreshLayout>(R.id.swipeRefreshUserPosts)
        layoutEmpty = findViewById<View>(R.id.layoutEmptyUserPosts)

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
                showSelectAvatarDialog()
            }

            btnProfileAction.text = "تعديل ✏️"
            btnProfileAction.setBackgroundResource(R.drawable.bg_tool_chip_idle)
            btnProfileAction.setTextColor(ContextCompat.getColor(this, R.color.text_primary))
            btnProfileAction.setOnClickListener { showEditProfileDialog() }

            btnLogoutTop.visibility = View.VISIBLE
            btnLogoutTop.setOnClickListener {
                AlertDialog.Builder(this)
                    .setTitle("تسجيل الخروج")
                    .setMessage("هل أنت متأكد من رغبتك في تسجيل الخروج؟")
                    .setPositiveButton("خروج") { _, _ ->
                        AuthManager.signOut(this) {
                            Toast.makeText(this, "تم تسجيل الخروج بنجاح", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                    }
                    .setNegativeButton("إلغاء", null)
                    .show()
            }
        } else {
            ivProfileAvatar.setOnClickListener(null)
            btnLogoutTop.visibility = View.GONE
            updateFollowButtonUI()

            btnProfileAction.setOnClickListener {
                if (activeUserId.isEmpty()) {
                    Toast.makeText(this, "يرجى تسجيل الدخول أولاً للمتابعة", Toast.LENGTH_SHORT).show()
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
                    val bio = data["bio"] as? String ?: "لا توجد نبذة شخصية بعد."
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
                            tvBio.text = "لا توجد نبذة شخصية بعد."
                        }
                    }
                }
            }
        }
    }

    private fun showSelectAvatarDialog() {
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
            updateUserProfilePhoto(googlePhotoUrl)
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
            R.id.ivAvatar8 to "avatar_8"
        )

        for ((viewId, avatarName) in avatars) {
            dialogView.findViewById<ImageView>(viewId)?.apply {
                val resId = resources.getIdentifier(avatarName, "drawable", packageName)
                if (resId != 0) {
                    setImageResource(resId)
                }
                
                setOnClickListener {
                    updateUserProfilePhoto(avatarName)
                    dialog.dismiss()
                }
            }
        }

        dialog.show()
    }

    private fun updateUserProfilePhoto(photoUrl: String) {
        FirestoreManager.updateUserProfilePhoto(this, activeUserId, photoUrl) { success ->
            if (success) {
                Toast.makeText(this, "تم تحديث الصورة الشخصية بنجاح", Toast.LENGTH_SHORT).show()
                updatePhotoInOldPosts(photoUrl)
                loadProfileData()
            } else {
                Toast.makeText(this, "فشل تحديث الصورة", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateFollowButtonUI() {
        if (isFollowing) {
            btnProfileAction.text = "متابع ✓"
            btnProfileAction.setBackgroundResource(R.drawable.bg_tool_chip_idle)
            btnProfileAction.setTextColor(Color.parseColor("#10B981"))
        } else {
            btnProfileAction.text = "+ متابعة"
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
            Toast.makeText(this, "يرجى تسجيل الدخول أولاً لإضافة منشور", Toast.LENGTH_SHORT).show()
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
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_profile, null)
        val etName = dialogView.findViewById<EditText>(R.id.etEditName)
        val etBio = dialogView.findViewById<EditText>(R.id.etEditBio)

        etName.setText(tvName.text.toString())
        etBio.setText(if (tvBio.text.toString() == "لا توجد نبذة شخصية بعد.") "" else tvBio.text.toString())

        AlertDialog.Builder(this)
            .setTitle("إعدادات الحساب")
            .setView(dialogView)
            .setNeutralButton("تغيير الصورة الشخصية") { _, _ ->
                showSelectAvatarDialog()
            }
            .setNegativeButton("تغيير اسم المستخدم (@)") { _, _ ->
                showChangeUsernameDialog()
            }
            .setPositiveButton("حفظ") { _, _ ->
                val newName = etName.text.toString().trim().ifEmpty { "فاعل خير" }
                val newBio = etBio.text.toString().trim()

                FirestoreManager.updateUserProfile(activeUserId, newName, newBio) { success ->
                    if (success) {
                        tvName.text = newName
                        tvBio.text = newBio.ifEmpty { "لا توجد نبذة شخصية بعد." }
                        getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                            .edit().putString("user_name", newName).apply()
                        Toast.makeText(this, "تم تحديث البيانات بنجاح", Toast.LENGTH_SHORT).show()
                        updateNameInCommunityPosts(newName)
                    }
                }
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }

    private fun showChangeUsernameDialog() {
        val input = EditText(this).apply {
            hint = "أدخل اسم مستخدم جديد (مثال: ahmed_99)"
            setText(currentUsername)
            setSingleLine()
        }

        AlertDialog.Builder(this)
            .setTitle("تعديل اسم المستخدم (@)")
            .setMessage("اسم المستخدم يجب أن يكون فريداً ولا يحتوي إلا على حروف إنجليزية، أرقام، أو _")
            .setView(input)
            .setPositiveButton("فحص وحفظ") { _, _ ->
                val requestedUsername = input.text.toString().trim()
                if (requestedUsername.isEmpty()) return@setPositiveButton

                AuthManager.changeUsername(
                    userId = activeUserId,
                    oldUsername = currentUsername,
                    newUsername = requestedUsername
                ) { success, errorMsg ->
                    runOnUiThread {
                        if (success) {
                            currentUsername = requestedUsername.lowercase()
                            tvProfileUsername.visibility = View.VISIBLE
                            tvProfileUsername.text = "@$currentUsername"
                            Toast.makeText(this, "تم تغيير اسم المستخدم بنجاح إلى @$currentUsername", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(this, errorMsg ?: "تعذر تغيير اسم المستخدم", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
            .setNegativeButton("إلغاء", null)
            .show()
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

        // سنحتفظ بهذا كاحتياط، لكننا سنجلب الاسم الفعلي من القاعدة قبل الإرسال
        val user = AuthManager.currentUser
        val currentUserName = user?.displayName 
            ?: getSharedPreferences("user_prefs", Context.MODE_PRIVATE).getString("user_name", "فاعل خير") 
            ?: "فاعل خير"

        rvComments.layoutManager = LinearLayoutManager(this)
        
        // >>> التعديل الأول: تفعيل النقر لفتح البروفايل <<<
        val commentsAdapter = CommentsAdapter(
            comments = emptyList(),
            currentUserId = activeUserId,
            postAuthorId = post.authorId,
            onUserClicked = { clickedUserId, clickedUserName ->
                // فتح صفحة الملف الشخصي عند النقر على صورة أو اسم صاحب التعليق
                val intent = android.content.Intent(this, UserProfileActivity::class.java).apply {
                    putExtra("USER_ID", clickedUserId)
                    putExtra("USER_NAME", clickedUserName)
                }
                startActivity(intent)
                dialog.dismiss() // إغلاق نافذة التعليقات
            },
            onDeleteClicked = { comment ->
                FirestoreManager.deleteComment(post.id, comment.id) {}
            }
        )
        rvComments.adapter = commentsAdapter

        FirestoreManager.listenToComments(post.id) { list ->
            runOnUiThread {
                commentsAdapter.updateData(list)
                if (list.isNotEmpty()) rvComments.scrollToPosition(list.size - 1)
            }
        }

        btnSend.setOnClickListener {
            val text = etInput.text.toString().trim()
            if (text.isEmpty()) return@setOnClickListener

            if (BadWordsFilter.containsBadWords(text)) {
                Toast.makeText(this, "التعليق يحتوي على كلمات غير لائقة", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnSend.isEnabled = false

            val user = AuthManager.currentUser
            if (user == null) {
                btnSend.isEnabled = true
                Toast.makeText(this@UserProfileActivity, "يرجى تسجيل الدخول أولاً للتعليق", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val currentActiveId = user.uid
            val currentAuthorName = user.displayName ?: currentUserName
            val currentPhotoUrl = user.photoUrl?.toString() ?: ""

            FirestoreManager.addComment(
                postId = post.id,
                authorId = currentActiveId,
                authorName = currentAuthorName,
                authorPhotoUrl = currentPhotoUrl,
                content = text
            ) { success ->
                btnSend.isEnabled = true
                if (success) {
                    etInput.setText("")
                    if (post.authorId != currentActiveId) {
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
                    Toast.makeText(this@UserProfileActivity, "تعذر إرسال التعليق، يرجى المحاولة لاحقاً", Toast.LENGTH_SHORT).show()
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
}