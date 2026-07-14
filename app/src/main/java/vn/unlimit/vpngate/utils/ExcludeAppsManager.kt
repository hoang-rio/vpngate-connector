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
        fun updateButtonText(count: Int)
        fun restartVpnIfRunning()
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

                    // Add small delay to ensure database operations are complete
                    kotlinx.coroutines.delay(100)
                }
                // Update button text and restart VPN if needed
                withContext(Dispatchers.Main) {
                    callback?.updateButtonText(selectedApps.size)

                    // Always call restart callback - it will check if any VPN is running
                    callback?.restartVpnIfRunning()

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

    private fun restartVpnForNewSettings() {
        try {
            // Stop current VPN connection
            de.blinkt.openvpn.core.ProfileManager.setConntectedVpnProfileDisconnected(context)

            // Note: We don't automatically restart VPN here as it requires user interaction
            // The next manual connect will use the updated settings
            android.util.Log.d("ExcludeAppsManager", "VPN will use updated exclude app settings on next connect")
        } catch (e: Exception) {
            android.util.Log.e("ExcludeAppsManager", "Error handling VPN restart for new settings", e)
        }
    }

    fun updateExcludeAppsButtonText(callback: (String) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val excludedAppsCount = App.instance?.excludedAppDao?.getAllExcludedApps()?.size ?: 0
                val text = context.getString(R.string.exclude_apps_text, excludedAppsCount)
                withContext(Dispatchers.Main) {
                    callback(text)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                val text = context.getString(R.string.exclude_apps_text, 0)
                withContext(Dispatchers.Main) {
                    callback(text)
                }
            }
        }
    }

    fun getExcludedAppsCount(): Int {
        return try {
            App.instance?.excludedAppDao?.getAllExcludedApps()?.size ?: 0
        } catch (e: Exception) {
            e.printStackTrace()
            0
        }
    }

    fun configureSplitTunneling(vpnProfile: VpnProfile?) {
        try {
            // Get all excluded apps from database (presence indicates exclusion)
            val excludedApps = App.instance?.excludedAppDao?.getAllExcludedApps() ?: emptyList()

            android.util.Log.d("ExcludeAppsManager", "Found ${excludedApps.size} excluded apps in database")

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
                    android.util.Log.d("ExcludeAppsManager", "Excluding app: ${excludedApp.packageName}")
                }

                // Set as disallowed - meaning these apps are excluded from VPN
                vpnProfile?.mAllowedAppsVpnAreDisallowed = true
                vpnProfile?.mAllowAppVpnBypass = true

                android.util.Log.d("ExcludeAppsManager", "Configured split tunneling for ${excludedApps.size} excluded apps")
            } else {
                // No excluded apps, ensure default behavior
                vpnProfile?.mAllowedAppsVpnAreDisallowed = true
                vpnProfile?.mAllowAppVpnBypass = false
                android.util.Log.d("ExcludeAppsManager", "No excluded apps, using default VPN behavior")
            }
        } catch (e: Exception) {
            android.util.Log.e("ExcludeAppsManager", "Error configuring split tunneling", e)
            // Ensure default behavior on error
            vpnProfile?.mAllowedAppsVpnAreDisallowed = true
            vpnProfile?.mAllowAppVpnBypass = false
        }
    }
}
