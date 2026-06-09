package com.example.cityexplorerchallenge_agh.finder

import org.json.JSONObject
enum class ChallengeCategory(
    val label: String,
    private val tagKey: String,
    private val tagValue: String?,
    // Matching category name in the Geoapify Places API.
    val geoapifyCategory: String
) {
    // historic=* (any historic place: castle, monument, ruins...)
    HISTORICAL("Historical", "historic", null, "tourism.sights"),

    // tourism=museum
    MUSEUM("Museum", "tourism", "museum", "entertainment.museum"),

    // leisure=park
    PARK("Park", "leisure", "park", "leisure.park");

    fun overpassClause(latitude: Double, longitude: Double, radiusMeters: Int): String {
        val filter = if (tagValue == null) "[\"$tagKey\"]" else "[\"$tagKey\"=\"$tagValue\"]"
        return "nwr$filter(around:$radiusMeters,$latitude,$longitude);"
    }

    fun matches(tags: JSONObject): Boolean {
        if (!tags.has(tagKey)) return false
        return tagValue == null || tags.optString(tagKey) == tagValue
    }

    // True if a Geoapify place (its list of category strings) belongs to this category.
    fun matchesGeoapify(categories: List<String>): Boolean =
        categories.any { it == geoapifyCategory || it.startsWith("$geoapifyCategory.") }
}