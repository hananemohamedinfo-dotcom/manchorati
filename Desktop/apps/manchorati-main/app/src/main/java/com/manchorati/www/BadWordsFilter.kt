
package com.manchorati.www

object BadWordsFilter {

    // قائمة الاستثناءات البيضاء (كلمات مسموحة دائماً ولا تخضع للفحص)
    private val whitelist = setOf(
        "رمضان", "كريم", "الرحمن", "الرحيم", "الله", "اللهم", 
        "محمد", "دين", "صوم", "صيام", "مبارك", "خير"
    )

    // قائمة الكلمات غير اللائقة
    private val badWords = setOf(
        // ضع هنا الكلمات المسيئة بدقة
         "قحب", "شرموط", "منيوك", "منكوح", "ديوث", "كسم", "طيز", "زبك", "زبي", 
        "قلاوي", "ترمتك", "مأبون", "مخنث", "عاهر", "خرية", "نياك", "تناك",
        "بزول", "طبون", "حوايا", "يحويك", "احويك", "تتحوى", "سلوغي", "خانز", 
        "زامل", "زوامل", "زويمل", "بزازل", "بزازيل", "كيلوط", "سليب"
    )

    fun containsBadWords(text: String): Boolean {
        if (text.isBlank()) return false

        // تنظيف النص وتفكيكه لكلمات مفردة
        // إزالة الحركات وعلامات الترقيم
        val normalized = text
            .replace(Regex("[\\p{Punct}\\p{Digit}]"), " ")
            .replace(Regex("[ً-ْ]"), "") // إزالة التشكيل
            .trim()

        // تقسيم النص إلى كلمات منفصلة
        val words = normalized.split(Regex("\\s+"))

        for (word in words) {
            val cleanWord = word.trim()
            
            // إذا كانت الكلمة في القائمة البيضاء نتجاوزها
            if (whitelist.contains(cleanWord)) {
                continue
            }

            // فحص الكلمة بمطابقة دقيقة ككلمة مستقلة وليست جزءاً من كلمة
            if (badWords.contains(cleanWord)) {
                return true
            }
        }

        return false
    }
}