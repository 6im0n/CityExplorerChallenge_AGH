package com.example.cityexplorerchallenge_agh

import android.content.Context
import android.location.Geocoder
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.cityexplorerchallenge_agh.finder.ChallengeCategory
import com.example.cityexplorerchallenge_agh.finder.ChallengeExplanation
import com.example.cityexplorerchallenge_agh.finder.ChallengeFinder
import com.example.cityexplorerchallenge_agh.finder.DeviceLocation
import com.example.cityexplorerchallenge_agh.finder.DistanceCalcSimple
import com.example.cityexplorerchallenge_agh.storage.AppDatabase
import com.example.cityexplorerchallenge_agh.storage.ChallengeEntity
import com.example.cityexplorerchallenge_agh.storage.NearbyChallenge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalTime
import java.util.Locale

/**
 * Explains WHY a challenge was generated: a description, the data the engine
 * used, and the rules behind it. Opened from the current and the new lists.
 */
class ChallengeDetails : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_challenge_details, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val isNew = arguments?.getBoolean(ARG_IS_NEW) ?: false
        val id = arguments?.getInt(ARG_ID) ?: 0
        val title = arguments?.getString(ARG_TITLE).orEmpty()
        val categoryName = arguments?.getString(ARG_CATEGORY).orEmpty()
        val address = arguments?.getString(ARG_ADDRESS).orEmpty()
        val latitude = arguments?.getDouble(ARG_LATITUDE) ?: 0.0
        val longitude = arguments?.getDouble(ARG_LONGITUDE) ?: 0.0
        val category = runCatching { ChallengeCategory.valueOf(categoryName) }.getOrNull()

        view.findViewById<TextView>(R.id.detailsName).text =
            if (category != null) "${category.label} • $title" else title

        setUpButtons(view, isNew, id, title, category, categoryName, address, latitude, longitude)

        if (category != null) loadExplanation(view, title, category, latitude, longitude)
    }

    private fun setUpButtons(
        view: View,
        isNew: Boolean,
        id: Int,
        title: String,
        category: ChallengeCategory?,
        categoryName: String,
        address: String,
        latitude: Double,
        longitude: Double
    ) {
        val primary = view.findViewById<Button>(R.id.primaryActionButton)
        val preview = view.findViewById<Button>(R.id.previewButton)
        val menu = requireActivity() as? MenuActivity

        if (isNew) {
            primary.text = "Add challenge"
            primary.setOnClickListener { addChallenge(title, category, address, latitude, longitude) }

            preview.visibility = View.VISIBLE
            preview.setOnClickListener {
                if (category != null) {
                    menu?.showPlacePreview(NearbyChallenge(title, category, address, latitude, longitude))
                }
            }
        } else {
            primary.text = "Go to on map"
            primary.setOnClickListener {
                menu?.showMapForChallenge(
                    ChallengeEntity(
                        id = id,
                        title = title,
                        category = categoryName,
                        address = address,
                        latitude = latitude,
                        longitude = longitude,
                        state = ChallengeEntity.STATE_CURRENT
                    )
                )
            }
        }

        view.findViewById<Button>(R.id.mainMenuButton).setOnClickListener { menu?.showMenu() }
    }

    // Build the explanation off the main thread, then show it.
    private fun loadExplanation(
        view: View,
        title: String,
        category: ChallengeCategory,
        latitude: Double,
        longitude: Double
    ) {
        val context = requireContext().applicationContext
        val userLocation = DeviceLocation().lastKnownOrDefault(requireContext())
        val distance = DistanceCalcSimple()
            .meters(userLocation.first, userLocation.second, latitude, longitude).toDouble()

        viewLifecycleOwner.lifecycleScope.launch {
            val explanation = withContext(Dispatchers.IO) {
                val engineContext = buildContext(context, userLocation.first, userLocation.second)
                ChallengeExplanation().explain(title, category, distance, engineContext)
            }
            view.findViewById<TextView>(R.id.detailsDescription).text = explanation.description
            view.findViewById<TextView>(R.id.detailsReasons).text = bullets(explanation.reasons)
            view.findViewById<TextView>(R.id.detailsInputs).text = bullets(explanation.inputData)
        }
    }

    // The same context the recommendation engine uses (history, recent, time).
    private fun buildContext(context: Context, userLat: Double, userLon: Double): ChallengeFinder.Context {
        val dao = AppDatabase.get(context).challengeDao()

        val history = mutableMapOf<ChallengeCategory, Int>()
        for (row in dao.categoryCounts()) {
            val category = runCatching { ChallengeCategory.valueOf(row.category) }.getOrNull()
            if (category != null) history[category] = row.count
        }

        val since = System.currentTimeMillis() - 24L * 60 * 60 * 1000
        val recent = mutableMapOf<ChallengeCategory, Int>()
        for (row in dao.byState(ChallengeEntity.STATE_FINISHED)) {
            if (row.finishedAt < since) continue
            val category = runCatching { ChallengeCategory.valueOf(row.category) }.getOrNull() ?: continue
            recent[category] = (recent[category] ?: 0) + 1
        }

        return ChallengeFinder.Context(userLat, userLon, history, recent, LocalTime.now().hour)
    }

    // Save a new challenge as "current" (same as the Add button in the list).
    private fun addChallenge(
        title: String,
        category: ChallengeCategory?,
        address: String,
        latitude: Double,
        longitude: Double
    ) {
        if (category == null) return
        val start = DeviceLocation().lastKnownOrDefault(requireContext())
        val context = requireContext().applicationContext

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val entity = ChallengeEntity(
                title = title,
                category = category.name,
                address = address,
                latitude = latitude,
                longitude = longitude,
                state = ChallengeEntity.STATE_CURRENT,
                startedAt = System.currentTimeMillis(),
                startLatitude = start.first,
                startLongitude = start.second,
                city = lookupCity(context, latitude, longitude)
            )
            AppDatabase.get(context).challengeDao().add(entity)
        }

        Toast.makeText(requireContext(), "Added: $title", Toast.LENGTH_SHORT).show()
        (requireActivity() as? MenuActivity)?.showCurrentChallenges()
    }

    private fun lookupCity(context: Context, latitude: Double, longitude: Double): String? {
        if (!Geocoder.isPresent()) return null
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val address = geocoder.getFromLocation(latitude, longitude, 1)?.firstOrNull()
            address?.locality ?: address?.subAdminArea ?: address?.adminArea
        } catch (e: Exception) {
            null
        }
    }

    private fun bullets(lines: List<String>): String = lines.joinToString("\n") { "• $it" }

    companion object {
        private const val ARG_IS_NEW = "isNew"
        private const val ARG_ID = "id"
        private const val ARG_TITLE = "title"
        private const val ARG_CATEGORY = "category"
        private const val ARG_ADDRESS = "address"
        private const val ARG_LATITUDE = "latitude"
        private const val ARG_LONGITUDE = "longitude"

        fun forNew(challenge: NearbyChallenge): ChallengeDetails =
            build(true, 0, challenge.title, challenge.category.name, challenge.address, challenge.latitude, challenge.longitude)

        fun forCurrent(challenge: ChallengeEntity): ChallengeDetails =
            build(false, challenge.id, challenge.title, challenge.category, challenge.address, challenge.latitude, challenge.longitude)

        private fun build(
            isNew: Boolean,
            id: Int,
            title: String,
            category: String,
            address: String,
            latitude: Double,
            longitude: Double
        ): ChallengeDetails {
            return ChallengeDetails().apply {
                arguments = Bundle().apply {
                    putBoolean(ARG_IS_NEW, isNew)
                    putInt(ARG_ID, id)
                    putString(ARG_TITLE, title)
                    putString(ARG_CATEGORY, category)
                    putString(ARG_ADDRESS, address)
                    putDouble(ARG_LATITUDE, latitude)
                    putDouble(ARG_LONGITUDE, longitude)
                }
            }
        }
    }
}
