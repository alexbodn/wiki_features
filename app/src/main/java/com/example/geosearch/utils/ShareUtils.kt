package com.example.geosearch.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.geosearch.model.Page
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileWriter

object ShareUtils {

    fun shareGeoJson(context: Context, pages: List<Page>, bbox: BoundingBoxUtils.Bbox?) {
        val features = JSONArray()
        for (page in pages) {
            if (!page.coordinates.isNullOrEmpty()) {
                val coord = page.coordinates[0]
                val feature = JSONObject().apply {
                    put("type", "Feature")
                    put("properties", JSONObject().apply {
                        put("name", page.title)
                        if (page.thumbnail != null) {
                            put("image", page.thumbnail.source)
                        }
                    })
                    put("geometry", JSONObject().apply {
                        put("type", "Point")
                        put("coordinates", JSONArray().apply {
                            put(coord.lon)
                            put(coord.lat)
                        })
                    })
                }
                features.put(feature)
            }
        }

        val featureCollection = JSONObject().apply {
            put("type", "FeatureCollection")
            if (bbox != null) {
                put("bbox", JSONArray().apply {
                    put(bbox.minLon)
                    put(bbox.minLat)
                    put(bbox.maxLon)
                    put(bbox.maxLat)
                })
            }
            put("features", features)
        }

        shareFile(context, featureCollection.toString(), "features.geojson", "application/geo+json")
    }

    fun shareSinglePageGeoJson(context: Context, page: Page) {
        if (page.coordinates.isNullOrEmpty()) return

        val coord = page.coordinates[0]
        val feature = JSONObject().apply {
            put("type", "Feature")
            put("properties", JSONObject().apply {
                put("name", page.title)
                if (page.thumbnail != null) {
                    put("image", page.thumbnail.source)
                }
            })
            put("geometry", JSONObject().apply {
                put("type", "Point")
                put("coordinates", JSONArray().apply {
                    put(coord.lon)
                    put(coord.lat)
                })
            })
        }

        shareFile(context, feature.toString(), "${page.title.replace(" ", "_")}.geojson", "application/geo+json")
    }

    fun shareGpx(context: Context, pages: List<Page>) {
        val gpxStr = StringBuilder()
        gpxStr.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\" ?>\n")
        gpxStr.append("<gpx version=\"1.1\" creator=\"GeoSearch\" xmlns=\"http://www.topografix.com/GPX/1/1\">\n")

        for (page in pages) {
            if (!page.coordinates.isNullOrEmpty()) {
                val coord = page.coordinates[0]
                gpxStr.append("  <wpt lat=\"${coord.lat}\" lon=\"${coord.lon}\">\n")
                gpxStr.append("    <name>${escapeXml(page.title)}</name>\n")
                gpxStr.append("  </wpt>\n")
            }
        }
        gpxStr.append("</gpx>\n")

        shareFile(context, gpxStr.toString(), "features.gpx", "application/gpx+xml")
    }

    fun viewSingleInOsmAnd(context: Context, page: Page) {
        if (page.coordinates.isNullOrEmpty()) return
        val coord = page.coordinates[0]
        val uri = Uri.parse("geo:${coord.lat},${coord.lon}?q=${coord.lat},${coord.lon}(${Uri.encode(page.title)})")
        val intent = Intent(Intent.ACTION_VIEW, uri)

        try {
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
            } else {
                Toast.makeText(context, "No app found to handle map links", Toast.LENGTH_SHORT).show()
                // In case resolveActivity fails despite manifest, just try:
                context.startActivity(intent)
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to open map app", Toast.LENGTH_SHORT).show()
        }
    }

    private fun shareFile(context: Context, content: String, filename: String, mimeType: String) {
        try {
            val cachePath = File(context.cacheDir, "shared_files")
            cachePath.mkdirs()
            val file = File(cachePath, filename)
            val writer = FileWriter(file)
            writer.write(content)
            writer.close()

            val contentUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val shareIntent = Intent(Intent.ACTION_SEND)
            shareIntent.type = mimeType
            shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri)
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

            context.startActivity(Intent.createChooser(shareIntent, "Share"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun escapeXml(input: String): String {
        return input.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}
