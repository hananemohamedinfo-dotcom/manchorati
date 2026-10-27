package com.manchorati.www

import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions

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
                    post.getTimeMillis()
                }
                onUpdate(list)
            }
    }

    fun publishCommunityPost(
        authorId: String,
        authorName: String,
        authorUsername: String,
        authorPhotoUrl: String,
        content: String,
        bgId: String,
        fontId: String,
        textColor: String = "",
        tag: String = "",
        authorBadge: String = "",
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val resolvedBadge = if (authorBadge.isNotEmpty()) {
            authorBadge
        } else {
            BadgeHelper.getCachedBadge(authorId)?.id ?: ""
        }

        val postMap = hashMapOf(
            "authorId" to authorId,
            "authorName" to authorName,
            "authorUsername" to authorUsername,
            "authorPhotoUrl" to authorPhotoUrl,
            "content" to content,
            "bgId" to bgId,
            "fontId" to fontId,
            "textColor" to textColor,
            "tag" to tag,
            "authorBadge" to resolvedBadge,
            "likesCount" to 0,
            "commentsCount" to 0,
            "likedBy" to emptyList<String>(),
            "timestamp" to System.currentTimeMillis()
        )

        db.collection("community_posts")
            .add(postMap)
            .addOnSuccessListener { docRef ->
                notifyFollowersOfNewPost(authorId, authorName, docRef.id)
                onSuccess()
            }
            .addOnFailureListener { onFailure(it) }
    }
    fun publishCommunityVideoPost(
        authorId: String,
        authorName: String,
        authorUsername: String,
        authorPhotoUrl: String,
        content: String,
        bgMotionId: String,
        audioPreset: String,
        animStyle: String = "typewriter",
        videoRatio: String = "9:16",
        bgId: String = "card_crimson_brand",
        fontId: String = "tajawal",
        textColor: String = "#FFFFFF",
        tag: String = "فيديو",
        authorBadge: String = "",
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val resolvedBadge = if (authorBadge.isNotEmpty()) {
            authorBadge
        } else {
            BadgeHelper.getCachedBadge(authorId)?.id ?: ""
        }

        val postMap = hashMapOf(
            "authorId" to authorId,
            "authorName" to authorName,
            "authorUsername" to authorUsername,
            "authorPhotoUrl" to authorPhotoUrl,
            "content" to content,
            "bgId" to bgId,
            "fontId" to fontId,
            "textColor" to textColor,
            "tag" to tag,
            "authorBadge" to resolvedBadge,
            "likesCount" to 0L,
            "commentsCount" to 0L,
            "likedBy" to emptyList<String>(),
            "timestamp" to System.currentTimeMillis(),
            "postType" to "VIDEO",
            "bgMotionId" to bgMotionId,
            "audioPreset" to audioPreset,
            "animStyle" to animStyle,
            "videoRatio" to videoRatio
        )

        db.collection("community_posts")
            .add(postMap)
            .addOnSuccessListener { docRef ->
                notifyFollowersOfNewPost(authorId, authorName, docRef.id)
                onSuccess()
            }
            .addOnFailureListener { onFailure(it) }
    }


    private fun notifyFollowersOfNewPost(authorId: String, authorName: String, postId: String) {
        if (authorId.isEmpty() || postId.isEmpty()) return
        db.collection("users").document(authorId).get()
            .addOnSuccessListener { snapshot ->
                if (snapshot != null && snapshot.exists()) {
                    val followersRaw = snapshot.get("followers")
                    val followersList = when (followersRaw) {
                        is List<*> -> followersRaw.mapNotNull { it?.toString() }
                        else -> emptyList()
                    }
                    for (followerId in followersList) {
                        if (followerId.isNotEmpty() && followerId != authorId) {
                            sendNotification(
                                recipientId = followerId,
                                senderId = authorId,
                                senderName = authorName,
                                type = "NEW_POST",
                                postId = postId,
                                message = "نشر $authorName منشوراً جديداً"
                            )
                        }
                    }
                }
            }
    }

    fun publishCommunityPost(
        authorId: String,
        authorName: String,
        authorUsername: String,
        authorPhotoUrl: String,
        content: String,
        bgId: String,
        fontId: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        publishCommunityPost(
            authorId, authorName, authorUsername, authorPhotoUrl,
            content, bgId, fontId,
            textColor = "", tag = "",
            onSuccess = onSuccess, onFailure = onFailure
        )
    }

    fun updateCommunityPost(
        postId: String,
        content: String,
        bgId: String,
        fontId: String,
        textColor: String = "",
        tag: String = "",
        onComplete: (Boolean) -> Unit
    ) {
        val updates = mutableMapOf<String, Any>(
            "content" to content,
            "bgId" to bgId,
            "fontId" to fontId
        )
        if (textColor.isNotEmpty()) updates["textColor"] = textColor
        if (tag.isNotEmpty()) updates["tag"] = tag

        db.collection("community_posts").document(postId)
            .update(updates)
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }

    fun updateCommunityPost(
        postId: String,
        content: String,
        bgId: String,
        fontId: String,
        onComplete: (Boolean) -> Unit
    ) {
        updateCommunityPost(postId, content, bgId, fontId, textColor = "", tag = "", onComplete = onComplete)
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

    fun toggleLike(postId: String, userId: String, isLiked: Boolean, onComplete: () -> Unit = {}) {
        if (userId.isEmpty() || postId.isEmpty()) return

        val docRef = db.collection("community_posts").document(postId)

        val updates: Map<String, Any> = if (isLiked) {
            mapOf(
                "likedBy" to FieldValue.arrayRemove(userId),
                "likesCount" to FieldValue.increment(-1)
            )
        } else {
            mapOf(
                "likedBy" to FieldValue.arrayUnion(userId),
                "likesCount" to FieldValue.increment(1)
            )
        }

        docRef.update(updates)
            .addOnSuccessListener {
                onComplete()
            }
            .addOnFailureListener { e ->
                Log.e("FirestoreManager", "update failed for post $postId, falling back to set merge", e)
                docRef.set(updates, SetOptions.merge())
                    .addOnCompleteListener {
                        onComplete()
                    }
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
                        val replyToAuthorName = doc.getString("replyToAuthorName") ?: ""
                        val replyToCommentId = doc.getString("replyToCommentId") ?: ""

                        val timeVal = when (val raw = doc.get("timestamp")) {
                            is Number -> raw.toLong()
                            is com.google.firebase.Timestamp -> raw.toDate().time
                            else -> System.currentTimeMillis()
                        }

                        val isEdited = doc.getBoolean("isEdited") ?: false

                        val comment = Comment(
                            id = doc.id,
                            authorId = authorId,
                            authorName = authorName,
                            authorPhotoUrl = authorPhotoUrl,
                            content = content,
                            replyToAuthorName = replyToAuthorName,
                            replyToCommentId = replyToCommentId,
                            timestamp = timeVal,
                            isEdited = isEdited
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
        replyToAuthorName: String = "",
        replyToCommentId: String = "",
        onComplete: (Boolean) -> Unit
    ) {
        val commentMap = hashMapOf<String, Any>(
            "authorId" to authorId,
            "authorName" to authorName,
            "authorPhotoUrl" to authorPhotoUrl,
            "content" to content,
            "replyToAuthorName" to replyToAuthorName,
            "replyToCommentId" to replyToCommentId,
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

    fun addComment(
        postId: String,
        authorId: String,
        authorName: String,
        authorPhotoUrl: String,
        content: String,
        onComplete: (Boolean) -> Unit
    ) {
        addComment(postId, authorId, authorName, authorPhotoUrl, content, "", "", onComplete)
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

    fun updateComment(postId: String, commentId: String, newContent: String, onComplete: (Boolean) -> Unit) {
        val postRef = db.collection("community_posts").document(postId)
        postRef.collection("comments").document(commentId)
            .update(
                mapOf(
                    "content" to newContent,
                    "isEdited" to true
                )
            )
            .addOnSuccessListener { onComplete(true) }
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
        val targetUserRef = db.collection("users").document(targetUserId)
        val currentUserRef = db.collection("users").document(currentUserId)

        db.runTransaction { transaction ->
            val targetSnapshot = transaction.get(targetUserRef)
            val followers = (targetSnapshot.get("followers") as? List<*>)?.mapNotNull { it?.toString() }?.toMutableList() ?: mutableListOf()

            val currentSnapshot = transaction.get(currentUserRef)
            val following = (currentSnapshot.get("following") as? List<*>)?.mapNotNull { it?.toString() }?.toMutableList() ?: mutableListOf()

            if (isFollowing) {
                followers.remove(currentUserId)
                following.remove(targetUserId)
            } else {
                if (!followers.contains(currentUserId)) followers.add(currentUserId)
                if (!following.contains(targetUserId)) following.add(targetUserId)
            }

            transaction.update(targetUserRef, "followers", followers)
            transaction.update(targetUserRef, "followersCount", followers.size.toLong())

            transaction.update(currentUserRef, "following", following)
            transaction.update(currentUserRef, "followingCount", following.size.toLong())
        }.addOnSuccessListener { onComplete(true) }
            .addOnFailureListener {
                // If current user doc doesn't have following field or transaction fails, update individually
                val targetUpdates = if (isFollowing) {
                    mapOf("followers" to FieldValue.arrayRemove(currentUserId), "followersCount" to FieldValue.increment(-1))
                } else {
                    mapOf("followers" to FieldValue.arrayUnion(currentUserId), "followersCount" to FieldValue.increment(1))
                }
                val currentUpdates = if (isFollowing) {
                    mapOf("following" to FieldValue.arrayRemove(targetUserId), "followingCount" to FieldValue.increment(-1))
                } else {
                    mapOf("following" to FieldValue.arrayUnion(targetUserId), "followingCount" to FieldValue.increment(1))
                }
                targetUserRef.set(targetUpdates, SetOptions.merge())
                currentUserRef.set(currentUpdates, SetOptions.merge())
                onComplete(true)
            }
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

    fun deleteNotification(notificationId: String, onComplete: ((Boolean) -> Unit)? = null) {
        if (notificationId.isEmpty()) return
        db.collection("notifications").document(notificationId)
            .delete()
            .addOnSuccessListener { onComplete?.invoke(true) }
            .addOnFailureListener { onComplete?.invoke(false) }
    }

    fun clearAllNotifications(userId: String, onComplete: ((Boolean) -> Unit)? = null) {
        db.collection("notifications")
            .whereEqualTo("recipientId", userId)
            .get()
            .addOnSuccessListener { snapshot ->
                val batch = db.batch()
                for (doc in snapshot.documents) {
                    batch.delete(doc.reference)
                }
                batch.commit()
                    .addOnSuccessListener { onComplete?.invoke(true) }
                    .addOnFailureListener { onComplete?.invoke(false) }
            }
            .addOnFailureListener { onComplete?.invoke(false) }
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

    fun deleteUserCompleteData(userId: String, onComplete: (Boolean) -> Unit) {
        if (userId.isEmpty()) {
            onComplete(false)
            return
        }
        db.collection("users").document(userId).delete()
            .addOnCompleteListener {
                db.collection("community_posts")
                    .whereEqualTo("authorId", userId)
                    .get()
                    .addOnSuccessListener { snapshot ->
                        if (snapshot.isEmpty) {
                            onComplete(true)
                        } else {
                            val batch = db.batch()
                            for (doc in snapshot.documents) {
                                batch.delete(doc.reference)
                            }
                            batch.commit().addOnCompleteListener {
                                onComplete(true)
                            }
                        }
                    }
                    .addOnFailureListener {
                        onComplete(true)
                    }
            }
    }
}
