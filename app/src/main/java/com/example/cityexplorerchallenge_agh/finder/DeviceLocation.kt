package com.example.cityexplorerchallenge_agh.finder

import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.CancellationSignal
import android.os.Handler
import android.os.Looper

// Reads where the phone is.
class DeviceLocation {

    // Fallback position: Krakow main square. Used when the real one is unknown.
    private val defaultLatitude = 50.0647
    private val defaultLongitude = 19.9450

    // Give up waiting for a fresh fix after this long and use the last known one.
    private val fixTimeoutMillis = 6000L

    // Last known position, or the Krakow fallback if it is not available.
    fun lastKnownOrDefault(context: Context): Pair<Double, Double> =
        lastKnown(context) ?: (defaultLatitude to defaultLongitude)

    // Ask the phone for a fresh position, then hand it back to onResult (on the
    // main thread). Falls back to the last known position if nothing comes.
    fun requestFresh(context: Context, onResult: (Pair<Double, Double>) -> Unit) {
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        val provider = when {
            manager == null -> null
            manager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            else -> null
        }
        if (manager == null || provider == null) {
            onResult(lastKnownOrDefault(context))
            return
        }

        // Make sure onResult runs exactly once: either the real fix, or a timeout.
        var delivered = false
        fun deliverOnce(result: Pair<Double, Double>) {
            if (!delivered) {
                delivered = true
                onResult(result)
            }
        }

        val cancel = CancellationSignal()
        val handler = Handler(Looper.getMainLooper())
        val timeout = Runnable {
            cancel.cancel()
            deliverOnce(lastKnownOrDefault(context))
        }
        handler.postDelayed(timeout, fixTimeoutMillis)

        try {
            manager.getCurrentLocation(provider, cancel, context.mainExecutor) { location ->
                handler.removeCallbacks(timeout)
                if (location != null) deliverOnce(location.latitude to location.longitude)
                else deliverOnce(lastKnownOrDefault(context))
            }
        } catch (e: SecurityException) {
            handler.removeCallbacks(timeout)
            deliverOnce(lastKnownOrDefault(context))
        }
    }

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