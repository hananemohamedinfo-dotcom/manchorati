package com.manchorati.www

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class FavoritesFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var dbHelper: DatabaseHelper

    private val imagePickerLauncher: ActivityResultLauncher<String> =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let { ImageUtils.handleImageUri(requireContext(), it) }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_favorites, container, false)
        recyclerView = view.findViewById(R.id.rvFavorites)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        dbHelper = DatabaseHelper(requireContext())

        val posts = dbHelper.getFavoritePosts()
        val postAdapter = PostAdapter(requireContext(), posts, 16f) {
            imagePickerLauncher.launch("image/*")
        }
        recyclerView.adapter = postAdapter

        return view
    }

    override fun onResume() {
        super.onResume()
        // تحديث المفضلة عند الرجوع للشاشة
        val posts = dbHelper.getFavoritePosts()
        recyclerView.adapter = PostAdapter(requireContext(), posts, 16f) {
            imagePickerLauncher.launch("image/*")
        }
    }
}
