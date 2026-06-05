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

class ListActualChallenge : Fragment() {
    private val allChallenges = mutableListOf(
        Challenge(1, "Old Town Photo Hunt"),
        Challenge(2, "River Walk Discovery"),
        Challenge(3, "Museum Route"),
        Challenge(4, "Hidden Park Visit"),
        Challenge(5, "City Landmark Sprint"),
        Challenge(6, "Street Art Search"),
        Challenge(7, "Historic Square Tour"),
        Challenge(8, "Local Cafe Trail"),
        Challenge(9, "Bridge View Challenge"),
        Challenge(10, "Evening Lights Walk"),
        Challenge(11, "Architecture Details"),
        Challenge(12, "Weekend Explorer Route")
    )

    private lateinit var adapter: ChallengeAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_list_actual_challenge, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = ChallengeAdapter(requireContext())
        view.findViewById<ListView>(R.id.currentChallengeList).adapter = adapter

        view.findViewById<Button>(R.id.mainMenuButton).setOnClickListener {
            (requireActivity() as? MenuActivity)?.showMenu()
        }

        view.findViewById<Button>(R.id.mapButton).setOnClickListener {
            (requireActivity() as? MenuActivity)?.showMap()
        }

        refreshChallengeList()
    }

    private fun refreshChallengeList() {
        adapter.submitList(allChallenges)
    }

    private fun deleteChallenge(challenge: Challenge) {
        allChallenges.remove(challenge)
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
}
