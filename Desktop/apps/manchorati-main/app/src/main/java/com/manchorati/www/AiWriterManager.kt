package com.manchorati.www

import android.content.Context
import android.content.SharedPreferences
import java.util.Calendar

object AiWriterManager {

    private const val PREFS_NAME = "ai_writer_prefs"
    private const val KEY_LAST_DAY = "last_day"
    private const val KEY_LAST_YEAR = "last_year"
    private const val KEY_USED_COUNT = "used_count"
    private const val KEY_LAST_CLICK_TIME = "last_click_time"
    const val MAX_DAILY_QUOTA = 5
    private const val COOLDOWN_MILLIS = 8000L // 8 seconds between generations

    fun getRemainingQuota(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        resetIfNewDay(prefs)
        val used = prefs.getInt(KEY_USED_COUNT, 0)
        return (MAX_DAILY_QUOTA - used).coerceAtLeast(0)
    }

    fun canGenerate(context: Context): Pair<Boolean, String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        resetIfNewDay(prefs)

        val lastClick = prefs.getLong(KEY_LAST_CLICK_TIME, 0L)
        val timeDiff = System.currentTimeMillis() - lastClick
        if (timeDiff < COOLDOWN_MILLIS) {
            val waitSec = ((COOLDOWN_MILLIS - timeDiff) / 1000) + 1
            return Pair(false, context.getString(R.string.ai_wait_seconds, waitSec))
        }

        val used = prefs.getInt(KEY_USED_COUNT, 0)
        if (used >= MAX_DAILY_QUOTA) {
            return Pair(false, context.getString(R.string.ai_quota_exceeded))
        }

        return Pair(true, "")
    }

    fun recordGeneration(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        resetIfNewDay(prefs)
        val currentUsed = prefs.getInt(KEY_USED_COUNT, 0)
        prefs.edit()
            .putInt(KEY_USED_COUNT, currentUsed + 1)
            .putLong(KEY_LAST_CLICK_TIME, System.currentTimeMillis())
            .apply()
    }

    private fun resetIfNewDay(prefs: SharedPreferences) {
        val cal = Calendar.getInstance()
        val currentDay = cal.get(Calendar.DAY_OF_YEAR)
        val currentYear = cal.get(Calendar.YEAR)

        val lastDay = prefs.getInt(KEY_LAST_DAY, -1)
        val lastYear = prefs.getInt(KEY_LAST_YEAR, -1)

        if (currentDay != lastDay || currentYear != lastYear) {
            prefs.edit()
                .putInt(KEY_LAST_DAY, currentDay)
                .putInt(KEY_LAST_YEAR, currentYear)
                .putInt(KEY_USED_COUNT, 0)
                .apply()
        }
    }

    // Curated rich AI Quote pool categorized by topic and keywords
    private val wisdomQuotes = listOf(
        "لا تقاس العقول بالأعمار، فكم من صغير عقله بارع، وكم من كبير قلبه فارغ.",
        "كن كالنخيل عن الأحقاد مرتفعاً، يرمى بصخر فيلقي أطيب الثمر.",
        "الهدوء ليس ضعفاً، بل هو أرقى درجات القوة في عالم مليء بالضجيج.",
        "من اعتاد أن يوزع الورد، فسيبقى شيء من العطر عالقاً بيده دائماً.",
        "لا تستصغر أي خطوة إلى الأمام، فالجبال الشاهقة تكونت من ذرات الرمل الصغيرة.",
        "عامل الناس بأخلاقك لا بأخلاقهم، فالأواني لا تنضح إلا بما فيها.",
        "راحة البال تبدأ عندما تدرك أن ليس كل رأي يستحق الرد، ولا كل موقف يستحق الغضب."
    )

    private val motivationQuotes = listOf(
        "لن تذبل روح كان لها مع الله موعد، ولن ينكسر قلب توكل على الحي الذي لا يموت.",
        "اصنع مجدك بصمت، ودع صدى نجاحك يملأ الأرجاء بكل ثقة.",
        "كل سقوط هو فرصة جديدة لنهوض أعظم، فالألماس لا يلمع إلا تحت أقسى درجات الضغط.",
        "لا تنتظر اللحظة المثالية، بل خذ اللحظة الحالية واجعلها أنت أسطورية بجهدك.",
        "ثق بنفسك حتى حين يشكك فيك الجميع، فالنجاح لا يعترف إلا بالمصرين على الوصول.",
        "الأحلام التي لا تغادر وسادتك لن تغير واقعك، انهض وحول تعبك إلى إنجاز يفتخر به الجميع.",
        "بينك وبين ما تتمنى مسافة دعاء وسعي وإصرار لا ينطفئ أبداً."
    )

    private val loveQuotes = listOf(
        "بعض الأرواح كالربيع، أينما حلت أنبتت في قلوبنا أزهار الأمل والسكينة.",
        "ليس الحب أن نبقى معاً دائماً، بل أن نبقى أوفياء لبعضنا حتى في أشد لحظات الغياب.",
        "سلامٌ على من قرأ في أعيننا صمتنا ففهمه، واحتضن تعبنا بابتسامة دون أن نسأل.",
        "أنت لست جزءاً من قلبي، بل أنت نبضه الذي أتنفس به كل صباح.",
        "القلوب النقية لا تطلب الكثير، يكفيها فقط أن تشعر بصدق الاهتمام والدفء.",
        "يا ليت اللقاء يطول، ويا ليت المسافات بين يديك ونبضي تختصر في لحظة عناق."
    )

    private val duaQuotes = listOf(
        "اللهم جبراً لقلوب أرهقها التمني، وفرجاً لكل كرب لا يعلم خفاياه إلا أنت.",
        "يا رب سخر لنا من الأقدار أجملها، ومن السعادة أكملها، ومن راحة البال أوفاها.",
        "اللهم ما قسمته في هذا اليوم من توفيق ورزق وصحة فاجعل لنا منه أوفر الحظ والنصيب.",
        "يا قاضي الحاجات، اقضِ ديوننا واشفِ مرضانا، واكتب لنا تيسيراً لكل أمر عسير.",
        "اللهم ارحم ضعفنا وقلة حيلتنا، ولا تكلنا إلى أنفسنا طرفة عين ولا أقل من ذلك.",
        "ربي إني توكلت عليك، وأنت حسبي ونعم الوكيل في كل ما أسر وما أعلن."
    )

    private val calmQuotes = listOf(
        "فنجان قهوة وكتاب دافئ وصمت جميل، كفيل بإعادة ترتيب شتات الروح المتعبة.",
        "في زاوية من الليل الهادئ، تجلس أفكارنا وحيدة تبحث عن سلام داخلي عميق.",
        "سلام على الذين يزرعون فينا السكينة بكلماتهم اللطيفة ونظراتهم الصادقة.",
        "أجمل ما في الحياة أن تعيشها ببساطة، بعيداً عن تصنع المتصنعين وتكلف المتكلفين.",
        "تعلم كيف تهدأ في عاصفة المشاعر، فالريح تمر والصخور الثابتة هي وحدها التي تبقى."
    )

    private val farewellQuotes = listOf(
        "أحياناً يكون الرحيل هو أسمى درجات الوفاء للكرامة وعزة النفس.",
        "لا تندم على حب صادق منحته يوماً لمن لم يقدره، فالورود تعطر اليد حتى لو جرحتها الأشواك.",
        "غادر بهدوء كما دخلت بهدوء، واترك خلفك أثراً جميلاً لا ينسى ولا يمحى.",
        "أصعب أنواع الوداع هو الذي يكون فيه الشخص حاضراً في المكان وغائباً في الشعور.",
        "نحن لا نخسر حين نترك من هان عليه ودنا، بل نستعيد أنفسنا التي أضعناها يوماً."
    )

    // Specialized keyword dictionary for smart matching
    private val keywordMap = mapOf(
        "أم" to listOf(
            "أمي هي سري الذي أخبئه بين ثنايا دعائي، هي النور الذي لا ينطفئ والجنة التي أرجوها.",
            "لو جمعوا نساء الكون في كفة وأمي في كفة، لرجحت كفة أمي دون تردد.",
            "كل النعم قد تزول، إلا دعوة أم حنونة تظل تحرسك في عتمة الأيام."
        ),
        "أب" to listOf(
            "أبي هو السند الذي لا يميل والجبل الذي أستند عليه كلما عصفت بي الحياة.",
            "الأب هو البطل الأول في عيون أبنائه، والحب الأبدي الذي لا يشوبه مصلحة ولا زوال."
        ),
        "صبر" to listOf(
            "الصبر ليس انتظار الفرج فحسب، بل هو حسن الظن بالله في أشد لحظات الانتظار قسوة.",
            "واصبر فإن بعد العسر يسراً، وبعد الليل فجراً يبدد كل الأحزان المتراكمة."
        ),
        "صداقة" to listOf(
            "الصديق الحقيقي هو الذي يرى دمعتك الأولى، ويمسح الثانية، ويمنع الثالثة بحبه ووفائه.",
            "سلام على الصحب الأوفياء، الذين إذا غبنا حفظوا ودنا وإذا حضرنا أسعدوا قلوبنا."
        ),
        "تفاؤل" to listOf(
            "تفاءل بما تهوى يكن، فالأيام القادمة تخبئ لك من البشائر ما ينسيك مرارة ما فات.",
            "انظر إلى النجوم في السماء، فالعتمة الشديدة هي وحدها التي تبرز بريق الضوء الحقيقي."
        )
    )

    fun generateQuote(topic: String, customPrompt: String, style: String): String {
        val trimmedPrompt = customPrompt.trim()

        // 1. Check keyword map if custom prompt contains specific keywords
        if (trimmedPrompt.isNotEmpty()) {
            for ((key, quotes) in keywordMap) {
                if (trimmedPrompt.contains(key)) {
                    return quotes.shuffled().first()
                }
            }
        }

        // 2. Select by topic
        val pool = when {
            topic.contains("حكم") -> wisdomQuotes
            topic.contains("تحفيز") || topic.contains("نجاح") -> motivationQuotes
            topic.contains("حب") || topic.contains("غرام") -> loveQuotes
            topic.contains("دعاء") || topic.contains("أدعية") -> duaQuotes
            topic.contains("هدوء") || topic.contains("قهوة") -> calmQuotes
            topic.contains("وداع") || topic.contains("فراق") -> farewellQuotes
            else -> {
                // If custom prompt has general text, blend all pools
                (wisdomQuotes + motivationQuotes + loveQuotes + duaQuotes + calmQuotes).shuffled()
            }
        }

        val baseQuote = pool.shuffled().first()

        // Apply style touch if poetry requested
        return when {
            style.contains("شعر") -> "« ${baseQuote.replace("،", "..\n")} »"
            style.contains("قصيرة") -> {
                if (baseQuote.contains("،")) baseQuote.substringBefore("،") + "."
                else baseQuote
            }
            else -> baseQuote
        }
    }
}
