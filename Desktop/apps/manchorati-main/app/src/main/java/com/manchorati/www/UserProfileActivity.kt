package com.manchorati.www

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.auth.FirebaseAuth
import coil.load
import coil.transform.CircleCropTransformation

class UserProfileActivity : AppCompatActivity() {

    private lateinit var rvPosts: RecyclerView
    private lateinit var adapter: CommunityAdapter
    private lateinit var tvName: TextView
    private lateinit var tvProfileUsername: TextView
    private lateinit var btnLogoutTop: TextView
    private lateinit var tvBio: TextView
    private lateinit var tvPostsCount: TextView
    private lateinit var tvFollowersCount: TextView
    private lateinit var btnBack: ImageView
    private lateinit var btnProfileAction: ImageView
    private lateinit var ivProfileAvatar: ImageView

    private var targetUserId: String = ""
    private var targetUserName: String = ""
    private var activeUserId: String = ""
    private var currentUsername: String = ""
    private var isFollowing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_profile)

        val deviceFallback = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
        activeUserId = FirebaseAuth.getInstance().currentUser?.uid ?: deviceFallback

        val passedId = intent.getStringExtra("USER_ID")
        targetUserId = if (!passedId.isNullOrEmpty()) passedId else activeUserId
        targetUserName = intent.getStringExtra("USER_NAME") ?: "فاعل خير"

        initViews()
        setupRecyclerView()
        loadProfileData()
        listenToPosts()
    }

    private fun setupRecyclerView() {
        rvPosts.layoutManager = LinearLayoutManager(this)
        adapter = CommunityAdapter(
            posts = emptyList(),
            currentUserId = activeUserId,
            onLikeClicked = { post, isLiked ->
                FirestoreManager.toggleLike(post.id, activeUserId, isLiked) {}
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
        val input = EditText(this).apply {
            setText(post.content)
            setSelection(post.content.length)
        }
        AlertDialog.Builder(this)
            .setTitle("تعديل المنشور")
            .setView(input)
            .setPositiveButton("حفظ") { _, _ ->
                val newContent = input.text.toString().trim()
                if (newContent.isNotEmpty() && newContent != post.content) {
                    FirestoreManager.updateCommunityPost(post.id, newContent, post.bgId, post.fontId) { success ->
                        if (success) {
                            Toast.makeText(this, "تم تعديل المنشور بنجاح", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }

    private fun initViews() {
        tvName = findViewById(R.id.tvProfileName)
        tvProfileUsername = findViewById(R.id.tvProfileUsername)
        btnLogoutTop = findViewById(R.id.btnLogoutTop)
        tvBio = findViewById(R.id.tvProfileBio)
        tvPostsCount = findViewById(R.id.tvPostsCount)
        tvFollowersCount = findViewById(R.id.tvFollowersCount)
        btnProfileAction = findViewById(R.id.btnProfileAction)
        btnBack = findViewById(R.id.btnBack)
        rvPosts = findViewById(R.id.rvUserPosts)
        ivProfileAvatar = findViewById(R.id.ivProfileAvatar)

        tvName.text = targetUserName
        btnBack.setOnClickListener { finish() }

        val isMyProfile = (targetUserId.isNotEmpty() && targetUserId == activeUserId)

        if (isMyProfile) {
            ivProfileAvatar.setOnClickListener {
                showSelectAvatarDialog()
            }

            btnProfileAction.setImageResource(R.drawable.ic_edit)
            btnProfileAction.setColorFilter(Color.parseColor("#64748B"))
            btnProfileAction.setOnClickListener { showEditProfileDialog() }

            btnLogoutTop.visibility = View.VISIBLE
            btnLogoutTop.setOnClickListener {
                AlertDialog.Builder(this)
                    .setTitle("تسجيل الخروج")
                    .setMessage("هل أنت متأكد من رغبتك في تسجيل الخروج؟")
                    .setPositiveButton("خروج") { _, _ ->
                        FirebaseAuth.getInstance().signOut()
                        AuthManager.getGoogleClient(this).signOut().addOnCompleteListener {
                            getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                                .edit()
                                .remove("user_name")
                                .apply()
                            Toast.makeText(this, "تم تسجيل الخروج بنجاح", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                    }
                    .setNegativeButton("إلغاء", null)
                    .show()
            }
        } else {
            btnLogoutTop.visibility = View.GONE
            updateFollowButtonUI()

            btnProfileAction.setOnClickListener {
                btnProfileAction.isEnabled = false
                FirestoreManager.toggleFollow(activeUserId, targetUserId, isFollowing) { success ->
                    btnProfileAction.isEnabled = true
                    if (success) {
                        isFollowing = !isFollowing
                        updateFollowButtonUI()
                        val currentCount = tvFollowersCount.text.toString().toLongOrNull() ?: 0L
                        val newCount = if (isFollowing) currentCount + 1 else maxOf(0L, currentCount - 1)
                        tvFollowersCount.text = "$newCount"
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

                    currentUsername = data["username"] as? String ?: ""
                    
                    val photoUrl = data["photoUrl"] as? String ?: ""
                   ivProfileAvatar.loadUserAvatar(photoUrl)

                    val followersRaw = data["followers"]
                    val followersList = when (followersRaw) {
                        is List<*> -> followersRaw.mapNotNull { it?.toString() }
                        else -> emptyList()
                    }

                    val followersCount = (data["followersCount"] as? Number)?.toLong() ?: followersList.size.toLong()

                    tvName.text = name

                    if (currentUsername.isNotEmpty()) {
                        tvProfileUsername.visibility = View.VISIBLE
                        tvProfileUsername.text = "@$currentUsername"
                    } else {
                        tvProfileUsername.visibility = View.GONE
                    }

                    tvBio.text = bio
                    tvFollowersCount.text = "$followersCount"

                    if (targetUserId != activeUserId) {
                        isFollowing = followersList.contains(activeUserId)
                        updateFollowButtonUI()
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
        
        val googlePhotoUrl = FirebaseAuth.getInstance().currentUser?.photoUrl?.toString() ?: ""
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
            btnProfileAction.setImageResource(R.drawable.ic_user_check)
            btnProfileAction.setColorFilter(Color.parseColor("#10B981"))
        } else {
            btnProfileAction.setImageResource(R.drawable.ic_user_follow)
            btnProfileAction.setColorFilter(Color.parseColor("#EF4444"))
        }
    }

    private fun listenToPosts() {
        if (targetUserId.isEmpty()) return

        FirestoreManager.listenToUserPosts(targetUserId) { posts ->
            runOnUiThread {
                adapter.updateData(posts)
                tvPostsCount.text = "${posts.size}"
            }
        }
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

    private fun showCommentsDialog(post: CommunityPost) {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.dialog_comments, null)
        dialog.setContentView(view)

        view.findViewById<TextView?>(R.id.tvCommentPostAuthor)?.text = post.authorName
        view.findViewById<TextView?>(R.id.tvCommentPostContent)?.text = post.content

        val rvComments = view.findViewById<RecyclerView>(R.id.rvComments)
        val etInput = view.findViewById<EditText>(R.id.etCommentInput)
        val btnSend = view.findViewById<ImageView>(R.id.btnSendComment)

        val user = FirebaseAuth.getInstance().currentUser
        val currentUserName = user?.displayName 
            ?: getSharedPreferences("user_prefs", Context.MODE_PRIVATE).getString("user_name", "فاعل خير") 
            ?: "فاعل خير"

        rvComments.layoutManager = LinearLayoutManager(this)
        val commentsAdapter = CommentsAdapter(
            comments = emptyList(),
            currentUserId = activeUserId,
            postAuthorId = post.authorId,
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
           // جلب الصورة أو الأفاتار المختار من الإعدادات المحلية أولاً
        val prefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val authorPhotoUrl = prefs.getString("user_photo", "")?.takeIf { it.isNotEmpty() } 
            ?: FirebaseAuth.getInstance().currentUser?.photoUrl?.toString() 
            ?: ""
            FirestoreManager.addComment(
                postId = post.id,
                authorId = activeUserId,
                authorName = currentUserName,
                authorPhotoUrl = authorPhotoUrl,
                content = text
            ) { success ->
                btnSend.isEnabled = true
                if (success) {
                    etInput.setText("")
                    if (post.authorId != activeUserId) {
                        FirestoreManager.sendNotification(
                            recipientId = post.authorId,
                            senderId = activeUserId,
                            senderName = currentUserName,
                            type = "COMMENT",
                            postId = post.id,
                            message = "علّق $currentUserName على منشورك: \"${text.take(30)}...\""
                        )
                    }
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

        // 2. تحديث الصورة في التعليقات (إذا كانت التعليقات محفوظة في مجموعة منفصلة باسم comments)
        // قم بإلغاء تعليق هذا الكود إذا كانت تعليقاتك غير متداخلة داخل المنشور
         
        db.collection("comments")
            .whereEqualTo("authorId", activeUserId)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val batch = db.batch()
                for (document in querySnapshot.documents) {
                    batch.update(document.reference, "authorPhotoUrl", newPhotoUrl)
                }
                batch.commit()
            }
        
    }
}