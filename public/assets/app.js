/**
 * منشوراتي (MANCHORATI) - APP CORE JAVASCRIPT (FACEBOOK SOCIAL NETWORK EDITION)
 * Real Firestore Data, Facebook Feed UX, Reactions, Stories, Create Post Modal, AI Writer, and Live Sync
 */

const FIRESTORE_QUERY_URL = 'https://firestore.googleapis.com/v1/projects/manchorati-4ace8/databases/(default)/documents:runQuery?key=AIzaSyD6z0FAV1puTKcF9CN85UyvQ19uKoVyjNo';

// Exact Card Templates matching ThemeHelper.kt in Android App
const CARD_TEMPLATES = {
    'card_luxury': { bg: 'linear-gradient(135deg, #0B0F19, #1E293B)', border: '#EAB308', text: '#F8FAFC', font: 'Tajawal' },
    'card_neon_cyber': { bg: 'linear-gradient(135deg, #091A28, #022C43)', border: '#0284C7', text: '#38BDF8', font: 'Cairo' },
    'card_sunset': { bg: 'linear-gradient(135deg, #4A0E2E, #9A3412)', border: '#F59E0B', text: '#FFFFFF', font: 'Cairo' },
    'card_crimson_brand': { bg: 'linear-gradient(135deg, #991B1B, #DC2626)', border: '#EF4444', text: '#FFFFFF', font: 'Tajawal' },
    'card_emerald': { bg: 'linear-gradient(135deg, #064E3B, #047857)', border: '#10B981', text: '#FFFFFF', font: 'Tajawal' },
    'card_classic_paper': { bg: 'linear-gradient(135deg, #F6F0EA, #DFD2C0)', border: '#D6C4AE', text: '#1E293B', font: 'Aref Ruqaa' },
    'card_glass_royal': { bg: 'linear-gradient(135deg, #1E1B4B, #312E81)', border: '#6366F1', text: '#FFFFFF', font: 'Kufam' },
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

// State
let communityPosts = [];
let currentMoodFilter = "ALL";
let currentCategoryFilter = "الكل";
let currentSearch = "";
let currentSort = "LATEST"; // LATEST or TOP_LIKES

let userReactions = JSON.parse(localStorage.getItem("manchorati_reactions") || "{}");
let savedBookmarks = JSON.parse(localStorage.getItem("manchorati_bookmarks") || "[]");
let userCustomPosts = JSON.parse(localStorage.getItem("manchorati_user_posts") || "[]");
let postCommentsMap = JSON.parse(localStorage.getItem("manchorati_post_comments") || "{}");

// Stories Data (Matching Android Top Writers and Featured Quotes)
const STORIES_DATA = [
    {
        id: 'story-add',
        isAdd: true,
        author: 'أنا',
        avatar: '',
        title: 'أضف خاطرتك'
    },
    {
        id: 'story-1',
        author: 'محمد (المتصدر 👑)',
        avatar: '',
        quote: 'الخوف لا يمنع من الموت، ولكنه يمنع من الحياة.\nكن شجاعاً في مواجهة أحلامك.',
        theme: 'card_luxury',
        time: 'منذ 3 ساعات'
    },
    {
        id: 'story-2',
        author: 'زينب اليعقوبي',
        avatar: '',
        quote: 'لا تنتظر الفرصة بل اصنعها بنفسك؛ الأيام تمضي والهمة تصنع المعجزات.',
        theme: 'card_neon_cyber',
        time: 'منذ 5 ساعات'
    },
    {
        id: 'story-3',
        author: 'سلسلة النشاط 🔥',
        avatar: '',
        quote: 'أنت في اليوم الـ 4 من سلسلة الإلهام المستمرة!\nحافظ على شغفك اليومي.',
        theme: 'card_crimson_brand',
        time: 'اليوم'
    },
    {
        id: 'story-4',
        author: 'Nouzhi Meryem',
        avatar: '',
        quote: 'الحب أن تجد في عيني رفيقك أماناً لا يزول أبداً مهما قست الظروف.',
        theme: 'card_sunset',
        time: 'منذ 6 ساعات'
    },
    {
        id: 'story-5',
        author: 'أمين الفاسي',
        avatar: '',
        quote: 'وما نيل المطالب بالتمني .. ولكن تؤخذ الدنيا غلابا.\nصباح الإصرار واليقين.',
        theme: 'card_emerald',
        time: 'منذ 8 ساعات'
    }
];

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

// Render Stories
function renderStories() {
    const container = document.getElementById("storiesCarousel");
    if (!container) return;

    container.innerHTML = "";

    STORIES_DATA.forEach((s, idx) => {
        const item = document.createElement("div");
        item.className = `story-card-item ${s.isAdd ? 'story-card-add' : ''}`;

        if (s.isAdd) {
            item.innerHTML = `
                <div class="story-add-avatar">
                    <div style="font-size:36px; font-weight:800; color:rgba(255,255,255,0.3);">✦</div>
                </div>
                <div class="story-add-plus-badge">+</div>
                <span class="story-add-label">${s.title}</span>
            `;
            item.onclick = () => openCreatePostModal();
        } else {
            const tpl = getTemplate(s.theme);
            item.style.background = tpl.bg;
            item.style.borderColor = tpl.border;
            item.innerHTML = `
                <div class="story-card-avatar" style="border-color:${tpl.border}">
                    ${s.author[0]}
                </div>
                <div class="story-card-text-preview" style="color:${tpl.text};">
                    ${escapeHtml(s.quote)}
                </div>
                <div class="story-card-author">${escapeHtml(s.author)}</div>
            `;
            item.onclick = () => openStoryViewer(idx);
        }

        container.appendChild(item);
    });
}

// Story Viewer
let storyTimer = null;
let currentStoryIdx = 1;

function openStoryViewer(idx) {
    if (idx === 0) return openCreatePostModal();
    currentStoryIdx = idx;
    const s = STORIES_DATA[idx];
    if (!s) return;

    const modal = document.getElementById("storyViewerModal");
    const authorEl = document.getElementById("storyViewerAuthorName");
    const quoteEl = document.getElementById("storyViewerQuoteText");
    const timeEl = document.getElementById("storyViewerTime");
    const contentBox = document.getElementById("storyViewerContentBox");
    const fillBar = document.getElementById("storyProgressFill");

    authorEl.textContent = s.author;
    timeEl.textContent = s.time || "اليوم";
    quoteEl.textContent = s.quote;
    const tpl = getTemplate(s.theme);
    contentBox.style.background = tpl.bg;
    quoteEl.style.color = tpl.text;

    modal.classList.add("show");

    // Animate progress bar
    if (fillBar) {
        fillBar.style.width = "0%";
        setTimeout(() => { fillBar.style.width = "100%"; }, 50);
    }

    clearTimeout(storyTimer);
    storyTimer = setTimeout(() => {
        if (currentStoryIdx + 1 < STORIES_DATA.length) {
            openStoryViewer(currentStoryIdx + 1);
        } else {
            closeStoryViewer();
        }
    }, 5000);
}

function closeStoryViewer() {
    clearTimeout(storyTimer);
    const modal = document.getElementById("storyViewerModal");
    if (modal) modal.classList.remove("show");
}

// Live Fetch from Firestore REST API
async function fetchLiveFirestoreFeed() {
    try {
        const body = JSON.stringify({
            structuredQuery: {
                from: [{ collectionId: 'community_posts' }],
                orderBy: [{ field: { fieldPath: 'timestamp' }, direction: 'DESCENDING' }],
                limit: 60
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
            // Merge custom user posts created locally
            communityPosts = [...userCustomPosts, ...live];
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

    let postsToRender = [...communityPosts];

    if (currentSort === "TOP_LIKES") {
        postsToRender.sort((a, b) => (b.likesCount || 0) - (a.likesCount || 0));
    }

    const filtered = postsToRender.filter(post => {
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
                <button class="btn-browse-category" onclick="resetFeedFilters()" style="margin-top:16px;">عرض كافة المنشورات</button>
            </div>
        `;
        return;
    }

    filtered.forEach((post, index) => {
        const userReact = userReactions[post.id] || null;
        const isBookmarked = savedBookmarks.includes(post.id);
        const tpl = getTemplate(post.bgId);
        const fontName = getFontFamily(post.fontId);

        let avatarHtml = `<div class="author-avatar-box">${(post.authorName || "ك")[0]}</div>`;
        if (post.authorPhotoUrl && (post.authorPhotoUrl.startsWith('http') || post.authorPhotoUrl.startsWith('data:'))) {
            avatarHtml = `<div class="author-avatar-box" style="padding:0; overflow:hidden;"><img src="${escapeHtml(post.authorPhotoUrl)}" alt="${escapeHtml(post.authorName)}" style="width:100%; height:100%; object-fit:cover;"></div>`;
        }

        const comments = postCommentsMap[post.id] || [];
        const totalComments = (post.commentsCount || 0) + comments.length;

        // Reaction Button Text & Icon
        let reactIcon = `<svg viewBox="0 0 24 24"><path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"/></svg>`;
        let reactLabel = "إعجاب";
        let reactClass = "";

        if (userReact === 'LOVE') {
            reactLabel = "أحببته";
            reactClass = "liked-love";
        } else if (userReact === 'INSPIRE') {
            reactLabel = "ملهم";
            reactClass = "liked-inspire";
        } else if (userReact === 'CLAP') {
            reactLabel = "رائع";
            reactClass = "liked-clap";
        } else if (userReact === 'LIKE') {
            reactLabel = "أعجبني";
            reactClass = "liked-blue";
        }

        const card = document.createElement("article");
        card.className = "post-item-card";
        card.id = `post-${post.id}`;

        card.innerHTML = `
            <!-- Facebook Author Header -->
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

                <button class="btn-card-menu" onclick="copyPostText('${post.id}')" title="نسخ أو خيارات">
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
                <a href="categories.html" class="tag-badge-link">#${escapeHtml((post.tag || "خواطر").replace(/\s+/g, '_'))}</a>
            </div>

            <!-- Facebook Reactions & Comments Stats Bar -->
            <div style="display: flex; align-items: center; justify-content: space-between; padding: 6px 4px; font-size: 12.5px; color: var(--text-secondary); border-bottom: 1px solid var(--divider-color);">
                <div style="display: flex; align-items: center; gap: 4px;">
                    <span style="font-size: 14px;">👍❤️💡</span>
                    <span id="like-count-${post.id}">${(post.likesCount || 0) + (userReact ? 1 : 0)}</span>
                </div>
                <div style="display: flex; gap: 12px;">
                    <span onclick="toggleCommentsDrawer('${post.id}')" style="cursor: pointer;"><span id="comment-count-${post.id}">${totalComments}</span> تعليق</span>
                    <span onclick="sharePost('${post.id}')" style="cursor: pointer;">مشاركة</span>
                </div>
            </div>

            <!-- Facebook 4-Action Bar -->
            <div class="card-action-bar">
                <div class="action-bar-left">
                    <div class="like-btn-wrapper">
                        <!-- Floating Reaction Dock -->
                        <div class="fb-reactions-dock" id="reaction-dock-${post.id}">
                            <span class="fb-reaction-icon" onclick="reactToPost('${post.id}', 'LIKE')" title="إعجاب">👍</span>
                            <span class="fb-reaction-icon" onclick="reactToPost('${post.id}', 'LOVE')" title="أحببته">❤️</span>
                            <span class="fb-reaction-icon" onclick="reactToPost('${post.id}', 'INSPIRE')" title="ملهم">💡</span>
                            <span class="fb-reaction-icon" onclick="reactToPost('${post.id}', 'CLAP')" title="رائع">👏</span>
                        </div>

                        <button class="card-action-btn ${reactClass}" onclick="toggleQuickLike('${post.id}')" title="إعجاب">
                            ${reactIcon}
                            <span>${reactLabel}</span>
                        </button>
                    </div>

                    <button class="card-action-btn" onclick="toggleCommentsDrawer('${post.id}')" title="تعليقات">
                        <svg viewBox="0 0 24 24"><path d="M20 2H4c-1.1 0-1.99.9-1.99 2L2 22l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zM6 9h12v2H6V9zm8 5H6v-2h8v2zm4-6H6V6h12v2z"/></svg>
                        <span>تعليق</span>
                    </button>
                </div>

                <div class="action-bar-right">
                    <button class="card-action-btn" onclick="openStudioWithPost('${post.id}')" title="تصميم في الاستوديو">
                        <svg viewBox="0 0 24 24"><path d="M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04c.39-.39.39-1.02 0-1.41l-2.34-2.34c-.39-.39-1.02-.39-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z"/></svg>
                        <span>تصميم</span>
                    </button>

                    <button class="card-action-btn" onclick="sharePost('${post.id}')" title="مشاركة">
                        <svg viewBox="0 0 24 24"><path d="M18 16.08c-.76 0-1.44.3-1.96.77L8.91 12.7c.05-.23.09-.46.09-.7s-.04-.47-.09-.7l7.05-4.11c.54.5 1.25.81 2.04.81 1.66 0 3-1.34 3-3s-1.34-3-3-3-3 1.34-3 3c0 .24.04.47.09.7L8.04 9.81C7.5 9.31 6.79 9 6 9c-1.66 0-3 1.34-3 3s1.34 3 3 3c.79 0 1.5-.31 2.04-.81l7.12 4.16c-.05.21-.08.43-.08.65 0 1.61 1.31 2.92 2.92 2.92s2.92-1.31 2.92-2.92c0-1.61-1.31-2.92-2.92-2.92z"/></svg>
                        <span>مشاركة</span>
                    </button>

                    <button class="card-action-btn ${isBookmarked ? 'bookmarked' : ''}" onclick="toggleBookmark('${post.id}')" title="حفظ">
                        <svg viewBox="0 0 24 24"><path d="M17 3H7c-1.1 0-1.99.9-1.99 2L5 21l7-3 7 3V5c0-1.1-.9-2-2-2z"/></svg>
                    </button>
                </div>
            </div>

            <!-- Facebook Inline Comments Drawer -->
            <div class="card-comments-drawer" id="comments-drawer-${post.id}" style="display:none;">
                <div class="comment-input-row">
                    <input type="text" class="comment-text-box" id="comment-input-${post.id}" placeholder="اكتب تعليقاً على منشور ${escapeHtml(post.authorName)}..." onkeydown="if(event.key==='Enter') submitComment('${post.id}')">
                    <button class="btn-send-comment" onclick="submitComment('${post.id}')">إرسال</button>
                </div>
                <div class="comments-list" id="comments-list-${post.id}">
                    ${renderExistingCommentsHtml(post.id)}
                </div>
            </div>
        `;

        feedContainer.appendChild(card);

        // Responsive Google AdSense Slot every 4 posts
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

function renderExistingCommentsHtml(postId) {
    const comments = postCommentsMap[postId] || [];
    if (comments.length === 0) {
        return `<p style="font-size: 12.5px; color: var(--text-faint); text-align: center; padding: 6px;">كن أول من يترك تعليقاً في المجتمع ✨</p>`;
    }
    return comments.map(c => `
        <div style="display:flex; gap:10px; margin-bottom:8px; align-items:flex-start;">
            <div style="width:28px; height:28px; border-radius:50%; background:var(--brand-primary-blue); color:#FFF; display:flex; align-items:center; justify-content:center; font-size:12px; font-weight:700; flex-shrink:0;">${(c.author || 'ك')[0]}</div>
            <div style="background:var(--bg-surface); padding:8px 12px; border-radius:12px; flex:1;">
                <div style="font-family:'Cairo', sans-serif; font-size:12px; font-weight:700; color:#FFFFFF;">${escapeHtml(c.author)} <span style="font-size:10px; color:var(--text-faint); font-weight:normal;">• ${c.time || 'الآن'}</span></div>
                <div style="font-size:13px; color:var(--text-primary); margin-top:2px;">${escapeHtml(c.text)}</div>
            </div>
        </div>
    `).join("");
}

// React to Post
function reactToPost(postId, reactionType) {
    userReactions[postId] = reactionType;
    localStorage.setItem("manchorati_reactions", JSON.stringify(userReactions));
    showAppToast("تم تسجيل تفاعلك بنجاح! ✨");
    renderFeed();
}

function toggleQuickLike(postId) {
    if (userReactions[postId]) {
        delete userReactions[postId];
    } else {
        userReactions[postId] = 'LIKE';
    }
    localStorage.setItem("manchorati_reactions", JSON.stringify(userReactions));
    renderFeed();
}

function triggerDoubleTapLike(postId) {
    const heart = document.getElementById(`heart-pop-${postId}`);
    if (heart) {
        heart.classList.add("animate");
        setTimeout(() => heart.classList.remove("animate"), 800);
    }
    reactToPost(postId, 'LOVE');
}

// Submit Comment
function submitComment(postId) {
    const input = document.getElementById(`comment-input-${postId}`);
    if (!input) return;
    const val = input.value.trim();
    if (!val) return;

    if (!postCommentsMap[postId]) {
        postCommentsMap[postId] = [];
    }

    postCommentsMap[postId].push({
        author: 'أنا',
        text: val,
        time: 'الآن'
    });

    localStorage.setItem("manchorati_post_comments", JSON.stringify(postCommentsMap));
    input.value = "";

    const list = document.getElementById(`comments-list-${postId}`);
    if (list) {
        list.innerHTML = renderExistingCommentsHtml(postId);
    }

    const counter = document.getElementById(`comment-count-${postId}`);
    if (counter) {
        const post = communityPosts.find(p => p.id === postId);
        const total = (post ? (post.commentsCount || 0) : 0) + postCommentsMap[postId].length;
        counter.textContent = total;
    }

    showAppToast("تمت إضافة تعليقك بنجاح! 💬");
}

function toggleCommentsDrawer(postId) {
    const drawer = document.getElementById(`comments-drawer-${postId}`);
    if (!drawer) return;
    drawer.style.display = drawer.style.display === "none" ? "block" : "none";
}

// Bookmark / Save
function toggleBookmark(postId) {
    const idx = savedBookmarks.indexOf(postId);
    if (idx > -1) {
        savedBookmarks.splice(idx, 1);
        showAppToast("تمت إزالة المنشور من المحفوظات");
    } else {
        savedBookmarks.push(postId);
        showAppToast("تم حفظ المنشور في المفضلة! 🔖");
    }
    localStorage.setItem("manchorati_bookmarks", JSON.stringify(savedBookmarks));
    renderFeed();
}

// Share
function sharePost(postId) {
    const post = communityPosts.find(p => p.id === postId);
    if (!post) return;
    const shareText = `«${post.content}»\n— ${post.authorName} عبر تطبيق منشوراتي`;

    if (navigator.share) {
        navigator.share({
            title: "منشوراتي - Manchorati",
            text: shareText,
            url: window.location.href
        }).catch(() => {});
    } else {
        copyPostText(postId);
    }
}

function copyPostText(postId) {
    const post = communityPosts.find(p => p.id === postId);
    if (!post) return;
    navigator.clipboard.writeText(post.content).then(() => {
        showAppToast("تم نسخ نص المنشور بنجاح! 📋");
    });
}

function openStudioWithPost(postId) {
    const post = communityPosts.find(p => p.id === postId);
    if (!post) return;
    window.location.href = `studio.html?quote=${encodeURIComponent(post.content)}&author=${encodeURIComponent(post.authorName)}&bg=${encodeURIComponent(post.bgId || 'card_luxury')}`;
}

// Create Post Modal State
let activeCreatePostTheme = 'card_luxury';

function openCreatePostModal(prefilledTopic = '') {
    const modal = document.getElementById("createPostModal");
    if (!modal) return;
    modal.classList.add("show");
    const textarea = document.getElementById("createPostTextarea");
    if (textarea && prefilledTopic) {
        textarea.value = prefilledTopic;
        updateCreatePostPreview();
    }
}

function closeCreatePostModal() {
    const modal = document.getElementById("createPostModal");
    if (modal) modal.classList.remove("show");
}

function selectCreatePostTheme(themeKey) {
    activeCreatePostTheme = themeKey;
    document.querySelectorAll(".theme-pill-chip").forEach(chip => {
        chip.classList.toggle("selected", chip.getAttribute("data-theme") === themeKey);
    });
    updateCreatePostPreview();
}

function updateCreatePostPreview() {
    const textarea = document.getElementById("createPostTextarea");
    const previewBox = document.getElementById("createPostPreviewCard");
    const previewText = document.getElementById("createPostPreviewText");
    if (!textarea || !previewBox || !previewText) return;

    const val = textarea.value.trim();
    previewText.textContent = val || "هنا تظهر معاينة حية لمنشورك كما سيظهر للجميع في المجتمع...";
    const tpl = getTemplate(activeCreatePostTheme);
    previewBox.style.background = tpl.bg;
    previewBox.style.borderColor = tpl.border;
    previewText.style.color = tpl.text;
    previewText.style.fontFamily = `'${tpl.font}', sans-serif`;
}

function submitNewCommunityPost() {
    const textarea = document.getElementById("createPostTextarea");
    const catSelect = document.getElementById("createPostCategorySelect");
    if (!textarea) return;

    const text = textarea.value.trim();
    if (!text) {
        alert("الرجاء كتابة نص المنشور أولاً");
        return;
    }

    const newPost = {
        id: 'local-' + Date.now(),
        authorName: 'أنا',
        authorUsername: 'my_profile',
        authorPhotoUrl: '',
        authorBadge: 'كاتب نشط',
        authorStreak: 4,
        content: text,
        bgId: activeCreatePostTheme,
        fontId: getTemplate(activeCreatePostTheme).font.toLowerCase(),
        textColor: '#FFFFFF',
        tag: catSelect ? catSelect.value : 'خواطر عميقة',
        mood: 'CHILL',
        likesCount: 1,
        commentsCount: 0,
        timestamp: Date.now()
    };

    userCustomPosts.unshift(newPost);
    localStorage.setItem("manchorati_user_posts", JSON.stringify(userCustomPosts));

    communityPosts.unshift(newPost);
    textarea.value = "";
    closeCreatePostModal();
    renderFeed();
    showAppToast("تم نشر منشورك في مجتمع منشوراتي بنجاح! 🎉");
}

// AI Literary Assistant
const AI_QUOTES_LIBRARY = {
    'wisdom': [
        { text: "ليس الفخر أن تسقط، بل الفخر أن تنهض في كل مرة تسقط فيها بعزيمة أكبر.", author: "حكمة فلسفية" },
        { text: "الصمت في بعض المواقف ليس ضعفاً، بل أبلغ رسالة احترام لنفسك ولمن يستحق.", author: "تأملات الحياة" },
        { text: "العقول الكبيرة تناقش الأفكار، والمتوسطة تناقش الأحداث، والصغيرة تناقش الأشخاص.", author: "إليانور روزفلت" }
    ],
    'love': [
        { text: "أنت لست جزءاً من قلبي، بل أنت القلب وكل ما ينبض بداخله من شعور.", author: "همسات عشق" },
        { text: "لو كان للجمال وطناً لكان عينيك، ولو كان للحب عاصمة لكانت ابتسامتك.", author: "نزار قباني" },
        { text: "سلامٌ على قلوبٍ نقية لا نملك في حضرتك إلا أن نحبها حباً صادقاً بلا قيود.", author: "وجدانيات" }
    ],
    'success': [
        { text: "النجاح ليس مصادفة، إنه إصرار، وعمل دؤوب، وتعلّم من كل عثرة تصادف دربك.", author: "قوة الإرادة" },
        { text: "إذا أردت أن ترى قوس قزح، فعليك أن تتحمل المطر أولاً.", author: "تحفيز وإصرار" },
        { text: "ما دامت الشمس تشرق كل صباح، فإن أمامك فرصة جديدة لتصنع مجدك.", author: "طاقة إيجابية" }
    ],
    'poetry': [
        { text: "ولربَّ نازلةٍ يضيقُ بها الفتى .. ذرعاً وعندَ اللهِ منها المخرجُ.", author: "الإمام الشافعي" },
        { text: "وما نيل المطالب بالتمني .. ولكن تؤخذ الدنيا غلابا.", author: "أحمد شوقي" },
        { text: "إذا غامَرْتَ في شَرَفٍ مَرُومِ .. فَلا تَقْنَعْ بما دونَ النّجُومِ.", author: "المتنبي" }
    ]
};

function openAiAssistantModal() {
    const modal = document.getElementById("aiAssistantModal");
    if (modal) modal.classList.add("show");
    generateAiQuotes('wisdom');
}

function closeAiAssistantModal() {
    const modal = document.getElementById("aiAssistantModal");
    if (modal) modal.classList.remove("show");
}

function generateAiQuotes(topic) {
    const list = AI_QUOTES_LIBRARY[topic] || AI_QUOTES_LIBRARY['wisdom'];
    const container = document.getElementById("aiGeneratedQuotesList");
    if (!container) return;

    container.innerHTML = `
        <div style="text-align:center; padding: 20px; color: var(--text-secondary);">
            <div style="display:inline-block; width:24px; height:24px; border:2.5px solid rgba(56,189,248,0.2); border-top-color:var(--brand-primary); border-radius:50%; animation: spin 0.7s linear infinite;"></div>
            <p style="margin-top:8px; font-size:13px; font-family:'Cairo', sans-serif;">جارِ صياغة الاقتباسات الأدبية بالذكاء الاصطناعي...</p>
        </div>
    `;

    setTimeout(() => {
        container.innerHTML = "";
        list.forEach(q => {
            const card = document.createElement("div");
            card.style.cssText = "background: var(--bg-surface); border: 1px solid var(--divider-color); border-radius: 14px; padding: 14px 16px; margin-bottom: 10px;";
            card.innerHTML = `
                <p style="font-family:'Tajawal', sans-serif; font-size: 14.5px; color: #FFFFFF; line-height: 1.7; margin-bottom: 8px;">«${escapeHtml(q.text)}»</p>
                <div style="display:flex; justify-content:space-between; align-items:center;">
                    <span style="font-size:12px; color:var(--brand-primary); font-family:'Cairo', sans-serif;">— ${escapeHtml(q.author)}</span>
                    <div style="display:flex; gap:8px;">
                        <button class="cat-action-btn" onclick="useAiQuote('${escapeJs(q.text)}')" style="font-size:11px; padding:4px 10px;">نشر بالمجتمع</button>
                        <button class="cat-action-btn btn-studio-action" onclick="openStudioWithText('${escapeJs(q.text)}', '${escapeJs(q.author)}')" style="font-size:11px; padding:4px 10px;">تصميم</button>
                    </div>
                </div>
            `;
            container.appendChild(card);
        });
    }, 350);
}

function useAiQuote(text) {
    closeAiAssistantModal();
    openCreatePostModal(text);
}

function openStudioWithText(text, author) {
    window.location.href = `studio.html?quote=${encodeURIComponent(text)}&author=${encodeURIComponent(author)}`;
}

// Saved Bookmarks Modal
function openSavedPostsModal() {
    const modal = document.getElementById("savedPostsModal");
    const container = document.getElementById("savedPostsList");
    if (!modal || !container) return;

    modal.classList.add("show");
    const saved = communityPosts.filter(p => savedBookmarks.includes(p.id));

    if (saved.length === 0) {
        container.innerHTML = `
            <div style="text-align:center; padding: 40px 10px; color: var(--text-secondary);">
                <div style="font-size:36px; margin-bottom:8px;">🔖</div>
                <h4 style="color:#FFF; font-size:16px;">لا توجد منشورات محفوظة بعد</h4>
                <p style="font-size:13px; margin-top:4px;">انقر على زر الحفظ في أي منشور لحفظه هنا والرجوع إليه لاحقاً.</p>
            </div>
        `;
        return;
    }

    container.innerHTML = "";
    saved.forEach(p => {
        const item = document.createElement("div");
        item.style.cssText = "background: var(--bg-surface); border: 1px solid var(--divider-color); border-radius: 12px; padding: 12px; margin-bottom: 10px;";
        item.innerHTML = `
            <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:6px;">
                <span style="font-family:'Cairo', sans-serif; font-size:12.5px; font-weight:700; color:#FFF;">${escapeHtml(p.authorName)}</span>
                <button onclick="toggleBookmark('${p.id}'); openSavedPostsModal();" style="background:none; border:none; color:var(--brand-rose); font-size:12px; cursor:pointer;">إزالة</button>
            </div>
            <p style="font-size:13.5px; color:var(--text-primary); line-height:1.6; margin-bottom:8px;">${escapeHtml(p.content)}</p>
            <div style="display:flex; justify-content:flex-end; gap:8px;">
                <button class="cat-action-btn" onclick="copyPostText('${p.id}')">نسخ</button>
                <button class="cat-action-btn btn-studio-action" onclick="openStudioWithPost('${p.id}')">تصميم</button>
            </div>
        `;
        container.appendChild(item);
    });
}

function closeSavedPostsModal() {
    const modal = document.getElementById("savedPostsModal");
    if (modal) modal.classList.remove("show");
}

// Popovers
function toggleNotificationsPopover() {
    const pop = document.getElementById("notificationsPopover");
    if (pop) pop.classList.toggle("show");
    const prof = document.getElementById("profileDropdown");
    if (prof) prof.classList.remove("show");
}

function toggleProfileDropdown() {
    const prof = document.getElementById("profileDropdown");
    if (prof) prof.classList.toggle("show");
    const pop = document.getElementById("notificationsPopover");
    if (pop) pop.classList.remove("show");
}

// Global Click Listener to close popovers
document.addEventListener("click", (e) => {
    const notifBtn = document.getElementById("btnNotificationsToggle");
    const notifPop = document.getElementById("notificationsPopover");
    if (notifPop && notifBtn && !notifBtn.contains(e.target) && !notifPop.contains(e.target)) {
        notifPop.classList.remove("show");
    }

    const profBtn = document.getElementById("btnProfileToggle");
    const profPop = document.getElementById("profileDropdown");
    if (profPop && profBtn && !profBtn.contains(e.target) && !profPop.contains(e.target)) {
        profPop.classList.remove("show");
    }
});

// Toast
let toastTimer = null;
function showAppToast(msg) {
    let t = document.getElementById("appToast");
    if (!t) {
        t = document.createElement("div");
        t.id = "appToast";
        t.className = "toast-notice";
        document.body.appendChild(t);
    }
    t.textContent = msg;
    t.classList.add("show");
    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => t.classList.remove("show"), 2500);
}

function escapeHtml(str) {
    if (!str) return "";
    return str.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;");
}

function escapeJs(str) {
    if (!str) return "";
    return str.replace(/\\/g, "\\\\").replace(/'/g, "\\'").replace(/"/g, '\\"').replace(/\n/g, " ");
}

function resetFeedFilters() {
    currentMoodFilter = "ALL";
    currentCategoryFilter = "الكل";
    currentSearch = "";
    document.querySelectorAll(".mood-chip").forEach(c => c.classList.remove("active"));
    const allChip = document.querySelector(".mood-chip[data-mood='ALL']");
    if (allChip) allChip.classList.add("active");
    const sInput = document.getElementById("globalSearchInput");
    if (sInput) sInput.value = "";
    renderFeed();
}

function filterByMood(mood) {
    currentMoodFilter = mood;
    document.querySelectorAll(".mood-chip").forEach(c => c.classList.remove("active"));
    const activeChip = document.querySelector(`.mood-chip[data-mood='${mood}']`);
    if (activeChip) activeChip.classList.add("active");
    renderFeed();
}

function filterByCategory(cat) {
    currentCategoryFilter = cat;
    renderFeed();
}

function setFeedSort(sortMode) {
    currentSort = sortMode;
    document.querySelectorAll(".sort-tab-btn").forEach(b => b.classList.toggle("active", b.getAttribute("data-sort") === sortMode));
    renderFeed();
}

// Init
window.addEventListener("DOMContentLoaded", () => {
    // 1. Initial initial posts
    if (window.__INITIAL_POSTS__ && Array.isArray(window.__INITIAL_POSTS__)) {
        communityPosts = [...userCustomPosts, ...window.__INITIAL_POSTS__];
    }

    renderStories();
    renderFeed();

    // 2. Live fetch
    fetchLiveFirestoreFeed();

    // 3. Search bindings
    const sInput = document.getElementById("globalSearchInput");
    if (sInput) {
        sInput.addEventListener("input", (e) => {
            currentSearch = e.target.value;
            renderFeed();
        });
    }
});
