/**
 * منشوراتي (MANCHORATI) - FIREBASE AUTH & FIRESTORE CORE CLIENT SDK
 * Complete Real-time Client for Web App, Authentication, User Profiles, and Admin Dashboard
 */

const firebaseConfig = {
    apiKey: "AIzaSyD6z0FAV1puTKcF9CN85UyvQ19uKoVyjNo",
    authDomain: "manchorati-4ace8.firebaseapp.com",
    projectId: "manchorati-4ace8",
    storageBucket: "manchorati-4ace8.firebasestorage.app",
    messagingSenderId: "52451964200",
    appId: "1:52451964200:web:manchorati-web"
};

// Initialize Firebase if not already initialized
if (!firebase.apps.length) {
    firebase.initializeApp(firebaseConfig);
}

const auth = firebase.auth();
const db = firebase.firestore();

// Enforce Local Persistence so sessions survive page refreshes and browser restarts
auth.setPersistence(firebase.auth.Auth.Persistence.LOCAL).catch((err) => {
    console.warn("Firebase setPersistence error:", err);
});

// Global Auth State
let currentAuthUser = null;
let currentProfileData = null;
let isCurrentAdmin = false;
let currentAdminRole = null; // 'super_admin' | 'moderator' | 'editor'

const SUPER_ADMIN_EMAILS = [
    'hananemohamedinfo@gmail.com',
    'admin@manchorati.com'
];

/**
 * Listen to Auth State Changes
 */
auth.onAuthStateChanged(async (user) => {
    currentAuthUser = user;
    if (user) {
        console.log("Logged in as:", user.email, user.uid);
        try {
            // 1. Fetch or create user profile from Firestore 'users/{uid}'
            await loadOrCreateUserProfile(user);

            // 2. Check Admin Status
            await checkAdminPermissions(user);

            // 3. Update UI across all pages
            updateNavbarAuthUI();
        } catch (err) {
            console.error("Error loading user profile or admin rights:", err);
            updateNavbarAuthUI();
        }
    } else {
        console.log("No user is currently signed in.");
        currentProfileData = null;
        isCurrentAdmin = false;
        currentAdminRole = null;
        updateNavbarAuthUI();
    }

    // Trigger custom event for other pages / dashboard
    window.dispatchEvent(new CustomEvent('manchorati-auth-changed', {
        detail: {
            user: currentAuthUser,
            profile: currentProfileData,
            isAdmin: isCurrentAdmin,
            role: currentAdminRole
        }
    }));
});

/**
 * Load or Create User Profile in Firestore
 */
async function loadOrCreateUserProfile(user) {
    const userDocRef = db.collection('users').doc(user.uid);
    const snap = await userDocRef.get();

    if (snap.exists) {
        currentProfileData = snap.data() || {};
        let needsSync = false;
        const syncUpdates = {};

        // Backfill name & photo from Google / Auth user if missing or generic
        if (!currentProfileData.authorName || currentProfileData.authorName === 'مستخدم منشوراتي') {
            if (user.displayName || user.email) {
                currentProfileData.authorName = user.displayName || user.email.split('@')[0];
                syncUpdates.authorName = currentProfileData.authorName;
                needsSync = true;
            }
        }
        if (!currentProfileData.authorPhotoUrl && user.photoURL) {
            currentProfileData.authorPhotoUrl = user.photoURL;
            syncUpdates.authorPhotoUrl = user.photoURL;
            needsSync = true;
        }

        if (needsSync) {
            userDocRef.set(syncUpdates, { merge: true }).catch(() => {});
        }
    } else {
        // Auto-generate profile from Google or Email info
        const cleanName = user.displayName || user.email.split('@')[0];
        const generatedUsername = cleanName.toLowerCase().replace(/[^a-z0-9_]/g, '_') + '_' + Math.floor(1000 + Math.random() * 9000);
        
        currentProfileData = {
            authorName: cleanName,
            authorUsername: generatedUsername,
            authorPhotoUrl: user.photoURL || '',
            authorBadge: 'كاتب جديد',
            authorBio: 'محب للكلمة الطيبة والاقتباسات الراقية في منشوراتي.',
            authorStreak: 1,
            totalLikes: 0,
            postsCount: 0,
            email: user.email,
            createdAt: Date.now(),
            isBlocked: false
        };

        await userDocRef.set(currentProfileData, { merge: true });
        
        // Also register username in 'usernames' collection
        await db.collection('usernames').doc(generatedUsername).set({
            userId: user.uid,
            createdAt: Date.now()
        }, { merge: true });
    }
}

/**
 * Check Admin Permissions
 */
async function checkAdminPermissions(user) {
    if (!user || !user.email) {
        isCurrentAdmin = false;
        currentAdminRole = null;
        return;
    }

    const emailLower = user.email.toLowerCase().trim();

    // Check hardcoded super admins
    if (SUPER_ADMIN_EMAILS.includes(emailLower)) {
        isCurrentAdmin = true;
        currentAdminRole = 'super_admin';
        return;
    }

    // Check 'admins' collection in Firestore by uid or email
    try {
        const adminDoc = await db.collection('admins').doc(user.uid).get();
        if (adminDoc.exists) {
            isCurrentAdmin = true;
            currentAdminRole = adminDoc.data().role || 'moderator';
            return;
        }

        // Fallback check by email query
        const querySnap = await db.collection('admins').where('email', '==', emailLower).limit(1).get();
        if (!querySnap.empty) {
            isCurrentAdmin = true;
            currentAdminRole = querySnap.docs[0].data().role || 'moderator';
            return;
        }

        isCurrentAdmin = false;
        currentAdminRole = null;
    } catch (err) {
        console.warn("Could not query admins collection:", err);
        // If super admin email matches, keep super_admin
        if (SUPER_ADMIN_EMAILS.includes(emailLower)) {
            isCurrentAdmin = true;
            currentAdminRole = 'super_admin';
        } else {
            isCurrentAdmin = false;
            currentAdminRole = null;
        }
    }
}

// Google Sign-In In-Progress Flag to prevent duplicate clicks & conflicting popup requests
let isGoogleSignInInProgress = false;

function showAuthError(msg) {
    const el = document.getElementById("authErrorMessage");
    if (el) {
        el.textContent = msg;
        el.style.display = "block";
    } else {
        showAppToast(msg, true);
    }
}

function clearAuthError() {
    const el = document.getElementById("authErrorMessage");
    if (el) {
        el.textContent = "";
        el.style.display = "none";
    }
}

/**
 * Sign in with Google (Popup)
 */
async function signInWithGoogle() {
    if (isGoogleSignInInProgress) {
        console.warn("Google sign-in already in progress, ignoring extra click.");
        return;
    }
    isGoogleSignInInProgress = true;
    clearAuthError();

    const gBtn = document.getElementById("btnGoogleAuth");
    let originalHtml = "";
    if (gBtn) {
        originalHtml = gBtn.innerHTML;
        gBtn.disabled = true;
        gBtn.style.opacity = "0.75";
        gBtn.style.cursor = "wait";
        gBtn.innerHTML = `
            <svg style="width:16px; height:16px; animation:spin 1s linear infinite;" viewBox="0 0 24 24"><circle cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" fill="none" opacity="0.25"/><path fill="currentColor" d="M4 12a8 8 0 018-8v8H4z"/></svg>
            <span>جاري الاتصال بـ Google...</span>
        `;
    }

    // Failsafe timer to unlock button if popup hangs or is closed without callback
    const safetyTimer = setTimeout(() => {
        if (isGoogleSignInInProgress) {
            isGoogleSignInInProgress = false;
            if (gBtn) {
                gBtn.disabled = false;
                gBtn.style.opacity = "1";
                gBtn.style.cursor = "pointer";
                if (originalHtml) gBtn.innerHTML = originalHtml;
            }
        }
    }, 12000);

    const provider = new firebase.auth.GoogleAuthProvider();
    provider.addScope('profile');
    provider.addScope('email');
    provider.setCustomParameters({ prompt: 'select_account' });

    try {
        const result = await auth.signInWithPopup(provider);
        clearTimeout(safetyTimer);
        showAppToast(`مرحباً بك مجدداً ${result.user.displayName || ''}! 🎉`);
        closeLoginModal();
        return result.user;
    } catch (err) {
        clearTimeout(safetyTimer);
        console.warn("Google sign in catch handled:", err);
        const code = (err && err.code) || '';
        const msg = (err && err.message) || '';

        // Harmless cancellations: user closed popup or duplicate popup was cancelled
        if (
            code === 'auth/popup-closed-by-user' || 
            code === 'auth/cancelled-popup-request' ||
            code === 'auth/user-cancelled' ||
            code === 'auth/popup-closed' ||
            msg.includes('cancelled-popup-request') ||
            msg.includes('popup-closed-by-user') ||
            msg.includes('user-cancelled')
        ) {
            console.log("Google popup cancelled or closed cleanly by user. No error displayed.");
            return null;
        } else if (code === 'auth/popup-blocked' || msg.includes('popup-blocked')) {
            showAuthError("⚠️ المتصفح حظر النافذة المنبثقة. يرجى السماح بالنوافذ المنبثقة (Pop-ups) من شريط العنوان للمتابعة.");
        } else if (code === 'auth/network-request-failed') {
            showAuthError("تعذر الاتصال بخوادم Google. يرجى التحقق من اتصال الإنترنت والمحاولة مرة أخرى.");
        } else {
            showAuthError("تعذر تسجيل الدخول بـ Google: " + (msg || code));
        }
        return null;
    } finally {
        clearTimeout(safetyTimer);
        isGoogleSignInInProgress = false;
        if (gBtn) {
            gBtn.disabled = false;
            gBtn.style.opacity = "1";
            gBtn.style.cursor = "pointer";
            if (originalHtml) gBtn.innerHTML = originalHtml;
        }
    }
}

/**
 * Sign in with Email and Password
 */
async function signInWithEmail(email, password) {
    clearAuthError();
    try {
        const result = await auth.signInWithEmailAndPassword(email.trim(), password);
        showAppToast("تم تسجيل الدخول بنجاح! 🌟");
        closeLoginModal();
        return result.user;
    } catch (err) {
        console.error("Email sign in error:", err);
        let msg = "البريد الإلكتروني أو كلمة المرور غير صحيحة.";
        if (err.code === 'auth/user-not-found') msg = "هذا الحساب غير موجود، يرجى إنشاء حساب جديد.";
        if (err.code === 'auth/wrong-password') msg = "كلمة المرور غير صحيحة.";
        if (err.code === 'auth/invalid-email') msg = "صيغة البريد الإلكتروني غير صحيحة.";
        if (err.code === 'auth/network-request-failed') msg = "مشكلة في الاتصال بالإنترنت، يرجى المحاولة ثانية.";
        showAuthError(msg);
        throw err;
    }
}

/**
 * Sign up with Email and Password
 */
async function signUpWithEmail(email, password, displayName, customUsername) {
    clearAuthError();
    try {
        const result = await auth.createUserWithEmailAndPassword(email.trim(), password);
        const user = result.user;
        
        await user.updateProfile({ displayName: displayName.trim() });
        
        const usernameClean = (customUsername || displayName).toLowerCase().replace(/[^a-z0-9_]/g, '_') + '_' + Math.floor(100 + Math.random() * 900);

        currentProfileData = {
            authorName: displayName.trim(),
            authorUsername: usernameClean,
            authorPhotoUrl: '',
            authorBadge: 'كاتب جديد',
            authorBio: 'محب للكلمة الطيبة والاقتباسات الراقية في منشوراتي.',
            authorStreak: 1,
            totalLikes: 0,
            postsCount: 0,
            email: email.trim(),
            createdAt: Date.now(),
            isBlocked: false
        };

        await db.collection('users').doc(user.uid).set(currentProfileData);
        await db.collection('usernames').doc(usernameClean).set({ userId: user.uid, createdAt: Date.now() });

        showAppToast("تم إنشاء حسابك بنجاح! مرحباً بك في منشوراتي ✨");
        closeLoginModal();
        return user;
    } catch (err) {
        console.error("Sign up error:", err);
        let msg = "تعذر إنشاء الحساب: " + (err.message || '');
        if (err.code === 'auth/email-already-in-use') msg = "هذا البريد الإلكتروني مسجل مسبقاً، يرجى تسجيل الدخول.";
        if (err.code === 'auth/weak-password') msg = "كلمة المرور ضعيفة جداً (يجب أن تكون 6 أحرف على الأقل).";
        if (err.code === 'auth/invalid-email') msg = "صيغة البريد الإلكتروني غير صحيحة.";
        if (err.code === 'auth/network-request-failed') msg = "مشكلة في الاتصال بالإنترنت، يرجى المحاولة ثانية.";
        showAuthError(msg);
        throw err;
    }
}

/**
 * Sign Out
 */
async function signOutUser() {
    await auth.signOut();
    showAppToast("تم تسجيل الخروج بنجاح.");
    setTimeout(() => {
        window.location.reload();
    }, 600);
}

/**
 * Update UI for Logged In / Logged Out state in Header & Navbars
 */
function updateNavbarAuthUI() {
    const authContainer = document.getElementById("navAuthSection");
    if (!authContainer) return;

    if (currentAuthUser) {
        const name = currentProfileData?.authorName || currentAuthUser.displayName || "الكاتب";
        const uname = currentProfileData?.authorUsername || "user";
        const photo = currentProfileData?.authorPhotoUrl || currentAuthUser.photoURL || "";
        const initial = name[0] || "ك";

        let avatarHtml = `<div class="fb-user-thumb" style="width:38px; height:38px; font-size:14px; cursor:pointer;">${initial}</div>`;
        if (photo && (photo.startsWith('http') || photo.startsWith('data:'))) {
            avatarHtml = `<div class="fb-user-thumb" style="width:38px; height:38px; padding:0; overflow:hidden; cursor:pointer;"><img src="${photo}" alt="${name}" style="width:100%;height:100%;object-fit:cover;"></div>`;
        }

        authContainer.innerHTML = `
            <div style="position:relative; display:flex; align-items:center; gap:8px;">
                <div onclick="toggleProfileDropdown()" style="display:flex; align-items:center; gap:8px; cursor:pointer;">
                    ${avatarHtml}
                    <div class="hide-mobile" style="text-align:right;">
                        <div style="font-family:'Cairo',sans-serif; font-size:13px; font-weight:700; color:#FFFFFF; line-height:1.2;">${name}</div>
                        <div style="font-size:11px; color:var(--text-secondary);">@${uname}</div>
                    </div>
                </div>
            </div>
        `;

        // Update profile dropdown menu items
        const profilePop = document.getElementById("profileDropdown");
        if (profilePop) {
            profilePop.innerHTML = `
                <div style="display:flex; align-items:center; gap:10px; padding-bottom:10px; border-bottom:1px solid var(--divider-color);">
                    ${avatarHtml}
                    <div>
                        <div style="font-family:'Cairo',sans-serif; font-size:14px; font-weight:800; color:#FFF;">${name}</div>
                        <div style="font-size:12px; color:var(--brand-primary);">@${uname} • ${currentProfileData?.authorBadge || 'كاتب'}</div>
                    </div>
                </div>
                <a href="profile.html" class="fb-dropdown-item">
                    <svg viewBox="0 0 24 24"><path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/></svg>
                    <span>ملفي الشخصي (Profile)</span>
                </a>
                <a href="studio.html" class="fb-dropdown-item">
                    <svg viewBox="0 0 24 24"><path d="M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04c.39-.39.39-1.02 0-1.41l-2.34-2.34c-.39-.39-1.02-.39-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z"/></svg>
                    <span>استوديو التصميم</span>
                </a>
                <a href="#" onclick="openSavedPostsModal(); return false;" class="fb-dropdown-item">
                    <svg viewBox="0 0 24 24"><path d="M17 3H7c-1.1 0-1.99.9-1.99 2L5 21l7-3 7 3V5c0-1.1-.9-2-2-2z"/></svg>
                    <span>منشوراتي المحفوظة</span>
                </a>
                ${isCurrentAdmin ? `
                <a href="admin.html" class="fb-dropdown-item" style="color:#F59E0B; background:rgba(245, 158, 11, 0.08); border-radius:10px; font-weight:700;">
                    <svg viewBox="0 0 24 24"><path d="M12 1L3 5v6c0 5.55 3.84 10.74 9 12 5.16-1.26 9-6.45 9-12V5l-9-4zm0 10.99h7c-.53 4.12-3.28 7.79-7 8.94V12H5V6.3l7-3.11v8.8z"/></svg>
                    <span>لوحة تحكم الأدمن (Dashboard) ⚡</span>
                </a>
                ` : ''}
                <div style="height:1px; background:var(--divider-color); margin:4px 0;"></div>
                <button onclick="signOutUser()" class="fb-dropdown-item" style="background:none; border:none; width:100%; text-align:right; color:#EF4444;">
                    <svg viewBox="0 0 24 24"><path d="M17 7l-1.41 1.41L18.17 11H8v2h10.17l-2.58 2.58L17 17l5-5zM4 5h8V3H4c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h8v-2H4V5z"/></svg>
                    <span>تسجيل الخروج</span>
                </button>
            `;
        }
    } else {
        authContainer.innerHTML = `
            <button onclick="openLoginModal()" class="btn-browse-category" style="padding: 7px 16px; font-size: 13px; font-weight: 700; gap: 6px;">
                <svg viewBox="0 0 24 24" style="width:16px;height:16px;fill:currentColor;"><path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/></svg>
                <span>تسجيل الدخول</span>
            </button>
        `;
    }
}

/**
 * Publish Post Directly to Firestore (Syncs to Mobile App & Web Instantly)
 */
async function publishPostToFirestore(postData) {
    if (!currentAuthUser) {
        openLoginModal();
        showAppToast("يرجى تسجيل الدخول أولاً لنشر خاطرتك في المجتمع!");
        throw new Error("User not authenticated");
    }

    const authorName = currentProfileData?.authorName || currentAuthUser.displayName || "كاتب منشوراتي";
    const authorUsername = currentProfileData?.authorUsername || "user";
    const authorPhotoUrl = currentProfileData?.authorPhotoUrl || currentAuthUser.photoURL || "";
    const authorStreak = currentProfileData?.authorStreak || 1;
    const authorBadge = currentProfileData?.authorBadge || "كاتب";

    const timestamp = Date.now();
    const docData = {
        authorId: currentAuthUser.uid,
        authorName: authorName,
        authorUsername: authorUsername,
        authorPhotoUrl: authorPhotoUrl,
        authorBadge: authorBadge,
        authorStreak: authorStreak,
        content: postData.content.trim(),
        bgId: postData.bgId || "card_luxury",
        fontId: postData.fontId || "tajawal",
        textColor: postData.textColor || "#FFFFFF",
        tag: postData.tag || "خواطر عميقة",
        mood: postData.mood || "CHILL",
        likesCount: 0,
        commentsCount: 0,
        reportCount: 0,
        postType: "TEXT",
        timestamp: timestamp
    };

    const docRef = await db.collection('community_posts').add(docData);

    // Update user post count
    await db.collection('users').doc(currentAuthUser.uid).update({
        postsCount: firebase.firestore.FieldValue.increment(1)
    }).catch(() => {});

    return { id: docRef.id, ...docData };
}

/**
 * Update an Existing Post in Firestore
 */
async function editPostInFirestore(postId, updateData) {
    if (!currentAuthUser) {
        openLoginModal();
        throw new Error("User not authenticated");
    }

    const payload = {
        content: updateData.content.trim(),
        bgId: updateData.bgId || "card_luxury",
        fontId: updateData.fontId || "tajawal",
        textColor: updateData.textColor || "#FFFFFF",
        tag: updateData.tag || "خواطر عميقة",
        isEdited: true,
        editedAt: Date.now()
    };

    await db.collection('community_posts').doc(postId).update(payload);
    return payload;
}

/**
 * Delete a Post from Firestore
 */
async function deletePostFromFirestore(postId) {
    if (!currentAuthUser) {
        openLoginModal();
        throw new Error("User not authenticated");
    }

    await db.collection('community_posts').doc(postId).delete();
    
    // Decrement user post count if own post
    await db.collection('users').doc(currentAuthUser.uid).update({
        postsCount: firebase.firestore.FieldValue.increment(-1)
    }).catch(() => {});
}

/**
 * Real Comments CRUD in Firestore
 */
async function addCommentToFirestore(postId, commentText) {
    if (!currentAuthUser) {
        openLoginModal();
        showAppToast("يرجى تسجيل الدخول أولاً لإضافة تعليقك!");
        throw new Error("User not authenticated");
    }

    const authorName = currentProfileData?.authorName || currentAuthUser.displayName || "كاتب";
    const authorUsername = currentProfileData?.authorUsername || "user";
    const authorPhotoUrl = currentProfileData?.authorPhotoUrl || currentAuthUser.photoURL || "";

    const commentData = {
        postId: postId,
        authorId: currentAuthUser.uid,
        authorName: authorName,
        authorUsername: authorUsername,
        authorPhotoUrl: authorPhotoUrl,
        content: commentText.trim(),
        likesCount: 0,
        timestamp: Date.now()
    };

    const docRef = await db.collection('community_posts').doc(postId).collection('comments').add(commentData);

    // Increment commentsCount on the post
    await db.collection('community_posts').doc(postId).update({
        commentsCount: firebase.firestore.FieldValue.increment(1)
    }).catch(() => {});

    return { id: docRef.id, ...commentData };
}

async function fetchCommentsForPost(postId) {
    try {
        const snap = await db.collection('community_posts').doc(postId).collection('comments').orderBy('timestamp', 'asc').get();
        return snap.docs.map(doc => ({ id: doc.id, ...doc.data() }));
    } catch (err) {
        console.warn("Could not fetch comments from Firestore:", err);
        return [];
    }
}

async function deleteCommentFromFirestore(postId, commentId) {
    if (!currentAuthUser) {
        openLoginModal();
        throw new Error("User not authenticated");
    }

    await db.collection('community_posts').doc(postId).collection('comments').doc(commentId).delete();

    // Decrement commentsCount on the post
    await db.collection('community_posts').doc(postId).update({
        commentsCount: firebase.firestore.FieldValue.increment(-1)
    }).catch(() => {});
}

async function editCommentInFirestore(postId, commentId, newText) {
    if (!currentAuthUser) {
        openLoginModal();
        throw new Error("User not authenticated");
    }

    await db.collection('community_posts').doc(postId).collection('comments').doc(commentId).update({
        content: newText.trim(),
        isEdited: true,
        editedAt: Date.now()
    });
}

/**
 * Report a Post to Admins
 */
async function reportPostToFirestore(postId, reason, postContent, authorName) {
    const reportData = {
        postId: postId,
        postContent: postContent || '',
        postAuthor: authorName || '',
        reporterId: currentAuthUser ? currentAuthUser.uid : 'guest',
        reporterEmail: currentAuthUser ? currentAuthUser.email : 'guest',
        reason: reason || 'محتوى غير لائق',
        status: 'PENDING',
        timestamp: Date.now()
    };

    await db.collection('reports').add(reportData);
    
    // Increment report counter on the post
    await db.collection('community_posts').doc(postId).update({
        reportCount: firebase.firestore.FieldValue.increment(1)
    }).catch(() => {});

    showAppToast("تم إرسال بلاغك للإدارة بنجاح. شكراً لحرصك على نظافة المجتمع!");
}

/**
 * Universal Login & Registration Modal Injector
 */
function ensureLoginModalInDOM() {
    let modalEl = document.getElementById("manchoratiLoginModal");
    if (modalEl) {
        modalEl.remove(); // Always replace with latest ultra-compact version
    }

    const modalHtml = `
    <div id="manchoratiLoginModal" onclick="if(event.target === this) closeLoginModal();" onwheel="event.stopPropagation();" ontouchmove="if(event.target === this) event.preventDefault();" style="display:none; position:fixed; inset:0; background:rgba(4,9,16,0.92); backdrop-filter:blur(14px); -webkit-backdrop-filter:blur(14px); z-index:999999; overflow-y:auto; -webkit-overflow-scrolling:touch; overscroll-behavior:contain; padding:20px 12px 60px 12px; box-sizing:border-box;">
        <div class="manchorati-auth-card" style="background:#0F1A2A; border:1px solid #1E314B; border-radius:18px; max-width:430px; width:100%; margin:20px auto; padding:18px 20px; box-shadow:0 20px 60px rgba(0,0,0,0.8); position:relative; text-align:right; font-family:'Tajawal', sans-serif; box-sizing:border-box; flex-shrink:0;">
            <button type="button" onclick="closeLoginModal()" title="إغلاق" style="position:absolute; top:12px; left:12px; width:28px; height:28px; border-radius:50%; background:rgba(255,255,255,0.06); border:1px solid #1E314B; color:#94A3B8; font-size:14px; cursor:pointer; display:flex; align-items:center; justify-content:center; line-height:1; transition:all 0.2s;">✕</button>
            
            <!-- Compact Header -->
            <div style="display:flex; align-items:center; justify-content:center; gap:10px; margin-bottom:12px; padding-right:16px;">
                <img src="assets/icon.png" alt="منشوراتي" style="width:34px; height:34px; border-radius:10px; box-shadow:0 3px 10px rgba(0,0,0,0.3); display:inline-block; flex-shrink:0;">
                <div>
                    <h2 style="font-family:'Cairo', sans-serif; font-size:16px; font-weight:800; color:#FFFFFF; margin:0; line-height:1.2;">انضم إلى مجتمع منشوراتي</h2>
                    <p style="font-size:11.5px; color:#94A3B8; margin:0; line-height:1.3;">سجّل الدخول للتفاعل ونشر خواطرك</p>
                </div>
            </div>

            <!-- Inline Error Message Display -->
            <div id="authErrorMessage" style="display:none; background:rgba(239,68,68,0.15); border:1px solid rgba(239,68,68,0.4); color:#FCA5A5; font-size:12px; font-weight:700; padding:8px 12px; border-radius:8px; margin-bottom:10px; text-align:center; line-height:1.4;"></div>

            <!-- Google One-Click Login -->
            <button type="button" id="btnGoogleAuth" onclick="signInWithGoogle()" style="width:100%; display:flex; align-items:center; justify-content:center; gap:8px; padding:9px 12px; background:#172437; border:1px solid #1E314B; border-radius:8px; color:#FFFFFF; font-size:12.5px; font-weight:700; cursor:pointer; transition:all 0.2s; margin-bottom:10px;">
                <svg viewBox="0 0 24 24" style="width:16px; height:16px; fill:currentColor;"><path d="M12.545,10.239v3.821h5.445c-0.712,2.315-2.647,3.972-5.445,3.972c-3.332,0-6.033-2.701-6.033-6.032s2.701-6.032,6.033-6.032c1.498,0,2.866,0.549,3.921,1.453l2.814-2.814C17.503,2.988,15.139,2,12.545,2C7.021,2,2.543,6.477,2.543,12s4.478,10,10.002,10c8.396,0,10.249-7.85,9.426-11.761H12.545z"/></svg>
                <span>المتابعة بـ Google (سريع)</span>
            </button>

            <div style="display:flex; align-items:center; gap:8px; margin-bottom:10px;">
                <div style="flex:1; height:1px; background:#1E314B;"></div>
                <span style="font-size:11px; color:#64748B;">أو بالبريد الإلكتروني</span>
                <div style="flex:1; height:1px; background:#1E314B;"></div>
            </div>

            <!-- Tab Switcher: Sign in / Sign up -->
            <div id="authTabsContainer" style="display:flex; background:#172437; border-radius:8px; padding:3px; margin-bottom:10px; border:1px solid #1E314B;">
                <button type="button" id="tabAuthLoginBtn" onclick="switchAuthTab('login')" style="flex:1; padding:6px; border:none; border-radius:6px; background:#38BDF8; color:#0B131F; font-weight:700; font-size:12px; cursor:pointer; transition:all 0.2s; font-family:'Tajawal', sans-serif;">تسجيل الدخول</button>
                <button type="button" id="tabAuthSignupBtn" onclick="switchAuthTab('signup')" style="flex:1; padding:6px; border:none; border-radius:6px; background:transparent; color:#94A3B8; font-weight:700; font-size:12px; cursor:pointer; transition:all 0.2s; font-family:'Tajawal', sans-serif;">حساب جديد</button>
            </div>

            <!-- Login Form -->
            <form id="firebaseLoginForm" onsubmit="handleFirebaseLoginForm(event)">
                <div style="margin-bottom:8px;">
                    <label style="display:block; font-size:11px; font-weight:700; color:#CBD5E1; margin-bottom:3px;">البريد الإلكتروني</label>
                    <input type="email" id="loginFormEmail" required placeholder="name@example.com" style="width:100%; box-sizing:border-box; background:#172437; border:1px solid #1E314B; border-radius:8px; padding:7px 11px; color:#FFF; font-size:12.5px; outline:none; transition:border 0.2s;">
                </div>
                <div style="margin-bottom:6px;">
                    <label style="display:block; font-size:11px; font-weight:700; color:#CBD5E1; margin-bottom:3px;">كلمة المرور</label>
                    <input type="password" id="loginFormPassword" required placeholder="••••••••" style="width:100%; box-sizing:border-box; background:#172437; border:1px solid #1E314B; border-radius:8px; padding:7px 11px; color:#FFF; font-size:12.5px; outline:none; transition:border 0.2s;">
                </div>
                <div style="text-align:left; margin-bottom:12px;">
                    <a href="#" onclick="switchAuthTab('reset'); return false;" style="font-size:11px; color:#38BDF8; text-decoration:none; font-weight:600;">نسيت كلمة المرور؟</a>
                </div>
                <button type="submit" id="btnLoginSubmit" style="width:100%; padding:9px; background:#38BDF8; color:#0B131F; border:none; border-radius:8px; font-size:13px; font-weight:800; cursor:pointer; font-family:'Cairo',sans-serif; transition:all 0.2s;">دخول</button>
            </form>

            <!-- Signup Form (Hidden by default) -->
            <form id="firebaseSignupForm" onsubmit="handleFirebaseSignupForm(event)" style="display:none;">
                <div style="display:grid; grid-template-columns:1fr 1fr; gap:8px; margin-bottom:8px;">
                    <div>
                        <label style="display:block; font-size:11px; font-weight:700; color:#CBD5E1; margin-bottom:3px;">الاسم الكامل *</label>
                        <input type="text" id="signupFormName" required placeholder="محمد أمين" style="width:100%; box-sizing:border-box; background:#172437; border:1px solid #1E314B; border-radius:8px; padding:7px 10px; color:#FFF; font-size:12.5px; outline:none; transition:border 0.2s;">
                    </div>
                    <div>
                        <label style="display:block; font-size:11px; font-weight:700; color:#CBD5E1; margin-bottom:3px;">اسم المستخدم (@) *</label>
                        <input type="text" id="signupFormUsername" required placeholder="mohamed_amine" style="width:100%; box-sizing:border-box; background:#172437; border:1px solid #1E314B; border-radius:8px; padding:7px 10px; color:#FFF; font-size:12.5px; outline:none; transition:border 0.2s;">
                    </div>
                </div>
                <div style="margin-bottom:8px;">
                    <label style="display:block; font-size:11px; font-weight:700; color:#CBD5E1; margin-bottom:3px;">البريد الإلكتروني *</label>
                    <input type="email" id="signupFormEmail" required placeholder="name@example.com" style="width:100%; box-sizing:border-box; background:#172437; border:1px solid #1E314B; border-radius:8px; padding:7px 10px; color:#FFF; font-size:12.5px; outline:none; transition:border 0.2s;">
                </div>
                <div style="margin-bottom:12px;">
                    <label style="display:block; font-size:11px; font-weight:700; color:#CBD5E1; margin-bottom:3px;">كلمة المرور (6 أحرف فأكثر) *</label>
                    <input type="password" id="signupFormPassword" required minlength="6" placeholder="••••••••" style="width:100%; box-sizing:border-box; background:#172437; border:1px solid #1E314B; border-radius:8px; padding:7px 10px; color:#FFF; font-size:12.5px; outline:none; transition:border 0.2s;">
                </div>
                <button type="submit" id="btnSignupSubmit" style="width:100%; padding:9px; background:#10B981; color:#FFFFFF; border:none; border-radius:8px; font-size:13px; font-weight:800; cursor:pointer; font-family:'Cairo',sans-serif; transition:all 0.2s; box-shadow:0 4px 14px rgba(16,185,129,0.3);">إنشاء الحساب فوراً</button>
            </form>

            <!-- Password Reset Form (Hidden by default) -->
            <form id="firebaseResetForm" onsubmit="handleFirebaseResetForm(event)" style="display:none;">
                <div style="margin-bottom:10px;">
                    <label style="display:block; font-size:11px; font-weight:700; color:#CBD5E1; margin-bottom:3px;">البريد الإلكتروني المسجل للحساب</label>
                    <input type="email" id="resetFormEmail" required placeholder="name@example.com" style="width:100%; box-sizing:border-box; background:#172437; border:1px solid #1E314B; border-radius:8px; padding:7px 11px; color:#FFF; font-size:12.5px; outline:none; transition:border 0.2s;">
                </div>
                <div id="authSuccessMessage" style="display:none; background:rgba(16,185,129,0.15); border:1px solid rgba(16,185,129,0.4); color:#6EE7B7; font-size:11.5px; font-weight:700; padding:8px 10px; border-radius:8px; margin-bottom:10px; text-align:center; line-height:1.4;"></div>
                <button type="submit" id="btnResetSubmit" style="width:100%; padding:9px; background:#0284C7; color:#FFFFFF; border:none; border-radius:8px; font-size:13px; font-weight:800; cursor:pointer; font-family:'Cairo',sans-serif; transition:all 0.2s; margin-bottom:10px;">إرسال رابط الاستعادة 📬</button>
                <div style="text-align:center;">
                    <a href="#" onclick="switchAuthTab('login'); return false;" style="font-size:11.5px; color:#94A3B8; text-decoration:none; font-weight:600;">العودة إلى تسجيل الدخول</a>
                </div>
            </form>
        </div>
    </div>
    `;

    document.body.insertAdjacentHTML('beforeend', modalHtml);
}

function openLoginModal() {
    ensureLoginModalInDOM();
    const m = document.getElementById("manchoratiLoginModal");
    if (m) {
        m.style.display = "flex";
        m.style.flexDirection = "column";
        m.style.alignItems = "center";
        const scrollY = window.scrollY || document.documentElement.scrollTop || 0;
        document.body.dataset.authModalScrollY = scrollY;
        document.body.classList.add("modal-open");
        document.documentElement.classList.add("modal-open");
        document.body.style.position = "fixed";
        document.body.style.top = `-${scrollY}px`;
        document.body.style.width = "100%";
        document.body.style.overflow = "hidden";
    }
}

function closeLoginModal() {
    const m = document.getElementById("manchoratiLoginModal");
    if (m) {
        m.style.display = "none";
        const scrollY = parseInt(document.body.dataset.authModalScrollY || "0", 10);
        document.body.classList.remove("modal-open");
        document.documentElement.classList.remove("modal-open");
        document.body.style.position = "";
        document.body.style.top = "";
        document.body.style.width = "";
        document.body.style.overflow = "";
        window.scrollTo(0, scrollY);
    }
}

if (!window._manchoratiEscKeyRegistered) {
    window._manchoratiEscKeyRegistered = true;
    window.addEventListener("keydown", (e) => {
        if (e.key === "Escape") closeLoginModal();
    });
}

function switchAuthTab(tab) {
    clearAuthError();
    const successMsg = document.getElementById("authSuccessMessage");
    if (successMsg) successMsg.style.display = "none";

    const loginForm = document.getElementById("firebaseLoginForm");
    const signupForm = document.getElementById("firebaseSignupForm");
    const resetForm = document.getElementById("firebaseResetForm");
    const tabsContainer = document.getElementById("authTabsContainer");
    const loginBtn = document.getElementById("tabAuthLoginBtn");
    const signupBtn = document.getElementById("tabAuthSignupBtn");

    if (tab === 'login') {
        if (tabsContainer) tabsContainer.style.display = "flex";
        if (loginForm) loginForm.style.display = "block";
        if (signupForm) signupForm.style.display = "none";
        if (resetForm) resetForm.style.display = "none";
        if (loginBtn) { loginBtn.style.background = "#38BDF8"; loginBtn.style.color = "#0B131F"; }
        if (signupBtn) { signupBtn.style.background = "transparent"; signupBtn.style.color = "#94A3B8"; }
    } else if (tab === 'signup') {
        if (tabsContainer) tabsContainer.style.display = "flex";
        if (loginForm) loginForm.style.display = "none";
        if (signupForm) signupForm.style.display = "block";
        if (resetForm) resetForm.style.display = "none";
        if (signupBtn) { signupBtn.style.background = "#10B981"; signupBtn.style.color = "#FFFFFF"; }
        if (loginBtn) { loginBtn.style.background = "transparent"; loginBtn.style.color = "#94A3B8"; }
    } else if (tab === 'reset') {
        if (tabsContainer) tabsContainer.style.display = "none";
        if (loginForm) loginForm.style.display = "none";
        if (signupForm) signupForm.style.display = "none";
        if (resetForm) resetForm.style.display = "block";
    }
}

async function handleFirebaseResetForm(e) {
    e.preventDefault();
    const email = document.getElementById("resetFormEmail").value;
    await sendPasswordReset(email);
}

/**
 * Send Password Reset Email via Firebase Auth
 */
async function sendPasswordReset(email) {
    clearAuthError();
    if (!email || !email.trim()) {
        showAuthError("يرجى إدخال البريد الإلكتروني أولاً.");
        return;
    }
    const btn = document.getElementById("btnResetSubmit");
    if (btn) {
        btn.disabled = true;
        btn.textContent = "جاري الإرسال...";
    }
    try {
        await auth.sendPasswordResetEmail(email.trim());
        showAppToast("تم إرسال رابط استعادة كلمة المرور إلى بريدك! 📬");
        const box = document.getElementById("authSuccessMessage");
        if (box) {
            box.textContent = "تم إرسال الرابط بنجاح. تفقد صندوق الوارد ومجلد الرسائل غير المرغوب فيها (Spam).";
            box.style.display = "block";
        }
    } catch (err) {
        console.error("Password reset error:", err);
        let msg = "تعذر إرسال رابط الاستعادة: " + (err.message || '');
        if (err.code === 'auth/user-not-found') msg = "لا يوجد حساب مسجل بهذا البريد الإلكتروني.";
        if (err.code === 'auth/invalid-email') msg = "صيغة البريد الإلكتروني غير صحيحة.";
        showAuthError(msg);
    } finally {
        if (btn) {
            btn.disabled = false;
            btn.textContent = "إرسال رابط الاستعادة 📬";
        }
    }
}

/**
 * Bottom Nav Profile click handler for Mobile
 */
function handleBottomNavProfile() {
    if (currentAuthUser) {
        window.location.href = "profile.html";
    } else {
        openLoginModal();
    }
}

/**
 * Cookie Consent Manager (GDPR / CCPA / Google AdSense Compliance)
 */
function initCookieConsent() {
    const consent = localStorage.getItem('manchorati_cookie_consent');
    if (!consent) {
        const banner = document.getElementById("manchoratiCookieBanner");
        if (banner) {
            banner.style.display = "flex";
        }
    }
}

function acceptCookieConsent() {
    localStorage.setItem('manchorati_cookie_consent', 'accepted');
    const banner = document.getElementById("manchoratiCookieBanner");
    if (banner) banner.style.display = "none";
    showAppToast("تم حفظ تفضيلات ملفات تعريف الارتباط بنجاح 🍪");
}

function rejectCookieConsent() {
    localStorage.setItem('manchorati_cookie_consent', 'essential_only');
    const banner = document.getElementById("manchoratiCookieBanner");
    if (banner) banner.style.display = "none";
    showAppToast("تم تفعيل الكوكيز الضرورية فقط.");
}

if (document.readyState === 'loading') {
    document.addEventListener("DOMContentLoaded", initCookieConsent);
} else {
    initCookieConsent();
}

async function handleFirebaseLoginForm(e) {
    e.preventDefault();
    const email = document.getElementById("loginFormEmail").value;
    const pass = document.getElementById("loginFormPassword").value;
    const btn = document.getElementById("btnLoginSubmit");
    btn.disabled = true;
    btn.textContent = "جاري التحقق...";
    try {
        await signInWithEmail(email, pass);
    } catch (err) {
        // Error alert is handled in signInWithEmail
    } finally {
        btn.disabled = false;
        btn.textContent = "دخول";
    }
}

async function handleFirebaseSignupForm(e) {
    e.preventDefault();
    const name = document.getElementById("signupFormName").value;
    const username = document.getElementById("signupFormUsername").value;
    const email = document.getElementById("signupFormEmail").value;
    const pass = document.getElementById("signupFormPassword").value;
    const btn = document.getElementById("btnSignupSubmit");
    btn.disabled = true;
    btn.textContent = "جاري إنشاء الحساب...";
    try {
        await signUpWithEmail(email, pass, name, username);
    } catch (err) {
        // Error alert handled in signUpWithEmail
    } finally {
        btn.disabled = false;
        btn.textContent = "إنشاء الحساب فوراً";
    }
}

function toggleProfileDropdown() {
    const pop = document.getElementById("profileDropdown");
    if (pop) {
        pop.classList.toggle("show");
    }
}

function showAppToast(msg) {
    let toast = document.getElementById("manchoratiAppToast");
    if (!toast) {
        toast = document.createElement("div");
        toast.id = "manchoratiAppToast";
        toast.style.cssText = "position:fixed;bottom:24px;left:24px;background:#0F1A2A;color:#FFFFFF;border-right:4px solid #38BDF8;padding:12px 20px;border-radius:12px;box-shadow:0 10px 30px rgba(0,0,0,0.5);font-size:13.5px;font-weight:700;z-index:99999;transition:opacity 0.3s;font-family:'Tajawal',sans-serif;";
        document.body.appendChild(toast);
    }
    toast.textContent = msg;
    toast.style.display = "block";
    toast.style.opacity = "1";
    setTimeout(() => {
        toast.style.opacity = "0";
        setTimeout(() => { toast.style.display = "none"; }, 300);
    }, 3500);
}

// Close popovers on click outside
document.addEventListener("click", (e) => {
    const pop = document.getElementById("profileDropdown");
    if (pop && pop.classList.contains("show")) {
        const btn = document.getElementById("btnProfileToggle");
        const authSection = document.getElementById("navAuthSection");
        if (!pop.contains(e.target) && (!btn || !btn.contains(e.target)) && (!authSection || !authSection.contains(e.target))) {
            pop.classList.remove("show");
        }
    }
});
