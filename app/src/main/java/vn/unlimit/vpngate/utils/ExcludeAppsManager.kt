package vn.unlimit.vpngate.utils

import android.content.Context
import android.widget.Toast
import androidx.fragment.app.FragmentManager
import de.blinkt.openvpn.VpnProfile
import kotlinx.coroutines.*
import vn.unlimit.vpngate.App
import vn.unlimit.vpngate.R
import vn.unlimit.vpngate.dialog.AppSelectionDialog
import vn.unlimit.vpngate.models.ExcludedApp

class ExcludeAppsManager(private val context: Context) {

    interface ExcludeAppsCallback {
        fun updateButtonText()
    }

    private var callback: ExcludeAppsCallback? = null

    fun setCallback(callback: ExcludeAppsCallback) {
        this.callback = callback
    }

    fun openExcludeAppsManager(fragmentManager: FragmentManager) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Get currently excluded apps on background thread
                val excludedApps = App.instance?.excludedAppDao?.getAllExcludedApps() ?: emptyList()

                // Create dialog on main thread
                withContext(Dispatchers.Main) {
                    val dialog = AppSelectionDialog()
                    dialog.setExcludedApps(excludedApps)
                    dialog.setAppSelectionListener(object : AppSelectionDialog.AppSelectionListener {
                        override fun onAppsSelected(apps: List<ExcludedApp>) {
                            // Save selected apps to database
                            saveSelectedApps(apps)
                        }
                    })
                    dialog.show(fragmentManager, "AppSelectionDialog")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, context.getString(R.string.error_opening_app_selection), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun saveSelectedApps(selectedApps: List<ExcludedApp>) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                App.instance?.excludedAppDao?.let { dao ->
                    // Simple approach: delete all and insert selected
                    dao.getAllExcludedApps().forEach { dao.deleteExcludedApp(it) }
                    selectedApps.forEach { dao.insertExcludedApp(it) }
                }
                // Update button text on main thread
                withContext(Dispatchers.Main) {
                    callback?.updateButtonText()
                    Toast.makeText(context, context.getString(R.string.apps_updated_successfully), Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, context.getString(R.string.error_saving_apps), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun updateExcludeAppsButtonText(): String {
        return try {
            val excludedAppsCount = App.instance?.excludedAppDao?.getAllExcludedApps()?.size ?: 0
            context.getString(R.string.exclude_apps_text, excludedAppsCount)
        } catch (e: Exception) {
            e.printStackTrace()
            context.getString(R.string.exclude_apps_text, 0)
        }
    }

    fun configureSplitTunneling(vpnProfile: VpnProfile?) {
        try {
            // Get all excluded apps from database (presence indicates exclusion)
            val excludedApps = App.instance?.excludedAppDao?.getAllExcludedApps() ?: emptyList()

            if (excludedApps.isNotEmpty()) {
                // Initialize the allowed apps set if not already done
                if (vpnProfile?.mAllowedAppsVpn == null) {
                    vpnProfile?.mAllowedAppsVpn = java.util.HashSet()
                }

                // Clear existing apps
                vpnProfile?.mAllowedAppsVpn?.clear()

                // Add excluded apps as disallowed (they bypass VPN)
                for (excludedApp in excludedApps) {
                    vpnProfile?.mAllowedAppsVpn?.add(excludedApp.packageName)
                }

                // Set as disallowed - meaning these apps are excluded from VPN
                vpnProfile?.mAllowedAppsVpnAreDisallowed = true
                vpnProfile?.mAllowAppVpnBypass = true

                android.util.Log.d("ExcludeAppsManager", "Configured split tunneling for ${excludedApps.size} excluded apps")
            } else {
                // No excluded apps, ensure default behavior
                vpnProfile?.mAllowedAppsVpnAreDisallowed = true
                vpnProfile?.mAllowAppVpnBypass = false
            }
        } catch (e: Exception) {
            android.util.Log.e("ExcludeAppsManager", "Error configuring split tunneling", e)
        }
    }
}
