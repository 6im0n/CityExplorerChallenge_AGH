package com.example.cityexplorerchallenge_agh.finder

import com.example.cityexplorerchallenge_agh.storage.NearbyChallenge

// The heart of the app: it turns the raw nearby places into a personalised set
// of challenges. Challenges are NOT fixed entries in a database; they are built
// on the fly here, by combining several rules at once:
//
//   Rule 1 - Variety:       the more of a category the user already has, the
//                           fewer of it we suggest.
//   Rule 2 - No repetition: a category finished very recently is cooled down,
//                           so the user does not get the same thing again.
//   Rule 3 - Time of day:   outdoor places (parks) are favoured in daylight,
//                           indoor ones (museums) in the evening.
//   Rule 4 - Diversity:     every available category gets at least one slot.
//   Rule 5 - Distance:      closer places are preferred; far ones are only used
//                           when there are not enough close alternatives.
//   Rule 6 - Novelty:       places already added or completed are removed before
//                           we get here (see ListNearbyChallenge), so nothing repeats.
//
// Factors used: current location, distance, category, completed challenges,
// activity history and the time of day.
class ChallengeFinder {

    data class Context(
        val userLatitude: Double,
        val userLongitude: Double,
        val historyCount: Map<ChallengeCategory, Int>, // all challenges ever added, per category
        val recentCount: Map<ChallengeCategory, Int>,  // challenges finished in the last 24 h, per category
        val hourOfDay: Int                             // 0..23, for the time-of-day rule
    )

    private val comfortableMeters = 2000.0

    fun suggest(
        budget: Int,
        found: Map<ChallengeCategory, List<NearbyChallenge>>,
        context: Context
    ): List<NearbyChallenge> {
        val available = found.mapValues { it.value.size }
        val quota = decideQuota(budget, context, available)

        val suggestions = mutableListOf<NearbyChallenge>()
        for ((category, count) in quota) {
            suggestions += pickClosest(found.getValue(category), count, context)
        }
        return suggestions.sortedBy { distance(context, it) }
    }

    private fun pickClosest(
        places: List<NearbyChallenge>,
        count: Int,
        context: Context
    ): List<NearbyChallenge> {
        val sorted = places.sortedBy { distance(context, it) }
        val near = sorted.filter { distance(context, it) <= comfortableMeters }
        // Use the near ones if there are enough; otherwise fall back to the closest far ones.
        return (if (near.size >= count) near else sorted).take(count)
    }

    fun decideQuota(
        budget: Int,
        context: Context,
        available: Map<ChallengeCategory, Int>
    ): Map<ChallengeCategory, Int> {
        val categories = available.filterValues { it > 0 }.keys
        if (categories.isEmpty()) return emptyMap()

        val quota = mutableMapOf<ChallengeCategory, Int>()

        for (category in categories) quota[category] = 1
        val remaining = (budget - categories.size).coerceAtLeast(0)

        val weight = categories.associateWith { categoryWeight(it, context) }
        val weightSum = weight.values.sum().coerceAtLeast(0.0001)
        for (category in categories) {
            val share = remaining * (weight.getValue(category) / weightSum)
            quota[category] = quota.getValue(category) + Math.round(share).toInt()
        }

        // Never ask for more places than we actually found.
        for (category in categories) {
            quota[category] = quota.getValue(category).coerceAtMost(available.getValue(category))
        }
        return quota
    }

    private fun categoryWeight(category: ChallengeCategory, context: Context): Double {
        var weight = 1.0 / (1 + (context.historyCount[category] ?: 0))

        weight *= 1.0 / (1 + (context.recentCount[category] ?: 0))

        weight *= timeFactor(category, context.hourOfDay)

        return weight
    }

    private fun timeFactor(category: ChallengeCategory, hour: Int): Double {
        val daytime = hour in 8..18
        return when (category) {
            ChallengeCategory.PARK -> if (daytime) 1.5 else 0.6
            ChallengeCategory.MUSEUM -> if (daytime) 1.0 else 1.4
            ChallengeCategory.HISTORICAL -> 1.2
        }
    }

    private fun distance(context: Context, place: NearbyChallenge): Double =
        DistanceCalcSimple().meters(
            context.userLatitude, context.userLongitude, place.latitude, place.longitude
        ).toDouble()
}
