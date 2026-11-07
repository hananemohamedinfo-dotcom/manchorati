package com.manchorati.www

import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.Calendar
import java.util.concurrent.TimeUnit

object AiWriterManager {

    private const val TAG = "AiWriterManager"
    private const val PREFS_NAME = "ai_writer_prefs"
    private const val KEY_LAST_DAY = "last_day"
    private const val KEY_LAST_YEAR = "last_year"
    private const val KEY_USED_COUNT = "used_count"
    private const val KEY_LAST_CLICK_TIME = "last_click_time"
    private const val KEY_GEMINI_API_KEY = "gemini_api_key"

    const val MAX_DAILY_QUOTA = 10
    private const val COOLDOWN_MILLIS = 5000L // 5 seconds cooldown

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(18, TimeUnit.SECONDS)
        .build()

    private val mainHandler = Handler(Looper.getMainLooper())

    fun setCustomApiKey(context: Context, apiKey: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_GEMINI_API_KEY, apiKey.trim()).apply()
    }

    fun getApiKey(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_GEMINI_API_KEY, "") ?: ""
    }

    /**
     * جلب مفتاح Gemini API من السحابة (Firestore) تلقائياً عند تشغيل التطبيق أو الحاجة
     */
    fun syncApiKeyFromFirestore(context: Context, onComplete: ((String) -> Unit)? = null) {
        val localKey = getApiKey(context)
        try {
            val db = FirebaseFirestore.getInstance()
            db.collection("category_updates").document("ai_settings")
                .get()
                .addOnSuccessListener { doc ->
                    val remoteKey = doc.getString("gemini_api_key")?.trim() ?: ""
                    if (remoteKey.isNotEmpty()) {
                        setCustomApiKey(context, remoteKey)
                        onComplete?.invoke(remoteKey)
                    } else {
                        // Fallback check app_config
                        db.collection("app_config").document("ai_settings")
                            .get()
                            .addOnSuccessListener { doc2 ->
                                val remoteKey2 = doc2.getString("gemini_api_key")?.trim() ?: ""
                                if (remoteKey2.isNotEmpty()) {
                                    setCustomApiKey(context, remoteKey2)
                                    onComplete?.invoke(remoteKey2)
                                } else {
                                    onComplete?.invoke(localKey)
                                }
                            }
                            .addOnFailureListener {
                                onComplete?.invoke(localKey)
                            }
                    }
                }
                .addOnFailureListener {
                    onComplete?.invoke(localKey)
                }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to sync AI key from Firestore: ${e.message}")
            onComplete?.invoke(localKey)
        }
    }

    /**
     * حفظ مفتاح Gemini API في السحابة (Firestore) لتعميمه على جميع مستخدمي التطبيق
     */
    fun saveApiKeyToFirestore(apiKey: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val data = hashMapOf(
            "gemini_api_key" to apiKey.trim(),
            "updatedAt" to System.currentTimeMillis()
        )
        try {
            val db = FirebaseFirestore.getInstance()
            db.collection("category_updates").document("ai_settings")
                .set(data, SetOptions.merge())
                .addOnSuccessListener {
                    onSuccess()
                }
                .addOnFailureListener {
                    db.collection("app_config").document("ai_settings")
                        .set(data, SetOptions.merge())
                        .addOnSuccessListener { onSuccess() }
                        .addOnFailureListener { err -> onFailure(err) }
                }
        } catch (e: Exception) {
            onFailure(e)
        }
    }

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

    private fun isOnline(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = cm.activeNetwork ?: return false
        val cap = cm.getNetworkCapabilities(network) ?: return false
        return cap.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    // =========================================================================
    // 1. توليد ذكي سحابي عبر Gemini API مع الرجوع التلقائي للأوفلاين
    // =========================================================================
    fun generateQuoteSmart(
        context: Context,
        topic: String,
        customPrompt: String,
        style: String,
        onResult: (quote: String, isRealAi: Boolean) -> Unit
    ) {
        generateQuoteSmartAdvanced(context, topic, customPrompt, style) { quote, isRealAi, _ ->
            onResult(quote, isRealAi)
        }
    }

    fun generateQuoteSmartAdvanced(
        context: Context,
        topic: String,
        customPrompt: String,
        style: String,
        onResult: (quote: String, isRealAi: Boolean, statusMessage: String?) -> Unit
    ) {
        val apiKey = getApiKey(context)

        // إذا لم يكن متصلاً بالإنترنت
        if (!isOnline(context)) {
            val offlineQuote = generateQuote(topic, customPrompt, style)
            onResult(offlineQuote, false, context.getString(R.string.ai_offline_fallback_toast))
            return
        }

        // إذا كان المفتاح غير موجود محلياً، نحاول المزامنة من السحابة بسرعة
        if (apiKey.isEmpty()) {
            syncApiKeyFromFirestore(context) { syncedKey ->
                if (syncedKey.isNotEmpty()) {
                    executeGeminiRequest(context, syncedKey, topic, customPrompt, style, onResult)
                } else {
                    val offlineQuote = generateQuote(topic, customPrompt, style)
                    onResult(offlineQuote, false, context.getString(R.string.ai_key_missing_toast))
                }
            }
            return
        }

        executeGeminiRequest(context, apiKey, topic, customPrompt, style, onResult)
    }

    private fun executeGeminiRequest(
        context: Context,
        apiKey: String,
        topic: String,
        customPrompt: String,
        style: String,
        onResult: (quote: String, isRealAi: Boolean, statusMessage: String?) -> Unit
    ) {
        val primaryEndpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=$apiKey"
        val fallbackEndpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey"

        val isPolishMode = style.contains("تحسين") || style.contains("Polish") || style.contains("صياغة")

        val userInstruction = if (isPolishMode && customPrompt.isNotBlank()) {
            buildString {
                append("أنت أديب وخبير لغوي عربي فذ.")
                append("\nالمطلوب: أعد صياغة وتحسين النص التالي ليكون غاية في البلاغة والتأثير مع الحفاظ على فكرته الأصلية وتصحيح أي أخطاء لغوية أو إملائية:")
                append("\nالنص المطلوب تحسينه: $customPrompt")
                append("\nالتصنيف: $topic")
                append("\nشروط حتمية: أخرج فقط النص المحسن باللغة العربية الفصحى البليغة، بدون شروحات أو مقدمات أو خواتيم، وبدون أي علامات تنصيص زائدة. فقط النص المحسن.")
            }
        } else {
            buildString {
                append("اكتب اقتباساً أدبياً أو خاطرة أو حكمة عربية راقية جداً ومؤثرة.")
                append("\nالتصنيف: $topic")
                append("\nالأسلوب: $style")
                if (customPrompt.isNotBlank()) {
                    append("\nالفكرة أو المعنى المطلوب: $customPrompt")
                }
                append("\nشروط حتمية: اجعل النص باللغة العربية الفصحى البليغة، مؤثراً، مفعماً بالعمق، بدون أي شروحات أو مقدمات أو خواتيم، وبدون علامات تنصيص زائدة. فقط نص الاقتباس.")
            }
        }

        val jsonPayload = JSONObject().apply {
            val contents = JSONArray().apply {
                val partObj = JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", userInstruction) })
                    })
                }
                put(partObj)
            }
            put("contents", contents)
            val genConfig = JSONObject().apply {
                put("temperature", 0.75)
                put("maxOutputTokens", 450)
            }
            put("generationConfig", genConfig)
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = jsonPayload.toString().toRequestBody(mediaType)

        fun doRequest(endpoint: String, isRetry: Boolean = false) {
            val request = Request.Builder()
                .url(endpoint)
                .post(requestBody)
                .build()

            httpClient.newCall(request).enqueue(object : okhttp3.Callback {
                override fun onFailure(call: okhttp3.Call, e: IOException) {
                    if (!isRetry) {
                        doRequest(fallbackEndpoint, true)
                        return
                    }
                    mainHandler.post {
                        val fallbackQuote = generateQuote(topic, customPrompt, style)
                        onResult(fallbackQuote, false, context.getString(R.string.ai_offline_fallback_toast))
                    }
                }

                override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                    response.use { res ->
                        if (!res.isSuccessful) {
                            if (res.code == 404 && !isRetry) {
                                doRequest(fallbackEndpoint, true)
                                return
                            }
                            mainHandler.post {
                                val fallbackQuote = generateQuote(topic, customPrompt, style)
                                val status = if (res.code == 400 || res.code == 403) {
                                    context.getString(R.string.ai_key_missing_toast)
                                } else {
                                    context.getString(R.string.ai_offline_fallback_toast)
                                }
                                onResult(fallbackQuote, false, status)
                            }
                            return
                        }

                        try {
                            val bodyStr = res.body?.string() ?: ""
                            val rootJson = JSONObject(bodyStr)
                            val candidates = rootJson.optJSONArray("candidates")
                            val firstCandidate = candidates?.optJSONObject(0)
                            val contentObj = firstCandidate?.optJSONObject("content")
                            val partsArr = contentObj?.optJSONArray("parts")
                            val rawText = partsArr?.optJSONObject(0)?.optString("text")?.trim() ?: ""

                            val cleanedText = rawText
                                .replace(Regex("^(?:اقتباس|الخاطرة|النص المحسن)[:\\s]+", RegexOption.IGNORE_CASE), "")
                                .replace(Regex("^[\"«'“`]|[\"»'”`]$"), "")
                                .replace(Regex("\\*\\*"), "")
                                .trim()

                            val finalQuote = if (cleanedText.isNotEmpty()) cleanedText else generateQuote(topic, customPrompt, style)

                            mainHandler.post {
                                onResult(finalQuote, true, null)
                            }
                        } catch (e: Exception) {
                            mainHandler.post {
                                val fallbackQuote = generateQuote(topic, customPrompt, style)
                                onResult(fallbackQuote, false, context.getString(R.string.ai_offline_fallback_toast))
                            }
                        }
                    }
                }
            })
        }

        doRequest(primaryEndpoint, false)
    }

    // =========================================================================
    // 2. المكتبة الأدبية الفاخرة للأوفلاين (Offline Curated Library)
    // =========================================================================
    private val wisdomQuotes = listOf(
        "لا تقاس العقول بالأعمار، فكم من صغير عقله بارع، وكم من كبير قلبه فارغ.",
        "كن كالنخيل عن الأحقاد مرتفعاً، يرمى بصخر فيلقي أطيب الثمر.",
        "الهدوء ليس ضعفاً، بل هو أرقى درجات القوة في عالم مليء بالضجيج والزيف.",
        "من اعتاد أن يوزع الورد، فسيبقى شيء من العطر عالقاً بيده وذاكرته دائماً.",
        "لا تستصغر أي خطوة إلى الأمام، فالجبال الشاهقة تكونت من تراكم ذرات الرمل الصغيرة.",
        "عامل الناس بأخلاقك الرفيعة لا بأخلاقهم، فالأواني لا تنضح إلا بما فيها.",
        "راحة البال تبدأ عندما تدرك أن ليس كل رأي يستحق الرد، ولا كل موقف يستحق الغضب.",
        "عزة النفس ليست كبراً ولا تعالياً، بل هي أن تبتعد عن كل من لا يدرك قيمتك الحقيقية.",
        "الزمن كفيل بأن يكشف لك معادن القلوب، فلا تتعجل بالحكم ولا تفرط بالثقة.",
        "إذا أردت أن تبني مجداً، فلا تلتفت لصياح الواقفين على قارعة الطريق."
    )

    private val motivationQuotes = listOf(
        "لن تذبل روح كان لها مع الله موعد، ولن ينكسر قلب توكل على الحي الذي لا يموت.",
        "اصنع مجدك بصمت وإتقان، ودع صدى نجاحك يملأ الأرجاء بكل ثقة وثبات.",
        "كل سقوط هو فرصة جديدة لنهوض أعظم، فالألماس لا يلمع إلا تحت أقسى درجات الضغط والحرارة.",
        "لا تنتظر اللحظة المثالية، بل خذ اللحظة الحالية واجعلها أنت أسطورية بجهدك وعزيمتك.",
        "ثق بنفسك حتى حين يشكك فيك الجميع، فالوصول للقمة لا يعترف إلا بالمصرين على المستحيل.",
        "الأحلام التي لا تغادر وسادتك لن تغير واقعك، انهض وحول تعبك إلى إنجاز يفتخر به الجميع.",
        "بينك وبين ما تتمنى مسافة دعاء صادق وسعي متواصل وإصرار لا ينطفئ أبداً.",
        "كلما زادت الصعاب تعقيداً، اقتربت لحظة الفرج التي تليق بصبرك وكفاحك.",
        "لا تقارن بدايتك بمواسم حصاد غيرك، لكل زهرة وقتها الخاص لتتفتح وتزهر."
    )

    private val loveQuotes = listOf(
        "بعض الأرواح كالربيع، أينما حلت أنبتت في قلوبنا أزهار الأمل والسكينة والطمأنينة.",
        "ليس الحب أن نبقى معاً دائماً، بل أن نبقى أوفياء لبعضنا حتى في أشد لحظات الغياب والمسافات.",
        "سلامٌ على من قرأ في أعيننا صمتنا ففهمه، واحتضن تعبنا بابتسامة دافئة دون أن نسأل.",
        "أنت لست جزءاً من قلبي، بل أنت نبضه الذي أتنفس به كل صباح ومساء.",
        "القلوب النقية لا تطلب الكثير، يكفيها فقط أن تشعر بصدق الاهتمام وعمق الاحتواء.",
        "يا ليت اللقاء يطول، ويا ليت المسافات بين يديك ونبضي تختصر في لحظة عناق أبدي.",
        "الحب الحقيقي أمان لا خوف فيه، وركيزة تطمئن بها الروح بعد عناء الأيام."
    )

    private val duaQuotes = listOf(
        "اللهم جبراً لقلوب أرهقها التمني، وفرجاً لكل كرب لا يعلم خفاياه إلا أنت يا رحمن.",
        "يا رب سخر لنا من الأقدار أجملها، ومن السعادة أكملها، ومن راحة البال أوفاها وأدومها.",
        "اللهم ما قسمته في هذا اليوم من توفيق ورزق وصحة وبركة فاجعل لنا منه أوفر الحظ والنصيب.",
        "يا قاضي الحاجات، اقضِ ديوننا واشفِ مرضانا، واكتب لنا تيسيراً لكل أمر ظنناه عسيراً.",
        "اللهم ارحم ضعفنا وقلة حيلتنا، ولا تكلنا إلى أنفسنا طرفة عين ولا أقل من ذلك.",
        "ربي إني توكلت عليك، وأنت حسبي ونعم الوكيل في كل ما أسر وما أعلن وما أنت به أعلم.",
        "اللهم اجعلنا ممن طاب ذكرهم، وحسنت سيرتهم، واستبشرت بهم القلوب أينما حلوا."
    )

    private val calmQuotes = listOf(
        "فنجان قهوة، وكتاب دافئ، وصمت جميل، كفيل بإعادة ترتيب شتات الروح المتعبة.",
        "في زاوية من الليل الهادئ، تجلس أفكارنا وحيدة تبحث عن سلام داخلي عميق ونقي.",
        "سلام على الذين يزرعون فينا السكينة بكلماتهم اللطيفة ونظراتهم الصادقة الهادئة.",
        "أجمل ما في الحياة أن تعيشها ببساطة ورضا، بعيداً عن تصنع المتصنعين وتكلف المتكلفين.",
        "تعلم كيف تهدأ في عاصفة المشاعر، فالريح تمر والصخور الثابتة هي وحدها التي تبقى راسخة.",
        "العزلة أحياناً ليست هروباً من البشر، بل استراحة محارب لترتيب الأولويات وصيانة الروح."
    )

    private val farewellQuotes = listOf(
        "أحياناً يكون الرحيل هو أسمى درجات الوفاء للكرامة وعزة النفس والوقار.",
        "لا تندم على ود صادق منحته يوماً لمن لم يقدره، فالورود تعطر اليد حتى لو جرحتها الأشواك.",
        "غادر بهدوء كما دخلت بهدوء، واترك خلفك أثراً جميلاً لا ينسى ولا يمحى مع تقادم السنين.",
        "أصعب أنواع الوداع هو الذي يكون فيه الشخص حاضراً في المكان وغائباً في المشاعر.",
        "نحن لا نخسر حين نترك من هان عليه ودنا، بل نستعيد أنفسنا التي أضعناها في انتظار اهتمامهم."
    )

    private val keywordMap = mapOf(
        "أم" to listOf(
            "أمي هي سري الذي أخبئه بين ثنايا دعائي، هي النور الذي لا ينطفئ والجنة التي أرجوها كل حين.",
            "لو جمعوا نساء الكون في كفة وأمي في كفة، لرجحت كفة أمي وبهاء ابتسامتها دون تردد.",
            "كل النعم قد تزول أو تتبدل، إلا دعوة أم حنونة تظل تحرسك في عتمة الأيام ووعورة الدروب."
        ),
        "أب" to listOf(
            "أبي هو السند الذي لا يميل، والجبل الأشم الذي أستند عليه كلما عصفت بي رياح الحياة.",
            "الأب هو البطل الأول في عيون أبنائه، والحب الأبدي الذي لا يشوبه مصلحة ولا زوال."
        ),
        "صبر" to listOf(
            "الصبر ليس انتظار الفرج فحسب، بل هو حسن الظن بالله في أشد لحظات الانتظار قسوة وعتمة.",
            "واصبر فإن بعد العسر يسراً يتلوه يسر، وبعد حلكة الليل فجراً يبدد كل الأحزان المتراكمة."
        ),
        "صداقة" to listOf(
            "الصديق الحقيقي هو الذي يرى دمعتك الأولى، ويمسح الثانية، ويمنع الثالثة بوفائه وصدق أخوته.",
            "سلام على الصحب الأوفياء، الذين إذا غبنا حفظوا ودنا وإذا حضرنا أسعدوا قلوبنا بصدق حديثهم."
        ),
        "تفاؤل" to listOf(
            "تفاءل بما تهوى يكن، فالأيام القادمة تخبئ لك من البشائر ما ينسيك مرارة ما فات وما انكسر.",
            "انظر إلى النجوم في السماء، فالعتمة الشديدة هي وحدها التي تبرز بريق الضوء الحقيقي وتلألؤه."
        ),
        "مطر" to listOf(
            "مع كل قطرة مطر تهطل، يتجدد فينا يقين عميق بأن الرحمة قريبة، وأن القلوب ستزهر من جديد بعد جفافها.",
            "رائحة المطر وصوت انسكابه على النوافذ يغسل هموم الأيام ويعيد للروح براءتها الأولى."
        )
    )

    fun generateQuote(topic: String, customPrompt: String, style: String): String {
        val trimmedPrompt = customPrompt.trim()

        if (trimmedPrompt.isNotEmpty()) {
            for ((key, quotes) in keywordMap) {
                if (trimmedPrompt.contains(key)) {
                    return quotes.shuffled().first()
                }
            }
        }

        val pool = when {
            topic.contains("حكم") -> wisdomQuotes
            topic.contains("تحفيز") || topic.contains("نجاح") -> motivationQuotes
            topic.contains("حب") || topic.contains("غرام") -> loveQuotes
            topic.contains("دعاء") || topic.contains("أدعية") || topic.contains("أمل") -> duaQuotes
            topic.contains("هدوء") || topic.contains("قهوة") || topic.contains("سكينة") -> calmQuotes
            topic.contains("وداع") || topic.contains("فراق") -> farewellQuotes
            else -> {
                (wisdomQuotes + motivationQuotes + loveQuotes + duaQuotes + calmQuotes).shuffled()
            }
        }

        val baseQuote = pool.shuffled().first()

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
