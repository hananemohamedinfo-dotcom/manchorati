package com.manchorati.www

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class ReelsFragment : Fragment() {

    private lateinit var vpReels: ViewPager2
    private lateinit var pbReelsLoading: ProgressBar
    private val reelsList = mutableListOf<CommunityPost>()
    private var adapter: ReelsAdapter? = null
    private var currentPosition = 0
    private var isTabVisible = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_reels, container, false)
        vpReels = view.findViewById(R.id.vpReels)
        pbReelsLoading = view.findViewById(R.id.pbReelsLoading)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = ReelsAdapter(requireContext(), reelsList)
        vpReels.orientation = ViewPager2.ORIENTATION_VERTICAL
        vpReels.adapter = adapter

        vpReels.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                val prevHolder = adapter?.getViewHolderForPosition(currentPosition)
                prevHolder?.pause()

                currentPosition = position
                if (isTabVisible) {
                    val newHolder = adapter?.getViewHolderForPosition(currentPosition)
                    newHolder?.play()
                }
            }
        })

        loadReels()
    }

    private fun loadReels() {
        pbReelsLoading.visibility = View.VISIBLE

        // 1. Initialise with fast local curated quotes so user NEVER sees a blank screen
        try {
            val dbHelper = DatabaseHelper(requireContext())
            val localQuotes = dbHelper.getRandomCuratedPosts(25)
            val bgs = listOf("STARS", "AURORA", "GOLD", "OCEAN", "RAIN")
            val audios = listOf("piano", "rain", "oud", "nature")
            val initialPosts = localQuotes.mapIndexed { index, post ->
                CommunityPost(
                    id = "local_${post.id}",
                    authorId = "manchorati_curated",
                    authorName = "منشوراتي",
                    authorUsername = "manchorati",
                    authorBadge = "official",
                    content = post.content,
                    tag = post.categoryName.ifEmpty { "خواطر" },
                    bgMotionId = bgs[index % bgs.size],
                    audioPreset = audios[index % audios.size],
                    animStyle = "typewriter",
                    videoRatio = "9:16",
                    likesCount = (120L..980L).random()
                )
            }
            reelsList.clear()
            reelsList.addAll(initialPosts)
            adapter?.notifyDataSetChanged()
        } catch (_: Exception) {}

        pbReelsLoading.visibility = View.GONE

        // 2. Fetch fresh online video and community posts from Firestore
        FirebaseFirestore.getInstance().collection("community_posts")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(30)
            .get()
            .addOnSuccessListener { snapshots ->
                if (!isAdded || snapshots == null || snapshots.isEmpty) return@addOnSuccessListener
                val onlineList = mutableListOf<CommunityPost>()
                for (doc in snapshots.documents) {
                    try {
                        val p = doc.toObject(CommunityPost::class.java)?.apply { id = doc.id }
                        if (p != null && p.content.isNotEmpty()) {
                            onlineList.add(p)
                        }
                    } catch (_: Exception) {}
                }
                if (onlineList.isNotEmpty()) {
                    reelsList.clear()
                    reelsList.addAll(onlineList)
                    adapter?.notifyDataSetChanged()
                    if (isTabVisible) {
                        vpReels.postDelayed({
                            adapter?.getViewHolderForPosition(currentPosition)?.play()
                        }, 250)
                    }
                }
            }
    }

    fun onTabSelected(selected: Boolean) {
        isTabVisible = selected
        val holder = adapter?.getViewHolderForPosition(currentPosition)
        if (selected) {
            holder?.play()
        } else {
            holder?.pause()
        }
    }

    override fun onResume() {
        super.onResume()
        if (isTabVisible) {
            adapter?.getViewHolderForPosition(currentPosition)?.play()
        }
    }

    override fun onPause() {
        super.onPause()
        adapter?.getViewHolderForPosition(currentPosition)?.pause()
    }
}
