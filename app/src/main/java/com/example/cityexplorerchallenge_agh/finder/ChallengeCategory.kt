package com.example.cityexplorerchallenge_agh.finder

enum class ChallengeCategory(
    val label: String,
    // Matching category name in the Geoapify Places API.
    val geoapifyCategory: String
) {
    HISTORICAL("Historical", "tourism.sights"),
    MUSEUM("Museum", "entertainment.museum"),
    PARK("Park", "leisure.park");

    // True if Geoapify place (its list of category strings) belongs to this category.
    fun matchesGeoapify(categories: List<String>): Boolean =
        categories.any { it == geoapifyCategory || it.startsWith("$geoapifyCategory.") }
}