package com.example.cityexplorerchallenge_agh

import android.location.Geocoder
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.cityexplorerchallenge_agh.finder.DeviceLocation
import com.example.cityexplorerchallenge_agh.storage.AppDatabase
import com.example.cityexplorerchallenge_agh.storage.ChallengeEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class MenuActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_menu)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<Button>(R.id.viewCurrentChallengeButton).setOnClickListener {
            showCurrentChallenges()
        }
        findViewById<Button>(R.id.viewMap).setOnClickListener {
            showMap()
        }
        findViewById<Button>(R.id.viewHistory).setOnClickListener {
            showCompletedChallenges()
        }
        findViewById<Button>(R.id.selectNewChallengeButton).setOnClickListener {
            showNearbyChallenges()
        }

        refreshStats()

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (findViewById<View>(R.id.menuFragmentContainer).visibility == View.VISIBLE) {
                        showMenu()
                    } else {
                        isEnabled = false
                        onBackPressedDispatcher.onBackPressed()
                    }
                }
            }
        )
    }

    fun showMenu() {
        supportFragmentManager.findFragmentById(R.id.menuFragmentContainer)?.let { fragment ->
            supportFragmentManager.beginTransaction()
                .remove(fragment)
                .commit()
        }
        findViewById<View>(R.id.menuFragmentContainer).visibility = View.GONE
        findViewById<View>(R.id.mainMenu).visibility = View.VISIBLE
        refreshStats() // numbers may have changed while a fragment was open
    }

    private fun refreshStats() {
        // Counts come from the database, no location needed.
        lifecycleScope.launch {
            val counts = withContext(Dispatchers.IO) {
                val dao = AppDatabase.get(this@MenuActivity).challengeDao()
                dao.byState(ChallengeEntity.STATE_CURRENT).size to
                    dao.byState(ChallengeEntity.STATE_FINISHED).size
            }
            findViewById<TextView>(R.id.statActiveChallenges).text =
                "You current active challenges: ${counts.first}"
            findViewById<TextView>(R.id.statFinishedChallenges).text =
                "You finished challenges: ${counts.second}"
        }

        // not perfect, city needs a fresh GPS fix, then a geocoder lookup off the main thread.
        DeviceLocation().requestFresh(this) { location ->
            lifecycleScope.launch {
                val city = withContext(Dispatchers.IO) { cityName(location) }
                findViewById<TextView>(R.id.statCity).text = "Your actual city: $city"
            }
        }
    }

    private fun cityName(location: Pair<Double, Double>): String {
        if (!Geocoder.isPresent()) return "Unknown"
        return try {
            val geocoder = Geocoder(this, Locale.getDefault())
            val address = geocoder.getFromLocation(location.first, location.second, 1)?.firstOrNull()
            address?.locality ?: address?.subAdminArea ?: address?.adminArea ?: "Unknown"
        } catch (e: Exception) {
            "Unknown"
        }
    }

    fun showCurrentChallenges() {
        showFragment(ListActualChallenge())
    }

    fun showNearbyChallenges() {
        showFragment(ListNearbyChallenge())
    }

    fun showCompletedChallenges() {
        showFragment(list_completed_chalenge())
    }

    fun showCompletedChallengeInfo(
        title: String,
        address: String,
        latitude: Double,
        longitude: Double
    ) {
        showFragment(
            completeChalengeInfo.newInstance(
                title = title,
                address = address,
                latitude = latitude,
                longitude = longitude
            )
        )
    }

    fun showMap() {
        showFragment(MapView())
    }

    fun showMapForChallenge(challenge: ChallengeEntity) {
        showFragment(
            MapView.forChallenge(
                id = challenge.id,
                title = challenge.title,
                latitude = challenge.latitude,
                longitude = challenge.longitude
            )
        )
    }

    private fun showFragment(fragment: Fragment) {
        findViewById<View>(R.id.mainMenu).visibility = View.GONE
        findViewById<View>(R.id.menuFragmentContainer).visibility = View.VISIBLE
        supportFragmentManager.beginTransaction()
            .replace(R.id.menuFragmentContainer, fragment)
            .commit()
    }
}
