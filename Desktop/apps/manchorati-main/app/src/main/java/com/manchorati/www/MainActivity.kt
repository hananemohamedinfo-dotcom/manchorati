package com.manchorati.www

import android.Manifest
import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SwitchCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var syncManager: SyncManager
    private lateinit var viewPager: ViewPager2
    private lateinit var drawerLayout: DrawerLayout

    private lateinit var navItemCommunity: LinearLayout
    private lateinit var navItemCategories: LinearLayout
    private lateinit var navItemFavorites: LinearLayout
    private lateinit var navItemProfile: LinearLayout

    private lateinit var ivNavCommunity: ImageView
    private lateinit var tvNavCommunity: TextView
    private lateinit var ivNavCategories: ImageView
    private lateinit var tvNavCategories: TextView
    private lateinit var ivNavFavorites: ImageView
    private lateinit var tvNavFavorites: TextView
    private lateinit var ivNavProfile: ImageView
    private lateinit var tvNavProfile: TextView

    private lateinit var tvToolbarTitle: TextView
    private lateinit var btnOptionsMenu: ImageView
    private lateinit var btnMainNotificationsContainer: View
    private lateinit var mainBadgeNotificationDot: View
    private lateinit var layoutNormalHeader: View
    private lateinit var layoutSearchHeader: View
    private lateinit var btnSearchToggle: ImageView
    private lateinit var btnSearchClose: ImageView
    private lateinit var btnSearchClear: ImageView
    private lateinit var etSearch: EditText

    private var currentTab = 0
    private var backPressedTime: Long = 0
    private var userNotifications: List<AppNotification> = emptyList()
    private var notificationsListener: ListenerRegistration? = null
    private var authStateListener: FirebaseAuth.AuthStateListener? = null

    private val imagePickerLauncher: ActivityResultLauncher<String> = ImageUtils.registerImagePicker(this)

    // لاونشر طلب إذن إظهار الإشعارات لأندرويد 13 فما فوق
    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Toast.makeText(this, "تم تفعيل إشعارات منشور اليوم", Toast.LENGTH_SHORT).show()
        }
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.applyLocale(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // قراءة حالة الوضع الليلي وتطبيقها قبل استدعاء setContentView
        val themePrefs = getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
        val isDarkMode = themePrefs.getBoolean("is_dark_mode", false)
        AppCompatDelegate.setDefaultNightMode(
            if (isDarkMode) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )

        super.onCreate(savedInstanceState)
        LocaleHelper.applyLayoutDirection(this)
        setContentView(R.layout.activity_main)

        checkAndRequestNotificationPermission()
        DailyNotificationHelper.setupDailyNotifications(this)

        dbHelper = DatabaseHelper(this)
        syncManager = SyncManager(this, dbHelper)

        initViews()
        setupViewPager()
        setupTabs()
        viewPager.setCurrentItem(0, false)
        highlightTab(0)
        setupSearch()
        setupDrawerNavigation()
        setupBackNavigation()
        setupHeaderActions()
        setupAuthWatcher()

        lifecycleScope.launch {
            syncManager.checkAndUpdatePosts()
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        handleNotificationIntent(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntent(intent)
    }

    private fun checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        listenToNotifications()
        updateDrawerHeader()

    }

    override fun onDestroy() {
        super.onDestroy()
        notificationsListener?.remove()
        authStateListener?.let { FirebaseAuth.getInstance().removeAuthStateListener(it) }
    }

    private fun setupAuthWatcher() {
        authStateListener = FirebaseAuth.AuthStateListener {
            listenToNotifications()
            updateDrawerHeader()
        }
        FirebaseAuth.getInstance().addAuthStateListener(authStateListener!!)
    }
    
    private fun initViews() {
        drawerLayout = findViewById(R.id.drawerLayout)
        viewPager = findViewById(R.id.viewPager)

        navItemCommunity = findViewById(R.id.navItemCommunity)
        navItemCategories = findViewById(R.id.navItemCategories)
        navItemFavorites = findViewById(R.id.navItemFavorites)
        navItemProfile = findViewById(R.id.navItemProfile)

        ivNavCommunity = findViewById(R.id.ivNavCommunity)
        tvNavCommunity = findViewById(R.id.tvNavCommunity)
        ivNavCategories = findViewById(R.id.ivNavCategories)
        tvNavCategories = findViewById(R.id.tvNavCategories)
        ivNavFavorites = findViewById(R.id.ivNavFavorites)
        tvNavFavorites = findViewById(R.id.tvNavFavorites)
        ivNavProfile = findViewById(R.id.ivNavProfile)
        tvNavProfile = findViewById(R.id.tvNavProfile)

        tvToolbarTitle = findViewById(R.id.tvToolbarTitle)
        btnOptionsMenu = findViewById(R.id.btnOptionsMenu)
        btnMainNotificationsContainer = findViewById(R.id.btnMainNotificationsContainer)
        mainBadgeNotificationDot = findViewById(R.id.mainBadgeNotificationDot)
        layoutNormalHeader = findViewById(R.id.layoutNormalHeader)
        layoutSearchHeader = findViewById(R.id.layoutSearchHeader)
        btnSearchToggle = findViewById(R.id.btnSearchToggle)
        btnSearchClose = findViewById(R.id.btnSearchClose)
        btnSearchClear = findViewById(R.id.btnSearchClear)
        etSearch = findViewById(R.id.etSearch)
    }

    private fun setupHeaderActions() {
      
        btnMainNotificationsContainer.setOnClickListener {
            val user = FirebaseAuth.getInstance().currentUser
            if (user == null) {
                Toast.makeText(this, getString(R.string.login_required_notifications), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            showNotificationsBottomSheet()
        }
    }

    private var lastListenerStartTime: Long = 0L
    private var hasLoadedInitialSnapshot = false

    private fun listenToNotifications() {
        notificationsListener?.remove()

        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid.isNullOrEmpty()) {
            userNotifications = emptyList()
            mainBadgeNotificationDot.visibility = View.GONE
            return
        }

        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        lastListenerStartTime = System.currentTimeMillis()
        hasLoadedInitialSnapshot = false

        notificationsListener = FirestoreManager.listenToUserNotifications(uid) { list ->
            runOnUiThread {
                userNotifications = list
                val lastReadTime = prefs.getLong("last_notification_read_time_$uid", 0L)
                val lastNotifiedTime = prefs.getLong("last_system_notified_time_$uid", 0L)

                val hasTrulyUnread = list.any { notif ->
                    !notif.isRead && notif.timestamp > lastReadTime
                }
                mainBadgeNotificationDot.visibility = if (hasTrulyUnread) View.VISIBLE else View.GONE

                // إذا وصل إشعار جديد في الوقت الفعلي ولم يتم تنبيه المستخدم به بعد على الهاتف
                if (hasLoadedInitialSnapshot) {
                    val newUnreadNotifs = list.filter { notif ->
                        !notif.isRead && notif.timestamp > lastNotifiedTime && notif.timestamp >= (lastListenerStartTime - 10000L)
                    }
                    if (newUnreadNotifs.isNotEmpty()) {
                        for (newNotif in newUnreadNotifs) {
                            showSystemNotification(newNotif)
                        }
                        val maxTime = newUnreadNotifs.maxOf { it.timestamp }
                        prefs.edit().putLong("last_system_notified_time_$uid", maxTime).apply()
                    }
                } else {
                    hasLoadedInitialSnapshot = true
                }
            }
        }
    }

    private fun showSystemNotification(notif: AppNotification) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "community_activity_channel_v1"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "إشعارات المجتمع والتفاعل",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "إشعارات المنشورات والتعليقات والمتابعين"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 150, 250)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("OPEN_POST_ID", notif.postId)
            putExtra("OPEN_POST_TYPE", notif.type)
            putExtra("OPEN_SENDER_ID", notif.senderId)
            putExtra("OPEN_SENDER_NAME", notif.senderName)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            (notif.id.ifEmpty { notif.timestamp.toString() }).hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val sender = if (notif.senderName.isNotEmpty()) notif.senderName else getString(R.string.app_name)
        val title = sender
        val body = when (notif.type) {
            "LIKE" -> getString(R.string.notif_liked_post, sender)
            "COMMENT" -> getString(R.string.notif_commented_post, sender)
            "FOLLOW" -> getString(R.string.notif_followed_you, sender)
            "NEW_POST" -> getString(R.string.notif_new_post, sender)
            "REPLY" -> getString(R.string.notif_replied_comment, sender)
            else -> notif.message.ifEmpty { getString(R.string.app_name) }
        }
        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val notifId = (notif.timestamp % 100000).toInt() + 200
        notificationManager.notify(notifId, builder.build())
    }

    private fun handleNotificationIntent(intent: Intent?) {
        if (intent == null) return
        val postId = intent.getStringExtra("OPEN_POST_ID") ?: ""
        val type = intent.getStringExtra("OPEN_POST_TYPE") ?: ""
        val senderId = intent.getStringExtra("OPEN_SENDER_ID") ?: ""
        val senderName = intent.getStringExtra("OPEN_SENDER_NAME") ?: ""

        if (type == "FOLLOW" && senderId.isNotEmpty()) {
            val profileIntent = Intent(this, UserProfileActivity::class.java).apply {
                putExtra("USER_ID", senderId)
                putExtra("USER_NAME", senderName)
            }
            startActivity(profileIntent)
        } else if (postId.isNotEmpty()) {
            viewPager.currentItem = 0
            viewPager.postDelayed({
                supportFragmentManager.fragments.forEach { fragment ->
                    if (fragment is CommunityFragment) {
                        fragment.handleNotificationClick(postId, type)
                    }
                }
            }, 300)
        }
    }

    private fun showNotificationsBottomSheet() {
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        mainBadgeNotificationDot.visibility = View.GONE

        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        prefs.edit().putLong("last_notification_read_time_$currentUid", System.currentTimeMillis()).apply()

        FirestoreManager.markAllNotificationsAsRead(currentUid)

        userNotifications = userNotifications.map { it.copy(isRead = true) }

        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.dialog_notifications, null)
        dialog.setContentView(view)

        val rv = view.findViewById<RecyclerView>(R.id.rvNotifications)
        val tvEmpty = view.findViewById<TextView>(R.id.tvEmptyNotifications)
        val btnClearAll = view.findViewById<TextView>(R.id.btnClearAllNotifications)

        rv.layoutManager = LinearLayoutManager(this)
        val notifAdapter = NotificationsAdapter(userNotifications) { notif ->
            dialog.dismiss()

            when (notif.type) {
                "FOLLOW" -> {
                    if (notif.senderId.isNotEmpty()) {
                        val intent = Intent(this, UserProfileActivity::class.java).apply {
                            putExtra("USER_ID", notif.senderId)
                            putExtra("USER_NAME", notif.senderName)
                        }
                        startActivity(intent)
                    }
                }
                "LIKE", "COMMENT", "NEW_POST" -> {
                    viewPager.currentItem = 0
                    viewPager.postDelayed({
                        supportFragmentManager.fragments.forEach { fragment ->
                            if (fragment is CommunityFragment) {
                                fragment.handleNotificationClick(notif.postId, notif.type)
                            }
                        }
                    }, 250)
                }
            }
        }
        rv.adapter = notifAdapter

        fun updateEmptyState() {
            val isEmpty = notifAdapter.itemCount == 0
            tvEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
            btnClearAll?.visibility = if (isEmpty) View.GONE else View.VISIBLE
        }
        updateEmptyState()

        btnClearAll?.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle(getString(R.string.clear_notifications_title))
                .setMessage(getString(R.string.clear_notifications_confirm))
                .setPositiveButton(getString(R.string.clear_all)) { _, _ ->
                    FirestoreManager.clearAllNotifications(currentUid) {
                        userNotifications = emptyList()
                        notifAdapter.updateData(emptyList())
                        updateEmptyState()
                        Toast.makeText(this, getString(R.string.notifications_cleared), Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton(getString(R.string.no), null)
                .show()
        }

        // خاصية السحب للحذف Swipe-to-delete
        val swipeHandler = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val removed = notifAdapter.removeItem(position)
                    if (removed != null) {
                        FirestoreManager.deleteNotification(removed.id)
                        userNotifications = userNotifications.filter { it.id != removed.id }
                        updateEmptyState()
                        Toast.makeText(this@MainActivity, "تم حذف الإشعار", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                val itemView = viewHolder.itemView
                val bg = ColorDrawable(Color.parseColor("#EF4444"))
                if (dX > 0) {
                    bg.setBounds(itemView.left, itemView.top, itemView.left + dX.toInt(), itemView.bottom)
                } else if (dX < 0) {
                    bg.setBounds(itemView.right + dX.toInt(), itemView.top, itemView.right, itemView.bottom)
                } else {
                    bg.setBounds(0, 0, 0, 0)
                }
                bg.draw(c)
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }
        }
        ItemTouchHelper(swipeHandler).attachToRecyclerView(rv)

        dialog.show()
    }

    private fun setupViewPager() {
        val pagerAdapter = MainPagerAdapter(this)
        viewPager.adapter = pagerAdapter

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                highlightTab(position)
                tvToolbarTitle.text = when (position) {
                    0 -> getString(R.string.tab_community)
                    1 -> getString(R.string.tab_categories)
                    2 -> getString(R.string.tab_favorites)
                    else -> getString(R.string.app_name)
                }
                val currentText = etSearch.text.toString()
                notifySearchToFragments(currentText)
            }
        })
    }

    private fun setupTabs() {
        navItemCommunity.setOnClickListener { viewPager.currentItem = 0 }
        navItemCategories.setOnClickListener { viewPager.currentItem = 1 }
        navItemFavorites.setOnClickListener { viewPager.currentItem = 2 }
        navItemProfile.setOnClickListener {
            val user = FirebaseAuth.getInstance().currentUser
            if (user == null) {
                Toast.makeText(this, getString(R.string.login_required_profile), Toast.LENGTH_SHORT).show()
                viewPager.currentItem = 0
                viewPager.postDelayed({
                    supportFragmentManager.fragments.forEach { fragment ->
                        if (fragment is CommunityFragment) {
                            fragment.showLoginDialog()
                        }
                    }
                }, 200)
            } else {
                val intent = Intent(this, UserProfileActivity::class.java).apply {
                    putExtra("USER_ID", user.uid)
                    putExtra("USER_NAME", user.displayName ?: getString(R.string.default_user_name))
                }
                startActivity(intent)
            }
        }
    }

    private fun highlightTab(index: Int) {
        currentTab = index
        val activeColor = Color.parseColor("#EF4444")
        val inactiveColor = ContextCompat.getColor(this, R.color.text_secondary)

        ivNavCommunity.imageTintList = android.content.res.ColorStateList.valueOf(if (index == 0) activeColor else inactiveColor)
        tvNavCommunity.setTextColor(if (index == 0) activeColor else inactiveColor)
        tvNavCommunity.typeface = if (index == 0) android.graphics.Typeface.DEFAULT_BOLD else android.graphics.Typeface.DEFAULT

        ivNavCategories.imageTintList = android.content.res.ColorStateList.valueOf(if (index == 1) activeColor else inactiveColor)
        tvNavCategories.setTextColor(if (index == 1) activeColor else inactiveColor)
        tvNavCategories.typeface = if (index == 1) android.graphics.Typeface.DEFAULT_BOLD else android.graphics.Typeface.DEFAULT

        ivNavFavorites.imageTintList = android.content.res.ColorStateList.valueOf(if (index == 2) activeColor else inactiveColor)
        tvNavFavorites.setTextColor(if (index == 2) activeColor else inactiveColor)
        tvNavFavorites.typeface = if (index == 2) android.graphics.Typeface.DEFAULT_BOLD else android.graphics.Typeface.DEFAULT

        ivNavProfile.imageTintList = android.content.res.ColorStateList.valueOf(inactiveColor)
        tvNavProfile.setTextColor(inactiveColor)
        tvNavProfile.typeface = android.graphics.Typeface.DEFAULT
    }

    private fun setupSearch() {
        btnSearchToggle.setOnClickListener {
            openSearch()
        }

        btnSearchClose.setOnClickListener {
            closeSearch()
        }

        btnSearchClear.setOnClickListener {
            etSearch.setText("")
        }

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString() ?: ""
                btnSearchClear.visibility = if (query.isNotEmpty()) View.VISIBLE else View.GONE
                notifySearchToFragments(query)
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun openSearch() {
        layoutNormalHeader.visibility = View.GONE
        layoutSearchHeader.visibility = View.VISIBLE
        etSearch.requestFocus()
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
        imm?.showSoftInput(etSearch, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
    }

    private fun closeSearch() {
        etSearch.setText("")
        layoutSearchHeader.visibility = View.GONE
        layoutNormalHeader.visibility = View.VISIBLE
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
        imm?.hideSoftInputFromWindow(etSearch.windowToken, 0)
    }

    private fun notifySearchToFragments(query: String) {
        supportFragmentManager.fragments.forEach { fragment ->
            if (fragment is CommunityFragment) {
                fragment.onSearchQueryChanged(query)
            }
        }
    }

    private fun setupDrawerNavigation() {
        btnOptionsMenu.setOnClickListener {
            updateDrawerHeader()
            drawerLayout.openDrawer(GravityCompat.END)
        }

        val switchDarkMode = findViewById<SwitchCompat>(R.id.switchDarkMode)
        val themePrefs = getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
        val currentDark = themePrefs.getBoolean("is_dark_mode", false)
        switchDarkMode?.isChecked = currentDark

        switchDarkMode?.setOnCheckedChangeListener { _, isChecked ->
            themePrefs.edit().putBoolean("is_dark_mode", isChecked).apply()
            AppCompatDelegate.setDefaultNightMode(
                if (isChecked) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            )
        }

        val tvDrawerCurrentLang = findViewById<TextView>(R.id.tvDrawerCurrentLang)
        fun updateLangDisplay() {
            val curr = LocaleHelper.getSelectedLanguage(this)
            tvDrawerCurrentLang?.text = when (curr) {
                LocaleHelper.LANG_FR -> "Français"
                LocaleHelper.LANG_EN -> "English"
                else -> "العربية"
            }
        }
        updateLangDisplay()

        findViewById<View>(R.id.btnDrawerLanguage)?.setOnClickListener {
            val languages = arrayOf(
                getString(R.string.lang_ar),
                getString(R.string.lang_fr),
                getString(R.string.lang_en)
            )
            val langCodes = arrayOf(
                LocaleHelper.LANG_AR,
                LocaleHelper.LANG_FR,
                LocaleHelper.LANG_EN
            )
            val currentLang = LocaleHelper.getSelectedLanguage(this)
            val checkedIndex = langCodes.indexOf(currentLang).coerceAtLeast(0)

            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(getString(R.string.select_language))
                .setSingleChoiceItems(languages, checkedIndex) { dialog, which ->
                    dialog.dismiss()
                    val selectedCode = langCodes[which]
                    if (selectedCode != currentLang) {
                        LocaleHelper.setLocale(this, selectedCode)
                        drawerLayout.closeDrawers()
                        val intent = Intent(this, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                        startActivity(intent)
                        finish()
                    }
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }

        findViewById<View>(R.id.btnDrawerContact)?.setOnClickListener {
            drawerLayout.closeDrawers()
            startActivity(Intent(this, ContactActivity::class.java))
        }

        findViewById<View>(R.id.btnDrawerNotifications)?.setOnClickListener {
            drawerLayout.closeDrawers()
            startActivity(Intent(this, NotificationSettingsActivity::class.java))
        }

        findViewById<View>(R.id.btnDrawerRate)?.setOnClickListener {
            drawerLayout.closeDrawers()
            openPlayStore()
        }

        findViewById<View>(R.id.btnDrawerShare)?.setOnClickListener {
            drawerLayout.closeDrawers()
            shareApp()
        }

        findViewById<View>(R.id.btnDrawerMoreApps)?.setOnClickListener {
            drawerLayout.closeDrawers()
            openMoreApps()
        }

        findViewById<View>(R.id.btnDrawerLogout)?.setOnClickListener {
            drawerLayout.closeDrawers()
            performLogout()
        }
    }

  private fun updateDrawerHeader() {
        val user = FirebaseAuth.getInstance().currentUser
        val tvName = findViewById<TextView>(R.id.tvDrawerName)
        val tvUsername = findViewById<TextView>(R.id.tvDrawerUsername)
        val ivAvatar = findViewById<ImageView>(R.id.ivDrawerAvatar)
        val btnLogout = findViewById<View>(R.id.btnDrawerLogout)

        val currLang = LocaleHelper.getSelectedLanguage(this)
        findViewById<TextView>(R.id.tvDrawerCurrentLang)?.text = when (currLang) {
            LocaleHelper.LANG_FR -> "Français"
            LocaleHelper.LANG_EN -> "English"
            else -> "العربية"
        }

        if (user != null) {
            // جلب الاسم المحدث من SharedPreferences
            val prefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
            val savedName = prefs.getString("user_name", user.displayName ?: "فاعل خير")
            tvName?.text = savedName

            tvUsername?.text = user.email ?: "@مستخدم"
            btnLogout?.visibility = View.VISIBLE

            val photoUrl = user.photoUrl?.toString()
            if (!photoUrl.isNullOrEmpty()) {
                Glide.with(this)
                    .load(photoUrl)
                    .circleCrop()
                    .placeholder(R.drawable.ic_profile)
                    .into(ivAvatar)
            } else {
                ivAvatar?.setImageResource(R.drawable.ic_profile)
            }

            // ---- كود الانتقال للملف الشخصي ----
            val goToProfile = View.OnClickListener {
                startActivity(Intent(this, UserProfileActivity::class.java))
            }

            // تطبيق النقر على الصورة، الاسم، والجيميل
            ivAvatar?.setOnClickListener(goToProfile)
            tvName?.setOnClickListener(goToProfile)
            tvUsername?.setOnClickListener(goToProfile)

        } else {
            // ---- كود الزائر ----
            tvName?.text = getString(R.string.guest_user)
            tvUsername?.text = getString(R.string.tap_to_login)
            btnLogout?.visibility = View.GONE
            ivAvatar?.setImageResource(R.drawable.ic_profile)

            val loginAction = View.OnClickListener {
                drawerLayout.closeDrawers()
                viewPager.currentItem = 0
                viewPager.postDelayed({
                    supportFragmentManager.fragments.forEach { fragment ->
                        if (fragment is CommunityFragment) {
                            fragment.showLoginDialog()
                        }
                    }
                }, 200)
            }

            // للزائر أيضاً نقوم بتفعيل النقر لتسجيل الدخول
            ivAvatar?.setOnClickListener(loginAction)
            tvName?.setOnClickListener(loginAction)
            tvUsername?.setOnClickListener(loginAction)
        }
    }
    
    private fun performLogout() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.logout_confirm_title))
            .setMessage(getString(R.string.logout_confirm_msg))
            .setPositiveButton(getString(R.string.drawer_logout)) { _, _ ->
                FirebaseAuth.getInstance().signOut()
                AuthManager.getGoogleClient(this).signOut().addOnCompleteListener {
                    getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                        .edit()
                        .remove("user_name")
                        .apply()
                    updateDrawerHeader()
                    Toast.makeText(this, getString(R.string.logout_success), Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun openPlayStore() {
        val uri = Uri.parse("market://details?id=$packageName")
        val goToMarket = Intent(Intent.ACTION_VIEW, uri)
        try {
            startActivity(goToMarket)
        } catch (e: Exception) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName")))
        }
    }

    private fun shareApp() {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, getString(R.string.share_app_text, packageName))
        }
        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_app_chooser)))
    }

    private fun openMoreApps() {
        val uri = Uri.parse("https://play.google.com/store/apps/developer?id=hanane")
        startActivity(Intent.createChooser(Intent(Intent.ACTION_VIEW, uri), getString(R.string.share_post_chooser)))
    }

    private fun setupBackNavigation() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.END) || drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawers()
                } else if (layoutSearchHeader.visibility == View.VISIBLE) {
                    closeSearch()
                } else if (viewPager.currentItem != 0) {
                    viewPager.currentItem = 0
                } else {
                    if (System.currentTimeMillis() - backPressedTime < 2000) {
                        finish()
                    } else {
                        backPressedTime = System.currentTimeMillis()
                        Toast.makeText(this@MainActivity, getString(R.string.press_back_again_to_exit), Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })  
    }

    fun openCategoryPosts(categoryId: Int, categoryTitle: String) {
        val intent = Intent(this, CategoryPostsActivity::class.java).apply {
            putExtra("CATEGORY_ID", categoryId)
            putExtra("CATEGORY_TITLE", categoryTitle)
        }
        startActivity(intent)
    }
}