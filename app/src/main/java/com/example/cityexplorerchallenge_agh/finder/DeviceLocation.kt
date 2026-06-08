package com.example.cityexplorerchallenge_agh.finder

import android.content.Context
import android.location.Location
import android.location.LocationManager

// Reads where the phone is.
class DeviceLocation {

    // Fallback position: Krakow main square. Used when the real one is unknown.
    private val defaultLatitude = 50.0647
    private val defaultLongitude = 19.9450

    // Last known position, or the Krakow fallback if it is not available.
    fun lastKnownOrDefault(context: Context): Pair<Double, Double> =
        lastKnown(context) ?: (defaultLatitude to defaultLongitude)

    // Straight-line distance in metres between two GPS points.
    fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val result = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, result)
        return result[0]
    }

    fun lastKnown(context: Context): Pair<Double, Double>? {
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return null

        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        for (provider in providers) {
            val location = try {
                manager.getLastKnownLocation(provider)
            } catch (e: SecurityException) {
                null
            }
            if (location != null) return location.latitude to location.longitude
        }
        return null
    }
}