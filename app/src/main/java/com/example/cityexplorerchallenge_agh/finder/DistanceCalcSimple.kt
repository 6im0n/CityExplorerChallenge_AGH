package com.example.cityexplorerchallenge_agh.finder

import kotlin.math.cos
import kotlin.math.sqrt

// Straight-line distance between two GPS points.
// Simple maths only (flat-earth approximation): no Android API, no network.
class DistanceCalcSimple {

    private val earthRadiusMeters = 6_371_000.0

    fun meters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        // Gap between the points, turned into radians.
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val meanLat = Math.toRadians((lat1 + lat2) / 2)

        // Treat the small area as a flat plane: x east-west, y north-south.
        val x = dLon * cos(meanLat)
        val y = dLat
        return earthRadiusMeters * sqrt(x * x + y * y)
    }
}
