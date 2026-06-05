package com.example.cityexplorerchallenge_agh

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView as OsmMapView

class MapView : Fragment() {
    private var osmMapView: OsmMapView? = null

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
        return inflater.inflate(R.layout.fragment_map_view, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        osmMapView = view.findViewById<OsmMapView>(R.id.osmMapView).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(14.0)
            controller.setCenter(GeoPoint(50.0647, 19.9450))
        }

        view.findViewById<Button>(R.id.mainMenuButton).setOnClickListener {
            (requireActivity() as? MenuActivity)?.showMenu()
        }

        view.findViewById<Button>(R.id.currentChallengeListButton).setOnClickListener {
            (requireActivity() as? MenuActivity)?.showCurrentChallenges()
        }
    }

    override fun onResume() {
        super.onResume()
        osmMapView?.onResume()
    }

    override fun onPause() {
        osmMapView?.onPause()
        super.onPause()
    }
}
