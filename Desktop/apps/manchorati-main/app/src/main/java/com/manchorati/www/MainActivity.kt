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

    private lateinit var tabCategories: LinearLayout
    private lateinit var tabFeatured: LinearLayout
    private lateinit var tabFavorites: LinearLayout

    private lateinit var indicatorCategories: View
    private lateinit var indicatorFeatured: View
    private lateinit var indicatorFavorites: View

    private lateinit var tvToolbarTitle: TextView
    private lateinit var btnOptionsMenu: ImageView
    private lateinit var btnMainNotificationsContainer: View
    private lateinit var mainBadgeNotificationDot: View
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
            Toast.makeText(this, "تم تفعيل إشعارات منشور اليوم 🌟", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // قراءة حالة الوضع الليلي وتطبيقها قبل استدعاء setContentView
        val themePrefs = getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
        val isDarkMode = themePrefs.getBoolean("is_dark_mode", false)
        AppCompatDelegate.setDefaultNightMode(
            if (isDarkMode) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkAndRequestNotificationPermission()
        DailyNotificationHelper.setupDailyNotifications(this)

        dbHelper = DatabaseHelper(this)
        syncManager = SyncManager(this, dbHelper)

        initViews()
        setupViewPager()
        setupTabs()
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
        tabCategories = findViewById(R.id.tabCategories)
        tabFeatured = findViewById(R.id.tabFeatured)
        tabFavorites = findViewById(R.id.tabFavorites)

        indicatorCategories = findViewById(R.id.indicatorCategories)
        indicatorFeatured = findViewById(R.id.indicatorFeatured)
        indicatorFavorites = findViewById(R.id.indicatorFavorites)

        tvToolbarTitle = findViewById(R.id.tvToolbarTitle)
        btnOptionsMenu = findViewById(R.id.btnOptionsMenu)
        btnMainNotificationsContainer = findViewById(R.id.btnMainNotificationsContainer)
        mainBadgeNotificationDot = findViewById(R.id.mainBadgeNotificationDot)
        etSearch = findViewById(R.id.etSearch)
    }

    private fun setupHeaderActions() {
      
        btnMainNotificationsContainer.setOnClickListener {
            val user = FirebaseAuth.getInstance().currentUser
            if (user == null) {
                Toast.makeText(this, "سجل دخولك لعرض الإشعارات", Toast.LENGTH_SHORT).show()
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

        val title = if (notif.senderName.isNotEmpty()) notif.senderName else "منشوراتي"
        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(notif.message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(notif.message))
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
            viewPager.currentItem = 1
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
                    viewPager.currentItem = 1
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
                .setTitle("مسح الإشعارات")
                .setMessage("هل تريد مسح جميع الإشعارات نهائياً؟")
                .setPositiveButton("مسح الكل") { _, _ ->
                    FirestoreManager.clearAllNotifications(currentUid) {
                        userNotifications = emptyList()
                        notifAdapter.updateData(emptyList())
                        updateEmptyState()
                        Toast.makeText(this, "تم مسح جميع الإشعارات 🧹", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("إلغاء", null)
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
                        Toast.makeText(this@MainActivity, "تم حذف الإشعار 🗑️", Toast.LENGTH_SHORT).show()
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
                    0 -> "منشوراتي"
                    1 -> "المجتمع"
                    2 -> "المفضلة"
                    else -> "منشوراتي"
                }
                val currentText = etSearch.text.toString()
                notifySearchToFragments(currentText)
            }
        })
    }

    private fun setupTabs() {
        tabCategories.setOnClickListener { viewPager.currentItem = 0 }
        tabFeatured.setOnClickListener { viewPager.currentItem = 1 }
        tabFavorites.setOnClickListener { viewPager.currentItem = 2 }
    }

    private fun highlightTab(index: Int) {
        currentTab = index
        val activeColor = Color.parseColor("#EF4444")
        val transparent = Color.TRANSPARENT

        indicatorCategories.setBackgroundColor(if (index == 0) activeColor else transparent)
        indicatorFeatured.setBackgroundColor(if (index == 1) activeColor else transparent)
        indicatorFavorites.setBackgroundColor(if (index == 2) activeColor else transparent)
    }

    private fun setupSearch() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString() ?: ""
                notifySearchToFragments(query)
            }
            override fun afterTextChanged(s: Editable?) {}
        })
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

        findViewById<View>(R.id.btnDrawerContact)?.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.END)
            startActivity(Intent(this, ContactActivity::class.java))
        }

        findViewById<View>(R.id.btnDrawerNotifications)?.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.END)
            startActivity(Intent(this, NotificationSettingsActivity::class.java))
        }

        findViewById<View>(R.id.btnDrawerRate)?.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.END)
            openPlayStore()
        }

        findViewById<View>(R.id.btnDrawerShare)?.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.END)
            shareApp()
        }

        findViewById<View>(R.id.btnDrawerMoreApps)?.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.END)
            openMoreApps()
        }

        findViewById<View>(R.id.btnDrawerLogout)?.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.END)
            performLogout()
        }
    }

  private fun updateDrawerHeader() {
        val user = FirebaseAuth.getInstance().currentUser
        val tvName = findViewById<TextView>(R.id.tvDrawerName)
        val tvUsername = findViewById<TextView>(R.id.tvDrawerUsername)
        val ivAvatar = findViewById<ImageView>(R.id.ivDrawerAvatar)
        val btnLogout = findViewById<View>(R.id.btnDrawerLogout)

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
            tvName?.text = "زائر كريم"
            tvUsername?.text = "اضغط لتسجيل الدخول"
            btnLogout?.visibility = View.GONE
            ivAvatar?.setImageResource(R.drawable.ic_profile)

            val loginAction = View.OnClickListener {
                drawerLayout.closeDrawer(GravityCompat.END) // أو androidx.core.view.GravityCompat.END حسب استيراداتك
                viewPager.currentItem = 1
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
            .setTitle("تسجيل الخروج")
            .setMessage("هل أنت متأكد من رغبتك في تسجيل الخروج؟")
            .setPositiveButton("خروج") { _, _ ->
                FirebaseAuth.getInstance().signOut()
                AuthManager.getGoogleClient(this).signOut().addOnCompleteListener {
                    getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                        .edit()
                        .remove("user_name")
                        .apply()
                    updateDrawerHeader()
                    Toast.makeText(this, "تم تسجيل الخروج بنجاح", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("إلغاء", null)
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
            putExtra(Intent.EXTRA_TEXT, "حمل تطبيق منشوراتي لأجمل الكلمات والمنشورات: https://play.google.com/store/apps/details?id=$packageName")
        }
        startActivity(Intent.createChooser(shareIntent, "مشاركة التطبيق عبر"))
    }

    private fun openMoreApps() {
        val uri = Uri.parse("https://play.google.com/store/apps/developer?id=hanane")
        startActivity(Intent.createChooser(Intent(Intent.ACTION_VIEW, uri), "فتح عبر"))
    }

    private fun setupBackNavigation() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
                    drawerLayout.closeDrawer(GravityCompat.END)
                } else if (viewPager.currentItem != 0) {
                    viewPager.currentItem = 0
                } else {
                    if (System.currentTimeMillis() - backPressedTime < 2000) {
                        finish()
                    } else {
                        backPressedTime = System.currentTimeMillis()
                        Toast.makeText(this@MainActivity, "اضغط مرة أخرى للخروج", Toast.LENGTH_SHORT).show()
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