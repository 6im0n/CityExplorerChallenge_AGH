package com.example.cityexplorerchallenge_agh.finder


class ChallengeExplanation {

    data class Explanation(
        val description: String,
        val inputData: List<String>,
        val reasons: List<String>
    )

    fun explain(
        title: String,
        category: ChallengeCategory,
        distanceMeters: Double,
        context: ChallengeFinder.Context
    ): Explanation {
        return Explanation(
            describe(title, category),
            inputs(distanceMeters, context),
            reasons(category, distanceMeters, context)
        )
    }

    private fun describe(title: String, category: ChallengeCategory): String = when (category) {
        ChallengeCategory.HISTORICAL ->
            "Discover a piece of local history at \"$title\". Walk there and explore the site."
        ChallengeCategory.MUSEUM ->
            "Visit the museum \"$title\" and learn something new about the city."
        ChallengeCategory.PARK ->
            "Take a relaxing walk through \"$title\", a green space near you."
    }

    private fun inputs(distanceMeters: Double, context: ChallengeFinder.Context): List<String> {
        val daytime = if (context.hourOfDay in 8..18) "daytime" else "evening"
        return listOf(
            "Your location: current GPS position",
            "Distance to the place: ${formatDistance(distanceMeters)}",
            "Time of day: ${context.hourOfDay}:00 ($daytime)",
            "Your challenges so far: ${countsText(context.historyCount)}",
            "Finished in the last 24h: ${countsText(context.recentCount)}"
        )
    }

    private fun reasons(
        category: ChallengeCategory,
        distanceMeters: Double,
        context: ChallengeFinder.Context
    ): List<String> {
        val reasons = mutableListOf<String>()

        val have = context.historyCount[category] ?: 0
        reasons += if (have == 0) {
            "Variety: you have no ${category.label} challenge yet, so it is suggested."
        } else {
            "Variety: you already have $have ${category.label}, so fewer of them are suggested."
        }

        val recent = context.recentCount[category] ?: 0
        if (recent > 0) {
            reasons += "No repetition: you finished $recent ${category.label} in the last 24h, " +
                "so this category is cooled down."
        }

        reasons += timeReason(category, context.hourOfDay)

        reasons += if (distanceMeters <= 2000) {
            "Distance: it is within a comfortable walking distance."
        } else {
            "Distance: it is a bit far, shown because there were few closer options."
        }

        return reasons
    }

    private fun timeReason(category: ChallengeCategory, hour: Int): String {
        val daytime = hour in 8..18
        return when (category) {
            ChallengeCategory.PARK ->
                if (daytime) "Time of day: it is daytime, so parks are favoured."
                else "Time of day: it is evening, so parks are less favoured."
            ChallengeCategory.MUSEUM ->
                if (daytime) "Time of day: museums are fine during the day."
                else "Time of day: it is evening, so museums are favoured."
            ChallengeCategory.HISTORICAL ->
                "Time of day: historical sites work at any time."
        }
    }

    private fun countsText(counts: Map<ChallengeCategory, Int>): String {
        if (counts.isEmpty()) return "none"
        return counts.entries.joinToString(", ") { "${it.key.label}: ${it.value}" }
    }

    private fun formatDistance(meters: Double): String =
        if (meters < 1000) "${Math.round(meters)} m" else "%.1f km".format(meters / 1000)
}
