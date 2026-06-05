package com.example.cityexplorerchallenge_agh

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

class AcceptDevicePermissions : Fragment() {
    private var hasAskedForPermissions = false

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        if (hasRequiredPermissions()) {
            openMenu()
        } else {
            showDeniedMessage()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_accept_device_permissions, container, false)
    }

    // not perfect here because we check the user permission and after we skip the permission, so the windows appear for some time.
    //TODO fix it.
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (hasRequiredPermissions()) {
            openMenu()
            return
        }

        view.findViewById<Button>(R.id.acceptDevicePermissions).setOnClickListener {
            requestMissingPermissions()
        }
    }

    override fun onResume() {
        super.onResume()

        if (view != null && hasRequiredPermissions()) {
            openMenu()
        }
    }

    //check the user permission before asking
    private fun requestMissingPermissions() {
        val permissions = mutableListOf<String>()

        if (!hasLocationPermission()) {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
            permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }

        if (!hasPermission(Manifest.permission.POST_NOTIFICATIONS)) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        if (permissions.isEmpty()) {
            openMenu()
        } else if (hasAskedForPermissions && cannotShowPermissionPopup(permissions)) {
            showAppSettingsDialog()
        } else {
            hasAskedForPermissions = true
            permissionLauncher.launch(permissions.toTypedArray())
        }
    }

    // if we pop-up one time to the user and the user has refused the permissions
    private fun cannotShowPermissionPopup(permissions: List<String>): Boolean {
        return permissions.none { shouldShowRequestPermissionRationale(it) }
    }

    private fun hasRequiredPermissions(): Boolean {
        return hasLocationPermission() && hasPermission(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun hasLocationPermission(): Boolean {
        return hasPermission(Manifest.permission.ACCESS_FINE_LOCATION) ||
            hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
    }

    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(requireContext(), permission) ==
            PackageManager.PERMISSION_GRANTED
    }

    //litle toast for error of getting permissions
    private fun showDeniedMessage() {
        Toast.makeText(
            requireContext(),
            "Permissions are required. Tap Give permission again.",
            Toast.LENGTH_SHORT
        ).show()
    }

    //making an dialogue to request to the use to go in the app setting to enable the permission manually (we cannot do anything more)
    private fun showAppSettingsDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Permissions required")
            .setMessage("Android is not showing the permission popup anymore. Enable permissions in app settings to continue.")
            .setPositiveButton("Open settings") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", requireContext().packageName, null)
                }
                startActivity(intent)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun openMenu() { // this co,ntinue to the menu
        startActivity(Intent(requireContext(), MenuActivity::class.java))
        requireActivity().finish()
    }
}
