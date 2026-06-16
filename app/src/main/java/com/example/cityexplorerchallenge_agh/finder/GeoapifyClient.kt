package com.example.cityexplorerchallenge_agh.finder

import com.example.cityexplorerchallenge_agh.BuildConfig
import com.example.cityexplorerchallenge_agh.storage.NearbyChallenge
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

// Talks to the Geoapify Places API (https://www.geoapify.com/places-api/).
// Needs an API key in local.properties (GEOAPIFY_API_KEY). Returns real names
// and addresses.
// Call findPlaces() from a background thread (it does network I/O).
class GeoapifyClient {
    private val endpoint = "https://api.geoapify.com/v2/places"

    // Find places for every category around a point, grouped by category.
    fun findPlaces(
        latitude: Double,
        longitude: Double,
        radiusMeters: Int,
        categories: List<ChallengeCategory>
    ): Map<ChallengeCategory, List<NearbyChallenge>> {
        val apiKey = BuildConfig.GEOAPIFY_API_KEY
        if (apiKey.isBlank()) throw IllegalStateException("No Geoapify API key set")

        val answer = httpGet(buildUrl(latitude, longitude, radiusMeters, categories, apiKey))
        return parseAnswer(answer, categories)
    }

    // Build the request URL: all categories, inside a circle around the user.
    private fun buildUrl(
        latitude: Double,
        longitude: Double,
        radiusMeters: Int,
        categories: List<ChallengeCategory>,
        apiKey: String
    ): String {
        val cats = URLEncoder.encode(categories.joinToString(",") { it.geoapifyCategory }, "UTF-8")
        val filter = URLEncoder.encode("circle:$longitude,$latitude,$radiusMeters", "UTF-8")
        return "$endpoint?categories=$cats&filter=$filter&limit=60&apiKey=$apiKey"
    }

    // Read the GeoJSON answer and sort each place into its category.
    private fun parseAnswer(
        json: String,
        categories: List<ChallengeCategory>
    ): Map<ChallengeCategory, List<NearbyChallenge>> {
        val result = categories.associateWith { mutableListOf<NearbyChallenge>() }
        val features = JSONObject(json).optJSONArray("features") ?: return result

        for (i in 0 until features.length()) {
            val props = features.getJSONObject(i).optJSONObject("properties") ?: continue

            val name = props.optString("name")
            if (name.isBlank()) continue                       // skip unnamed places

            val placeCategories = readCategories(props)
            val category = categories.firstOrNull { it.matchesGeoapify(placeCategories) } ?: continue

            val latitude = props.optDouble("lat", Double.NaN)
            val longitude = props.optDouble("lon", Double.NaN)
            if (latitude.isNaN() || longitude.isNaN()) continue

            val address = props.optString("formatted").ifBlank { "Address not available" }
            result.getValue(category).add(
                NearbyChallenge(name, category, address, latitude, longitude)
            )
        }
        return result
    }

    // The "categories" property is a list of strings like ["leisure.park", ...].
    private fun readCategories(props: JSONObject): List<String> {
        val array = props.optJSONArray("categories") ?: return emptyList()
        return (0 until array.length()).map { array.optString(it) }
    }

    // Minimal HTTP GET that returns the response body as text.
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
                throw IOException("Geoapify returned HTTP $code")
            }
            return connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }
}
