package com.example.cityexplorerchallenge_agh

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<Button>(R.id.enterGameButton).setOnClickListener {
            if (hasRequiredPermissions()) {
                openMenu()
            } else {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.mainContent, AcceptDevicePermissions())
                    .addToBackStack(null)
                    .commit()
            }
        }
    }

    private fun hasRequiredPermissions(): Boolean {
        return hasLocationPermission() && hasPermission(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun hasLocationPermission(): Boolean {
        return hasPermission(Manifest.permission.ACCESS_FINE_LOCATION) ||
            hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
    }

    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(this, permission) ==
            PackageManager.PERMISSION_GRANTED
    }

    private fun openMenu() {
        startActivity(Intent(this, MenuActivity::class.java))
        finish()
    }
}
