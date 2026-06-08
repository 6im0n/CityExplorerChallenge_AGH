package com.example.cityexplorerchallenge_agh.finder

import com.example.cityexplorerchallenge_agh.storage.NearbyChallenge

// Decides WHICH challenges to suggest, based on what the user already played.
// Rule: the more a category is in the history, the FEWER of it we suggest,
// so the user gets variety. Every category still gets at least one.
class ChallengeFinder {

    // Pick a mix of challenges from the found places, adapting to history.
    fun suggest(
        budget: Int,
        found: Map<ChallengeCategory, List<NearbyChallenge>>,
        historyCount: Map<ChallengeCategory, Int>
    ): List<NearbyChallenge> {
        val available = found.mapValues { it.value.size }
        val quota = decideQuota(budget, historyCount, available)

        val suggestions = mutableListOf<NearbyChallenge>()
        for ((category, count) in quota) {
            suggestions += found.getValue(category).shuffled().take(count)
        }
        return suggestions.shuffled()
    }

    // How many challenges to take from each category.
    fun decideQuota(
        budget: Int,
        historyCount: Map<ChallengeCategory, Int>,
        available: Map<ChallengeCategory, Int>
    ): Map<ChallengeCategory, Int> {
        // Only categories that actually returned places can be suggested.
        val categories = available.filterValues { it > 0 }.keys
        if (categories.isEmpty()) return emptyMap()

        val quota = mutableMapOf<ChallengeCategory, Int>()

        // Give every category one slot (the guaranteed minimum).
        for (category in categories) quota[category] = 1
        val remaining = (budget - categories.size).coerceAtLeast(0)

        // Weight each category. Less history -> bigger weight -> more slots.
        val weight = categories.associateWith { 1.0 / (1 + (historyCount[it] ?: 0)) }
        val weightSum = weight.values.sum()

        // Share the remaining slots out by weight.
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
}