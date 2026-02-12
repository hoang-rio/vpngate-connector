package vn.unlimit.vpngate.utils

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.VpnService
import android.os.Build
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.preference.PreferenceManager
import vn.unlimit.vpngate.R
import vn.unlimit.vpngate.models.VPNGateConnection

/**
 * Helper class for managing SoftEther VPN connections
 * Note: This is a stub implementation for Phase 5 integration.
 * Full implementation will connect to the SoftEtherClient module service.
 */
class SoftEtherConnectionHelper(private val activity: AppCompatActivity) {

    companion object {
        private const val TAG = "SoftEtherHelper"
        const val ACTION_VPN_CONNECT = "vn.unlimit.softether.CONNECT"
        const val ACTION_VPN_DISCONNECT = "vn.unlimit.softether.DISCONNECT"
        const val PREFS_KEY_SOFTETHER_CONNECTED = "softether_connected"
        const val PREFS_KEY_SOFTETHER_HOSTNAME = "softether_hostname"
        const val PREFS_KEY_SOFTETHER_IP = "softether_ip"
        const val PREFS_KEY_SOFTETHER_COUNTRY = "softether_country"
        const val PREFS_KEY_SOFTETHER_PORT = "softether_port"

        // Placeholder constants for service extras
        private const val EXTRA_HOSTNAME = "hostname"
        private const val EXTRA_IP = "ip"
        private const val EXTRA_PORT = "port"
        private const val EXTRA_USERNAME = "username"
        private const val EXTRA_PASSWORD = "password"
        private const val EXTRA_CONNECTION_STATE = "connection_state"
        private const val EXTRA_ERROR = "error"

        // Placeholder state constants
        private const val STATE_CONNECTED = "CONNECTED"
        private const val STATE_DISCONNECTED = "DISCONNECTED"
        private const val STATE_ERROR = "ERROR"
        private const val ACTION_CONNECTION_STATE = "vn.unlimit.softether.CONNECTION_STATE"

        /**
         * Check if SoftEther is currently connected
         */
        fun isConnected(context: Context): Boolean {
            return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(PREFS_KEY_SOFTETHER_CONNECTED, false)
        }

        /**
         * Get the currently connected SoftEther hostname
         */
        fun getConnectedHostname(context: Context): String? {
            return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(PREFS_KEY_SOFTETHER_HOSTNAME, null)
        }
    }

    private val prefs = PreferenceManager.getDefaultSharedPreferences(activity)
    private var connectionListener: SoftEtherConnectionListener? = null
    private var currentConnection: VPNGateConnection? = null
    private var isConnecting = false

    interface SoftEtherConnectionListener {
        fun onConnected(hostname: String, ip: String)
        fun onDisconnected()
        fun onConnectionError(error: String)
        fun onStateChanged(state: String)
    }

    private val vpnPermissionLauncher: ActivityResultLauncher<Intent> =
        activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                startVpnService()
            } else {
                connectionListener?.onConnectionError(activity.getString(R.string.vpn_permission_denied))
                isConnecting = false
            }
        }

    private val connectionStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                ACTION_CONNECTION_STATE -> {
                    val state = intent.getStringExtra(EXTRA_CONNECTION_STATE)
                    val hostname = intent.getStringExtra(EXTRA_HOSTNAME)
                    val ip = intent.getStringExtra(EXTRA_IP)
                    val error = intent.getStringExtra(EXTRA_ERROR)

                    Log.d(TAG, "Connection state: $state")

                    when (state) {
                        STATE_CONNECTED -> {
                            isConnecting = false
                            saveConnectionState(true)
                            if (hostname != null && ip != null) {
                                connectionListener?.onConnected(hostname, ip)
                            }
                        }
                        STATE_DISCONNECTED -> {
                            isConnecting = false
                            saveConnectionState(false)
                            connectionListener?.onDisconnected()
                        }
                        STATE_ERROR -> {
                            isConnecting = false
                            saveConnectionState(false)
                            connectionListener?.onConnectionError(error ?: "Unknown error")
                        }
                        else -> {
                            connectionListener?.onStateChanged(state ?: "Unknown")
                        }
                    }
                }
            }
        }
    }

    init {
        // Register broadcast receiver for connection state
        val filter = IntentFilter().apply {
            addAction(ACTION_CONNECTION_STATE)
        }
        LocalBroadcastManager.getInstance(activity).registerReceiver(connectionStateReceiver, filter)
    }

    fun setConnectionListener(listener: SoftEtherConnectionListener) {
        this.connectionListener = listener
    }

    /**
     * Connect to SoftEther VPN
     */
    fun connect(connection: VPNGateConnection, excludedApps: List<String> = emptyList()) {
        if (isConnecting) {
            Log.d(TAG, "Already connecting, ignoring connect request")
            return
        }

        currentConnection = connection
        isConnecting = true

        // Save connection info to preferences
        prefs.edit {
            putString(PREFS_KEY_SOFTETHER_HOSTNAME, connection.calculateHostName)
            putString(PREFS_KEY_SOFTETHER_IP, connection.ip)
            putString(PREFS_KEY_SOFTETHER_COUNTRY, connection.countryShort)
            putInt(PREFS_KEY_SOFTETHER_PORT, 443) // SoftEther typically uses port 443
            putStringSet("excluded_apps", excludedApps.toSet())
        }

        // Request VPN permission
        val intent = VpnService.prepare(activity)
        if (intent != null) {
            vpnPermissionLauncher.launch(intent)
        } else {
            startVpnService()
        }
    }

    /**
     * Disconnect from SoftEther VPN
     */
    fun disconnect() {
        isConnecting = false
        sendVpnAction(ACTION_VPN_DISCONNECT)
    }

    /**
     * Check if currently connected to this specific server
     */
    fun isConnectedTo(connection: VPNGateConnection): Boolean {
        return isConnected(activity) &&
                getConnectedHostname(activity) == connection.calculateHostName
    }

    /**
     * Check if SoftEther is connected (any server)
     */
    fun isConnected(): Boolean {
        return isConnected(activity)
    }

    /**
     * Clean up resources
     */
    fun cleanup() {
        LocalBroadcastManager.getInstance(activity).unregisterReceiver(connectionStateReceiver)
    }

    private fun startVpnService() {
        // Stub implementation - will start the actual SoftEtherVpnService when module is integrated
        Log.d(TAG, "Starting VPN service (stub implementation)")

        // Simulate connection success for now
        activity.runOnUiThread {
            // In real implementation, this would start the service
            // For now, just mark as connected for UI testing
            saveConnectionState(true)
            currentConnection?.let {
                connectionListener?.onConnected(it.calculateHostName, it.ip ?: "")
            }
        }
    }

    private fun sendVpnAction(action: String) {
        // Stub implementation
        Log.d(TAG, "Sending VPN action: $action (stub implementation)")

        if (action == ACTION_VPN_DISCONNECT) {
            saveConnectionState(false)
            connectionListener?.onDisconnected()
        }
    }

    private fun saveConnectionState(connected: Boolean) {
        prefs.edit {
            putBoolean(PREFS_KEY_SOFTETHER_CONNECTED, connected)
        }
    }
}
