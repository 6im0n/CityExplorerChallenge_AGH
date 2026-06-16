package com.example.cityexplorerchallenge_agh

import android.content.Context
import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.cityexplorerchallenge_agh.storage.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import org.osmdroid.views.MapView as OsmMapView

class completeChalengeInfo : Fragment() {
    private var previewMap: OsmMapView? = null
    private var challengeId = 0

    // Photo picker (no storage permission needed). Saves the chosen image.
    private val pickImage = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) onImagePicked(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
        return inflater.inflate(R.layout.fragment_complete_chalenge_info, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        challengeId = arguments?.getInt(ARG_ID) ?: 0
        val title = arguments?.getString(ARG_TITLE).orEmpty().ifBlank { "Completed challenge" }
        val address = arguments?.getString(ARG_ADDRESS).orEmpty()
        val latitude = arguments?.getDouble(ARG_LATITUDE) ?: 50.0647
        val longitude = arguments?.getDouble(ARG_LONGITUDE) ?: 19.9450
        val startedAt = arguments?.getLong(ARG_STARTED) ?: 0L
        val finishedAt = arguments?.getLong(ARG_FINISHED) ?: 0L
        val imagePath = arguments?.getString(ARG_IMAGE)
        val point = GeoPoint(latitude, longitude)

        view.findViewById<TextView>(R.id.challengeName).text = "Name: $title"
        view.findViewById<TextView>(R.id.challengeStarted).text = "Started: ${formatDate(startedAt)}"
        view.findViewById<TextView>(R.id.challengeFinished).text = "Finished: ${formatDate(finishedAt)}"
        showAddress(view.findViewById(R.id.challengeAddress), address, latitude, longitude)

        // Show an existing photo, or let the user add one.
        if (!imagePath.isNullOrBlank()) showImage(imagePath)
        view.findViewById<Button>(R.id.addImageButton).setOnClickListener {
            pickImage.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }

        previewMap = view.findViewById<OsmMapView>(R.id.completedChallengeMap).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(false)
            controller.setZoom(16.0)
            controller.setCenter(point)

            // Drop a pin on the place.
            val marker = Marker(this)
            marker.position = point
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            marker.title = title
            overlays.add(marker)
        }

        view.findViewById<Button>(R.id.mainMenuButton).setOnClickListener {
            (requireActivity() as? MenuActivity)?.showMenu()
        }
        view.findViewById<Button>(R.id.mapButton).setOnClickListener {
            (requireActivity() as? MenuActivity)?.showMap()
        }
    }

    // A photo was chosen: copy it into app storage, remember it, and show it.
    private fun onImagePicked(uri: Uri) {
        val path = copyImageToStorage(uri) ?: return
        showImage(path)
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            AppDatabase.get(requireContext()).challengeDao().setImage(challengeId, path)
        }
    }

    // Copy the picked image into the app's own files so it stays available later.
    private fun copyImageToStorage(uri: Uri): String? {
        return try {
            val file = File(requireContext().filesDir, "challenge_$challengeId.jpg")
            requireContext().contentResolver.openInputStream(uri)?.use { input ->
                file.outputStream().use { output -> input.copyTo(output) }
            }
            file.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    private fun showImage(path: String) {
        val image = view?.findViewById<ImageView>(R.id.challengeImage) ?: return
        image.setImageURI(null) // clear cache so a replaced photo really refreshes
        image.setImageURI(Uri.fromFile(File(path)))
        image.visibility = View.VISIBLE
        view?.findViewById<Button>(R.id.addImageButton)?.text = "Change photo"
    }

    private fun showAddress(view: TextView, stored: String, latitude: Double, longitude: Double) {
        if (stored.isNotBlank() && stored != "Address not available") {
            view.text = "Address: $stored"
            return
        }

        view.text = "Address: finding…"
        val context = requireContext().applicationContext
        viewLifecycleOwner.lifecycleScope.launch {
            val found = withContext(Dispatchers.IO) { lookupAddress(context, latitude, longitude) }
            view.text = "Address: $found"
        }
    }

    private fun lookupAddress(context: Context, latitude: Double, longitude: Double): String {
        if (!Geocoder.isPresent()) return "Not available"
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val address = geocoder.getFromLocation(latitude, longitude, 1)?.firstOrNull()
            address?.getAddressLine(0) ?: "Not available"
        } catch (e: Exception) {
            "Not available"
        }
    }

    private fun formatDate(epochMillis: Long): String {
        if (epochMillis <= 0) return "Unknown"
        val format = SimpleDateFormat("d MMM yyyy, HH:mm", Locale.getDefault())
        return format.format(Date(epochMillis))
    }

    override fun onResume() {
        super.onResume()
        previewMap?.onResume()
    }

    override fun onPause() {
        previewMap?.onPause()
        super.onPause()
    }

    companion object {
        private const val ARG_ID = "id"
        private const val ARG_TITLE = "title"
        private const val ARG_ADDRESS = "address"
        private const val ARG_LATITUDE = "latitude"
        private const val ARG_LONGITUDE = "longitude"
        private const val ARG_STARTED = "started"
        private const val ARG_FINISHED = "finished"
        private const val ARG_IMAGE = "image"

        fun newInstance(
            id: Int,
            title: String,
            address: String,
            latitude: Double,
            longitude: Double,
            startedAt: Long,
            finishedAt: Long,
            imagePath: String?
        ): completeChalengeInfo {
            return completeChalengeInfo().apply {
                arguments = Bundle().apply {
                    putInt(ARG_ID, id)
                    putString(ARG_TITLE, title)
                    putString(ARG_ADDRESS, address)
                    putDouble(ARG_LATITUDE, latitude)
                    putDouble(ARG_LONGITUDE, longitude)
                    putLong(ARG_STARTED, startedAt)
                    putLong(ARG_FINISHED, finishedAt)
                    putString(ARG_IMAGE, imagePath)
                }
            }
        }
    }
}
