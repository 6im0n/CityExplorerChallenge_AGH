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
import androidx.lifecycle.lifecycleScope
import com.example.cityexplorerchallenge_agh.storage.AppDatabase
import com.example.cityexplorerchallenge_agh.storage.ChallengeEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The challenges the user has added and is currently doing (state = "current").
 *
 * Tap a challenge to open it on the map (that one becomes the "go to" target).
 * The side button removes it from the list.
 */
class ListActualChallenge : Fragment() {

    private lateinit var adapter: CurrentAdapter
    private lateinit var titleText: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_list_actual_challenge, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        titleText = view.findViewById(R.id.currentChallengeListTitle)

        adapter = CurrentAdapter(requireContext())
        view.findViewById<ListView>(R.id.currentChallengeList).adapter = adapter

        view.findViewById<Button>(R.id.mainMenuButton).setOnClickListener {
            (requireActivity() as? MenuActivity)?.showMenu()
        }
        view.findViewById<Button>(R.id.mapButton).setOnClickListener {
            (requireActivity() as? MenuActivity)?.showMap()
        }
    }

    override fun onResume() {
        super.onResume()
        loadCurrentChallenges() // refresh in case one was completed/added elsewhere
    }

    private fun loadCurrentChallenges() {
        viewLifecycleOwner.lifecycleScope.launch {
            val items = withContext(Dispatchers.IO) {
                AppDatabase.get(requireContext()).challengeDao()
                    .byState(ChallengeEntity.STATE_CURRENT)
            }
            adapter.submitList(items)
            titleText.text =
                if (items.isEmpty()) "No current challenges" else "Current challenge list"
        }
    }

    /** Open the map on this challenge; it becomes the "go to" target. */
    private fun openOnMap(challenge: ChallengeEntity) {
        (requireActivity() as? MenuActivity)?.showMapForChallenge(challenge)
    }

    private fun deleteChallenge(challenge: ChallengeEntity) {
        viewLifecycleOwner.lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                AppDatabase.get(requireContext()).challengeDao().delete(challenge)
            }
            loadCurrentChallenges()
        }
    }

    /** Simple list adapter that shows one current challenge per row. */
    private inner class CurrentAdapter(context: Context) : BaseAdapter() {
        private val inflater = LayoutInflater.from(context)
        private val challenges = mutableListOf<ChallengeEntity>()

        fun submitList(newChallenges: List<ChallengeEntity>) {
            challenges.clear()
            challenges.addAll(newChallenges)
            notifyDataSetChanged()
        }

        override fun getCount(): Int = challenges.size

        override fun getItem(position: Int): ChallengeEntity = challenges[position]

        override fun getItemId(position: Int): Long = challenges[position].id.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val row = convertView ?: inflater.inflate(R.layout.item_challenge, parent, false)
            val challenge = getItem(position)

            row.findViewById<TextView>(R.id.challengeTitle).text =
                "${challenge.categoryLabel()} • ${challenge.title}"
            row.findViewById<RadioButton>(R.id.challengeSelectedButton).isChecked = false

            // Tap the row to open it on the map.
            row.setOnClickListener { openOnMap(challenge) }

            row.findViewById<Button>(R.id.deleteChallengeButton).apply {
                text = "Delete"
                setOnClickListener { deleteChallenge(challenge) }
            }

            return row
        }
    }
}
