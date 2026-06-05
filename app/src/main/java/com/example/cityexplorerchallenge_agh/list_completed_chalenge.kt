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

class list_completed_chalenge : Fragment() {
    private val completedChallenges = listOf(
        CompletedChallenge(1, "Old Town Photo Hunt"),
        CompletedChallenge(2, "River Walk Discovery"),
        CompletedChallenge(3, "Museum Route"),
        CompletedChallenge(4, "Hidden Park Visit"),
        CompletedChallenge(5, "City Landmark Sprint")
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
        val title: String
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
                Toast.makeText(requireContext(), challenge.title, Toast.LENGTH_SHORT).show()
            }

            return row
        }
    }
}
