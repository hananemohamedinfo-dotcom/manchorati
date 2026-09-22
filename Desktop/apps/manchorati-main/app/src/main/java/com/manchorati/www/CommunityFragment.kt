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
import com.google.firebase.firestore.ListenerRegistration

class CommunityFragment : Fragment() {

    private lateinit var rvCommunity: RecyclerView
    private lateinit var fabAddPost: FloatingActionButton
    private lateinit var communityAdapter: CommunityAdapter

    private var allPostsList: List<CommunityPost> = emptyList()
    private var deviceId: String = ""
    private var loginDialog: BottomSheetDialog? = null

    private fun getActiveUserId(): String {
        return FirebaseAuth.getInstance().currentUser?.uid ?: ""
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

        rvCommunity.layoutManager = LinearLayoutManager(requireContext())

        communityAdapter = CommunityAdapter(
            posts = emptyList(),
            currentUserId = getActiveUserId(),
            onLikeClicked = { post, isLiked ->
                val currentUser = FirebaseAuth.getInstance().currentUser
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
                val currentUser = FirebaseAuth.getInstance().currentUser
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

        FirestoreManager.listenToCommunityPosts(
            onUpdate = { list ->
                activity?.runOnUiThread {
                    allPostsList = list
                    communityAdapter.updateData(list)
                }
            },
            onError = {}
        )

        fabAddPost.setOnClickListener {
            checkUserAndOpenPostDialog()
        }

        return view
    }

    override fun onResume() {
        super.onResume()
        // تحديث معرّف المستخدم في الـ Adapter لضمان تلوين القلوب حسب الحساب المتصل حالياً أو تفريغها عند الخروج
        communityAdapter.updateCurrentUserId(getActiveUserId())
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
            Toast.makeText(requireContext(), "المنشور غير متوفر حالياً", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkUserAndOpenPostDialog() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            showLoginDialog()
            return
        }

        FirebaseFirestore.getInstance().collection("users").document(user.uid).get()
            .addOnSuccessListener { doc ->
                val username = doc.getString("username")
                if (doc.exists() && !username.isNullOrEmpty()) {
                    val displayName = doc.getString("displayName") ?: user.displayName ?: "مستخدم"
                    showPostBottomSheet(user.uid, displayName, username, null)
                } else {
                    showLoginDialog(showUsernameOnly = true)
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "تعذر التحقق من الحساب", Toast.LENGTH_SHORT).show()
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
            val user = FirebaseAuth.getInstance().currentUser
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

        val user = FirebaseAuth.getInstance().currentUser
        val authorId = user?.uid ?: return
        val authorName = user.displayName ?: "فاعل خير"

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
            val authorPhotoUrl = user.photoUrl?.toString() ?: ""

            FirestoreManager.addComment(post.id, authorId, authorName, authorPhotoUrl, text) { success ->
                btnSend.isEnabled = true
                if (success) {
                    etInput.setText("")
                    if (post.authorId != authorId) {
                        FirestoreManager.sendNotification(
                            recipientId = post.authorId,
                            senderId = authorId,
                            senderName = authorName,
                            type = "COMMENT",
                            postId = post.id,
                            message = "علّق $authorName على منشورك: \"${text.take(30)}...\""
                        )
                    }
                }
            }
        }

        dialog.show()
    }

    private fun showEditDialog(post: CommunityPost) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val authorId = user.uid
        val authorName = user.displayName ?: post.authorName
        showPostBottomSheet(authorId, authorName, post.authorUsername, post)
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
        val reporterId = getActiveUserId()
        if (reporterId.isEmpty()) {
            Toast.makeText(requireContext(), "يرجى تسجيل الدخول أولاً للإبلاغ", Toast.LENGTH_SHORT).show()
            showLoginDialog()
            return
        }

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
    private fun showPostBottomSheet(
        authorId: String,
        authorName: String,
        authorUsername: String,
        editingPost: CommunityPost?
    ) {
        val dialog = BottomSheetDialog(requireContext())
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_post, null)
        dialog.setContentView(dialogView)

        dialog.setOnShowListener {
            val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let {
                val behavior = BottomSheetBehavior.from(it)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.skipCollapsed = true
            }
        }

        val previewContainer = dialogView.findViewById<LinearLayout>(R.id.previewContainer)
        val etContent = dialogView.findViewById<EditText>(R.id.etPostContent)
        val btnPublish = dialogView.findViewById<Button>(R.id.btnPublish)
        val layoutColors = dialogView.findViewById<LinearLayout>(R.id.layoutColorsContainer)
        val layoutFonts = dialogView.findViewById<LinearLayout>(R.id.layoutFontsContainer)

        var selectedBg = editingPost?.bgId ?: "#FFFFFF"
        var selectedFont = editingPost?.fontId ?: "tajawal"

        if (editingPost != null) {
            etContent.setText(editingPost.content)
            btnPublish.text = "حفظ التعديل"
        }

        fun updatePreview() {
            ThemeHelper.applyBackground(previewContainer, selectedBg, 16f)
            etContent.typeface = ThemeHelper.getFontTypeface(requireContext(), selectedFont)
            etContent.setTextColor(ThemeHelper.getTextColor(selectedBg))
        }
        updatePreview()

        val colorsList = listOf(
            "#FFFFFF",
            "#FF512F,#DD2476",
            "#2193B0,#6DD5ED",
            "#8A2387,#E94057",
            "#11998E,#38EF7D",
            "#232526,#414345",
            "#00c996,#003d4d",
            "#9400d3,#4b0082",
            "#29eac4,#4284db",
            "#009ffc,#1da1f2",
            "#0F766E",
            "#4338CA",
            "#D97706",
            "#7C3AED",
            "#BE123C"
        )

        layoutColors.removeAllViews()
        val density = resources.displayMetrics.density
        val sizePx = (38 * density).toInt()
        val marginPx = (8 * density).toInt()

        for (colorCode in colorsList) {
            val colorView = View(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(sizePx, sizePx).apply {
                    marginEnd = marginPx
                }
                ThemeHelper.applyBackground(this, colorCode, 10f)
                setOnClickListener {
                    selectedBg = colorCode
                    updatePreview()
                }
            }
            layoutColors.addView(colorView)
        }

        val fontsList = listOf(
            Pair("tajawal", "تجوال"),
            Pair("cairoplay", "كايرو"),
            Pair("arefruqaa", "رقعة"),
            Pair("kufam", "كوفام"),
            Pair("marhey", "مرحي"),
            Pair("notonastaliqurdu", "نستعليق")
        )

        layoutFonts.removeAllViews()
        for ((fontId, fontTitle) in fontsList) {
            val tvFont = TextView(requireContext()).apply {
                text = fontTitle
                textSize = 16f
                // setTextColor(Color.parseColor("#1E293B"))
                setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.text_secondary))
                typeface = ThemeHelper.getFontTypeface(requireContext(), fontId)
                setPadding((12 * density).toInt(), (6 * density).toInt(), (12 * density).toInt(), (6 * density).toInt())
                background = null
                setOnClickListener {
                    selectedFont = fontId
                    updatePreview()
                }
            }
            layoutFonts.addView(tvFont)
        }

        btnPublish.setOnClickListener {
            val content = etContent.text.toString().trim()
            if (content.isEmpty()) {
                etContent.error = "اكتب كلمات المنشور"
                return@setOnClickListener
            }

            if (BadWordsFilter.containsBadWords(content)) {
                Toast.makeText(requireContext(), "عذراً، يحتوي النص على كلمات مخالفة لسياسة النشر", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            btnPublish.isEnabled = false
            if (editingPost == null) {
                val prefs = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                val authorPhotoUrl = prefs.getString("user_photo", "") ?: FirebaseAuth.getInstance().currentUser?.photoUrl?.toString() ?: ""
                FirestoreManager.publishCommunityPost(
                    authorId = authorId,
                    authorName = authorName,
                    authorUsername = authorUsername,
                    authorPhotoUrl = authorPhotoUrl,
                    content = content,
                    bgId = selectedBg,
                    fontId = selectedFont,
                    onSuccess = {
                        dialog.dismiss()
                        Toast.makeText(requireContext(), "تم النشر بنجاح!", Toast.LENGTH_SHORT).show()
                    },
                    onFailure = {
                        btnPublish.isEnabled = true
                        Toast.makeText(requireContext(), "فشل النشر", Toast.LENGTH_SHORT).show()
                    }
                )
            } else {
                FirestoreManager.updateCommunityPost(editingPost.id, content, selectedBg, selectedFont) { ok ->
                    dialog.dismiss()
                    Toast.makeText(requireContext(), if (ok) "تم التعديل" else "فشل التعديل", Toast.LENGTH_SHORT).show()
                }
            }
        }

        dialog.show()
    }
}