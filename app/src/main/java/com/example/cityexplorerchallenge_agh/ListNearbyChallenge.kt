package com.example.cityexplorerchallenge_agh

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.RadioButton
import android.widget.TextView
import androidx.fragment.app.Fragment

class ListNearbyChallenge : Fragment() {
    private val allChallenges = mutableListOf(
        Challenge(1, "Nearby Landmark Route"),
        Challenge(2, "City Center Discovery"),
        Challenge(3, "Short Park Walk"),
        Challenge(4, "Coffee Spot Visit"),
        Challenge(5, "Local Museum Stop"),
        Challenge(6, "Street Art Nearby"),
        Challenge(7, "Bridge Photo Route"),
        Challenge(8, "Old Building Search"),
        Challenge(9, "Hidden Square Walk"),
        Challenge(10, "Waterfront Challenge")
    )

    private lateinit var adapter: ChallengeAdapter
    private lateinit var loadMoreButton: Button
    private var visibleCount = CHALLENGES_PER_PAGE

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_list_nearby_challenge, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = ChallengeAdapter(requireContext())
        view.findViewById<ListView>(R.id.nearbyChallengeList).adapter = adapter

        loadMoreButton = view.findViewById(R.id.loadMoreChallengesButton)
        loadMoreButton.setOnClickListener {
            visibleCount = (visibleCount + CHALLENGES_PER_PAGE).coerceAtMost(allChallenges.size)
            refreshChallengeList()
        }

        view.findViewById<Button>(R.id.mainMenuButton).setOnClickListener {
            (requireActivity() as? MenuActivity)?.showMenu()
        }

        view.findViewById<Button>(R.id.mapButton).setOnClickListener {
            (requireActivity() as? MenuActivity)?.showMap()
        }

        refreshChallengeList()
    }

    private fun refreshChallengeList() {
        val visibleChallenges = allChallenges.take(visibleCount)
        adapter.submitList(visibleChallenges)

        loadMoreButton.isEnabled = visibleCount < allChallenges.size
        loadMoreButton.text = if (loadMoreButton.isEnabled) "Load more" else "All loaded"
    }

    private fun deleteChallenge(challenge: Challenge) {
        allChallenges.remove(challenge)
        visibleCount = visibleCount.coerceAtMost(allChallenges.size)
        refreshChallengeList()
    }

    private data class Challenge(
        val id: Int,
        val title: String
    )

    private inner class ChallengeAdapter(context: Context) : BaseAdapter() {
        private val inflater = LayoutInflater.from(context)
        private val challenges = mutableListOf<Challenge>()
        private var selectedChallengeId: Int? = null

        fun submitList(newChallenges: List<Challenge>) {
            challenges.clear()
            challenges.addAll(newChallenges)

            if (selectedChallengeId == null || challenges.none { it.id == selectedChallengeId }) {
                selectedChallengeId = challenges.firstOrNull()?.id
            }

            notifyDataSetChanged()
        }

        override fun getCount(): Int = challenges.size

        override fun getItem(position: Int): Challenge = challenges[position]

        override fun getItemId(position: Int): Long = challenges[position].id.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val row = convertView ?: inflater.inflate(R.layout.item_challenge, parent, false)
            val challenge = getItem(position)

            row.findViewById<TextView>(R.id.challengeTitle).text = challenge.title
            row.findViewById<RadioButton>(R.id.challengeSelectedButton).isChecked =
                challenge.id == selectedChallengeId

            row.setOnClickListener {
                selectedChallengeId = challenge.id
                notifyDataSetChanged()
            }

            row.findViewById<Button>(R.id.deleteChallengeButton).setOnClickListener {
                deleteChallenge(challenge)
            }

            return row
        }
    }

    companion object {
        private const val CHALLENGES_PER_PAGE = 5
    }
}
