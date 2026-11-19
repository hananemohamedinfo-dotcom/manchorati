/**
 * منشوراتي (MANCHORATI) - APP CORE JAVASCRIPT
 * Live Firestore Sync via REST API, Real Data, Card Templates, Likes & Comments
 */

const FIRESTORE_QUERY_URL = 'https://firestore.googleapis.com/v1/projects/manchorati-4ace8/databases/(default)/documents:runQuery?key=AIzaSyD6z0FAV1puTKcF9CN85UyvQ19uKoVyjNo';

// Exact Card Templates matching ThemeHelper.kt in Android App
const CARD_TEMPLATES = {
    'card_crimson_brand': { bg: 'linear-gradient(135deg, #991B1B, #DC2626)', border: '#EF4444', text: '#FFFFFF', font: 'Tajawal' },
    'card_luxury': { bg: 'linear-gradient(135deg, #0B0F19, #1E293B)', border: '#EAB308', text: '#F8FAFC', font: 'Tajawal' },
    'card_classic_paper': { bg: 'linear-gradient(135deg, #F6F0EA, #DFD2C0)', border: '#D6C4AE', text: '#1E293B', font: 'Aref Ruqaa' },
    'card_neon_cyber': { bg: 'linear-gradient(135deg, #091A28, #022C43)', border: '#0284C7', text: '#38BDF8', font: 'Cairo' },
    'card_sunset': { bg: 'linear-gradient(135deg, #4A0E2E, #9A3412)', border: '#F59E0B', text: '#FFFFFF', font: 'Cairo' },
    'card_glass_royal': { bg: 'linear-gradient(135deg, #1E1B4B, #312E81)', border: '#6366F1', text: '#FFFFFF', font: 'Kufam' },
    'card_emerald': { bg: 'linear-gradient(135deg, #064E3B, #047857)', border: '#10B981', text: '#FFFFFF', font: 'Tajawal' },
    'card_ocean_breeze': { bg: 'linear-gradient(135deg, #0F172A, #0369A1)', border: '#38BDF8', text: '#FFFFFF', font: 'Marhey' },
    'card_pure_minimal': { bg: 'linear-gradient(135deg, #FFFFFF, #F8FAFC)', border: '#E2E8F0', text: '#0F172A', font: 'Tajawal' }
};

const FONT_MAP = {
    'tajawal': 'Tajawal',
    'cairoplay': 'Cairo',
    'cairo': 'Cairo',
    'kufam': 'Kufam',
    'amiri': 'Amiri',
    'arefruqaa': 'Aref Ruqaa',
    'marhey': 'Marhey'
};

// Initial posts data
let communityPosts = [];
let currentMoodFilter = "ALL";
let currentCategoryFilter = "الكل";
let currentSearch = "";
let likedPosts = JSON.parse(localStorage.getItem("manchorati_likes") || "[]");
let savedBookmarks = JSON.parse(localStorage.getItem("manchorati_bookmarks") || "[]");

function getTemplate(bgId) {
    if (bgId && CARD_TEMPLATES[bgId]) return CARD_TEMPLATES[bgId];
    if (bgId && bgId.startsWith('#')) {
        return { bg: bgId, border: 'rgba(255,255,255,0.1)', text: '#FFFFFF', font: 'Tajawal' };
    }
    return CARD_TEMPLATES['card_luxury'];
}

function getFontFamily(fontId) {
    if (fontId && FONT_MAP[fontId.toLowerCase()]) return FONT_MAP[fontId.toLowerCase()];
    return 'Tajawal';
}

function formatTimeAgo(timeMs) {
    if (!timeMs) return "مؤخراً";
    const diff = Math.floor((Date.now() - Number(timeMs)) / 1000);
    if (diff < 60) return "الآن";
    if (diff < 3600) return "منذ " + Math.floor(diff / 60) + " دقيقة";
    if (diff < 86400) return "منذ " + Math.floor(diff / 3600) + " ساعة";
    return "منذ " + Math.floor(diff / 86400) + " يوم";
}

// Live Fetch from Firestore REST API
async function fetchLiveFirestoreFeed() {
    try {
        const body = JSON.stringify({
            structuredQuery: {
                from: [{ collectionId: 'community_posts' }],
                orderBy: [{ field: { fieldPath: 'timestamp' }, direction: 'DESCENDING' }],
                limit: 50
            }
        });

        const res = await fetch(FIRESTORE_QUERY_URL, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: body
        });

        if (!res.ok) throw new Error("Firestore HTTP " + res.status);
        const data = await res.json();
        const live = [];

        data.forEach(item => {
            if (!item.document) return;
            const doc = item.document;
            const f = doc.fields;
            const id = doc.name.split('/').pop();
            live.push({
                id: id,
                authorName: f.authorName?.stringValue || 'كاتب منشوراتي',
                authorUsername: f.authorUsername?.stringValue || 'user',
                authorPhotoUrl: f.authorPhotoUrl?.stringValue || '',
                authorBadge: f.authorBadge?.stringValue || '',
                authorStreak: parseInt(f.authorStreak?.integerValue || '0'),
                content: f.content?.stringValue || '',
                bgId: f.bgId?.stringValue || 'card_luxury',
                fontId: f.fontId?.stringValue || 'tajawal',
                textColor: f.textColor?.stringValue || '#FFFFFF',
                tag: f.tag?.stringValue || 'خواطر عميقة',
                mood: f.mood?.stringValue || 'CHILL',
                likesCount: parseInt(f.likesCount?.integerValue || '0'),
                commentsCount: parseInt(f.commentsCount?.integerValue || '0'),
                timestamp: parseInt(f.timestamp?.integerValue || Date.now().toString())
            });
        });

        if (live.length > 0) {
            communityPosts = live;
            renderFeed();
            console.log("Loaded " + live.length + " live posts directly from Firebase Firestore!");
        }
    } catch (err) {
        console.warn("Live fetch fallback to initial data:", err);
    }
}

// Render Feed
function renderFeed() {
    const feedContainer = document.getElementById("postsFeedStream");
    if (!feedContainer) return;

    const filtered = communityPosts.filter(post => {
        if (currentMoodFilter !== "ALL" && post.mood !== currentMoodFilter) return false;
        if (currentCategoryFilter !== "الكل" && post.tag !== currentCategoryFilter) return false;
        if (currentSearch.trim() !== "") {
            const q = currentSearch.toLowerCase().trim();
            const hitText = (post.content || "").toLowerCase().includes(q);
            const hitAuthor = (post.authorName || "").toLowerCase().includes(q);
            const hitTag = (post.tag || "").toLowerCase().includes(q);
            if (!hitText && !hitAuthor && !hitTag) return false;
        }
        return true;
    });

    feedContainer.innerHTML = "";

    if (filtered.length === 0) {
        feedContainer.innerHTML = `
            <div style="background: var(--bg-card); border: 1px solid var(--divider-color); border-radius: var(--radius-card); padding: 50px 20px; text-align: center; color: var(--text-secondary);">
                <h3 style="color: #FFFFFF; font-size: 18px; margin-bottom: 6px;">لا توجد منشورات مطابقة</h3>
                <p style="font-size: 13.5px;">جرب اختيار تصنيف آخر أو مسح حقل البحث</p>
            </div>
        `;
        return;
    }

    filtered.forEach((post, index) => {
        const isLiked = likedPosts.includes(post.id);
        const isBookmarked = savedBookmarks.includes(post.id);
        const tpl = getTemplate(post.bgId);
        const fontName = getFontFamily(post.fontId);

        let avatarHtml = `<div class="author-avatar-box">${(post.authorName || "ك")[0]}</div>`;
        if (post.authorPhotoUrl && (post.authorPhotoUrl.startsWith('http') || post.authorPhotoUrl.startsWith('data:'))) {
            avatarHtml = `<div class="author-avatar-box" style="padding:0; overflow:hidden;"><img src="${escapeHtml(post.authorPhotoUrl)}" alt="${escapeHtml(post.authorName)}" style="width:100%; height:100%; object-fit:cover;"></div>`;
        }

        const card = document.createElement("article");
        card.className = "post-item-card";
        card.id = `post-${post.id}`;

        card.innerHTML = `
            <!-- Author Header -->
            <div class="card-author-header">
                <a href="writers.html" class="author-profile-link">
                    ${avatarHtml}
                    <div class="author-name-col">
                        <div class="author-name-row">
                            <span class="author-name-text">${escapeHtml(post.authorName)}</span>
                            ${post.authorStreak > 0 ? `<span class="author-streak-badge" title="سلسلة التفاعل">${post.authorStreak} يوم</span>` : ''}
                        </div>
                        <div class="author-meta-row">
                            <span>@${escapeHtml(post.authorUsername)}</span>
                            <span>•</span>
                            <span>${formatTimeAgo(post.timestamp)}</span>
                        </div>
                    </div>
                </a>

                <button class="btn-card-menu" onclick="copyPostText('${post.id}')" title="خيارات">
                    <svg style="width: 20px; height: 20px; fill: currentColor;" viewBox="0 0 24 24"><path d="M12 8c1.1 0 2-.9 2-2s-.9-2-2-2-2 .9-2 2 .9 2 2 2zm0 2c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2zm0 6c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2z"/></svg>
                </button>
            </div>

            <!-- Inner Quote Card with exact Android gradient and font -->
            <div class="inner-quote-card" style="background: ${tpl.bg}; border: 1.5px solid ${tpl.border};" ondblclick="triggerDoubleTapLike('${post.id}')">
                <svg class="heart-pop" id="heart-pop-${post.id}" viewBox="0 0 24 24"><path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"/></svg>
                <p class="quote-body-text" style="font-family: '${fontName}', sans-serif; color: ${post.textColor || tpl.text};">${escapeHtml(post.content)}</p>
            </div>

            <!-- Hashtag Row -->
            <div class="card-tag-row">
                <a href="#category" class="tag-badge-link" onclick="filterByCategory('${escapeHtml(post.tag || "خواطر")}'); return false;">#${escapeHtml((post.tag || "خواطر").replace(/\s+/g, '_'))}</a>
            </div>

            <!-- Action Bar -->
            <div class="card-action-bar">
                <div class="action-bar-left">
                    <button class="card-action-btn ${isLiked ? 'liked' : ''}" onclick="toggleLikePost('${post.id}')" title="إعجاب">
                        <svg viewBox="0 0 24 24"><path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"/></svg>
                        <span id="like-count-${post.id}">${post.likesCount || 0}</span>
                    </button>

                    <button class="card-action-btn" onclick="toggleCommentsDrawer('${post.id}')" title="تعليقات">
                        <svg viewBox="0 0 24 24"><path d="M20 2H4c-1.1 0-1.99.9-1.99 2L2 22l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zM6 9h12v2H6V9zm8 5H6v-2h8v2zm4-6H6V6h12v2z"/></svg>
                        <span id="comment-count-${post.id}">${post.commentsCount || 0}</span>
                    </button>
                </div>

                <div class="action-bar-right">
                    <button class="card-action-btn" onclick="openStudioWithPost('${post.id}')" title="تصميم في الاستوديو">
                        <svg viewBox="0 0 24 24"><path d="M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04c.39-.39.39-1.02 0-1.41l-2.34-2.34c-.39-.39-1.02-.39-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z"/></svg>
                        <span>تصميم</span>
                    </button>

                    <button class="card-action-btn" onclick="sharePost('${post.id}')" title="مشاركة">
                        <svg viewBox="0 0 24 24"><path d="M18 16.08c-.76 0-1.44.3-1.96.77L8.91 12.7c.05-.23.09-.46.09-.7s-.04-.47-.09-.7l7.05-4.11c.54.5 1.25.81 2.04.81 1.66 0 3-1.34 3-3s-1.34-3-3-3-3 1.34-3 3c0 .24.04.47.09.7L8.04 9.81C7.5 9.31 6.79 9 6 9c-1.66 0-3 1.34-3 3s1.34 3 3 3c.79 0 1.5-.31 2.04-.81l7.12 4.16c-.05.21-.08.43-.08.65 0 1.61 1.31 2.92 2.92 2.92s2.92-1.31 2.92-2.92c0-1.61-1.31-2.92-2.92-2.92z"/></svg>
                    </button>

                    <button class="card-action-btn ${isBookmarked ? 'bookmarked' : ''}" onclick="toggleBookmark('${post.id}')" title="حفظ المنشور">
                        <svg viewBox="0 0 24 24"><path d="M17 3H7c-1.1 0-1.99.9-1.99 2L5 21l7-3 7 3V5c0-1.1-.9-2-2-2z"/></svg>
                    </button>
                </div>
            </div>

            <!-- Comments Drawer -->
            <div class="card-comments-drawer" id="comments-drawer-${post.id}">
                <div class="comment-input-row">
                    <input type="text" class="comment-text-box" id="comment-input-${post.id}" placeholder="اكتب تعليقاً على هذا المنشور...">
                    <button class="btn-send-comment" onclick="submitComment('${post.id}')">إرسال</button>
                </div>
                <div class="comments-list" id="comments-list-${post.id}">
                    <p style="font-size: 12.5px; color: var(--text-faint); text-align: center; padding: 6px;">كن أول من يترك تعليقاً ✨</p>
                </div>
            </div>
        `;

        feedContainer.appendChild(card);

        if ((index + 1) % 4 === 0) {
            const adBox = document.createElement("div");
            adBox.className = "feed-adsense-slot";
            adBox.innerHTML = `
                <span>مساحة إعلانية • Google AdSense</span>
                <p style="font-size: 13px; color: var(--text-secondary);">إعلانات الشركاء المعتمدة من Google</p>
            `;
            feedContainer.appendChild(adBox);
        }
    });
}

// Interactive Handlers
window.toggleLikePost = function(postId) {
    const post = communityPosts.find(p => p.id === postId);
    if (!post) return;
    const idx = likedPosts.indexOf(postId);
    if (idx === -1) {
        likedPosts.push(postId);
        post.likesCount = (post.likesCount || 0) + 1;
        showAppToast("تم تسجيل الإعجاب");
    } else {
        likedPosts.splice(idx, 1);
        post.likesCount = Math.max(0, (post.likesCount || 0) - 1);
    }
    localStorage.setItem("manchorati_likes", JSON.stringify(likedPosts));
    renderFeed();
};

window.triggerDoubleTapLike = function(postId) {
    const heart = document.getElementById(`heart-pop-${postId}`);
    if (heart) {
        heart.classList.add("active");
        setTimeout(() => heart.classList.remove("active"), 600);
    }
    if (!likedPosts.includes(postId)) {
        toggleLikePost(postId);
    }
};

window.toggleBookmark = function(postId) {
    const idx = savedBookmarks.indexOf(postId);
    if (idx === -1) {
        savedBookmarks.push(postId);
        showAppToast("تم حفظ المنشور");
    } else {
        savedBookmarks.splice(idx, 1);
        showAppToast("تمت الإزالة من المحفوظات");
    }
    localStorage.setItem("manchorati_bookmarks", JSON.stringify(savedBookmarks));
    renderFeed();
};

window.toggleCommentsDrawer = function(postId) {
    const drawer = document.getElementById(`comments-drawer-${postId}`);
    if (drawer) {
        drawer.classList.toggle("open");
    }
};

window.submitComment = function(postId) {
    const input = document.getElementById(`comment-input-${postId}`);
    if (!input || !input.value.trim()) return;
    const post = communityPosts.find(p => p.id === postId);
    if (!post) return;

    post.commentsCount = (post.commentsCount || 0) + 1;
    const list = document.getElementById(`comments-list-${postId}`);
    if (list) {
        const item = document.createElement("div");
        item.className = "comment-item";
        item.innerHTML = `
            <div class="comment-header">
                <span class="comment-author-name">مستخدم</span>
                <span class="comment-time">الآن</span>
            </div>
            <div class="comment-content">${escapeHtml(input.value.trim())}</div>
        `;
        list.prepend(item);
    }
    input.value = "";
    showAppToast("تم إرسال التعليق");
};

window.copyPostText = function(postId) {
    const post = communityPosts.find(p => p.id === postId);
    if (!post) return;
    const full = `«${post.content}»\n— ${post.authorName}\nعبر تطبيق منشوراتي: https://manchorati.com/`;
    navigator.clipboard.writeText(full).then(() => {
        showAppToast("تم نسخ نص الاقتباس بنجاح");
    });
};

window.sharePost = function(postId) {
    const post = communityPosts.find(p => p.id === postId);
    if (!post) return;
    const shareData = {
        title: "اقتباس من منشوراتي",
        text: `«${post.content}» — ${post.authorName}`,
        url: `https://manchorati.com/?post=${postId}`
    };
    if (navigator.share) {
        navigator.share(shareData).catch(() => {});
    } else {
        copyPostText(postId);
    }
};

window.openStudioWithPost = function(postId) {
    const post = communityPosts.find(p => p.id === postId);
    if (post) {
        window.location.href = `studio.html?text=${encodeURIComponent(post.content)}&author=${encodeURIComponent(post.authorName)}`;
    } else {
        window.location.href = 'studio.html';
    }
};

window.filterByCategory = function(categoryName) {
    currentCategoryFilter = categoryName;
    document.querySelectorAll(".chip-category-item").forEach(c => {
        if (c.getAttribute("data-cat") === categoryName) c.classList.add("active");
        else c.classList.remove("active");
    });
    renderFeed();
};

function showAppToast(msg) {
    let toast = document.getElementById("appGlobalToast");
    if (!toast) {
        toast = document.createElement("div");
        toast.id = "appGlobalToast";
        toast.style.cssText = `
            position: fixed;
            bottom: 75px;
            left: 50%;
            transform: translateX(-50%);
            background: #1E293B;
            color: #FFFFFF;
            border: 1px solid var(--brand-primary);
            padding: 10px 20px;
            border-radius: 9999px;
            font-size: 13.5px;
            font-weight: 600;
            z-index: 1000;
            box-shadow: 0 10px 25px rgba(0,0,0,0.5);
            transition: opacity 0.3s ease;
            pointer-events: none;
        `;
        document.body.appendChild(toast);
    }
    toast.innerText = msg;
    toast.style.opacity = "1";
    setTimeout(() => {
        toast.style.opacity = "0";
    }, 2500);
}

function escapeHtml(str) {
    if (!str) return "";
    return str.toString()
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#039;");
}

// Global Init
window.addEventListener("DOMContentLoaded", () => {
    // 1. Initial posts from pre-rendered JSON
    if (window.__INITIAL_POSTS__ && window.__INITIAL_POSTS__.length > 0) {
        communityPosts = window.__INITIAL_POSTS__;
        renderFeed();
    }

    // 2. Live fetch from Firestore REST API
    fetchLiveFirestoreFeed();

    // Mood chips
    document.querySelectorAll(".chip-mood-item").forEach(chip => {
        chip.addEventListener("click", () => {
            document.querySelectorAll(".chip-mood-item").forEach(c => c.classList.remove("active"));
            chip.classList.add("active");
            currentMoodFilter = chip.getAttribute("data-mood") || "ALL";
            renderFeed();
        });
    });

    // Category chips
    document.querySelectorAll(".chip-category-item").forEach(chip => {
        chip.addEventListener("click", () => {
            document.querySelectorAll(".chip-category-item").forEach(c => c.classList.remove("active"));
            chip.classList.add("active");
            currentCategoryFilter = chip.getAttribute("data-cat") || "الكل";
            renderFeed();
        });
    });

    // Search bar
    const search = document.getElementById("mainSearchInput");
    if (search) {
        search.addEventListener("input", (e) => {
            currentSearch = e.target.value;
            renderFeed();
        });
    }
});
