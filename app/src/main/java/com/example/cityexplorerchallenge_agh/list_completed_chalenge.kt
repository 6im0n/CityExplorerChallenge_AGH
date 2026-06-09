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
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.cityexplorerchallenge_agh.finder.DistanceCalcSimple
import com.example.cityexplorerchallenge_agh.storage.AppDatabase
import com.example.cityexplorerchallenge_agh.storage.ChallengeEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The challenges the user has finished (state = "finished").
 * "More info" opens a small map preview of where it was.
 */
class list_completed_chalenge : Fragment() {

    private lateinit var adapter: CompletedChallengeAdapter
    private lateinit var titleText: TextView

    // Stats card at the top of the page.
    private lateinit var statCompleted: TextView
    private lateinit var statCategories: TextView
    private lateinit var statDistance: TextView
    private lateinit var statCity: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_list_completed_chalenge, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        titleText = view.findViewById(R.id.completedChallengeListTitle)
        statCompleted = view.findViewById(R.id.histStatCompleted)
        statCategories = view.findViewById(R.id.histStatCategories)
        statDistance = view.findViewById(R.id.histStatDistance)
        statCity = view.findViewById(R.id.histStatCity)

        adapter = CompletedChallengeAdapter(requireContext())
        view.findViewById<ListView>(R.id.completedChallengeList).adapter = adapter

        view.findViewById<Button>(R.id.mainMenuButton).setOnClickListener {
            (requireActivity() as? MenuActivity)?.showMenu()
        }
        view.findViewById<Button>(R.id.mapButton).setOnClickListener {
            (requireActivity() as? MenuActivity)?.showMap()
        }

        loadCompletedChallenges()
    }

    private fun loadCompletedChallenges() {
        viewLifecycleOwner.lifecycleScope.launch {
            val items = withContext(Dispatchers.IO) {
                AppDatabase.get(requireContext()).challengeDao()
                    .byState(ChallengeEntity.STATE_FINISHED)
            }
            adapter.submit(items)
            titleText.text =
                if (items.isEmpty()) "No completed challenges yet" else "Completed challenges"
            showStats(items)
        }
    }

    // Fill the stats card from the finished challenges.
    private fun showStats(items: List<ChallengeEntity>) {
        // How many finished challenges in each category.
        val categoryCounts = items.groupingBy { it.categoryLabel() }.eachCount()

        // Total distance = sum of each challenge's start -> place distance.
        val totalMeters = items.sumOf { item ->
            if (item.startLatitude == 0.0 && item.startLongitude == 0.0) {
                0.0
            } else {
                DistanceCalcSimple().meters(
                    item.startLatitude, item.startLongitude, item.latitude, item.longitude
                ).toDouble()
            }
        }

        // Most visited city = the city that appears the most often.
        val mostCity = items.mapNotNull { it.city }
            .groupingBy { it }.eachCount()
            .maxByOrNull { it.value }?.key

        statCompleted.text = "Completed challenges: ${items.size}"
        statCategories.text = "Categories visited: " + (
            if (categoryCounts.isEmpty()) "—"
            else categoryCounts.entries.joinToString(", ") { "${it.key}: ${it.value}" }
            )
        statDistance.text = "Total distance: ${formatDistance(totalMeters)}"
        statCity.text = "Most visited city: ${mostCity ?: "—"}"
    }

    private fun formatDistance(meters: Double): String {
        return if (meters < 1000) "${Math.round(meters)} m"
        else "%.1f km".format(meters / 1000)
    }

    private inner class CompletedChallengeAdapter(context: Context) : BaseAdapter() {
        private val inflater = LayoutInflater.from(context)
        private val challenges = mutableListOf<ChallengeEntity>()

        fun submit(newChallenges: List<ChallengeEntity>) {
            challenges.clear()
            challenges.addAll(newChallenges)
            notifyDataSetChanged()
        }

        override fun getCount(): Int = challenges.size

        override fun getItem(position: Int): ChallengeEntity = challenges[position]

        override fun getItemId(position: Int): Long = challenges[position].id.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val row = convertView ?: inflater.inflate(R.layout.item_completed, parent, false)
            val challenge = getItem(position)

            row.findViewById<TextView>(R.id.challengeTitle).text = challenge.title
            row.findViewById<Button>(R.id.moreInfoButton).setOnClickListener {
                (requireActivity() as? MenuActivity)?.showCompletedChallengeInfo(challenge)
            }

            return row
        }
    }
}