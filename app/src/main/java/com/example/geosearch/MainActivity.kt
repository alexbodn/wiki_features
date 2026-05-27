package com.example.geosearch

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.example.geosearch.api.OverpassRetrofitClient
import com.example.geosearch.databinding.ActivityMainBinding
import com.example.geosearch.model.Coordinate
import com.example.geosearch.model.Page
import com.example.geosearch.model.Thumbnail
import com.example.geosearch.ui.PageAdapter
import com.example.geosearch.utils.BoundingBoxUtils
import com.example.geosearch.utils.ShareUtils
import kotlinx.coroutines.launch
import java.security.MessageDigest

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
            onPageClick = { _ -> },
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

        // Overpass QL bounding box is (minLat, minLon, maxLat, maxLon)
        // We will fetch nodes, ways, and relations that have either 'image', 'wikimedia_commons', or 'wikidata' tags.
        val bboxString = "${bbox.minLat},${bbox.minLon},${bbox.maxLat},${bbox.maxLon}"
        val overpassQuery = """
            [out:json][timeout:25];
            (
              node["image"]($bboxString);
              way["image"]($bboxString);
              relation["image"]($bboxString);
              node["wikimedia_commons"]($bboxString);
              way["wikimedia_commons"]($bboxString);
              relation["wikimedia_commons"]($bboxString);
            );
            out center;
        """.trimIndent()

        lifecycleScope.launch {
            try {
                val response = OverpassRetrofitClient.instance.getFeatures(overpassQuery)

                binding.progressBar.visibility = View.GONE

                val elements = response.elements
                if (!elements.isNullOrEmpty()) {
                    val pages = elements.mapNotNull { element ->
                        val lat = element.lat ?: element.center?.lat ?: return@mapNotNull null
                        val lon = element.lon ?: element.center?.lon ?: return@mapNotNull null
                        val title = element.tags?.get("name") ?: element.tags?.get("name:en") ?: "Unknown Feature (${element.id})"

                        var imageUrl: String? = null
                        if (element.tags?.containsKey("image") == true) {
                            val imgTag = element.tags["image"]!!
                            imageUrl = if (imgTag.startsWith("http")) {
                                imgTag
                            } else if (imgTag.startsWith("File:")) {
                                getWikimediaCommonsUrl(imgTag)
                            } else {
                                null
                            }
                        } else if (element.tags?.containsKey("wikimedia_commons") == true) {
                            val commonsTag = element.tags["wikimedia_commons"]!!
                            val filename = if (commonsTag.startsWith("File:")) commonsTag else "File:$commonsTag"
                            imageUrl = getWikimediaCommonsUrl(filename)
                        }

                        val thumbnail = imageUrl?.let { Thumbnail(it, 300, 300) }

                        Page(
                            pageid = element.id,
                            title = title,
                            coordinates = listOf(Coordinate(lat, lon)),
                            thumbnail = thumbnail
                        )
                    }
                    adapter.setPages(pages)
                } else {
                    Toast.makeText(this@MainActivity, "No features with images found in this area", Toast.LENGTH_LONG).show()
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

    private fun getWikimediaCommonsUrl(filename: String): String {
        // Wikimedia Commons format: https://upload.wikimedia.org/wikipedia/commons/a/ab/Filename.jpg
        // The first hash char is md5(filename)[0], second is md5(filename)[0..1]
        var name = filename.replace("File:", "").replace(" ", "_")
        try {
            val md = MessageDigest.getInstance("MD5")
            val hash = md.digest(name.toByteArray(Charsets.UTF_8))
            val hexString = hash.joinToString("") { "%02x".format(it) }
            val a = hexString.substring(0, 1)
            val ab = hexString.substring(0, 2)

            // Try to append standard width for thumbnail to reduce download size
            return "https://upload.wikimedia.org/wikipedia/commons/thumb/$a/$ab/${Uri.encode(name)}/300px-${Uri.encode(name)}"
        } catch (e: Exception) {
            // Fallback to special file path if MD5 fails (redirects automatically)
            return "https://commons.wikimedia.org/wiki/Special:FilePath/${Uri.encode(name)}?width=300"
        }
    }
}
