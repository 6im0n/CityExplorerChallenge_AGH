package com.example.cityexplorerchallenge_agh

import android.content.Context
import android.location.Geocoder
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.cityexplorerchallenge_agh.finder.ChallengeCategory
import com.example.cityexplorerchallenge_agh.finder.ChallengeFinder
import com.example.cityexplorerchallenge_agh.finder.DeviceLocation
import com.example.cityexplorerchallenge_agh.finder.DistanceCalcSimple
import com.example.cityexplorerchallenge_agh.finder.GeoapifyClient
import com.example.cityexplorerchallenge_agh.storage.NearbyChallenge
import com.example.cityexplorerchallenge_agh.storage.AppDatabase
import com.example.cityexplorerchallenge_agh.storage.ChallengeEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalTime
import java.util.Locale

/**
 * "Find new challenge" screen.
 */
class ListNearbyChallenge : Fragment() {

    private val categories = listOf(
        ChallengeCategory.HISTORICAL,
        ChallengeCategory.MUSEUM,
        ChallengeCategory.PARK
    )

    private val suggestions = mutableListOf<NearbyChallenge>()

    private var userLatitude = 0.0
    private var userLongitude = 0.0

    private lateinit var adapter: NearbyAdapter
    private lateinit var loadMoreButton: Button
    private lateinit var titleText: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_list_nearby_challenge, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        titleText = view.findViewById(R.id.nearbyChallengeListTitle)

        adapter = NearbyAdapter(requireContext())
        view.findViewById<ListView>(R.id.nearbyChallengeList).adapter = adapter

        loadMoreButton = view.findViewById(R.id.loadMoreChallengesButton)
        loadMoreButton.setOnClickListener {
            // Each click fetches another batch of new places.
            fetchSuggestions(append = true)
        }

        view.findViewById<Button>(R.id.mainMenuButton).setOnClickListener {
            (requireActivity() as? MenuActivity)?.showMenu()
        }
        view.findViewById<Button>(R.id.mapButton).setOnClickListener {
            (requireActivity() as? MenuActivity)?.showPlainMap()
        }

        loadSuggestions()
    }

    private fun loadSuggestions() {
        titleText.text = "Finding your location…"
        DeviceLocation().requestFresh(requireContext()) { location ->
            if (!isAdded) return@requestFresh
            userLatitude = location.first
            userLongitude = location.second
            fetchSuggestions(append = false)
        }
    }

    // Ask Geoapify for up to BUDGET new places, skipping ones already shown or added.
    private fun fetchSuggestions(append: Boolean) {
        titleText.text = if (append) "Finding more challenges…" else "Finding nearby challenges…"
        loadMoreButton.isEnabled = false
        val alreadyShown = suggestions.map { it.latitude to it.longitude }.toSet()

        viewLifecycleOwner.lifecycleScope.launch {
            val result = try {
                withContext(Dispatchers.IO) {
                    val places = searchPlaces(userLatitude, userLongitude)
                    val fresh = removeAlreadyShownOrAdded(places, alreadyShown)
                    val context = ChallengeFinder.Context(
                        userLatitude = userLatitude,
                        userLongitude = userLongitude,
                        historyCount = readHistoryCounts(),
                        recentCount = recentlyFinishedCounts(),
                        hourOfDay = LocalTime.now().hour
                    )
                    ChallengeFinder().suggest(BUDGET, fresh, context)
                }
            } catch (e: Exception) {
                titleText.text = "Nearby challenge list"
                Toast.makeText(requireContext(), "Could not load challenges. Check your connection.", Toast.LENGTH_LONG).show()
                loadMoreButton.isEnabled = true
                return@launch
            }

            titleText.text = "Nearby challenge list"
            if (!append) suggestions.clear()
            suggestions.addAll(result)
            adapter.submitList(suggestions.toList())

            if (result.isEmpty()) {
                val message = if (append) "No more challenges nearby." else "No challenges found nearby."
                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
            }
            // Only offer another fetch if this one actually found something new.
            loadMoreButton.isEnabled = result.isNotEmpty()
            loadMoreButton.text = if (result.isNotEmpty()) "Load more" else "No more nearby"
        }
    }

    private fun searchPlaces(
        latitude: Double,
        longitude: Double
    ): Map<ChallengeCategory, List<NearbyChallenge>> {
        return GeoapifyClient().findPlaces(latitude, longitude, SEARCH_RADIUS_METERS, categories)
    }

    private fun removeAlreadyShownOrAdded(
        places: Map<ChallengeCategory, List<NearbyChallenge>>,
        alreadyShown: Set<Pair<Double, Double>>
    ): Map<ChallengeCategory, List<NearbyChallenge>> {
        val takenFromDb = AppDatabase.get(requireContext()).challengeDao().allChallenges()
            .map { it.latitude to it.longitude }
        val taken = alreadyShown + takenFromDb
        return places.mapValues { (_, list) ->
            list.filter { (it.latitude to it.longitude) !in taken }
        }
    }

    private fun readHistoryCounts(): Map<ChallengeCategory, Int> {
        val counts = mutableMapOf<ChallengeCategory, Int>()
        for (row in AppDatabase.get(requireContext()).challengeDao().categoryCounts()) {
            val category = runCatching { ChallengeCategory.valueOf(row.category) }.getOrNull()
            if (category != null) counts[category] = row.count
        }
        return counts
    }

    private fun recentlyFinishedCounts(): Map<ChallengeCategory, Int> {
        val since = System.currentTimeMillis() - 24L * 60 * 60 * 1000
        val counts = mutableMapOf<ChallengeCategory, Int>()
        val finished = AppDatabase.get(requireContext()).challengeDao()
            .byState(ChallengeEntity.STATE_FINISHED)
        for (row in finished) {
            if (row.finishedAt < since) continue
            val category = runCatching { ChallengeCategory.valueOf(row.category) }.getOrNull() ?: continue
            counts[category] = (counts[category] ?: 0) + 1
        }
        return counts
    }

    private fun addChallenge(challenge: NearbyChallenge) {
        val startLatitude = userLatitude
        val startLongitude = userLongitude
        val context = requireContext().applicationContext

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val entity = ChallengeEntity(
                title = challenge.title,
                category = challenge.category.name,
                address = challenge.address,
                latitude = challenge.latitude,
                longitude = challenge.longitude,
                state = ChallengeEntity.STATE_CURRENT,
                startedAt = System.currentTimeMillis(),
                startLatitude = startLatitude,
                startLongitude = startLongitude,
                city = lookupCity(context, challenge.latitude, challenge.longitude)
            )
            AppDatabase.get(context).challengeDao().add(entity)
        }

        suggestions.remove(challenge)
        adapter.submitList(suggestions.toList())
        Toast.makeText(requireContext(), "Added: ${challenge.title}", Toast.LENGTH_SHORT).show()
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

    private inner class NearbyAdapter(context: Context) : BaseAdapter() {
        private val inflater = LayoutInflater.from(context)
        private val challenges = mutableListOf<NearbyChallenge>()

        fun submitList(newChallenges: List<NearbyChallenge>) {
            challenges.clear()
            challenges.addAll(newChallenges)
            notifyDataSetChanged()
        }

        override fun getCount(): Int = challenges.size

        override fun getItem(position: Int): NearbyChallenge = challenges[position]

        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val row = convertView ?: inflater.inflate(R.layout.item_nearby_challenge, parent, false)
            val challenge = getItem(position)

            row.findViewById<TextView>(R.id.nearbyCategory).text = challenge.category.label
            row.findViewById<TextView>(R.id.nearbyTitle).text = challenge.title

            val meters = DistanceCalcSimple().meters(
                userLatitude, userLongitude, challenge.latitude, challenge.longitude
            )
            val maxScore = challenges.maxOfOrNull { it.score } ?: 0.0
            val percent = if (maxScore > 0) Math.round(challenge.score / maxScore * 100).toInt() else 0
            row.findViewById<TextView>(R.id.nearbyDistance).text =
                "${formatDistance(meters)} · match $percent%"

            row.findViewById<Button>(R.id.nearbyAddButton).setOnClickListener {
                addChallenge(challenge)
            }

            row.setOnClickListener {
                (requireActivity() as? MenuActivity)?.showNewChallengeDetails(challenge)
            }

            return row
        }
    }

    private fun formatDistance(meters: Double): String {
        return if (meters < 1000) "${Math.round(meters)} m"
        else "%.1f km".format(meters / 1000)
    }

    companion object {
        private const val BUDGET = 10
        // 1.5 km keeps results local and still finds plenty of places.
        private const val SEARCH_RADIUS_METERS = 1500
    }
}