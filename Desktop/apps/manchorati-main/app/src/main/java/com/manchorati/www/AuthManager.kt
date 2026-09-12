package com.manchorati.www

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.random.Random

object AuthManager {

    val auth: FirebaseAuth get() = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore get() = FirebaseFirestore.getInstance()

    fun getGoogleClient(context: Context): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        return GoogleSignIn.getClient(context, gso)
    }

    // تسجيل الدخول في فايربيز باستخدام حساب جوجل
    fun firebaseAuthWithGoogle(
        account: GoogleSignInAccount,
        onSuccess: (isNewUser: Boolean) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        auth.signInWithCredential(credential)
            .addOnSuccessListener { result ->
                val user = result.user
                if (user == null) {
                    onFailure("تعذر التعرف على المستخدم")
                    return@addOnSuccessListener
                }

                // فحص هل المستخدم مسجل مسبقاً ولديه username
                db.collection("users").document(user.uid).get()
                    .addOnSuccessListener { doc ->
                        val hasUsername = doc.exists() && doc.contains("username")
                        onSuccess(!hasUsername)
                    }
                    .addOnFailureListener {
                        onSuccess(true)
                    }
            }
            .addOnFailureListener { e ->
                onFailure(e.message ?: "فشل تسجيل الدخول")
            }
    }

    // التحقق من توفر اسم المستخدم
    fun isUsernameAvailable(username: String, onResult: (Boolean) -> Unit) {
        val clean = username.trim().lowercase()
        db.collection("usernames").document(clean).get()
            .addOnSuccessListener { doc -> onResult(!doc.exists()) }
            .addOnFailureListener { onResult(false) }
    }

    // حجز اسم المستخدم لأول مرة وحفظ بيانات المستخدم
    fun registerUsername(
        userId: String,
        username: String,
        displayName: String,
        email: String,
        onComplete: (Boolean, String?) -> Unit
    ) {
        val clean = username.trim().lowercase()
        val usernameRef = db.collection("usernames").document(clean)
        val userRef = db.collection("users").document(userId)

        db.runTransaction { transaction ->
            val snapshot = transaction.get(usernameRef)
            if (snapshot.exists()) {
                throw Exception("اسم المستخدم مأخوذ بالفعل")
            }
            transaction.set(usernameRef, mapOf("userId" to userId))
            transaction.set(
                userRef,
                mapOf(
                    "userId" to userId,
                    "username" to clean,
                    "displayName" to displayName,
                    "email" to email,
                    "createdAt" to System.currentTimeMillis()
                )
            )
        }.addOnSuccessListener {
            onComplete(true, null)
        }.addOnFailureListener { e ->
            onComplete(false, e.message)
        }
    }

    // تغيير اسم العرض فقط (Display Name) بحرية تامة
    fun updateDisplayName(
        userId: String,
        newDisplayName: String,
        onComplete: (Boolean, String?) -> Unit
    ) {
        val trimmed = newDisplayName.trim()
        if (trimmed.isEmpty()) {
            onComplete(false, "الاسم لا يمكن أن يكون فارغاً")
            return
        }

        db.collection("users").document(userId)
            .update("displayName", trimmed)
            .addOnSuccessListener { onComplete(true, null) }
            .addOnFailureListener { e -> onComplete(false, e.message) }
    }

 
fun changeUsername(
        userId: String,
        oldUsername: String,
        newUsername: String,
        onComplete: (Boolean, String?) -> Unit
    ) {
        val cleanOld = oldUsername.trim().lowercase()
        val cleanNew = newUsername.trim().lowercase()

        if (cleanNew.length < 3) {
            onComplete(false, "يجب أن يتكون اسم المستخدم من 3 أحرف على الأقل")
            return
        }
        val validRegex = "^[a-z0-9_.]+$".toRegex()
        if (!cleanNew.matches(validRegex)) {
            onComplete(false, "يسمح فقط بالحروف الإنجليزية، الأرقام، _ أو .")
            return
        }
        if (cleanNew == cleanOld) {
            onComplete(false, "هذا هو اسم المستخدم الحالي بالفعل")
            return
        }

        // 1. فحص وقائي في المنشورات القديمة التي لم تسجل في جدول usernames
        db.collection("community_posts")
            .whereEqualTo("authorUsername", cleanNew)
            .limit(1)
            .get()
            .addOnSuccessListener { postsSnapshot ->
                val takenByAnotherInPosts = postsSnapshot.documents.any { 
                    it.getString("authorId") != userId 
                }

                if (takenByAnotherInPosts) {
                    onComplete(false, "اسم المستخدم @$cleanNew مأخوذ بالفعل في منشورات سابقة")
                    return@addOnSuccessListener
                }

                // 2. إذا لم يكن مستخدماً في المنشورات القديمة، نتابع الفحص والحجز الذري في usernames
                val oldUsernameRef = db.collection("usernames").document(cleanOld)
                val newUsernameRef = db.collection("usernames").document(cleanNew)
                val userRef = db.collection("users").document(userId)

                db.runTransaction { transaction ->
                    val newSnapshot = transaction.get(newUsernameRef)
                    if (newSnapshot.exists()) {
                        val ownerId = newSnapshot.getString("userId")
                        if (ownerId != userId) {
                            throw Exception("اسم المستخدم @$cleanNew مأخوذ بالفعل")
                        }
                    }

                    transaction.set(newUsernameRef, mapOf("userId" to userId))

                    if (cleanOld.isNotEmpty() && cleanOld != cleanNew) {
                        transaction.delete(oldUsernameRef)
                    }

                    transaction.update(userRef, "username", cleanNew)
                }.addOnSuccessListener {
                    // 3. تحديث جميع منشورات المستخدم في المجتمع لتعكس الاسم الجديد
                    db.collection("community_posts")
                        .whereEqualTo("authorId", userId)
                        .get()
                        .addOnSuccessListener { querySnapshot ->
                            if (!querySnapshot.isEmpty) {
                                val batch = db.batch()
                                for (doc in querySnapshot.documents) {
                                    batch.update(doc.reference, "authorUsername", cleanNew)
                                }
                                batch.commit()
                                    .addOnSuccessListener { onComplete(true, null) }
                                    .addOnFailureListener { onComplete(true, null) }
                            } else {
                                onComplete(true, null)
                            }
                        }
                        .addOnFailureListener {
                            onComplete(true, null)
                        }
                }.addOnFailureListener { e ->
                    onComplete(false, e.message)
                }
            }
            .addOnFailureListener { e ->
                onComplete(false, e.message ?: "تعذر التحقق من توفر الاسم")
            }
    }

    // تسجيل الخروج الكامل (Firebase + Google)
    fun signOut(context: Context, onComplete: () -> Unit) {
        // 1. تسجيل الخروج من Firebase
        auth.signOut()

        // 2. تسجيل الخروج من حساب Google
        getGoogleClient(context).signOut().addOnCompleteListener {
            // 3. مسح الاسم المخزن محلياً
            context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                .edit()
                .remove("user_name")
                .apply()

            onComplete()
        }
    }
    fun generateBaseUsername(displayName: String?, email: String?): String {
    val source = when {
        !displayName.isNullOrBlank() -> displayName
        !email.isNullOrBlank() -> email.substringBefore("@")
        else -> "user"
    }
    // إبقاء الحروف الإنجليزية والأرقام فقط، وتحويل المسافات إلى _
    val clean = source.lowercase()
        .replace("\\s+".toRegex(), "_")
        .filter { it.isLetterOrDigit() || it == '_' }

    return if (clean.length >= 3) clean.take(12) else "user"
}

    // توليد يوزرنيم فريد تلقائياً مع فحص تكراره في Firestore
    fun generateUniqueUsername(
        base: String,
        onResult: (String) -> Unit
    ) {
        val candidate = "${base}_${Random.nextInt(100, 9999)}"
        isUsernameAvailable(candidate) { available ->
            if (available) {
                onResult(candidate)
            } else {
                // في حال وُجد مسبقاً، إعادة المحاولة برقم إضافي
                generateUniqueUsername(base, onResult)
            }
        }
    }
}