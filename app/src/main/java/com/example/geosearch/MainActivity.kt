package com.example.geosearch

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.example.geosearch.api.RetrofitClient
import com.example.geosearch.databinding.ActivityMainBinding
import com.example.geosearch.model.Page
import com.example.geosearch.ui.PageAdapter
import com.example.geosearch.utils.BoundingBoxUtils
import com.example.geosearch.utils.ShareUtils
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: PageAdapter
    private var currentBbox: BoundingBoxUtils.Bbox? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupButtons()

        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let { handleIntent(it) }
    }

    private fun setupRecyclerView() {
        adapter = PageAdapter(
            onPageClick = { page -> },
            onViewClick = { page -> ShareUtils.viewSingleInOsmAnd(this, page) },
            onShareClick = { page -> ShareUtils.shareSinglePageGeoJson(this, page) },
            onDelClick = { page -> adapter.removePage(page) }
        )
        binding.recyclerView.layoutManager = GridLayoutManager(this, 2)
        binding.recyclerView.adapter = adapter
    }

    private fun setupButtons() {
        binding.btnShareAll.setOnClickListener {
            val pages = adapter.getPages()
            if (pages.isNotEmpty()) {
                ShareUtils.shareGeoJson(this, pages, currentBbox)
            } else {
                Toast.makeText(this, "No features to share", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnViewAllOsmAnd.setOnClickListener {
            val pages = adapter.getPages()
            if (pages.isNotEmpty()) {
                ShareUtils.shareGpx(this, pages)
            } else {
                Toast.makeText(this, "No features to view", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handleIntent(intent: Intent) {
        if (intent.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (sharedText != null) {
                val bbox = BoundingBoxUtils.parseBboxFromIntentText(sharedText)
                if (bbox != null) {
                    currentBbox = bbox
                    fetchFeatures(bbox)
                } else {
                    Toast.makeText(this, "Could not parse bounding box from shared text", Toast.LENGTH_LONG).show()
                }
            }
        } else {
            binding.tvEmptyState.visibility = View.VISIBLE
        }
    }

    private fun fetchFeatures(bbox: BoundingBoxUtils.Bbox) {
        binding.tvEmptyState.visibility = View.GONE
        binding.progressBar.visibility = View.VISIBLE

        val (centerLat, centerLon, radius) = BoundingBoxUtils.calculateCenterAndRadius(bbox)
        val coordString = "$centerLat|$centerLon"

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getNearbyPagesWithImages(
                    ggscoord = coordString,
                    ggsradius = radius
                )

                binding.progressBar.visibility = View.GONE

                val pagesMap = response.query?.pages
                if (pagesMap != null) {
                    val pages = pagesMap.values.toList()
                    adapter.setPages(pages)
                } else {
                    Toast.makeText(this@MainActivity, "No features found in this area", Toast.LENGTH_LONG).show()
                    adapter.setPages(emptyList())
                    binding.tvEmptyState.visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this@MainActivity, "Error fetching features: ${e.message}", Toast.LENGTH_LONG).show()
                e.printStackTrace()
            }
        }
    }
}
