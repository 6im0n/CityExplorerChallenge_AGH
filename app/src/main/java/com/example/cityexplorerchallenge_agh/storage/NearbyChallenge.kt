package com.example.cityexplorerchallenge_agh.storage

import com.example.cityexplorerchallenge_agh.finder.ChallengeCategory

//Data class that defined a chalenge
data class NearbyChallenge(
    val title: String,
    val category: ChallengeCategory,
    val address: String,
    val latitude: Double,
    val longitude: Double
)