package com.manchorati.www

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class CategoriesFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var categoryAdapter: CategoryAdapter

    private val updateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            refreshCategories()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_categories, container, false)
        recyclerView = view.findViewById(R.id.rvCategories)
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        dbHelper = DatabaseHelper(requireContext())

        val prefs = requireContext().getSharedPreferences("viewed_categories", Context.MODE_PRIVATE)

        // جلب الأقسام وفحص ما إذا كان المستخدم قد رآها مسبقاً
        val categories = dbHelper.getCategoriesWithCounts()
        for (cat in categories) {
            val isSeen = prefs.getBoolean("seen_${cat.id}", false)
            if (isSeen) {
                cat.hasNew = false
            }
        }

        categoryAdapter = CategoryAdapter(categories) { selectedCat ->
            // 1. إذا كان القسم يحمل شارة جديد، نقوم بإخفائها وحفظ الحالة
            if (selectedCat.hasNew) {
                selectedCat.hasNew = false
                prefs.edit().putBoolean("seen_${selectedCat.id}", true).apply()
                categoryAdapter.notifyDataSetChanged()
            }

            // 2. فتح منشورات القسم
            (activity as? MainActivity)?.openCategoryPosts(selectedCat.id, selectedCat.name)
        }

        recyclerView.adapter = categoryAdapter

        return view
    }

    override fun onResume() {
        super.onResume()
        refreshCategories()
        context?.let {
            LocalBroadcastManager.getInstance(it).registerReceiver(
                updateReceiver,
                IntentFilter("com.manchorati.CATEGORY_UPDATED")
            )
        }
    }

    override fun onPause() {
        super.onPause()
        context?.let {
            try {
                LocalBroadcastManager.getInstance(it).unregisterReceiver(updateReceiver)
            } catch (_: Exception) {}
        }
    }

    fun refreshCategories() {
        if (!::categoryAdapter.isInitialized || !::dbHelper.isInitialized || context == null) return
        val prefs = requireContext().getSharedPreferences("viewed_categories", Context.MODE_PRIVATE)
        val categories = dbHelper.getCategoriesWithCounts()
        for (cat in categories) {
            val isSeen = prefs.getBoolean("seen_${cat.id}", false)
            if (isSeen) {
                cat.hasNew = false
            }
        }
        categoryAdapter.updateData(categories)
    }
}