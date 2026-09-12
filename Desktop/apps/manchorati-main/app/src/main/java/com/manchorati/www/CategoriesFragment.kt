package com.manchorati.www

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class CategoriesFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var categoryAdapter: CategoryAdapter

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
}