package com.example.geosearch.utils

import kotlin.math.*

object BoundingBoxUtils {

    data class Bbox(val minLat: Double, val minLon: Double, val maxLat: Double, val maxLon: Double)

    fun parseBboxFromIntentText(text: String): Bbox? {
        // Regex to find things like ?bbox=minLon,minLat,maxLon,maxLat
        val bboxRegex = Regex("(?i)bbox=(-?\\d+\\.\\d+),(-?\\d+\\.\\d+),(-?\\d+\\.\\d+),(-?\\d+\\.\\d+)")
        val match = bboxRegex.find(text)
        if (match != null) {
            val (minLon, minLat, maxLon, maxLat) = match.destructured
            return Bbox(minLat.toDouble(), minLon.toDouble(), maxLat.toDouble(), maxLon.toDouble())
        }

        // Sometimes it's just minLat,minLon,maxLat,maxLon
        val simpleRegex = Regex("(-?\\d+\\.\\d+),(-?\\d+\\.\\d+),(-?\\d+\\.\\d+),(-?\\d+\\.\\d+)")
        val simpleMatch = simpleRegex.find(text)
        if (simpleMatch != null) {
            val (minLon, minLat, maxLon, maxLat) = simpleMatch.destructured
            return Bbox(minLat.toDouble(), minLon.toDouble(), maxLat.toDouble(), maxLon.toDouble())
        }

        // Fallback: parse geo URI or standard http URLs for lat, lon, and z (zoom)
        // e.g. geo:37.78,-122.40?z=14 OR https://osmand.net/go?lat=37.78&lon=-122.40&z=14
        val geoZoomRegex = Regex("(?i)(?:geo:|[?&]lat=)(-?\\d+\\.\\d+)[,&](?:lon=)?(-?\\d+\\.\\d+).*?[?&]z=(\\d+)")
        val geoZoomMatch = geoZoomRegex.find(text)
        if (geoZoomMatch != null) {
            val (latStr, lonStr, zoomStr) = geoZoomMatch.destructured
            val lat = latStr.toDouble()
            val lon = lonStr.toDouble()
            val zoom = zoomStr.toDouble()

            val radiusMeters = calculateRadiusFromZoom(lat, zoom)
            return createBboxFromCenterAndRadius(lat, lon, radiusMeters)
        }

        // geo:lat,lon without zoom, assume zoom 15
        val geoRegex = Regex("(?i)geo:(-?\\d+\\.\\d+),(-?\\d+\\.\\d+)")
        val geoMatch = geoRegex.find(text)
        if (geoMatch != null) {
            val (latStr, lonStr) = geoMatch.destructured
            val lat = latStr.toDouble()
            val lon = lonStr.toDouble()
            val radiusMeters = calculateRadiusFromZoom(lat, 15.0)
            return createBboxFromCenterAndRadius(lat, lon, radiusMeters)
        }

        return null
    }

    private fun calculateRadiusFromZoom(lat: Double, zoom: Double): Double {
        val earthCircumference = 40075016.686
        val metersPerPixel = earthCircumference * cos(lat * Math.PI / 180) / (256 * 2.0.pow(zoom))
        // Assume a visible area of roughly 1080 pixels (540 pixels radius)
        return metersPerPixel * 540.0
    }

    private fun createBboxFromCenterAndRadius(centerLat: Double, centerLon: Double, radiusMeters: Double): Bbox {
        val rEarth = 6371e3 // meters

        val latOffset = (radiusMeters / rEarth) * (180 / Math.PI)
        val lonOffset = (radiusMeters / (rEarth * cos(centerLat * Math.PI / 180))) * (180 / Math.PI)

        return Bbox(
            minLat = centerLat - latOffset,
            minLon = centerLon - lonOffset,
            maxLat = centerLat + latOffset,
            maxLon = centerLon + lonOffset
        )
    }

    // Returns center Lat, center Lon, and radius in meters
    fun calculateCenterAndRadius(bbox: Bbox): Triple<Double, Double, Int> {
        val centerLat = (bbox.minLat + bbox.maxLat) / 2.0
        val centerLon = (bbox.minLon + bbox.maxLon) / 2.0

        // Radius should be half the width. We calculate distance between minLon and maxLon at centerLat
        val radiusMeters = calculateDistance(centerLat, bbox.minLon, centerLat, bbox.maxLon) / 2.0

        // MediaWiki limits radius to 10000 meters
        val clampedRadius = min(10000, max(10, radiusMeters.toInt()))

        return Triple(centerLat, centerLon, clampedRadius)
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371e3 // metres
        val phi1 = lat1 * Math.PI / 180
        val phi2 = lat2 * Math.PI / 180
        val deltaPhi = (lat2 - lat1) * Math.PI / 180
        val deltaLambda = (lon2 - lon1) * Math.PI / 180

        val a = sin(deltaPhi / 2) * sin(deltaPhi / 2) +
                cos(phi1) * cos(phi2) *
                sin(deltaLambda / 2) * sin(deltaLambda / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return r * c
    }
}
