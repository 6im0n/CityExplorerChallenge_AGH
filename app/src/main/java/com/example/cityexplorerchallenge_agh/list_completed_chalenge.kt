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

class list_completed_chalenge : Fragment() {
    private val completedChallenges = listOf(
        CompletedChallenge(1, "Old Town Photo Hunt", "Rynek Glowny 1, Krakow", 50.0619, 19.9373),
        CompletedChallenge(2, "River Walk Discovery", "Bulwar Czerwienski, Krakow", 50.0547, 19.9345),
        CompletedChallenge(3, "Museum Route", "al. 3 Maja 1, Krakow", 50.0591, 19.9237),
        CompletedChallenge(4, "Hidden Park Visit", "Park Jordana, Krakow", 50.0611, 19.9189),
        CompletedChallenge(5, "City Landmark Sprint", "Wawel 5, Krakow", 50.0540, 19.9356)
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_list_completed_chalenge, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<ListView>(R.id.completedChallengeList).adapter =
            CompletedChallengeAdapter(requireContext(), completedChallenges)

        view.findViewById<Button>(R.id.mainMenuButton).setOnClickListener {
            (requireActivity() as? MenuActivity)?.showMenu()
        }

        view.findViewById<Button>(R.id.mapButton).setOnClickListener {
            (requireActivity() as? MenuActivity)?.showMap()
        }
    }

    private data class CompletedChallenge(
        val id: Int,
        val title: String,
        val address: String,
        val latitude: Double,
        val longitude: Double
    )

    private inner class CompletedChallengeAdapter(
        context: Context,
        private val challenges: List<CompletedChallenge>
    ) : BaseAdapter() {
        private val inflater = LayoutInflater.from(context)

        override fun getCount(): Int = challenges.size

        override fun getItem(position: Int): CompletedChallenge = challenges[position]

        override fun getItemId(position: Int): Long = challenges[position].id.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val row = convertView ?: inflater.inflate(R.layout.item_completed, parent, false)
            val challenge = getItem(position)

            row.findViewById<TextView>(R.id.challengeTitle).text = challenge.title
            row.findViewById<Button>(R.id.moreInfoButton).setOnClickListener {
                (requireActivity() as? MenuActivity)?.showCompletedChallengeInfo(
                    title = challenge.title,
                    address = challenge.address,
                    latitude = challenge.latitude,
                    longitude = challenge.longitude
                )
            }

            return row
        }
    }
}
