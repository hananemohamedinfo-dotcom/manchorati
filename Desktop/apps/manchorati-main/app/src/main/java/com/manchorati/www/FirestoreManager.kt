package com.manchorati.www

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

object FirestoreManager {

    private val db = FirebaseFirestore.getInstance()

    fun listenToCommunityPosts(
        onUpdate: (List<CommunityPost>) -> Unit,
        onError: (Exception) -> Unit = {}
    ): ListenerRegistration {
        return db.collection("community_posts")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    onError(e)
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    val post = doc.toObject(CommunityPost::class.java)
                    post?.apply { id = doc.id }
                } ?: emptyList()
                onUpdate(list)
            }
    }

fun listenToUserPosts(
        userId: String,
        onUpdate: (List<CommunityPost>) -> Unit
    ): ListenerRegistration {
        return db.collection("community_posts")
            .whereEqualTo("authorId", userId)
            .addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) {
                    onUpdate(emptyList())
                    return@addSnapshotListener
                }
                val list = snapshot.documents.mapNotNull { doc ->
                    val post = doc.toObject(CommunityPost::class.java)
                    post?.apply { id = doc.id }
                }.sortedByDescending { post: CommunityPost ->
                    (post.timestamp as? Number)?.toLong() ?: 0L
                }
                onUpdate(list)
            }
    }

   fun publishCommunityPost(
        authorId: String,
        authorName: String,
        authorUsername: String,
        authorPhotoUrl: String, // استقبال رابط أو اسم الأفاتار
        content: String,
        bgId: String,
        fontId: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val postMap = hashMapOf(
            "authorId" to authorId,
            "authorName" to authorName,
            "authorUsername" to authorUsername,
            "authorPhotoUrl" to authorPhotoUrl, // تخزين الصورة في المنشور
            "content" to content,
            "bgId" to bgId,
            "fontId" to fontId,
            "likesCount" to 0,
            "commentsCount" to 0,
            "likedBy" to emptyList<String>(),
            "timestamp" to System.currentTimeMillis()
        )

        db.collection("community_posts")
            .add(postMap)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    fun updateCommunityPost(
        postId: String,
        content: String,
        bgId: String,
        fontId: String,
        onComplete: (Boolean) -> Unit
    ) {
        val updates = mapOf(
            "content" to content,
            "bgId" to bgId,
            "fontId" to fontId
        )
        db.collection("community_posts").document(postId)
            .update(updates)
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }

    fun updatePost(postId: String, content: String, onComplete: (Boolean) -> Unit) {
        db.collection("community_posts").document(postId)
            .update("content", content)
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }

    fun deleteCommunityPost(postId: String, onComplete: (Boolean) -> Unit) {
        db.collection("community_posts").document(postId)
            .delete()
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }

    fun deletePost(postId: String, onComplete: (Boolean) -> Unit) {
        deleteCommunityPost(postId, onComplete)
    }

    fun toggleLike(postId: String, userId: String, isLiked: Boolean, onComplete: () -> Unit) {
        if (userId.isEmpty() || postId.isEmpty()) return

        val docRef = db.collection("community_posts").document(postId)

        db.runTransaction { transaction ->
            val snapshot = transaction.get(docRef)
            val likedBy = (snapshot.get("likedBy") as? List<*>)?.mapNotNull { it?.toString() }?.toMutableList() 
                ?: mutableListOf()

            if (isLiked) {
                // إلغاء الإعجاب: نحذف المستخدم إذا كان موجوداً بالفعل
                likedBy.remove(userId)
            } else {
                // إضافة الإعجاب: نضيف المستخدم إذا لم يكن موجوداً
                if (!likedBy.contains(userId)) {
                    likedBy.add(userId)
                }
            }

            // حساب العداد مباشرة من عدد المعجبين الفعلي لضمان استحالة النزول تحت الصفر
            val newLikesCount = likedBy.size.toLong()

            transaction.update(docRef, mapOf(
                "likedBy" to likedBy,
                "likesCount" to newLikesCount
            ))
        }.addOnCompleteListener {
            onComplete()
        }
    }

    fun listenToComments(postId: String, onUpdate: (List<Comment>) -> Unit): ListenerRegistration {
        return db.collection("community_posts").document(postId)
            .collection("comments")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) {
                    onUpdate(emptyList())
                    return@addSnapshotListener
                }

                val commentsList = mutableListOf<Comment>()
                for (doc in snapshot.documents) {
                    try {
                        val authorId = doc.getString("authorId") ?: ""
                        val authorName = doc.getString("authorName") ?: "فاعل خير"
                        val authorPhotoUrl = doc.getString("authorPhotoUrl") ?: ""
                        val content = doc.getString("content") ?: ""

                        val timeVal = when (val raw = doc.get("timestamp")) {
                            is Number -> raw.toLong()
                            is com.google.firebase.Timestamp -> raw.toDate().time
                            else -> System.currentTimeMillis()
                        }

                        val comment = Comment(
                            id = doc.id,
                            authorId = authorId,
                            authorName = authorName,
                            authorPhotoUrl = authorPhotoUrl,
                            content = content,
                            timestamp = timeVal
                        )
                        commentsList.add(comment)
                    } catch (_: Exception) {
                    }
                }
                onUpdate(commentsList)
            }
    }

    fun addComment(
        postId: String,
        authorId: String,
        authorName: String,
        authorPhotoUrl: String,
        content: String,
        onComplete: (Boolean) -> Unit
    ) {
        val commentMap = hashMapOf(
            "authorId" to authorId,
            "authorName" to authorName,
            "authorPhotoUrl" to authorPhotoUrl,
            "content" to content,
            "timestamp" to System.currentTimeMillis()
        )

        val postRef = db.collection("community_posts").document(postId)
        postRef.collection("comments").add(commentMap)
            .addOnSuccessListener {
                postRef.update("commentsCount", FieldValue.increment(1))
                onComplete(true)
            }
            .addOnFailureListener { onComplete(false) }
    }

    fun deleteComment(postId: String, commentId: String, onComplete: (Boolean) -> Unit) {
        val postRef = db.collection("community_posts").document(postId)
        postRef.collection("comments").document(commentId).delete()
            .addOnSuccessListener {
                postRef.update("commentsCount", FieldValue.increment(-1))
                onComplete(true)
            }
            .addOnFailureListener { onComplete(false) }
    }

    fun getUserProfile(userId: String, onComplete: (Map<String, Any>?) -> Unit) {
        db.collection("users").document(userId).get()
            .addOnSuccessListener { doc -> onComplete(doc.data) }
            .addOnFailureListener { onComplete(null) }
    }

    fun updateUserProfile(userId: String, displayName: String, bio: String, onComplete: (Boolean) -> Unit) {
        val updates = mapOf(
            "displayName" to displayName,
            "bio" to bio
        )
        db.collection("users").document(userId)
            .update(updates)
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }

    fun toggleFollow(currentUserId: String, targetUserId: String, isFollowing: Boolean, onComplete: (Boolean) -> Unit) {
        val userRef = db.collection("users").document(targetUserId)
        db.runTransaction { transaction ->
            val snapshot = transaction.get(userRef)
            val followers = (snapshot.get("followers") as? List<*>)?.mapNotNull { it?.toString() }?.toMutableList() ?: mutableListOf()

            if (isFollowing) {
                followers.remove(currentUserId)
            } else {
                if (!followers.contains(currentUserId)) followers.add(currentUserId)
            }
            transaction.update(userRef, "followers", followers)
            transaction.update(userRef, "followersCount", followers.size.toLong())
        }.addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }

    fun listenToUserNotifications(userId: String, onUpdate: (List<AppNotification>) -> Unit): ListenerRegistration {
        return db.collection("notifications")
            .whereEqualTo("recipientId", userId)
            .addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) {
                    onUpdate(emptyList())
                    return@addSnapshotListener
                }

                val list = mutableListOf<AppNotification>()
                for (doc in snapshot.documents) {
                    try {
                        val timeVal = when (val raw = doc.get("timestamp")) {
                            is Number -> raw.toLong()
                            is com.google.firebase.Timestamp -> raw.toDate().time
                            else -> System.currentTimeMillis()
                        }

                        val notif = AppNotification(
                            id = doc.id,
                            recipientId = doc.getString("recipientId") ?: "",
                            senderId = doc.getString("senderId") ?: "",
                            senderName = doc.getString("senderName") ?: "",
                            type = doc.getString("type") ?: "",
                            postId = doc.getString("postId") ?: "",
                            message = doc.getString("message") ?: "",
                            isRead = doc.getBoolean("isRead") ?: false,
                            timestamp = timeVal
                        )
                        list.add(notif)
                    } catch (_: Exception) {}
                }
                list.sortByDescending { it.timestamp }
                onUpdate(list)
            }
    }

    fun markAllNotificationsAsRead(userId: String) {
        db.collection("notifications")
            .whereEqualTo("recipientId", userId)
            .whereEqualTo("isRead", false)
            .get()
            .addOnSuccessListener { snapshot ->
                val batch = db.batch()
                for (doc in snapshot.documents) {
                    batch.update(doc.reference, "isRead", true)
                }
                batch.commit()
            }
    }

    fun sendNotification(
        recipientId: String,
        senderId: String,
        senderName: String,
        type: String,
        postId: String,
        message: String
    ) {
        val notifMap = hashMapOf(
            "recipientId" to recipientId,
            "senderId" to senderId,
            "senderName" to senderName,
            "type" to type,
            "postId" to postId,
            "message" to message,
            "isRead" to false,
            "timestamp" to System.currentTimeMillis()
        )
        db.collection("notifications").add(notifMap)
    }

fun reportCommunityPost(postId: String, reporterId: String, reason: String, onComplete: (Boolean) -> Unit) {
        val reportMap = hashMapOf(
            "postId" to postId,
            "reporterId" to reporterId,
            "reason" to reason,
            "timestamp" to System.currentTimeMillis()
        )
        db.collection("reports").add(reportMap)
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }

    fun reportPost(postId: String, reason: String = "غير محدد", onComplete: (Boolean) -> Unit) {
        reportCommunityPost(postId, "anonymous", reason, onComplete)
    }
    fun updateUserProfilePhoto(context: android.content.Context, userId: String, photoUrl: String, onComplete: (Boolean) -> Unit) {
            db.collection("users").document(userId)
                .update("photoUrl", photoUrl)
                .addOnSuccessListener {
                    // حفظ الرابط أو اسم الأفاتار محلياً لتحديث المينيو والبروفايل فوراً
                    val prefs = context.getSharedPreferences("user_prefs", android.content.Context.MODE_PRIVATE)
                    prefs.edit().putString("user_photo", photoUrl).apply()
                    onComplete(true)
                }
                .addOnFailureListener { 
                    onComplete(false) 
                }
        }
}