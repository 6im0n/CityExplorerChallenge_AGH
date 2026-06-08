package com.example.cityexplorerchallenge_agh.finder

import com.example.cityexplorerchallenge_agh.storage.NearbyChallenge
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

// Talks to the OpenStreetMap "Overpass" API.
// Overpass lets us ask: "give me every museum / park / historic place
// within X metres of this GPS point". No API key is needed.
// Call findPlaces() from a background thread (it does network I/O).
class OverpassClient {
    private val endpoint = "https://overpass-api.de/api/interpreter"

    // Find places for every category around a point, grouped by category.
    fun findPlaces(
        latitude: Double,
        longitude: Double,
        radiusMeters: Int,
        categories: List<ChallengeCategory>
    ): Map<ChallengeCategory, List<NearbyChallenge>> {
        val query = buildQuery(latitude, longitude, radiusMeters, categories)
        val answer = httpPost(endpoint, "data=" + URLEncoder.encode(query, "UTF-8"))
        return parseAnswer(answer, categories)
    }

    // Build one Overpass query that asks for all categories at once.
    private fun buildQuery(
        latitude: Double,
        longitude: Double,
        radiusMeters: Int,
        categories: List<ChallengeCategory>
    ): String {
        val clauses = categories.joinToString("\n  ") {
            it.overpassClause(latitude, longitude, radiusMeters)
        }
        return """
            [out:json][timeout:25];
            (
              $clauses
            );
            out center 60;
        """.trimIndent()
    }

    // Read the JSON answer and sort each place into its category.
    private fun parseAnswer(
        json: String,
        categories: List<ChallengeCategory>
    ): Map<ChallengeCategory, List<NearbyChallenge>> {
        val result = categories.associateWith { mutableListOf<NearbyChallenge>() }
        val elements = JSONObject(json).optJSONArray("elements") ?: return result

        for (i in 0 until elements.length()) {
            val element = elements.getJSONObject(i)
            val tags = element.optJSONObject("tags") ?: continue

            val name = tags.optString("name")
            if (name.isBlank()) continue                       // skip unnamed places

            val category = categories.firstOrNull { it.matches(tags) } ?: continue
            val point = readPoint(element) ?: continue

            result.getValue(category).add(
                NearbyChallenge(name, category, readAddress(tags), point.first, point.second)
            )
        }
        return result
    }

    // A node has lat/lon directly; a way/relation reports a "center".
    private fun readPoint(element: JSONObject): Pair<Double, Double>? {
        if (element.has("lat") && element.has("lon")) {
            return element.getDouble("lat") to element.getDouble("lon")
        }
        val center = element.optJSONObject("center") ?: return null
        return center.getDouble("lat") to center.getDouble("lon")
    }

    // Build a readable address from the OpenStreetMap address tags.
    private fun readAddress(tags: JSONObject): String {
        val parts = listOf(
            tags.optString("addr:street"),
            tags.optString("addr:housenumber"),
            tags.optString("addr:city")
        ).filter { it.isNotBlank() }
        return if (parts.isEmpty()) "Address not available" else parts.joinToString(" ")
    }

    // Minimal HTTP POST that returns the response body as text.
    private fun httpPost(urlString: String, body: String): String {
        val connection = URL(urlString).openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
        connection.connectTimeout = 15000
        connection.readTimeout = 30000
        connection.doOutput = true
        connection.outputStream.use { it.write(body.toByteArray()) }
        connection.inputStream.bufferedReader().use { return it.readText() }
    }
}