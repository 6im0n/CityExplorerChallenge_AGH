package com.example.cityexplorerchallenge_agh

import android.content.Context
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
import com.example.cityexplorerchallenge_agh.storage.NearbyChallenge
import com.example.cityexplorerchallenge_agh.finder.OverpassClient
import com.example.cityexplorerchallenge_agh.storage.AppDatabase
import com.example.cityexplorerchallenge_agh.storage.ChallengeEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * "Find new challenge" screen.
 *
 * Flow: read GPS -> ask Overpass for places -> adapt the mix to the user's
 * history -> show the list. Tapping "Add" saves the challenge as "current".
 */
class ListNearbyChallenge : Fragment() {

    private val categories = listOf(
        ChallengeCategory.HISTORICAL,
        ChallengeCategory.MUSEUM,
        ChallengeCategory.PARK
    )

    private val suggestions = mutableListOf<NearbyChallenge>()
    private var visibleCount = CHALLENGES_PER_PAGE

    // Where the user is, so each row can show its distance.
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
            visibleCount = (visibleCount + CHALLENGES_PER_PAGE).coerceAtMost(suggestions.size)
            refreshChallengeList()
        }

        view.findViewById<Button>(R.id.mainMenuButton).setOnClickListener {
            (requireActivity() as? MenuActivity)?.showMenu()
        }
        view.findViewById<Button>(R.id.mapButton).setOnClickListener {
            (requireActivity() as? MenuActivity)?.showMap()
        }

        loadSuggestions()
    }

    /** GPS -> network -> adaptive mix, all off the main thread. */
    private fun loadSuggestions() {
        titleText.text = "Finding nearby challenges…"
        val location = DeviceLocation().lastKnownOrDefault(requireContext())
        userLatitude = location.first
        userLongitude = location.second

        viewLifecycleOwner.lifecycleScope.launch {
            val result = try {
                withContext(Dispatchers.IO) {
                    val places = OverpassClient().findPlaces(
                        location.first, location.second, SEARCH_RADIUS_METERS, categories
                    )
                    val fresh = removeAlreadyAdded(places)
                    ChallengeFinder().suggest(BUDGET, fresh, readHistoryCounts())
                }
            } catch (e: Exception) {
                titleText.text = "Nearby challenge list"
                Toast.makeText(requireContext(), "Could not load challenges. Check your connection.", Toast.LENGTH_LONG).show()
                return@launch
            }

            titleText.text = "Nearby challenge list"
            if (result.isEmpty()) {
                Toast.makeText(requireContext(), "No challenges found nearby.", Toast.LENGTH_LONG).show()
            }

            suggestions.clear()
            suggestions.addAll(result)
            visibleCount = CHALLENGES_PER_PAGE
            refreshChallengeList()
        }
    }

    /** Drop places the user already has (current or finished), matched by GPS point. */
    private fun removeAlreadyAdded(
        places: Map<ChallengeCategory, List<NearbyChallenge>>
    ): Map<ChallengeCategory, List<NearbyChallenge>> {
        val taken = AppDatabase.get(requireContext()).challengeDao().allChallenges()
            .map { it.latitude to it.longitude }
            .toSet()
        return places.mapValues { (_, list) ->
            list.filter { (it.latitude to it.longitude) !in taken }
        }
    }

    /** How many challenges per category the user already has (for the adaptation). */
    private fun readHistoryCounts(): Map<ChallengeCategory, Int> {
        val counts = mutableMapOf<ChallengeCategory, Int>()
        for (row in AppDatabase.get(requireContext()).challengeDao().categoryCounts()) {
            val category = runCatching { ChallengeCategory.valueOf(row.category) }.getOrNull()
            if (category != null) counts[category] = row.count
        }
        return counts
    }

    /** Save the challenge as "current" and take it off the suggestions. */
    private fun addChallenge(challenge: NearbyChallenge) {
        val entity = ChallengeEntity(
            title = challenge.title,
            category = challenge.category.name,
            address = challenge.address,
            latitude = challenge.latitude,
            longitude = challenge.longitude,
            state = ChallengeEntity.STATE_CURRENT
        )
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            AppDatabase.get(requireContext()).challengeDao().add(entity)
        }

        suggestions.remove(challenge)
        visibleCount = visibleCount.coerceAtMost(suggestions.size)
        refreshChallengeList()
        Toast.makeText(requireContext(), "Added: ${challenge.title}", Toast.LENGTH_SHORT).show()
    }

    private fun refreshChallengeList() {
        adapter.submitList(suggestions.take(visibleCount))
        loadMoreButton.isEnabled = visibleCount < suggestions.size
        loadMoreButton.text = if (loadMoreButton.isEnabled) "Load more" else "All loaded"
    }

    /** Simple list adapter that shows one suggested place per row. */
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

            // Category in its own colour, then the place name.
            row.findViewById<TextView>(R.id.nearbyCategory).text = challenge.category.label
            row.findViewById<TextView>(R.id.nearbyTitle).text = challenge.title

            // Distance from the user to this place.
            val meters = DistanceCalcSimple().meters(
                userLatitude, userLongitude, challenge.latitude, challenge.longitude
            )
            row.findViewById<TextView>(R.id.nearbyDistance).text = formatDistance(meters)

            row.findViewById<Button>(R.id.nearbyAddButton).setOnClickListener {
                addChallenge(challenge)
            }

            return row
        }
    }

    /** Friendly distance text, e.g. "420 m" or "1.3 km". */
    private fun formatDistance(meters: Double): String {
        return if (meters < 1000) "${Math.round(meters)} m"
        else "%.1f km".format(meters / 1000)
    }

    companion object {
        private const val BUDGET = 10
        private const val SEARCH_RADIUS_METERS = 3000
        private const val CHALLENGES_PER_PAGE = 5
    }
}