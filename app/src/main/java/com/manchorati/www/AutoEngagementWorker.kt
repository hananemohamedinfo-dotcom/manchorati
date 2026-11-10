package com.manchorati.www

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class AutoEngagementWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val TAG = "AutoEngagementWorker"

    override suspend fun doWork(): Result {
        val postId = inputData.getString("POST_ID") ?: return Result.failure()
        val authorId = inputData.getString("AUTHOR_ID") ?: return Result.failure()
        val authorName = inputData.getString("AUTHOR_NAME") ?: ""
        val content = inputData.getString("CONTENT") ?: ""
        val tag = inputData.getString("TAG") ?: ""
        val stage = inputData.getInt("STAGE", 1)

        val db = FirebaseFirestore.getInstance()

        try {
            // التحقق من وجود المنشور في Firestore قبل التفاعل
            val postSnapshot = db.collection("community_posts").document(postId).get().await()
            if (!postSnapshot.exists()) {
                Log.d(TAG, "Post $postId does not exist, skipping auto engagement")
                return Result.success()
            }

            val currentLikedBy = (postSnapshot.get("likedBy") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()

            when (stage) {
                1 -> {
                    // المرحلة 1: إضافة 2 إلى 3 إعجابات ذكية
                    val availablePersonas = VirtualCommunityManager.PERSONAS
                        .filter { !currentLikedBy.contains(it.id) && it.id != authorId }
                        .shuffled()
                        .take(3)

                    if (availablePersonas.isNotEmpty()) {
                        val personaIds = availablePersonas.map { it.id }
                        val now = System.currentTimeMillis()
                        val updateMap = mutableMapOf<String, Any>(
                            "likedBy" to FieldValue.arrayUnion(*personaIds.toTypedArray()),
                            "likesCount" to FieldValue.increment(personaIds.size.toLong()),
                            "lastLikedAt" to now
                        )
                        personaIds.forEach { pid ->
                            updateMap["likesTimeline.$pid"] = now
                        }
                        db.collection("community_posts").document(postId).update(updateMap).await()

                        val primaryPersona = availablePersonas[0]
                        val notifMessage = if (availablePersonas.size > 1) {
                            "${primaryPersona.name} وآخرون أُعجبوا بمنشورك"
                        } else {
                            "${primaryPersona.name} أعجب بمنشورك"
                        }

                        FirestoreManager.sendNotification(
                            recipientId = authorId,
                            senderId = primaryPersona.id,
                            senderName = primaryPersona.name,
                            type = "LIKE",
                            postId = postId,
                            message = notifMessage
                        )
                    }
                }

                2 -> {
                    // المرحلة 2: إضافة تعليق تشجيعي وسياقي مناسب للمنشور
                    val commenterPersona = VirtualCommunityManager.PERSONAS
                        .filter { it.id != authorId }
                        .shuffled()
                        .firstOrNull() ?: VirtualCommunityManager.PERSONAS[0]

                    val commentText = VirtualCommunityManager.getContextualComment(content, tag)

                    val commentMap = hashMapOf<String, Any>(
                        "authorId" to commenterPersona.id,
                        "authorName" to commenterPersona.name,
                        "authorPhotoUrl" to commenterPersona.avatarResource,
                        "content" to commentText,
                        "replyToAuthorName" to "",
                        "replyToCommentId" to "",
                        "timestamp" to System.currentTimeMillis()
                    )

                    val postRef = db.collection("community_posts").document(postId)
                    postRef.collection("comments").add(commentMap).await()
                    postRef.update("commentsCount", FieldValue.increment(1)).await()

                    FirestoreManager.sendNotification(
                        recipientId = authorId,
                        senderId = commenterPersona.id,
                        senderName = commenterPersona.name,
                        type = "COMMENT",
                        postId = postId,
                        message = "${commenterPersona.name} علّق على منشورك: \"${commentText.take(35)}...\""
                    )
                }

                3 -> {
                    // المرحلة 3: دفعة إعجابات ختامية لتثبيت التفاعل
                    val extraPersonas = VirtualCommunityManager.PERSONAS
                        .filter { !currentLikedBy.contains(it.id) && it.id != authorId }
                        .shuffled()
                        .take(2)

                    if (extraPersonas.isNotEmpty()) {
                        val ids = extraPersonas.map { it.id }
                        val now = System.currentTimeMillis()
                        val updateMap = mutableMapOf<String, Any>(
                            "likedBy" to FieldValue.arrayUnion(*ids.toTypedArray()),
                            "likesCount" to FieldValue.increment(ids.size.toLong()),
                            "lastLikedAt" to now
                        )
                        ids.forEach { pid ->
                            updateMap["likesTimeline.$pid"] = now
                        }
                        db.collection("community_posts").document(postId).update(updateMap).await()

                        val p = extraPersonas[0]
                        FirestoreManager.sendNotification(
                            recipientId = authorId,
                            senderId = p.id,
                            senderName = p.name,
                            type = "LIKE",
                            postId = postId,
                            message = "${p.name} أعجب بمنشورك"
                        )
                    }
                }
            }

            return Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error executing auto-engagement stage $stage for post $postId", e)
            return Result.failure()
        }
    }
}
