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
    private const val KEY_ACCEPTED = "has_accepted_privacy_v2"

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
            <p>أهلاً بك في <b>منشوراتي (Manchorati)</b>. نحن نلتزم التزاماً مطلقاً بحماية خصوصيتك وبياناتك الشخصية وتقديم تجربة آمنة وموثوقة، وفقاً لأعلى المعايير العالمية بما يشمل <b>اللائحة العامة لحماية البيانات (GDPR)</b> وسياسات مطوري <b>Google Play</b>.</p>
            <br/>
            <b>1. قبول الشروط ونطاق الخدمة</b><br/>
            باستخدامك لتطبيق منشوراتي، فإنك توافق على بنود هذه السياسة. يتيح التطبيق للمستخدمين قراءة وتصميم ونشر الاقتباسات، والتفاعل مع مجتمع الكُتّاب عبر الإعجابات والتعليقات والمتابعة، واستخدام أدوات التصميم والمساعد الذكي.
            <br/><br/>
            <b>2. البيانات التي نقوم بجمعها</b><br/>
            • <b>بيانات الحساب والمصادقة:</b> عند تسجيل الدخول عبر Google أو البريد الإلكتروني، نقوم بجمع اسم العرض، عنوان البريد الإلكتروني، الصورة الشخصية، ومُعرّف المستخدم الفريد (UID) لإنشاء الحساب وتوثيقه.<br/>
            • <b>المحتوى المنشأ بواسطة المستخدم (UGC):</b> المنشورات، الاقتباسات المصممة، التعليقات، الإعجابات، وقوائم المفضلة المحفوظة في قاعدة بيانات Google Cloud Firestore السحابية.<br/>
            • <b>الوسائط والصور:</b> الصور التي تختار رفعها كصورة للملف الشخصي أو خلفيات للتصميم، حيث تُخزن مشفرة على سحابة Google Firebase Storage.<br/>
            • <b>المساعد الذكي (AI Writer):</b> النصوص التي تطلب من المساعد الذكي (Gemini API) صياغتها تُعالج لحظياً لتوليد الاقتباس فقط، ولا تُباع ولا تُستخدم لتدريب نماذج خاصة.<br/>
            • <b>المعرفات التقنية والإشعارات:</b> رموز Firebase Cloud Messaging (FCM) لتوصيل إشعارات التفاعل واقتباس اليوم، مع حفظ تفضيلاتك المحلية (اللغة والمظهر الليلي).
            <br/><br/>
            <b>3. الأمان وتشفير البيانات</b><br/>
            تخضع كافة البيانات أثناء النقل للتشفير الكامل باستخدام بروتوكولات HTTPS و TLS 1.3 الآمنة، مع حظر كامل لأي اتصال غير مشفر (Cleartext Traffic). قواعد الأمان السحابية تضمن أن لكل مستخدم صلاحية الوصول لبياناته فقط.
            <br/><br/>
            <b>4. حذف الحساب والبيانات فورياً (In-App Deletion)</b><br/>
            امتثالاً لسياسات Google Play الصارمة وحق الحذف (GDPR Art. 17):<br/>
            • يمكنك <b>حذف حسابك نهائياً بضغطة زر واحدة</b> مباشرة من داخل التطبيق عبر صفحة الملف الشخصي (خيار "حذف الحساب").<br/>
            • عند تأكيد الحذف، يتم فوراً وبشكل دائم حذف حساب المصادقة، ملفك الشخصي، متابعاتك، وكافة بياناتك من خوادم التطبيق دون رجعة.
            <br/><br/>
            <b>5. حقوقك بموجب اللائحة الأوروبية (GDPR)</b><br/>
            • <b>حق الوصول والتصحيح:</b> تعديل اسمك، صورتك، ومعرفك في أي وقت.<br/>
            • <b>حق سحب الموافقة:</b> تعطيل الإشعارات أو تسجيل الخروج متى شئت.<br/>
            • <b>حق محو البيانات:</b> الحذف الفوري للحساب أو التواصل معنا لأي طلب مخصص.
            <br/><br/>
            <b>6. معايير المجتمع والمحتوى المقبول</b><br/>
            يحظر تماماً نشر أي محتوى يحث على الكراهية، العنف، التمييز، التحرش، أو انتهاك حقوق الملكية الفكرية. يحتفظ فريق الإدارة بالحق الكامل في حذف أي محتوى مخالف فوراً وحظر الحساب.
            <br/><br/>
            <b>7. حماية خصوصية الأطفال (COPPA & GDPR)</b><br/>
            التطبيق مخصص للمستخدمين الذين تبلغ أعمارهم 13 عاماً فأكثر، ولا نجمع عن قصد بيانات للأطفال دون هذا السن.
            <br/><br/>
            <b>8. التواصل والدعم الفني</b><br/>
            لأي استفسار أو طلب يتعلق ببياناتك وخصوصيتك، يمكنك مراسلتنا عبر خيار "تواصل معنا" داخل القائمة الجانبية للتطبيق أو عبر البريد الإلكتروني المعتمد.
            <br/><br/>
            <i>آخر تحديث: نوفمبر 2026</i>
        """.trimIndent()
    }

    private fun getFrenchPolicyHtml(): String {
        return """
            <p>Bienvenue sur <b>Manchorati</b>. Nous nous engageons fermement à protéger votre vie privée et vos données personnelles conformément au <b>Règlement Général sur la Protection des Données (RGPD)</b> et aux politiques de <b>Google Play</b>.</p>
            <br/>
            <b>1. Acceptation et objet du service</b><br/>
            En utilisant Manchorati, vous acceptez la présente politique. L'application vous permet de créer, concevoir et partager des citations, d'interagir avec la communauté (mentions J'aime, commentaires, abonnements) et d'utiliser notre assistant d'écriture IA.
            <br/><br/>
            <b>2. Données personnelles collectées</b><br/>
            • <b>Données de compte :</b> Nom d'affichage, adresse e-mail, photo de profil et identifiant unique (UID) collectés via Google/e-mail.<br/>
            • <b>Contenu utilisateur (UGC) :</b> Citations, publications, commentaires, mentions J'aime et favoris stockés sur Google Cloud Firestore.<br/>
            • <b>Médias et fichiers :</b> Photos de profil et images de fond téléversées, hébergées de manière sécurisée sur Google Firebase Storage.<br/>
            • <b>Assistant d'écriture IA :</b> Les requêtes soumises à l'assistant IA (API Gemini) sont traitées de manière transitoire uniquement pour générer les citations demandées.<br/>
            • <b>Identifiants techniques et notifications :</b> Jetons FCM pour les notifications quotidiennes et préférences locales (langue, mode sombre).
            <br/><br/>
            <b>3. Sécurité et chiffrement</b><br/>
            Toutes les données en transit sont strictement chiffrées via HTTPS et TLS 1.3. Le trafic en clair (Cleartext) est formellement interdit.
            <br/><br/>
            <b>4. Suppression instantanée du compte dans l'application</b><br/>
            Conformément aux exigences de Google Play et au Droit à l'effacement (Art. 17 RGPD) :<br/>
            • Vous pouvez <b>supprimer définitivement votre compte directement depuis l'application</b> via le profil (« Supprimer le compte »).<br/>
            • Dès confirmation, vos données d'authentification, votre profil et vos données associées sont immédiatement et irrévocablement supprimés de nos serveurs.
            <br/><br/>
            <b>5. Vos droits (RGPD)</b><br/>
            • <b>Accès et rectification :</b> Modifiez votre profil et vos publications à tout moment.<br/>
            • <b>Retrait du consentement :</b> Désactivez les notifications ou supprimez votre compte à tout moment.
            <br/><br/>
            <b>6. Règles de la communauté</b><br/>
            Les discours haineux, le harcèlement, la violence et la violation des droits d'auteur sont strictement interdits et entraînent la suppression immédiate du contenu et le bannissement du compte.
            <br/><br/>
            <b>7. Protection des mineurs</b><br/>
            Application réservée aux personnes âgées de 13 ans et plus.
            <br/><br/>
            <b>8. Contact</b><br/>
            Pour toute demande, contactez notre équipe via la section « Contactez-nous » de l'application ou par e-mail officiel.
            <br/><br/>
            <i>Dernière mise à jour : Novembre 2026</i>
        """.trimIndent()
    }

    private fun getEnglishPolicyHtml(): String {
        return """
            <p>Welcome to <b>Manchorati</b>. We are fully committed to protecting your privacy, data security, and transparent processing in full compliance with the <b>EU General Data Protection Regulation (GDPR)</b> and <b>Google Play Developer Policies</b>.</p>
            <br/>
            <b>1. Acceptance & Service Scope</b><br/>
            By using Manchorati, you agree to these Terms and Privacy Policy. Manchorati provides a creative platform to discover, design, and share inspirational quotes, connect with fellow writers through likes, comments, and follows, and utilize AI writing tools.
            <br/><br/>
            <b>2. Information We Collect</b><br/>
            • <b>Account & Authentication Data:</b> When you sign in via Google or email, we collect your display name, email address, profile picture, and unique user ID (UID) to securely authenticate your account.<br/>
            • <b>User-Generated Content (UGC):</b> Posts, designed quote cards, comments, likes, and bookmarks securely stored on Google Cloud Firestore.<br/>
            • <b>Media & Storage:</b> Custom profile avatars and background images you choose to upload, safely encrypted on Google Firebase Storage.<br/>
            • <b>AI Writing Assistant:</b> Prompts submitted to the AI Writer (powered by Gemini API) are processed transiently to generate literary content and are never sold or used for public training.<br/>
            • <b>Technical Identifiers & Notifications:</b> Anonymous Firebase Cloud Messaging (FCM) tokens for interaction alerts and quote of the day, alongside local device preferences (language, dark mode).
            <br/><br/>
            <b>3. Data Security & Encryption</b><br/>
            All communication between the app and cloud servers is strictly encrypted using HTTPS and TLS 1.3 protocols. Unencrypted cleartext traffic is completely blocked. Robust Firebase Security Rules safeguard your personal data from unauthorized access.
            <br/><br/>
            <b>4. Instant In-App Account & Data Deletion</b><br/>
            In strict compliance with Google Play Data Safety requirements and the GDPR Right to Erasure (Art. 17):<br/>
            • You can <b>permanently delete your account directly inside the app</b> at any time via your Profile settings ("Delete Account" option).<br/>
            • Once confirmed, your authentication credentials, user profile, social links, and associated data are instantly and irreversibly deleted from our servers.
            <br/><br/>
            <b>5. Your Rights Under GDPR</b><br/>
            • <b>Access & Rectification:</b> View and edit your profile name, handle, and avatar anytime.<br/>
            • <b>Withdrawal of Consent:</b> Manage or disable push notifications in settings whenever you wish.<br/>
            • <b>Erasure & Portability:</b> Delete your account instantly or contact support for data inquiries.
            <br/><br/>
            <b>6. Community Standards & Acceptable Use</b><br/>
            Hate speech, harassment, violence, defamation, and copyright infringement are strictly prohibited. Violating content will be removed immediately, and offending accounts permanently suspended.
            <br/><br/>
            <b>7. Children's Privacy (COPPA & GDPR)</b><br/>
            Manchorati is intended for users aged 13 and above. We do not knowingly collect personal information from children under 13.
            <br/><br/>
            <b>8. Contact Us & Inquiries</b><br/>
            For privacy inquiries or data requests, reach us via the in-app "Contact Us" feature in the navigation menu or via our official support email.
            <br/><br/>
            <i>Last updated: November 2026</i>
        """.trimIndent()
    }
}
