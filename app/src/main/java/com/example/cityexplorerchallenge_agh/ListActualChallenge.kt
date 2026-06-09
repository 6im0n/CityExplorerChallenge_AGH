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
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.cityexplorerchallenge_agh.finder.DeviceLocation
import com.example.cityexplorerchallenge_agh.finder.DistanceCalcSimple
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

    // Where the user is, so each row can show its distance.
    private var userLatitude = 0.0
    private var userLongitude = 0.0

    // Id of the challenge currently selected as the "go to" target (null if none).
    private var selectedChallengeId: Int? = null

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
        // Show distances right away with the cached position...
        val cached = DeviceLocation().lastKnownOrDefault(requireContext())
        userLatitude = cached.first
        userLongitude = cached.second

        // ...then refine them once a fresh GPS fix arrives.
        DeviceLocation().requestFresh(requireContext()) { location ->
            if (!isAdded) return@requestFresh
            userLatitude = location.first
            userLongitude = location.second
            adapter.notifyDataSetChanged()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val items = withContext(Dispatchers.IO) {
                AppDatabase.get(requireContext()).challengeDao()
                    .byState(ChallengeEntity.STATE_CURRENT)
            }
            adapter.submitList(items)
            selectedChallengeId = items.firstOrNull { it.selected }?.id
            titleText.text =
                if (items.isEmpty()) "No current challenges" else "Current challenge list"
        }
    }

    //Open the map on this challenge; it becomes the "go to" target.
    private fun openOnMap(challenge: ChallengeEntity) {
        (requireActivity() as? MenuActivity)?.showMapForChallenge(challenge)
    }

    //Radio tapped: select this challenge, asking first if another one is selected.
    private fun onRadioTapped(challenge: ChallengeEntity) {
        val current = selectedChallengeId
        if (current != null && current != challenge.id) {
            askChangeSelection(challenge)
        } else {
            applySelection(challenge)
        }
    }

    //Confirm before replacing an already selected challenge.
    private fun askChangeSelection(challenge: ChallengeEntity) {
        AlertDialog.Builder(requireContext())
            .setTitle("Change selected challenge?")
            .setMessage("Another challenge is already selected. Select \"${challenge.title}\" instead?")
            .setPositiveButton("Yes") { _, _ -> applySelection(challenge) }
            .setNegativeButton("No") { _, _ -> loadCurrentChallenges() }
            .setOnCancelListener { loadCurrentChallenges() }
            .show()
    }

    //Make this challenge the only selected one, then refresh the list.
    private fun applySelection(challenge: ChallengeEntity) {
        viewLifecycleOwner.lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                val dao = AppDatabase.get(requireContext()).challengeDao()
                dao.clearSelection()
                dao.select(challenge.id)
            }
            loadCurrentChallenges()
        }
    }

    private fun deleteChallenge(challenge: ChallengeEntity) {
        viewLifecycleOwner.lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                AppDatabase.get(requireContext()).challengeDao().delete(challenge)
            }
            loadCurrentChallenges()
        }
    }

    // simple list adapter that shows one current challenge per row
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

            // categorie change
            row.findViewById<TextView>(R.id.challengeCategory).text = challenge.categoryLabel()
            row.findViewById<TextView>(R.id.challengeTitle).text = challenge.title

            row.findViewById<RadioButton>(R.id.challengeSelectedButton).apply {
                isChecked = challenge.selected
                setOnClickListener { onRadioTapped(challenge) }
            }

            // Highlight selected  background.
            val background = if (challenge.selected) R.color.accent_primary else R.color.accent_secondary
            (row as CardView).setCardBackgroundColor(
                ContextCompat.getColor(requireContext(), background)
            )

            val meters = DistanceCalcSimple().meters(
                userLatitude, userLongitude, challenge.latitude, challenge.longitude
            )
            row.findViewById<TextView>(R.id.challengeDistance).text = formatDistance(meters)

            // tap top open on map
            row.setOnClickListener { openOnMap(challenge) }

            row.findViewById<Button>(R.id.deleteChallengeButton).setOnClickListener {
                deleteChallenge(challenge)
            }

            return row
        }
    }

    private fun formatDistance(meters: Double): String {
        return if (meters < 1000) "${Math.round(meters)} m"
        else "%.1f km".format(meters / 1000)
    }
}
