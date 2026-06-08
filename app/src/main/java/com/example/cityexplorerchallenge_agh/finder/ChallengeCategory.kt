package com.example.cityexplorerchallenge_agh.finder

import org.json.JSONObject
enum class ChallengeCategory(
    val label: String,
    private val tagKey: String,
    private val tagValue: String?
) {
    // historic=* (any historic place: castle, monument, ruins...)
    HISTORICAL("Historical", "historic", null),

    // tourism=museum
    MUSEUM("Museum", "tourism", "museum"),

    // leisure=park
    PARK("Park", "leisure", "park");

    fun overpassClause(latitude: Double, longitude: Double, radiusMeters: Int): String {
        val filter = if (tagValue == null) "[\"$tagKey\"]" else "[\"$tagKey\"=\"$tagValue\"]"
        return "nwr$filter(around:$radiusMeters,$latitude,$longitude);"
    }

    fun matches(tags: JSONObject): Boolean {
        if (!tags.has(tagKey)) return false
        return tagValue == null || tags.optString(tagKey) == tagValue
    }
}