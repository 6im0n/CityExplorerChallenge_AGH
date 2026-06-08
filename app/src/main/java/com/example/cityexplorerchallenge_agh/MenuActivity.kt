package com.example.cityexplorerchallenge_agh

import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.example.cityexplorerchallenge_agh.storage.ChallengeEntity

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
