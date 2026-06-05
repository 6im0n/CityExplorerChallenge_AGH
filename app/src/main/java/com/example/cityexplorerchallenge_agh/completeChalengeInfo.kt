package com.example.cityexplorerchallenge_agh

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView as OsmMapView

class completeChalengeInfo : Fragment() {
    private var previewMap: OsmMapView? = null

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

        val title = arguments?.getString(ARG_TITLE).orEmpty().ifBlank { "Completed challenge" }
        val address = arguments?.getString(ARG_ADDRESS).orEmpty().ifBlank { "Address: N/A" }
        val latitude = arguments?.getDouble(ARG_LATITUDE) ?: 50.0647
        val longitude = arguments?.getDouble(ARG_LONGITUDE) ?: 19.9450
        val point = GeoPoint(latitude, longitude)

        view.findViewById<TextView>(R.id.challengeName).text = "Name: $title"
        view.findViewById<TextView>(R.id.challengeAddress).text = "Address: $address"

        previewMap = view.findViewById<OsmMapView>(R.id.completedChallengeMap).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(false)
            controller.setZoom(16.0)
            controller.setCenter(point)
        }

        view.findViewById<Button>(R.id.mainMenuButton).setOnClickListener {
            (requireActivity() as? MenuActivity)?.showMenu()
        }

        view.findViewById<Button>(R.id.mapButton).setOnClickListener {
            (requireActivity() as? MenuActivity)?.showMap()
        }
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
        private const val ARG_TITLE = "title"
        private const val ARG_ADDRESS = "address"
        private const val ARG_LATITUDE = "latitude"
        private const val ARG_LONGITUDE = "longitude"

        fun newInstance(
            title: String,
            address: String,
            latitude: Double,
            longitude: Double
        ): completeChalengeInfo {
            return completeChalengeInfo().apply {
                arguments = Bundle().apply {
                    putString(ARG_TITLE, title)
                    putString(ARG_ADDRESS, address)
                    putDouble(ARG_LATITUDE, latitude)
                    putDouble(ARG_LONGITUDE, longitude)
                }
            }
        }
    }
}
