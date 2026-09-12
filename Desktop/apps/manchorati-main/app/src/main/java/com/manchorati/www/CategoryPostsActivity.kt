package com.manchorati.www

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class CategoryPostsActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var recyclerView: RecyclerView
    private lateinit var tvTitle: TextView
    private lateinit var btnBack: ImageView
    private lateinit var btnTextBigger: TextView
    private lateinit var btnTextSmaller: TextView

    private var postAdapter: PostAdapter? = null
    private var currentFontSize = 16f

    private val imagePickerLauncher: ActivityResultLauncher<String> = ImageUtils.registerImagePicker(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_category_posts)

        val categoryId = intent.getIntExtra("CATEGORY_ID", -1)
        val categoryTitle = intent.getStringExtra("CATEGORY_TITLE") ?: "المنشورات"

        dbHelper = DatabaseHelper(this)
        dbHelper.clearCategoryNewBadge(categoryId)

        tvTitle = findViewById(R.id.tvToolbarTitle)
        btnBack = findViewById(R.id.btnBack)
        btnTextBigger = findViewById(R.id.btnTextBigger)
        btnTextSmaller = findViewById(R.id.btnTextSmaller)
        recyclerView = findViewById(R.id.rvCategoryPosts)

        tvTitle.text = categoryTitle
        btnBack.setOnClickListener { finish() }

        recyclerView.layoutManager = LinearLayoutManager(this)
        val posts = dbHelper.getPostsByCategory(categoryId)
        postAdapter = PostAdapter(this, posts, currentFontSize) {
            imagePickerLauncher.launch("image/*")
        }
        recyclerView.adapter = postAdapter

        btnTextBigger.setOnClickListener {
            if (currentFontSize < 26f) {
                currentFontSize += 2f
                postAdapter?.updateFontSize(currentFontSize)
            }
        }

        btnTextSmaller.setOnClickListener {
            if (currentFontSize > 12f) {
                currentFontSize -= 2f
                postAdapter?.updateFontSize(currentFontSize)
            }
        }
    }
}