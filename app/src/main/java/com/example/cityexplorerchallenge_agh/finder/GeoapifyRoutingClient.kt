package com.example.cityexplorerchallenge_agh.finder

import com.example.cityexplorerchallenge_agh.BuildConfig
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

// Asks the Geoapify Routing API for a walking route between two points.
class GeoapifyRoutingClient {
    private val endpoint = "https://api.geoapify.com/v1/routing"

    fun walkingRoute(
        fromLatitude: Double,
        fromLongitude: Double,
        toLatitude: Double,
        toLongitude: Double
    ): List<Pair<Double, Double>> {
        val apiKey = BuildConfig.GEOAPIFY_API_KEY
        if (apiKey.isBlank()) throw IllegalStateException("No Geoapify API key set")

        val waypoints = URLEncoder.encode(
            "$fromLatitude,$fromLongitude|$toLatitude,$toLongitude", "UTF-8"
        )
        val answer = httpGet("$endpoint?waypoints=$waypoints&mode=walk&apiKey=$apiKey")
        return parseRoute(answer)
    }

    private fun parseRoute(json: String): List<Pair<Double, Double>> {
        val features = JSONObject(json).optJSONArray("features") ?: return emptyList()
        if (features.length() == 0) return emptyList()

        val geometry = features.getJSONObject(0).optJSONObject("geometry") ?: return emptyList()
        val coordinates = geometry.optJSONArray("coordinates") ?: return emptyList()

        val points = mutableListOf<Pair<Double, Double>>()
        if (geometry.optString("type") == "LineString") {
            readLine(coordinates, points)
        } else {
            for (i in 0 until coordinates.length()) {
                readLine(coordinates.getJSONArray(i), points)
            }
        }
        return points
    }

    private fun readLine(line: JSONArray, out: MutableList<Pair<Double, Double>>) {
        for (i in 0 until line.length()) {
            val point = line.getJSONArray(i)
            out.add(point.getDouble(1) to point.getDouble(0))
        }
    }

    private fun httpGet(urlString: String): String {
        val connection = URL(urlString).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "CityExplorerChallenge/1.0")
            connection.connectTimeout = 10000
            connection.readTimeout = 20000

            val code = connection.responseCode
            if (code !in 200..299) {
                connection.errorStream?.bufferedReader()?.use { it.readText() }
                throw IOException("Geoapify routing returned HTTP $code")
            }
            return connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }
}
