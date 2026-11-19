package com.manchorati.www

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

data class VirtualPersona(
    val id: String,
    val name: String,
    val username: String,
    val avatarResource: String,
    val badge: String = "",
    val bio: String = ""
)

object VirtualCommunityManager {

    private const val TAG = "VirtualCommunity"
    private const val PREFS_NAME = "virtual_community_prefs"
    private const val KEY_PERSONAS_SEEDED = "personas_seeded_v4"
    private const val KEY_LAST_AUTO_POST_TIME = "last_auto_post_time"

    val PERSONAS = listOf(
        VirtualPersona("persona_sara_b", "سارة بناني", "sara_bennani", "avatar_2", "verified", "شغوفة بالأدب والفكر والكلمة الطيبة "),
        VirtualPersona("persona_yassine_a", "ياسين العلمي", "yassine_al", "avatar_9", "writer", "كاتب ومهتم بتطوير الذات وبناء العادات "),
        VirtualPersona("persona_hamza_s", "حمزة الصالحي", "hamza_s", "avatar_11", "", "باحث عن الحكمة والهدوء في تفاصيل الحياة "),
        VirtualPersona("persona_ilham_m", "إلهام المنصوري", "ilham_m", "avatar_4", "writer", "أكتب لأجد نفسي بين السطور والخواطر "),
        VirtualPersona("persona_amine_f", "أمين الفاسي", "amine_elfassi", "avatar_5", "", "مهتم بالقراءة ونشر الطاقة الإيجابية "),
        VirtualPersona("persona_maryam_i", "مريم الإدريسي", "maryam_idrissi", "avatar_10", "verified", "محبة للأدب الراقي ولحظات التأمل "),
        VirtualPersona("persona_omar_t", "عمر التازي", "omar_tazi", "avatar_7", "", "عاشق للهدوء والكتب وفنجان قهوة الصباح "),
        VirtualPersona("persona_kaoutar_m", "كوثر المرابط", "kaoutar_m", "avatar_13", "writer", "الكلمات أثر لا يزول.. أشارككم ما راق لي "),
        VirtualPersona("persona_nabil_c", "نبيل الشريف", "nabil_cherif", "avatar_12", "", "طالب معرفة ومحب للحكم والأمثال القديمة "),
        VirtualPersona("persona_fz_zahra", "فاطمة الزهراء", "fz_zahra", "avatar_15", "", "ابتسم فالحياة أجمل بحسن الظن بالله "),
        VirtualPersona("persona_karim_n", "كريم الناصري", "karim_n", "avatar_14", "writer", "كلمات تصنع الفارق وأفكار تبني العزيمة "),
        VirtualPersona("persona_houda_a", "هدى العمراني", "houda_amrani", "avatar_16", "", "خواطر وكلمات دافئة من القلب إلى القلب "),
        VirtualPersona("persona_tariq_b", "طارق بنسودة", "tariq_b", "avatar_19", "", "النجاح يبدأ بخطوة واستمرار لا يتوقف "),
        VirtualPersona("persona_sanaa_b", "سناء بوزيد", "sanaa_b", "avatar_17", "verified", "مؤسسة نادي قراء ومحبة لنشر الإيجابية "),
        VirtualPersona("persona_youssef_r", "يوسف الرحماني", "youssef_r", "avatar_20", "", "في رحلة مستمرة للتعلم واكتساب الحكمة "),
        VirtualPersona("persona_zineb_y", "زينب اليعقوبي", "zineb_y", "avatar_18", "", "أحب التفاصيل الصغيرة واللحظات الصادقة "),
        VirtualPersona("persona_adel_m", "عادل المتوكل", "adel_m", "avatar_6", "writer", "حروف تنبض بالحياة ورسائل أمل دائمة "),
        VirtualPersona("persona_wafaa_s", "وفاء الصنهاجي", "wafaa_s", "avatar_1", "", "اللهم سعة وسلاماً يغمر القلب "),
        VirtualPersona("persona_mostafa_w", "مصطفى الوديع", "mostafa_w", "avatar_8", "", "الهدوء نعمة، والكلمة الطيبة صدقة "),
        VirtualPersona("persona_nadia_b", "نادية برادة", "nadia_barrada", "avatar_3", "", "نور وأمل يزهر في كل مكان "),
        VirtualPersona("persona_mehdi_k", "المهدي الكتاني", "mehdi_kettani", "avatar_7", "writer", "محب للتأمل في جمال الحياة وسحر الكلمات "),
        VirtualPersona("persona_salma_h", "سلمى الهلالي", "salma_hilali", "avatar_10", "verified", "قارئة شغوفة ومهتمة بالتربية الإيجابية وعلم النفس "),
        VirtualPersona("persona_bilal_m", "بلال المراكشي", "bilal_m", "avatar_12", "", "في رحاب الكلمات الصادقة التي تبني وتلهم "),
        VirtualPersona("persona_chaimaa_r", "شيماء الركراكي", "chaimaa_r", "avatar_2", "writer", "أكتب بحب وأنثر الفرح والسكينة أينما حللت "),
        VirtualPersona("persona_anass_b", "أنس البقالي", "anass_baqqali", "avatar_9", "", "عاشق للأمثال التراثية وحكم الزمن الجميل "),
        VirtualPersona("persona_soukaina_t", "سكينة التلمساني", "soukaina_t", "avatar_16", "verified", "باحثة في الأدب العربي وتطوير الفكر الإنساني "),
        VirtualPersona("persona_rachid_o", "رشيد العثماني", "rachid_othmani", "avatar_11", "", "الهدوء غاية، والرضا كنز لا يفنى أبداً "),
        VirtualPersona("persona_imane_c", "إيمان العلمي", "imane_alami", "avatar_4", "", "حين تعجز الحروف، تتحدث القلوب بنقاء وصدق "),
        VirtualPersona("persona_walid_s", "وليد السوسي", "walid_soussi", "avatar_5", "writer", "خطوة نحو الأمام كل يوم، والإصرار يصنع المعجزات "),
        VirtualPersona("persona_asmaa_z", "أسماء الزياني", "asmaa_zayani", "avatar_18", "", "ابتسامة دافئة وكلمة طيبة تصنع يوماً جميلاً ")
    )

    fun isVirtualUser(userId: String?): Boolean {
        if (userId.isNullOrEmpty()) return true
        return userId.startsWith("persona_") ||
                userId == "manchorati_curated" ||
                userId == "local_guest" ||
                PERSONAS.any { it.id == userId }
    }

    private val BG_PRESETS = listOf(
        "card_luxury",
        "card_crimson_brand",
        "card_ocean_breeze",
        "card_emerald",
        "card_sunset",
        "card_glass_royal",
        "card_neon_cyber",
        "card_classic_paper"
    )

    private val FONT_PRESETS = listOf("tajawal", "cairoplay", "kufam", "arefruqaa", "marhey")

    private var cachedCuratedPosts: List<CommunityPost> = emptyList()

    fun getPersonaById(id: String): VirtualPersona {
        return PERSONAS.find { it.id == id } ?: PERSONAS[Math.abs(id.hashCode()) % PERSONAS.size]
    }

    fun getPersonaForId(seed: Int): VirtualPersona {
        val idx = Math.abs(seed) % PERSONAS.size
        return PERSONAS[idx]
    }

    fun toVirtualCommunityPost(
        post: Post,
        context: Context,
        targetTag: String? = null,
        staggerMinutesAgo: Long = 0L
    ): CommunityPost {
        val persona = getPersonaForId(post.id)
        val selectedBg = BG_PRESETS[Math.abs(post.id.hashCode()) % BG_PRESETS.size]
        val selectedFont = FONT_PRESETS[Math.abs(post.id.hashCode() / 5) % FONT_PRESETS.size]
        // معدل إعجابات طبيعي ومتوازن للبوتات (بين 6 و 20 لايك بدلاً من الأرقام الضخمة السابقة)
        val baseLikes = (6 + (Math.abs(post.id.hashCode()) % 15)).toLong()
        val postId = "vpost_${post.id}"
        val isLiked = LocalLikesManager.isLiked(context, postId)
        val currentUid = AuthManager.currentUser?.uid ?: "local_guest"
        val likedByList = if (isLiked) listOf(currentUid) else emptyList()
        val baseComments = 0L

        val timeOffset = if (staggerMinutesAgo > 0L) {
            staggerMinutesAgo * 60 * 1000L
        } else {
            val hours = (Math.abs(post.id.hashCode()) % 30) + 1
            val mins = (Math.abs(post.id.hashCode() / 3) % 50)
            (hours * 3600L + mins * 60L) * 1000L
        }
        val postTimestamp = System.currentTimeMillis() - timeOffset

        val resolvedTag = targetTag?.takeIf { it.isNotEmpty() && it != "الكل" }
            ?: post.categoryName.takeIf { it.isNotEmpty() }
            ?: "خواطر"

        return CommunityPost(
            id = postId,
            authorId = persona.id,
            authorName = persona.name,
            authorUsername = persona.username,
            authorPhotoUrl = persona.avatarResource,
            content = post.content,
            bgId = selectedBg,
            fontId = selectedFont,
            textColor = "",
            tag = resolvedTag,
            likesCount = if (isLiked) baseLikes + 1 else baseLikes,
            commentsCount = baseComments,
            reportCount = 0L,
            likedBy = likedByList,
            authorBadge = persona.badge,
            authorStreak = when (Math.abs(persona.id.hashCode()) % 5) {
                0 -> 100
                1 -> 60
                2 -> 30
                3 -> 7
                else -> 0
            },
            lastLikedAt = System.currentTimeMillis(),
            timestamp = postTimestamp
        )
    }

    @Synchronized
    fun getCuratedFeedPosts(context: Context, forceRefresh: Boolean = false): List<CommunityPost> {
        if (cachedCuratedPosts.isNotEmpty() && !forceRefresh) {
            return cachedCuratedPosts
        }

        try {
            val dbHelper = DatabaseHelper(context)
            val rawPosts = dbHelper.getRandomCuratedPosts(35)
            if (rawPosts.isNotEmpty()) {
                val list = mutableListOf<CommunityPost>()
                for (i in rawPosts.indices) {
                    val p = rawPosts[i]
                    val staggerMinutes = 15L + (i * 28L) + (Math.abs(p.id.hashCode()) % 15)
                    list.add(toVirtualCommunityPost(p, context, staggerMinutesAgo = staggerMinutes))
                }
                cachedCuratedPosts = list
                return list
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error generating curated feed posts", e)
        }
        return cachedCuratedPosts
    }

    fun getContextualComment(content: String, tag: String, existingComments: List<String> = emptyList()): String {
        val lower = (content + " " + tag).lowercase()
        val pool = when {
            lower.contains("تحفيز") || lower.contains("نجاح") || lower.contains("هدف") ||
                    lower.contains("عزيمة") || lower.contains("تطوير") || lower.contains("طموح") ||
                    lower.contains("إصرار") || lower.contains("عمل") || lower.contains("شغف") -> {
                listOf(
                    "طاقة إيجابية نحتاجها في بداية يومنا، شكراً لك",
                    "كلام محفز وفي الصميم، استمر يا بطل",
                    "بالفعل، لا شيء مستحيل مع الإصرار والعزيمة",
                    "مشاركاتك دائماً تعطي دفعة أمل كبيرة",
                    "مبدع كالعادة، كلام يبني الهمم",
                    "عين العقل.. النجاح رحلة تبدأ بخطوة إيمان وثقة",
                    "جرعة تحفيز جاءت في وقتها تماماً!",
                    "من أجمل ما قرأت اليوم، كل التوفيق لك في مسيرتك",
                    "عزيمة لا تلين وطرح يبعث على التفاؤل، أحسنت",
                    "هذا هو التفكير الإيجابي الذي نحتاج نشره دائماً",
                    "كلمات تحرك الشغف وتجدد الأمل في النفوس",
                    "صدقت، الصعوبات تصنع الأقوياء فقط",
                    "الاستمرارية هي سر كل نجاح عظيم.. شكراً لك",
                    "حفظت المنشور عندي لأعود له كلما احتجت تذكيراً ودافعاً",
                    "ما شاء الله، فكر متقد وطاقة ملهمة للجميع",
                    "كلام يوزن بماء الذهب، سلمت يمينك",
                    "الخطوة الأولى دائماً هي الأهم، مشاركة دافعة للأمام",
                    "أبدعت في التعبير، نحتاج هذا الإلهام بشدة",
                    "أثرت فيّ هذه الكلمات، شكراً من القلب على هذا التحفيز",
                    "الإيمان بالقدرة نصف الوصول، سلم فكرك الراقي",
                    "كلما قرأت لك ازددت يقيناً أن الأمل لا يموت",
                    "قوة الطرح ودقة المعنى، دمت مميزاً وملهمنا",
                    "من أعظم ما قرأت اليوم.. تذكير ملهم يوقظ الشغف من جديد",
                    "كل عثرة هي درس يبني نجاح الغد، سلمت يداك على هذا الطرح القوي",
                    "القمة تتسع للمجتهدين دائماً، منشور يشحن الطاقة ويزرع الثقة",
                    "كلام حكيم يختصر مسافات طويلة في طريق تطوير الذات",
                    "الإصرار هو الفارق الحقيقي، شكراً على هذه الكلمات المؤثرة",
                    "طاقة وعزيمة وإلهام حقيقي.. منشور يستحق أن يقرأه الجميع",
                    "بداية كل إنجاز فكرة تؤمن بها، وأنت تنشر هذا الإيمان ببراعة",
                    "أحييك على هذا الفكر الإيجابي البنّاء، استمر في نشر هذا النور",
                    "أصبت كبد الحقيقة، النجاح لا يأتي بالصدفة بل بالاستمرار اليومي",
                    "رسالة تحفيزية ممتازة لامست هدفي وجددت فيّ الرغبة للمواصلة"
                )
            }
            lower.contains("دعاء") || lower.contains("يارب") || lower.contains("الله") ||
                    lower.contains("سكينة") || lower.contains("استغفار") || lower.contains("أذكار") ||
                    lower.contains("جمعة") || lower.contains("قرآن") || lower.contains("توكل") ||
                    lower.contains("رحمة") || lower.contains("حمد") -> {
                listOf(
                    "اللهم آمين يا رب العالمين، جزاك الله كل خير",
                    "ونعم بالله العلي العظيم، كلمات تريح القلب والروح",
                    "بارك الله فيك ونفع بك، أثابك الله",
                    "ألا بذكر الله تطمئن القلوب.. تذكير طيب ومبارك",
                    "ما أجمل هذا الدعاء، ربي يتقبل منا ومنكم صالح الأعمال",
                    "اللهم اجعلنا من المتوكلين عليك حق التوكل",
                    "سبحان الله وبحمده.. تذكرة عطرة في ميزان حسناتك",
                    "راحة نفسية وسكينة تسكن الكلمات، بارك الله في قلمك",
                    "ربي يفتح عليك أبواب الخير والبركة والرزق",
                    "اللهم استجب لدعواتنا ولصاحب المنشور ولكل من قرأ",
                    "لا إله إلا أنت سبحانك إني كنت من الظالمين",
                    "كلمات طيبة من روح نقية، جزاك الله الفردوس الأعلى",
                    "تذكير يلامس الفؤاد، ربي يكتب أجرك وأجر من شاركه",
                    "اللهم سكينة وطمأنينة تغمر قلوبنا جميعاً",
                    "اللهم صلِّ وسلم وبارك على نبينا وحبيبنا محمد",
                    "ما خاب من علّق قلبه بالله، أحسنت النشر والاختيار",
                    "دعوة صادقة من القلب: ربي يسعدك ويحقق لك أمانيك",
                    "تذكرة مباركة، رفع الله قدرك في الدارين",
                    "نور الله دربك كما أنرت يومنا بهذا التذكير المبارك",
                    "الحمد لله دائماً وأبداً على كل حال وفي كل حين",
                    "كلام مبارك يشرح الصدر ويهدئ البال، دمت في حفظ الله",
                    "اللهم استجب وتقبل وارزق صاحب المنشور سعادة الدارين",
                    "ما أجمل البدايات حين تتعطر بذكر الله والتوكل عليه، أثابك الله",
                    "سكينة تلامس القلب.. جزاك ربي خيراً على هذا التذكير العطر",
                    "من لزم الاستغفار جعل الله له من كل همٍّ فرجاً.. تذكرة مباركة",
                    "حروف تنبض بالإيمان والنور، رزقك الله راحة البال وطمأنينة النفس",
                    "اللهم آمين لكل ما في قلوبنا من أمنيات ودعوات، بارك الله فيك",
                    "اللهم اجعلنا ممن يذكرك في الرخاء والشدة، أحسن الله إليك",
                    "تذكير يبعث في النفس هدوءاً وسكينة لا توصف، رفع الله قدرك",
                    "يا رب فرجاً لكل صابر وشفاءً لكل مريض، كلمات رقيقة ومباركة",
                    "جعلها الله في ميزان حسناتك وصدقة جارية لك، أحسنت النشر"
                )
            }
            lower.contains("حزن") || lower.contains("صبر") || lower.contains("فراق") ||
                    lower.contains("ألم") || lower.contains("تعب") || lower.contains("هم") ||
                    lower.contains("وجع") || lower.contains("ضيق") || lower.contains("بكاء") ||
                    lower.contains("خيبة") || lower.contains("غياب") -> {
                listOf(
                    "طبطبة رقيقة على القلب.. كل مرٍّ سيمر بإذن الله",
                    "فرج الله قريب، ما ضاقت إلا لتفرج، دمت بخير وسلام",
                    "كلمات صادقة تلامس أعماق الروح، ربي يسعد قلبك ويبعد عنك الأحزان",
                    "إن بعد العسر يسراً.. كلام معبّر جداً يخفف وطأة الأيام",
                    "سلامٌ على قلبك، لا تحزن فالله أرحم بنا من أنفسنا",
                    "أعان الله كل قلب يحمل وجعاً لا يبوح به للناس",
                    "الصبر مفتاح الفرج، وأجمل الأيام لم نعشها بعد بإذن الله",
                    "حروف تنبض بصدق عميق، شفا الله قلبك وأراح بالك",
                    "مهما طال ليل الأحزان، فصبح الفرج آتٍ لا محالة",
                    "كلامك لامس جرحاً عميقاً.. شكراً على صدق هذا الشعور",
                    "عوض الله دائماً ينسينا ما فقدناه، تفاءل خيراً تجده",
                    "أحياناً تكون الكلمات هي الملاذ الوحيد، سلم هذا البوح العذب",
                    "ربي يبدل همك فرحاً وضيقك فرجاً وسعادة لا تنتهي",
                    "كل دمعة في عينك لها عند الله شأن ورعاية، لا تبتئس",
                    "أحسست بكل حرف كأنه يصف ما في داخلي.. دمت صامداً وقوياً",
                    "يا رب سكينة تنزل على قلبك كالبلسم الشافي المعافي",
                    "الصبر مر مذاقه، لكن عواقبه أحلى من كل رجاء",
                    "هوني عليك يا نفس، فما كُتب لك سيأتيك برحمة الله ولطفه",
                    "كتبت فأوجعت بصدق قلمك، حفظك الله من كل سوء وكرب",
                    "سيمر كل هذا الثقل وتشرق شمس الأيام الجميلة بإذن الله، كن بخير",
                    "القلب الذي يتحمل الكثير يستحق فرحاً ينسيه كل ما مضى، سلام على روحك",
                    "كلامك رقيق ومعبّر، خفف الله عنك وعن كل قلب أثقلته الأيام",
                    "ما بين غمضة عين وانتباهتها يغير الله من حال إلى حال، تفاءل",
                    "ألم الأمس يصنع حكمة اليوم وقوة الغد، كلمات صادقة جداً",
                    "طبطبة لطيفة جاءت في وقتها، شكراً لمن كتب ومن شارك هذا الإحساس",
                    "لا تيأس أبداً، فألطاف الله الخفية تأتي حين تنقطع كل الأسباب",
                    "حروف موجعة لكنها حقيقية وتفيض صدقاً، فرج الله همومك قريباً",
                    "لكل شدة نهاية ولكل حزن موعد مع الفرح، دمت صابراً ومحتسباً",
                    "حماك الله وأراح فؤادك، بوح إنساني صادق يلامس الوجدان بعمق"
                )
            }
            lower.contains("حب") || lower.contains("عشق") || lower.contains("غرام") ||
                    lower.contains("شوق") || lower.contains("حنين") || lower.contains("وفاء") ||
                    lower.contains("قلب") || lower.contains("أمان") || lower.contains("روح") -> {
                listOf(
                    "ما أرق هذه الحروف وأجمل معانيها الدافئة",
                    "إحساس عالي وذوق رفيع جداً يلامس الوجدان",
                    "حفظت المنشور عندي من جمال ما قرأت وتأملت",
                    "كلام يقطر عذوبة وجمالاً، سلمت أناملك يا غالي",
                    "رقة المشاعر وعمق الكلمات.. بوح استثنائي قلّ مثيله",
                    "القلوب الصادقة هي التي تكتب بمثل هذا النبض الرقيق",
                    "يا لجمال هذا الوصف، أبحرت في حروفك الجميلة",
                    "ما أجمل الوفاء وما أصدق مشاعر المحبة النقية",
                    "سطور تعيد للقلب نبضه الدافئ، دمت مبدعاً ومتألقاً",
                    "فيض من الجمال والرقة، دمت ودام ألق حروفك الساحرة",
                    "الحب الحقيقي يتجلى في مثل هذه المعاني الصادقة النبيلة",
                    "تناغم رائع بين اللفظ والشعور الصادق، راق لي جداً",
                    "جمال لا يوصف.. سحرتنا بهذا الإحساس المرهف والأصيل",
                    "من أرق ما تصفحت اليوم، تحياتي لذوقك الراقي والمميز",
                    "تنسيق عذب يلامس المشاعر برقة بالغة وأناقة واضحة",
                    "يا لروعة المعنى وعمق العاطفة، تحية لقلمك الأنيق",
                    "المشاعر الصادقة تصل للقلوب فوراً دون استئذان",
                    "كلمات دافئة تفيض وفاءً ونبلاً، ما أجمل هذا الإحساس الصادق",
                    "يا لروعة التعبير وعذوبة المعنى، نبض يدخل القلب بلا استئذان",
                    "الامتنان لغة الأرواح الراقية، منشور يبعث الدفء في كل زاوية",
                    "سحرتنا بجمال هذه السطور، ذوق رفيع ومشاعر تفيض بالصدق",
                    "أجمل ما يهديه الإنسان لمن يحب هو الكلمة الطيبة والمشاعر النقية",
                    "كلام يلامس أوتار القلب برقة بالغة، سلمت يمينك على هذا الجمال",
                    "طرح رومانسي وراقي جداً، دمت متألقاً في صياغة المشاعر الجميلة",
                    "حروف من حرير ومعانٍ تعيد للأرواح بهجتها، راق لي كثيراً",
                    "ما أصدق الحب حين يُكتب بهذا النقاء، تحياتي لقلمك المرهف",
                    "ذائقة فريدة وإحساس ناعم يستحق كل الإعجاب والتقدير"
                )
            }
            lower.contains("شعر") || lower.contains("خواطر") || lower.contains("أدب") ||
                    lower.contains("حكمة") || lower.contains("فلسفة") || lower.contains("عقل") ||
                    lower.contains("تجربة") || lower.contains("كتاب") || lower.contains("قراءة") -> {
                listOf(
                    "بلاغة وفصاحة قل نظيرها في هذا الزمان، أحسنت",
                    "اقتباس فاخر واختيار ينم عن ذائقة وثقافة واسعة",
                    "لله درك، صغت المعنى بأسلوب بديع ومحكم للغاية",
                    "درر منثورة وحكمة بليغة تستحق أن تُدوّن بماء الذهب",
                    "أدب رفيع وقلم يستحق كل الإشادة والتقدير والاحترام",
                    "فلسفة عميقة في فهم الحياة وطبائع الناس، أحييك",
                    "القراءة لك متعة فكرية ولغوية حقيقية لا تمل",
                    "من عيون الأدب، أحسنت الاختيار ووفقت في النشر",
                    "حكمة بليغة في كلمات موجزة، هذا هو السهل الممتنع بعينه",
                    "قلم واعد وفكر ناضج، دمت شعلة أدبية مضيئة في المجتمع",
                    "ميزان من الحكمة وخبرة تترجمها الحروف ببراعة متناهية",
                    "جمالية لغوية تبهر العقل قبل أن تطرب العين والروح",
                    "هكذا يكون الأدب وإلا فلا، سلمت يمناك وفكرك",
                    "أعدت قراءة المنشور عدة مرات لروعة بيانه وجزالة ألفاظه",
                    "كل بيت وكل سطر ينطق بالفخامة والوقار والجمال الأدبي",
                    "بيان ساحر وفصاحة تسبي العقول، لله درك ما أبلغ قلمك",
                    "حكمة تختصر مجلدات في أسطر معدودة، هذا هو الفكر الراقي حقاً",
                    "ذائقة أدبية رفيعة المستوى تعيدنا لزمن الكلمة الرزينة الأصيلة",
                    "عمق في الطرح وجزالة في اللفظ قلّما نجدها في هذه الأيام",
                    "من روائع ما قرأت، قلم يفيض حكمة ورؤية متزنة للحياة",
                    "أبحرت في معاني هذا المنشور وتأملت كل كلمة، إتقان أدبي رائع",
                    "منشور فاخر ينم عن ثقافة عميقة وبصيرة ثاقبة، أحسنت وأبدعت",
                    "الأدب الحقيقي هو ما يحرك التفكير ويصنع الدهشة، سلم هذا القلم",
                    "جمال الصياغة وتماسك المعنى يجعل هذه السطور تحفة فكرية متكاملة",
                    "تستحق هذه الكلمات أن تُحفظ وتُنشر، دمت شعلة أدبية نعتز بها"
                )
            }
            else -> {
                listOf(
                    "كلمات راقية جداً وفي الصميم، أبدعت",
                    "ما شاء الله، ذوق رفيع واختيار موفق ومميز",
                    "كلام يلامس القلب بعمق.. شكراً على هذه المشاركة الطيبة",
                    "أبدعت بلا شك! استمر في هذا العطاء الجميل والمثري",
                    "صدقت، نحتاج لمثل هذه الكلمات النقية دائماً في أيامنا",
                    "تبارك الله عليك، كلام موزون وجميل يعكس نبل صاحب الحساب",
                    "راقت لي هذه الكلمات كثيراً، سلمت يمينك ودام قلمك",
                    "طرح رائع يستحق التقدير والتأمل الطويل، أحسنت النشر",
                    "مشاركة تثري المنصة وتنير الأذهان، شكراً لك من القلب",
                    "حضور متميز كعادتك، لا تحرمنا من فيض منشوراتك القادمة",
                    "إبداع متواصل وبصمة فريدة تترك أثراً في كل منشور",
                    "سلم نبضك وفكرك، مشاركة في غاية الروعة والكمال",
                    "أرفع لك القبعة إعجاباً بهذا الذوق الفريد والراقي",
                    "منشور رائع يعكس نقاء سريرتك وجمال ذوقك الأدبي",
                    "دائماً متألق ومميز في انتقاء ما تشاركه معنا من درر",
                    "أصبت كبد الحقيقة، كلام ولا أروع يعبر عما يختلج في النفوس",
                    "أجمل ما مر في صفحتي اليوم بلا شك، تحياتي لشخصك الكريم",
                    "تحياتي وتقديري لقلمك المعطاء والمتميز دائماً في كل مناسبة",
                    "روعة وإتقان، كل الاحترام والتقدير لهذا الحضور المشرف",
                    "كلمات من ذهب لا يمل القارئ من تأملها وتكرار قراءتها",
                    "استمر في هذا الإبداع، حسابك من الحسابات المفضلة والمثمرة لدي",
                    "نشر في قمة الرقي والأناقة، شكراً من القلب على هذا الإمتاع",
                    "عين الصواب ومعدن الحكمة، وفقك الله لكل خير وسدد خطاك",
                    "حروف من نور تضيء هذا الفضاء الجميل وتترك أثراً طيباً",
                    "ممتن لهذه الإطلالة الجميلة والمنشور الملهم بكل المقاييس",
                    "إطلالة مميزة ومنشور يبعث الراحة والسرور، أحسنت الاختيار",
                    "بصمتك في التطبيق دائماً مختلفة وراقية جداً، لا تحرمنا من إبداعك",
                    "كلام موزون وفي غاية الأناقة، كل التقدير والاحترام لشخصك",
                    "جمال الفكرة وبساطة الأسلوب جعلت المنشور يدخل القلب فوراً",
                    "مشاركة قيّمة تزيد المكان بهاءً وأناقة، سلمت يمناك",
                    "دائماً تثرينا بأجمل ما لديك، شكراً على هذا الذوق الرفيع والاستثنائي",
                    "حروف كالمطر تروي الروح بالجمال، أبدعت بحق في هذا الاختيار",
                    "من أجمل الحسابات وأرقاها ذوقاً، استمر فنحن متابعون بشغف",
                    "طرح متقن وفكرة في منتهى النقاء، بوركت وبورك هذا القلم الجميل",
                    "منشور متكامل معنى ومبنى، كل الإشادة بهذا الحضور المضيء"
                )
            }
        }

        // استبعاد أي تعليق موجود بالفعل على المنشور لضمان التنوع وعدم التكرار نهائياً
        val filtered = pool.filter { candidate ->
            existingComments.none { existing -> existing.trim() == candidate.trim() }
        }
        return if (filtered.isNotEmpty()) filtered.random() else pool.random()
    }

    fun getContextualComment(content: String, tag: String): String = getContextualComment(content, tag, emptyList())

    /**
     * جدولة التفاعل التلقائي على منشور جديد:
     * - دفعة 1 (فورية بعد 45 ثانية): 3-5 إعجابات للتشجيع الفوري
     * - مرحلة 2 (بعد 3 دقائق): تعليق ذكي وسياقي
     * - دفعة 3 (اليوم الثاني - بعد 24 ساعة): 4-5 إعجابات
     * - دفعة 4 (اليوم الثالث - بعد 48 ساعة): 3-5 إعجابات
     * - دفعة 5 (اليوم الرابع - بعد 72 ساعة): 3-4 إعجابات
     * المجموع الإجمالي: نحو 15-20 إعجاباً موزعة بتدرج طبيعي عبر الأسبوع
     */
    fun scheduleAutoEngagement(
        context: Context,
        postId: String,
        authorId: String,
        authorName: String,
        content: String,
        tag: String
    ) {
        if (postId.isEmpty() || authorId.isEmpty() || authorId.startsWith("persona_")) return

        val workManager = WorkManager.getInstance(context.applicationContext)

        // مرحلة 1: أول إعجاب طبيعي هادئ (بعد 4 إلى 6 دقائق - لايك واحد فقط)
        val dataStage1 = Data.Builder()
            .putString("POST_ID", postId)
            .putString("AUTHOR_ID", authorId)
            .putString("AUTHOR_NAME", authorName)
            .putString("CONTENT", content)
            .putString("TAG", tag)
            .putInt("STAGE", 1)
            .build()

        val reqStage1 = OneTimeWorkRequestBuilder<AutoEngagementWorker>()
            .setInputData(dataStage1)
            .setInitialDelay(5, TimeUnit.MINUTES)
            .build()

        // مرحلة 2: ثاني إعجاب طبيعي مستقل (بعد 20 دقيقة - لايك واحد فقط)
        val dataStage2 = Data.Builder()
            .putString("POST_ID", postId)
            .putString("AUTHOR_ID", authorId)
            .putString("AUTHOR_NAME", authorName)
            .putString("CONTENT", content)
            .putString("TAG", tag)
            .putInt("STAGE", 2)
            .build()

        val reqStage2 = OneTimeWorkRequestBuilder<AutoEngagementWorker>()
            .setInputData(dataStage2)
            .setInitialDelay(20, TimeUnit.MINUTES)
            .build()

        // مرحلة 3: تعليق تشجيعي وسياقي (بعد 45 دقيقة)
        val dataStage3 = Data.Builder()
            .putString("POST_ID", postId)
            .putString("AUTHOR_ID", authorId)
            .putString("AUTHOR_NAME", authorName)
            .putString("CONTENT", content)
            .putString("TAG", tag)
            .putInt("STAGE", 3)
            .build()

        val reqStage3 = OneTimeWorkRequestBuilder<AutoEngagementWorker>()
            .setInputData(dataStage3)
            .setInitialDelay(45, TimeUnit.MINUTES)
            .build()

        // مرحلة 4: إعجاب مسائي إضافي (بعد 4 ساعات - لايك واحد)
        val dataStage4 = Data.Builder()
            .putString("POST_ID", postId)
            .putString("AUTHOR_ID", authorId)
            .putString("AUTHOR_NAME", authorName)
            .putString("CONTENT", content)
            .putString("TAG", tag)
            .putInt("STAGE", 4)
            .build()

        val reqStage4 = OneTimeWorkRequestBuilder<AutoEngagementWorker>()
            .setInputData(dataStage4)
            .setInitialDelay(4, TimeUnit.HOURS)
            .build()

        // مرحلة 5: دفعة اليوم التالي (بعد 20 ساعة - 4 إعجابات)
        val dataStage5 = Data.Builder()
            .putString("POST_ID", postId)
            .putString("AUTHOR_ID", authorId)
            .putString("AUTHOR_NAME", authorName)
            .putString("CONTENT", content)
            .putString("TAG", tag)
            .putInt("STAGE", 5)
            .build()

        val reqStage5 = OneTimeWorkRequestBuilder<AutoEngagementWorker>()
            .setInputData(dataStage5)
            .setInitialDelay(20, TimeUnit.HOURS)
            .build()

        // مرحلة 6: دفعة اليوم الثاني (بعد 36 ساعة - 3 إعجابات إضافية)
        val dataStage6 = Data.Builder()
            .putString("POST_ID", postId)
            .putString("AUTHOR_ID", authorId)
            .putString("AUTHOR_NAME", authorName)
            .putString("CONTENT", content)
            .putString("TAG", tag)
            .putInt("STAGE", 6)
            .build()

        val reqStage6 = OneTimeWorkRequestBuilder<AutoEngagementWorker>()
            .setInputData(dataStage6)
            .setInitialDelay(36, TimeUnit.HOURS)
            .build()

        workManager.enqueueUniqueWork("auto_engage_${postId}_stage1", ExistingWorkPolicy.KEEP, reqStage1)
        workManager.enqueueUniqueWork("auto_engage_${postId}_stage2", ExistingWorkPolicy.KEEP, reqStage2)
        workManager.enqueueUniqueWork("auto_engage_${postId}_stage3", ExistingWorkPolicy.KEEP, reqStage3)
        workManager.enqueueUniqueWork("auto_engage_${postId}_stage4", ExistingWorkPolicy.KEEP, reqStage4)
        workManager.enqueueUniqueWork("auto_engage_${postId}_stage5", ExistingWorkPolicy.KEEP, reqStage5)
        workManager.enqueueUniqueWork("auto_engage_${postId}_stage6", ExistingWorkPolicy.KEEP, reqStage6)

        Log.d(TAG, "Auto-engagement (realistic staggered drip 15-20 likes) scheduled successfully for post: $postId")
    }

    /**
     * نشر بروفايلات الأشخاص الافتراضيين في Firestore لضمان فتح صفحاتهم والتفاعل معهم بسلاسة
     */
    fun ensurePersonasInFirestore(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_PERSONAS_SEEDED, false)) return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = FirebaseFirestore.getInstance()
                val batch = db.batch()
                for (p in PERSONAS) {
                    val docRef = db.collection("users").document(p.id)
                    val data = hashMapOf(
                        "name" to p.name,
                        "displayName" to p.name,
                        "username" to p.username,
                        "photoUrl" to p.avatarResource,
                        "bio" to p.bio,
                        "badge" to p.badge,
                        "followersCount" to 0L,
                        "followingCount" to 0L,
                        "postsCount" to (3 + (Math.abs(p.id.hashCode()) % 8))
                    )
                    batch.set(docRef, data, SetOptions.merge())
                }
                batch.commit().addOnSuccessListener {
                    prefs.edit().putBoolean(KEY_PERSONAS_SEEDED, true).apply()
                    Log.d(TAG, "Virtual personas synced to Firestore successfully.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed syncing personas to Firestore", e)
            }
        }
    }

    /**
     * إذا مرت 8 ساعات دون أي منشور في Firestore، ينشر تلقائياً منشوراً جميلاً من أحد الأشخاص
     */
    fun maybeSeedDailyFirestorePost(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastAutoTime = prefs.getLong(KEY_LAST_AUTO_POST_TIME, 0L)
        val now = System.currentTimeMillis()
        if (now - lastAutoTime < 6 * 3600 * 1000L) {
            return
        }

        val db = FirebaseFirestore.getInstance()
        db.collection("community_posts")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .addOnSuccessListener { snapshot ->
                val latestTime = snapshot.documents.firstOrNull()?.getLong("timestamp") ?: 0L
                val hoursSinceLatest = (now - latestTime) / (3600 * 1000L)

                if (snapshot.isEmpty || hoursSinceLatest >= 8) {
                    // حان وقت نشر منشور جديد في المجتمع السحابي!
                    publishOrganicPostToFirestore(context)
                }
            }
            .addOnFailureListener {
                Log.w(TAG, "Error checking latest Firestore post timestamp", it)
            }
    }

    private fun publishOrganicPostToFirestore(context: Context) {
        try {
            val dbHelper = DatabaseHelper(context)
            val posts = dbHelper.getRandomCuratedPosts(3)
            val quote = posts.firstOrNull() ?: return
            val persona = getPersonaForId(quote.id)
            val bgId = BG_PRESETS[Math.abs(quote.id.hashCode()) % BG_PRESETS.size]
            val fontId = FONT_PRESETS[Math.abs(quote.id.hashCode() / 4) % FONT_PRESETS.size]
            val initialLikes = 6 + (Math.abs(quote.id.hashCode()) % 10)
            val sampleLikedBy = PERSONAS.shuffled().take(initialLikes).map { it.id }

            val postMap = hashMapOf<String, Any>(
                "authorId" to persona.id,
                "authorName" to persona.name,
                "authorUsername" to persona.username,
                "authorPhotoUrl" to persona.avatarResource,
                "content" to quote.content,
                "bgId" to bgId,
                "fontId" to fontId,
                "textColor" to "",
                "tag" to (quote.categoryName.takeIf { it.isNotEmpty() } ?: "خواطر"),
                "authorBadge" to persona.badge,
                "authorStreak" to when (Math.abs(persona.id.hashCode()) % 5) {
                    0 -> 100
                    1 -> 60
                    2 -> 30
                    3 -> 7
                    else -> 0
                },
                "lastLikedAt" to System.currentTimeMillis(),
                "likesCount" to initialLikes.toLong(),
                "commentsCount" to 1L,
                "likedBy" to sampleLikedBy,
                "timestamp" to System.currentTimeMillis()
            )

            val db = FirebaseFirestore.getInstance()
            db.collection("community_posts").add(postMap).addOnSuccessListener { docRef ->
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                prefs.edit().putLong(KEY_LAST_AUTO_POST_TIME, System.currentTimeMillis()).apply()
                Log.d(TAG, "Auto-seeded new post to Firestore: ${docRef.id}")

                // إضافة تعليق أولي لطيف
                val otherPersona = PERSONAS.filter { it.id != persona.id }.random()
                val commentText = getContextualComment(quote.content, quote.categoryName)
                FirestoreManager.addComment(
                    postId = docRef.id,
                    authorId = otherPersona.id,
                    authorName = otherPersona.name,
                    authorPhotoUrl = otherPersona.avatarResource,
                    content = commentText
                ) {}
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to publish organic post to Firestore", e)
        }
    }
}
