package com.manchorati.www

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
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
                    // مرحلة 1: دفعة تشجيعية فورية خفيفة (3 إعجابات بعد 5 دقائق)
                    performBatchLikes(db, postId, authorId, currentLikedBy, count = 3)
                }

                2 -> {
                    // مرحلة 2: دفعة ثانية طبيعية (3 إعجابات بعد 20 دقيقة)
                    performBatchLikes(db, postId, authorId, currentLikedBy, count = 3)
                }

                3 -> {
                    // المرحلة 3: إضافة تعليق تشجيعي وسياقي مناسب للمنشور + إعجابين
                    val postRef = db.collection("community_posts").document(postId)
                    val commentsSnap = postRef.collection("comments").get().await()
                    val existingComments = commentsSnap.documents.mapNotNull { it.getString("content") }
                    val existingAuthorIds = commentsSnap.documents.mapNotNull { it.getString("authorId") }.toSet()
                    val botIds = VirtualCommunityManager.PERSONAS.map { it.id }.toSet()
                    val hasBotComment = existingAuthorIds.any { botIds.contains(it) }

                    if (!hasBotComment && commentsSnap.size() < 2) {
                        val commenterPersona = VirtualCommunityManager.PERSONAS
                            .filter { it.id != authorId && !existingAuthorIds.contains(it.id) }
                            .shuffled()
                            .firstOrNull() ?: VirtualCommunityManager.PERSONAS[0]

                        val commentText = VirtualCommunityManager.getContextualComment(content, tag, existingComments)

                        val commentMap = hashMapOf<String, Any>(
                            "authorId" to commenterPersona.id,
                            "authorName" to commenterPersona.name,
                            "authorPhotoUrl" to commenterPersona.avatarResource,
                            "content" to commentText,
                            "replyToAuthorName" to "",
                            "replyToCommentId" to "",
                            "timestamp" to System.currentTimeMillis(),
                            "likesCount" to 0L,
                            "likedBy" to emptyList<String>()
                        )

                        postRef.collection("comments").add(commentMap).await()
                        val newTotal = (commentsSnap.size() + 1).toLong()
                        postRef.set(mapOf("commentsCount" to newTotal), SetOptions.merge()).await()

                        FirestoreManager.sendNotification(
                            recipientId = authorId,
                            senderId = commenterPersona.id,
                            senderName = commenterPersona.name,
                            type = "COMMENT",
                            postId = postId,
                            message = "${commenterPersona.name} علّق على منشورك: \"${commentText.take(35)}...\""
                        )
                    }

                    // إضافة إعجابين متزامنين مع التعليق
                    performBatchLikes(db, postId, authorId, currentLikedBy, count = 2)
                }

                4 -> {
                    // مرحلة 4: دفعة مسائية طبيعية (4 إعجابات بعد 4 ساعات)
                    performBatchLikes(db, postId, authorId, currentLikedBy, count = 4)
                }

                5 -> {
                    // مرحلة 5: دفعة اليوم التالي (4 إعجابات بعد 20 ساعة)
                    performBatchLikes(db, postId, authorId, currentLikedBy, count = 4)
                }

                6 -> {
                    // مرحلة 6: دفعة اليوم الثاني (3 إعجابات بعد 36 ساعة)
                    performBatchLikes(db, postId, authorId, currentLikedBy, count = 3)
                }
            }

            return Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error executing auto-engagement stage $stage for post $postId", e)
            return Result.failure()
        }
    }

    private suspend fun performBatchLikes(
        db: FirebaseFirestore,
        postId: String,
        authorId: String,
        currentLikedBy: List<String>,
        count: Int
    ) {
        val availablePersonas = VirtualCommunityManager.PERSONAS
            .filter { !currentLikedBy.contains(it.id) && it.id != authorId }
            .shuffled()
            .take(count)

        if (availablePersonas.isEmpty()) return

        val now = System.currentTimeMillis()
        val personaIds = availablePersonas.map { it.id }.toTypedArray()
        val updateMap = mutableMapOf<String, Any>(
            "likedBy" to FieldValue.arrayUnion(*personaIds),
            "likesCount" to FieldValue.increment(availablePersonas.size.toLong()),
            "lastLikedAt" to now
        )
        availablePersonas.forEach { p ->
            updateMap["likesTimeline.${p.id}"] = now
        }

        db.collection("community_posts").document(postId).update(updateMap).await()

        val mainPersona = availablePersonas.first()
        val isFemale = isFemalePersona(mainPersona.name)
        val notifMessage = if (availablePersonas.size > 1) {
            val othersCount = availablePersonas.size - 1
            if (isFemale) {
                "${mainPersona.name} و $othersCount آخرون أُعجبوا بمنشورك"
            } else {
                "${mainPersona.name} و $othersCount آخرون أعجبوا بمنشورك"
            }
        } else {
            if (isFemale) "${mainPersona.name} أُعجبت بمنشورك" else "${mainPersona.name} أعجب بمنشورك"
        }

        FirestoreManager.sendNotification(
            recipientId = authorId,
            senderId = mainPersona.id,
            senderName = mainPersona.name,
            type = "LIKE",
            postId = postId,
            message = notifMessage
        )
    }

    private fun isFemalePersona(personaName: String): Boolean {
        return listOf(
            "سارة", "إلهام", "مريم", "كوثر", "فاطمة", "هدى", "سناء", "زينب", "وفاء", "نادية",
            "سلمى", "شيماء", "سكينة", "إيمان", "أسماء"
        ).any { personaName.contains(it) }
    }
}
