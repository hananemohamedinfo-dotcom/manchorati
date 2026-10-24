package com.manchorati.www

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatButton
import androidx.core.text.HtmlCompat

object PrivacyPolicyHelper {

    private const val PREFS_NAME = "privacy_policy_prefs"
    private const val KEY_ACCEPTED = "has_accepted_privacy_v1"

    fun hasAcceptedPrivacy(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_ACCEPTED, false)
    }

    fun setAcceptedPrivacy(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_ACCEPTED, true).apply()
    }

    fun showPrivacyDialog(
        activity: Activity,
        isFirstLaunch: Boolean,
        onAccepted: (() -> Unit)? = null
    ) {
        if (activity.isFinishing || activity.isDestroyed) return

        val dialog = Dialog(activity)
        val view = LayoutInflater.from(activity).inflate(R.layout.dialog_terms_privacy, null)
        dialog.setContentView(view)

        dialog.window?.let { window ->
            window.setBackgroundDrawableResource(android.R.color.transparent)
            val width = (activity.resources.displayMetrics.widthPixels * 0.92).toInt()
            window.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
        }

        val tvContent = view.findViewById<TextView>(R.id.tvPrivacyContent)
        val layoutCheckbox = view.findViewById<View>(R.id.layoutAcceptCheckbox)
        val cbAccept = view.findViewById<CheckBox>(R.id.cbAcceptTerms)
        val btnAccept = view.findViewById<AppCompatButton>(R.id.btnAcceptPrivacy)
        val btnClose = view.findViewById<AppCompatButton>(R.id.btnClosePrivacy)

        val policyHtml = getPolicyHtml(activity)
        tvContent.text = HtmlCompat.fromHtml(policyHtml, HtmlCompat.FROM_HTML_MODE_COMPACT)

        if (isFirstLaunch) {
            dialog.setCancelable(false)
            dialog.setCanceledOnTouchOutside(false)
            layoutCheckbox.visibility = View.VISIBLE
            btnAccept.visibility = View.VISIBLE
            btnClose.visibility = View.GONE

            btnAccept.setOnClickListener {
                if (!cbAccept.isChecked) {
                    Toast.makeText(
                        activity,
                        activity.getString(R.string.privacy_toast_must_accept),
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    setAcceptedPrivacy(activity)
                    dialog.dismiss()
                    onAccepted?.invoke()
                }
            }
        } else {
            dialog.setCancelable(true)
            dialog.setCanceledOnTouchOutside(true)
            layoutCheckbox.visibility = View.GONE
            btnAccept.visibility = View.GONE
            btnClose.visibility = View.VISIBLE

            btnClose.setOnClickListener {
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun getPolicyHtml(context: Context): String {
        val lang = LocaleHelper.getSelectedLanguage(context)
        return when (lang) {
            LocaleHelper.LANG_FR -> getFrenchPolicyHtml()
            LocaleHelper.LANG_EN -> getEnglishPolicyHtml()
            else -> getArabicPolicyHtml()
        }
    }

    private fun getArabicPolicyHtml(): String {
        return """
            <p>أهلاً بك في تطبيق <b>منشوراتي (Manchorati)</b>. نحن نلتزم بحماية خصوصيتك وبياناتك الشخصية وفقاً لأعلى المعايير القانونية والتنظيمية، بما فيها <b>اللائحة العامة لحماية البيانات للاتحاد الأوروبي (GDPR)</b> وسياسات مطوري Google Play.</p>
            <br/>
            <b>1. المقدمة والموافقة</b><br/>
            باستخدامك لتطبيق منشوراتي، فإنك توافق على بنود هذه السياسة وشروط الاستخدام. تمنحك هذه الاتفاقية ترخيصاً شخصياً وغير حصري لاستخدام التطبيق لمشاركة وقراءة وتصميم الاقتباسات والبطاقات.
            <br/><br/>
            <b>2. البيانات التي نقوم بجمعها</b><br/>
            • <b>بيانات الحساب:</b> عند تسجيل الدخول باستخدام حساب Google أو البريد الإلكتروني، نقوم بجمع اسم العرض، عنوان البريد الإلكتروني، والصورة الشخصية لإنشاء وتوثيق حسابك.<br/>
            • <b>المحتوى المنشأ بواسطة المستخدم:</b> المنشورات، الاقتباسات، التعليقات، الإعجابات، وقوائم المفضلة المخزنة بأمان على سحابة Google Firebase.<br/>
            • <b>البيانات التقنية ورمز الجهاز:</b> معرفات مجهولة ورموز إشعارات FCM لتوصيل الإشعارات والتفضيلات المحلية (اللغة والوضع الداكن).
            <br/><br/>
            <b>3. الأساس القانوني للمعالجة (GDPR المادة 6)</b><br/>
            • الموافقة الصريحة عند تسجيل الدخول والموافقة على هذه السياسة.<br/>
            • تنفيذ العقد لتوفير ميزات النشر والتفاعل مع المجتمع.<br/>
            • المصلحة المشروعة لتحسين الأمان ومنع الأنشطة المسيئة.
            <br/><br/>
            <b>4. أمان وتشفير البيانات</b><br/>
            تخضع كافة الاتصالات ونقل البيانات بين التطبيق والخوادم السحابية للتشفير الكامل باستخدام بروتوكولات HTTPS و TLS الآمنة، مع حظر كامل لنقل البيانات بنص صريح (Cleartext Traffic).
            <br/><br/>
            <b>5. حقوقك بموجب GDPR وسياسات Google Play</b><br/>
            • <b>حق الوصول:</b> طلب نسخة من بياناتك في أي وقت.<br/>
            • <b>حق التصحيح:</b> تعديل ملفك الشخصي ومنشوراتك داخل التطبيق.<br/>
            • <b>حق الحذف التام (Right to Erasure):</b> طلب حذف حسابك وكافة مشاركاتك وبياناتك نهائياً عبر قسم تواصل معنا.<br/>
            • <b>حق سحب الموافقة:</b> إيقاف الإشعارات أو إيقاف الاستخدام متى شئت.
            <br/><br/>
            <b>6. معايير المحتوى والمجتمع</b><br/>
            يحظر تماماً نشر أي محتوى يحث على الكراهية، العنف، التمييز، التحرش، أو ينتهك حقوق الملكية الفكرية. يُحذف أي محتوى مخالف ويُحظر الحساب فوراً.
            <br/><br/>
            <b>7. حماية الأطفال (COPPA و GDPR)</b><br/>
            التطبيق موجه للمستخدمين الذين تبلغ أعمارهم 13 عاماً فأكثر.
            <br/><br/>
            <b>8. التواصل والاستفسارات</b><br/>
            لممارسة حقوقك أو تقديم أي استفسار، يرجى التواصل معنا عبر نموذج "تواصل معنا" المتاح داخل التطبيق.
        """.trimIndent()
    }

    private fun getFrenchPolicyHtml(): String {
        return """
            <p>Bienvenue sur <b>Manchorati</b>. Nous nous engageons fermement à protéger votre vie privée conformément aux normes les plus strictes, notamment le <b>Règlement Général sur la Protection des Données (RGPD)</b> et les politiques de Google Play.</p>
            <br/>
            <b>1. Introduction et consentement</b><br/>
            En utilisant Manchorati, vous acceptez les présentes conditions et la politique de confidentialité vous accordant une licence personnelle pour concevoir et partager des citations.
            <br/><br/>
            <b>2. Données personnelles collectées</b><br/>
            • <b>Données de compte :</b> Nom public, adresse e-mail et photo lors de la connexion Google/e-mail.<br/>
            • <b>Contenu utilisateur :</b> Citations, commentaires, mentions « J'aime » et favoris stockés sur Google Cloud Firestore.<br/>
            • <b>Données techniques :</b> Jetons FCM pour les notifications quotidiennes et préférences d'affichage (langue, mode sombre).
            <br/><br/>
            <b>3. Base juridique (Art. 6 RGPD)</b><br/>
            • Consentement explicite lors de l'acceptation.<br/>
            • Exécution du service pour vous permettre de publier et interagir.<br/>
            • Intérêt légitime pour assurer la sécurité de l'application.
            <br/><br/>
            <b>4. Sécurité et chiffrement</b><br/>
            Toutes les transmissions sont strictement chiffrées en HTTPS/TLS. Le trafic en clair (Cleartext) est interdit.
            <br/><br/>
            <b>5. Vos droits (RGPD & Google Play)</b><br/>
            • <b>Droit d'accès et rectification :</b> Consultez et modifiez votre profil à tout moment.<br/>
            • <b>Droit à l'effacement (« Droit à l'oubli ») :</b> Demandez la suppression intégrale et définitive de votre compte et de vos données via la rubrique Contactez-nous.<br/>
            • <b>Retrait du consentement :</b> Désactivez les notifications ou retirez votre accord à tout moment.
            <br/><br/>
            <b>6. Règles communautaires</b><br/>
            Tout contenu haineux, diffamatoire ou violant le droit d'auteur est strictement proscrit et entraînera le bannissement du compte.
            <br/><br/>
            <b>7. Protection des mineurs</b><br/>
            Application destinée aux utilisateurs de 13 ans et plus.
            <br/><br/>
            <b>8. Contact</b><br/>
            Pour toute demande d'accès ou suppression, contactez notre équipe via la section « Contactez-nous » de l'application.
        """.trimIndent()
    }

    private fun getEnglishPolicyHtml(): String {
        return """
            <p>Welcome to <b>Manchorati</b>. We are committed to safeguarding your privacy in compliance with international standards, including the <b>EU General Data Protection Regulation (GDPR)</b> and Google Play Developer Policies.</p>
            <br/>
            <b>1. Introduction & Acceptance</b><br/>
            By using Manchorati, you agree to these Terms of Service and Privacy Policy, granting you a personal license to create, read, and share inspiring quotes and card posts.
            <br/><br/>
            <b>2. Data We Collect</b><br/>
            • <b>Account Info:</b> Display name, email address, and profile photo collected via Google/email sign-in.<br/>
            • <b>User-Generated Content:</b> Posts, quotes, designed cards, comments, likes, and bookmarks stored on Google Cloud Firestore.<br/>
            • <b>Technical Identifiers:</b> Anonymous FCM push tokens for daily notifications and stored preferences (dark mode, language).
            <br/><br/>
            <b>3. Legal Basis for Processing (GDPR Art. 6)</b><br/>
            • Explicit user consent upon accepting these terms.<br/>
            • Performance of the contract to deliver social features and cloud sync.<br/>
            • Legitimate interest to maintain platform security and prevent abuse.
            <br/><br/>
            <b>4. Security & Encryption</b><br/>
            All data in transit is encrypted using HTTPS and TLS protocols. Unencrypted cleartext traffic is completely blocked.
            <br/><br/>
            <b>5. Your Rights Under GDPR & Google Play</b><br/>
            • <b>Right of Access & Rectification:</b> View and edit your profile and posts anytime.<br/>
            • <b>Right to Erasure ("Right to be Forgotten"):</b> Request permanent deletion of your account and all associated data via the Contact Us section.<br/>
            • <b>Right to Withdraw Consent:</b> Turn off push notifications or discontinue use at any time.
            <br/><br/>
            <b>6. Content Standards & Community Rules</b><br/>
            Hate speech, harassment, violence, and intellectual property infringement are strictly prohibited and subject to immediate removal and account termination.
            <br/><br/>
            <b>7. Children's Privacy</b><br/>
            Manchorati is intended for users aged 13 and older.
            <br/><br/>
            <b>8. Contact & Data Protection</b><br/>
            To exercise your GDPR rights or ask questions, please contact us via the in-app "Contact Us" feature.
        """.trimIndent()
    }
}
