package com.example.cityexplorerchallenge_agh

import android.content.Context
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

    // Target challenge (only set when opened from a current challenge).
    private var targetId = NO_TARGET
    private var targetTitle: String? = null
    private var targetLatitude = 0.0
    private var targetLongitude = 0.0

    private val hasTarget: Boolean get() = targetId != NO_TARGET

    /** Re-check distance every time the device reports a new position. */
    private val locationListener = LocationListener { location ->
        checkArrival(location.latitude, location.longitude)
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
            startCompletionWatch()
        } else {
            val center = DeviceLocation().lastKnownOrDefault(requireContext())
            map.controller.setCenter(GeoPoint(center.first, center.second))
        }

        view.findViewById<TextView>(R.id.currentChallenge).text =
            if (hasTarget) "Current challenge: $targetTitle" else "Current challenge: none"

        view.findViewById<Button>(R.id.mainMenuButton).setOnClickListener {
            (requireActivity() as? MenuActivity)?.showMenu()
        }
        view.findViewById<Button>(R.id.currentChallengeListButton).setOnClickListener {
            (requireActivity() as? MenuActivity)?.showCurrentChallenges()
        }
    }

    /** Pick up the challenge the user selected in the current list, if any. */
    private fun adoptSelectedChallenge() {
        val selected = AppDatabase.get(requireContext()).challengeDao().selectedChallenge() ?: return
        targetId = selected.id
        targetTitle = selected.title
        targetLatitude = selected.latitude
        targetLongitude = selected.longitude
    }

    /** Center on the target challenge and drop a pin on it. */
    private fun showTarget(map: OsmMapView) {
        val point = GeoPoint(targetLatitude, targetLongitude)
        map.controller.setCenter(point)

        val marker = Marker(map)
        marker.position = point
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        marker.title = targetTitle
        map.overlays.add(marker)
    }

    /** Listen to the device position to detect when the user reaches the target. */
    private fun startCompletionWatch() {
        val manager = requireContext().getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return
        locationManager = manager

        // Maybe the user is already there.
        DeviceLocation().lastKnown(requireContext())?.let { checkArrival(it.first, it.second) }

        try {
            manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000L, 5f, locationListener)
            manager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 2000L, 5f, locationListener)
        } catch (e: SecurityException) {
            // location permission was revoked; nothing we can do here
        }
    }

    /** Complete the challenge once the user is within the radius. */
    private fun checkArrival(latitude: Double, longitude: Double) {
        if (!hasTarget || alreadyCompleted) return

        val distance = DeviceLocation().distanceMeters(latitude, longitude, targetLatitude, targetLongitude)
        if (distance <= COMPLETION_RADIUS_METERS) {
            alreadyCompleted = true
            completeChallenge()
        }
    }

    private fun completeChallenge() {
        stopCompletionWatch()
        viewLifecycleOwner.lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                val dao = AppDatabase.get(requireContext()).challengeDao()
                dao.setState(targetId, ChallengeEntity.STATE_FINISHED)
                dao.clearSelection() // the goal is reached; no challenge stays selected
            }
            Toast.makeText(requireContext(), "Challenge completed: $targetTitle", Toast.LENGTH_LONG).show()
            (requireActivity() as? MenuActivity)?.showCompletedChallenges()
        }
    }

    private fun stopCompletionWatch() {
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
        stopCompletionWatch()
        super.onDestroyView()
    }

    companion object {
        private const val ARG_ID = "id"
        private const val ARG_TITLE = "title"
        private const val ARG_LATITUDE = "latitude"
        private const val ARG_LONGITUDE = "longitude"

        private const val NO_TARGET = -1
        private const val COMPLETION_RADIUS_METERS = 30f

        /** Open the map focused on a specific challenge (the "go to" target). */
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
