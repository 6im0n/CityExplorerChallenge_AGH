package com.example.cityexplorerchallenge_agh

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.cityexplorerchallenge_agh.finder.DeviceLocation
import com.example.cityexplorerchallenge_agh.storage.AppDatabase
import com.example.cityexplorerchallenge_agh.storage.ChallengeEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.MapView as OsmMapView

/**
 * Shows the map.
 *
 * Two modes:
 *  - no target  -> just a general map centred on the user.
 *  - a target   -> opened from a current challenge. We mark its position and
 *                  watch the user's location; within 30 m the challenge is
 *                  marked "finished" and moves to the completed list.
 */
class MapView : Fragment() {

    private var osmMapView: OsmMapView? = null
    private var locationManager: LocationManager? = null
    private var alreadyCompleted = false

    // Blue dot for the user's live position.
    private var userMarker: Marker? = null

    // Target challenge (only set when opened from a current challenge).
    private var targetId = NO_TARGET
    private var targetTitle: String? = null
    private var targetLatitude = 0.0
    private var targetLongitude = 0.0

    private val hasTarget: Boolean get() = targetId != NO_TARGET

    private val locationListener = LocationListener { location ->
        onUserLocation(location.latitude, location.longitude)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            targetId = it.getInt(ARG_ID, NO_TARGET)
            targetTitle = it.getString(ARG_TITLE)
            targetLatitude = it.getDouble(ARG_LATITUDE)
            targetLongitude = it.getDouble(ARG_LONGITUDE)
        }

        val context = requireContext().applicationContext
        Configuration.getInstance().load(
            context,
            context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE)
        )
        Configuration.getInstance().userAgentValue = context.packageName
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_map_view, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val map = view.findViewById<OsmMapView>(R.id.osmMapView).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(15.0)
        }
        osmMapView = map

        // Opened without a target? Use the challenge the user selected in the list.
        if (!hasTarget) {
            adoptSelectedChallenge()
        }

        if (hasTarget) {
            showTarget(map)
        } else {
            val center = DeviceLocation().lastKnownOrDefault(requireContext())
            map.controller.setCenter(GeoPoint(center.first, center.second))
        }

        // Show (and follow) the user's blue dot in both modes.
        startLocationWatch()

        view.findViewById<TextView>(R.id.currentChallenge).text =
            if (hasTarget) "Current challenge: $targetTitle" else "Current challenge: none"

        view.findViewById<Button>(R.id.mainMenuButton).setOnClickListener {
            (requireActivity() as? MenuActivity)?.showMenu()
        }
        view.findViewById<Button>(R.id.currentChallengeListButton).setOnClickListener {
            (requireActivity() as? MenuActivity)?.showCurrentChallenges()
        }
    }

    private fun adoptSelectedChallenge() {
        val selected = AppDatabase.get(requireContext()).challengeDao().selectedChallenge() ?: return
        targetId = selected.id
        targetTitle = selected.title
        targetLatitude = selected.latitude
        targetLongitude = selected.longitude
    }

    private fun showTarget(map: OsmMapView) {
        val point = GeoPoint(targetLatitude, targetLongitude)
        map.controller.setCenter(point)

        val marker = Marker(map)
        marker.position = point
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        marker.title = targetTitle
        map.overlays.add(marker)
    }

    private fun startLocationWatch() {
        val manager = requireContext().getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        if (manager == null) {
            notifyNoLocation()
            return
        }
        locationManager = manager

        // Show the dot right away from the last known position, if we have one.
        DeviceLocation().lastKnown(requireContext())?.let { onUserLocation(it.first, it.second) }

        // Register on both providers. This is safe even if a provider is off now:
        // updates simply start arriving once it is turned back on (keep trying).
        try {
            manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000L, 5f, locationListener)
            manager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 2000L, 5f, locationListener)
        } catch (e: SecurityException) {
            notifyNoLocation() // location permission was revoked
            return
        }

        // Nothing is on right now: warn the user, but keep the listeners ready.
        val gpsOn = manager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val networkOn = manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        if (!gpsOn && !networkOn) notifyNoLocation()
    }

    private fun notifyNoLocation() {
        Toast.makeText(
            requireContext(),
            "Location unavailable — turn on GPS to track arrival.",
            Toast.LENGTH_LONG
        ).show()
    }

    // A new position: move the blue dot, fit both points, maybe complete.
    private fun onUserLocation(latitude: Double, longitude: Double) {
        showUserLocation(latitude, longitude)
        if (hasTarget) {
            frameUserAndTarget(latitude, longitude)
            checkArrival(latitude, longitude)
        }
    }

    // Draw (or move) the blue dot at the user's position.
    private fun showUserLocation(latitude: Double, longitude: Double) {
        val map = osmMapView ?: return
        val marker = userMarker ?: Marker(map).also {
            it.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            it.icon = blueDot()
            it.title = "You"
            it.setInfoWindow(null)
            map.overlays.add(it)
            userMarker = it
        }
        marker.position = GeoPoint(latitude, longitude)
        map.invalidate()
    }

    // A small blue circle with a white border, built in code (no drawable file).
    private fun blueDot(): Drawable {
        val density = resources.displayMetrics.density
        return GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.parseColor("#1E88E5"))
            setStroke((2 * density).toInt(), Color.WHITE)
            setSize((16 * density).toInt(), (16 * density).toInt())
        }
    }

    // Keep the map fitted so the user and the challenge both stay visible.
    // Runs on every new position, so the crop resizes as the user moves.
    private fun frameUserAndTarget(userLatitude: Double, userLongitude: Double) {
        val map = osmMapView ?: return

        if (map.width == 0 || map.height == 0) {
            map.post { frameUserAndTarget(userLatitude, userLongitude) }
            return
        }

        val sameSpot = Math.abs(userLatitude - targetLatitude) < 1e-5 &&
            Math.abs(userLongitude - targetLongitude) < 1e-5
        if (sameSpot) {
            map.controller.setCenter(GeoPoint(targetLatitude, targetLongitude))
            return
        }

        val box = BoundingBox.fromGeoPoints(
            listOf(
                GeoPoint(userLatitude, userLongitude),
                GeoPoint(targetLatitude, targetLongitude)
            )
        )
        val padding = (48 * resources.displayMetrics.density).toInt()
        map.zoomToBoundingBox(box, false, padding)
    }

    private fun checkArrival(latitude: Double, longitude: Double) {
        if (!hasTarget || alreadyCompleted) return

        val distance = DeviceLocation().distanceMeters(latitude, longitude, targetLatitude, targetLongitude)
        if (distance <= COMPLETION_RADIUS_METERS) {
            alreadyCompleted = true
            completeChallenge()
        }
    }

    private fun completeChallenge() {
        stopLocationWatch()
        viewLifecycleOwner.lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                val dao = AppDatabase.get(requireContext()).challengeDao()
                dao.markFinished(targetId, System.currentTimeMillis())
                dao.clearSelection() // the goal is reached; no challenge stays selected
            }
            Toast.makeText(requireContext(), "Challenge completed: $targetTitle", Toast.LENGTH_LONG).show()
            (requireActivity() as? MenuActivity)?.showCompletedChallenges()
        }
    }

    private fun stopLocationWatch() {
        locationManager?.removeUpdates(locationListener)
    }

    override fun onResume() {
        super.onResume()
        osmMapView?.onResume()
    }

    override fun onPause() {
        osmMapView?.onPause()
        super.onPause()
    }

    override fun onDestroyView() {
        stopLocationWatch()
        super.onDestroyView()
    }

    companion object {
        private const val ARG_ID = "id"
        private const val ARG_TITLE = "title"
        private const val ARG_LATITUDE = "latitude"
        private const val ARG_LONGITUDE = "longitude"

        private const val NO_TARGET = -1
        private const val COMPLETION_RADIUS_METERS = 30f

        fun forChallenge(
            id: Int,
            title: String,
            latitude: Double,
            longitude: Double
        ): MapView {
            return MapView().apply {
                arguments = Bundle().apply {
                    putInt(ARG_ID, id)
                    putString(ARG_TITLE, title)
                    putDouble(ARG_LATITUDE, latitude)
                    putDouble(ARG_LONGITUDE, longitude)
                }
            }
        }
    }
}
