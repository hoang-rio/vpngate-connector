package vn.unlimit.vpngate.fragment

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.SharedPreferences
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.RemoteException
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import de.blinkt.openvpn.VpnProfile
import de.blinkt.openvpn.core.ConfigParser
import de.blinkt.openvpn.core.ConfigParser.ConfigParseError
import de.blinkt.openvpn.core.ConnectionStatus
import de.blinkt.openvpn.core.IOpenVPNServiceInternal
import de.blinkt.openvpn.core.OpenVPNManagement
import de.blinkt.openvpn.core.OpenVPNService
import de.blinkt.openvpn.core.ProfileManager
import de.blinkt.openvpn.core.VPNLaunchHelper
import de.blinkt.openvpn.core.VpnStatus
import de.blinkt.openvpn.core.VpnStatus.ByteCountListener
import de.blinkt.openvpn.utils.PropertiesService
import de.blinkt.openvpn.utils.TotalTraffic
import kittoku.osc.preference.OscPrefKey
import kittoku.osc.service.SstpVpnService
import vn.unlimit.vpngate.App
import vn.unlimit.vpngate.App.Companion.instance
import vn.unlimit.vpngate.R
import vn.unlimit.vpngate.activities.DetailActivity
import vn.unlimit.vpngate.activities.MainActivity
import vn.unlimit.vpngate.databinding.FragmentStatusBinding
import vn.unlimit.vpngate.models.VPNGateConnection
import vn.unlimit.vpngate.provider.BaseProvider
import vn.unlimit.vpngate.utils.DataUtil
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStreamReader

/**
 * Created by hoangnd on 2/9/2018.
 */
class StatusFragment : Fragment(), View.OnClickListener, VpnStatus.StateListener,
    ByteCountListener {
    companion object {
        private const val TAG = "StatusFragment"
        const val START_VPN_SSTP: Int = 80
        const val ACTION_VPN_CONNECT: String = "kittoku.osc.connect"
        const val ACTION_VPN_DISCONNECT: String = "kittoku.osc.disconnect"
    }

    private var mVPNService: IOpenVPNServiceInternal? = null
    private val mConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(
            className: ComponentName,
            service: IBinder
        ) {
            mVPNService = IOpenVPNServiceInternal.Stub.asInterface(service)
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            mVPNService = null
        }
    }
    private var dataUtil: DataUtil? = null
    private var mVpnGateConnection: VPNGateConnection? = null
    private var isConnecting = false
    private var isAuthFailed = false
    private var isDetached = false
    private var mInterstitialAd: InterstitialAd? = null
    private var vpnProfile: VpnProfile? = null
    private var mContext: Context? = null
    private var isFullScreenAdsLoaded = false
    private lateinit var binding: FragmentStatusBinding
    private lateinit var excludeAppsManager: vn.unlimit.vpngate.utils.ExcludeAppsManager
    private lateinit var prefs: SharedPreferences
    private lateinit var listener: SharedPreferences.OnSharedPreferenceChangeListener
    private var isSSTPConnected = false
    private var isSSTPConnectOrDisconnecting = false

    private val isFreeConnected: Boolean
        get() = (checkStatus() && !dataUtil!!.getBooleanSetting(DataUtil.IS_LAST_CONNECTED_PAID, false)) || isSSTPConnected

    private val isAnyConnected: Boolean
        get() = checkStatus() || isSSTPConnected

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentStatusBinding.inflate(layoutInflater)
        binding.btnOnOff.setOnClickListener(this)
        binding.btnExcludeApps?.setOnClickListener(this)
        binding.btnClearStatistics.setOnClickListener(this)
        binding.txtCheckIp?.setOnClickListener(this)

        // Initialize exclude apps manager
        excludeAppsManager = vn.unlimit.vpngate.utils.ExcludeAppsManager(requireContext())
        excludeAppsManager.setCallback(object : vn.unlimit.vpngate.utils.ExcludeAppsManager.ExcludeAppsCallback {
            override fun updateButtonText(count: Int) {
                binding.btnExcludeApps?.text = context?.getString(R.string.exclude_apps_text, count) ?: "Excluding $count app(s) from VPN"
            }

            override fun restartVpnIfRunning() {
                var vpnRestarted = false
                // Check if OpenVPN is currently running and restart it
                if (checkStatus()) {
                    // Disconnect first
                    stopVpn()
                    // Wait a bit then reconnect
                    Handler(Looper.getMainLooper()).postDelayed({
                        prepareVpn()
                    }, 500)
                    vpnRestarted = true
                }
                // Check if SSTP is currently running and restart it
                else if (isSSTPConnected && mVpnGateConnection != null) {
                    // Disconnect SSTP first
                    startVpnSSTPService(ACTION_VPN_DISCONNECT)
                    // Wait a bit then reconnect
                    Handler(Looper.getMainLooper()).postDelayed({
                        connectSSTPVPN()
                    }, 500)
                    vpnRestarted = true
                }
                if (vpnRestarted) {
                    Toast.makeText(context, getString(R.string.vpn_restarted_for_settings), Toast.LENGTH_LONG).show()
                }
            }
        })

        loadAdMob()
        bindData()
        excludeAppsManager.updateExcludeAppsButtonText { text ->
            binding.btnExcludeApps?.text = text
        }
        initSSTP()
        onHiddenChanged(true)
        VpnStatus.addStateListener(this)
        VpnStatus.addByteCountListener(this)
        return binding.root
    }

    override fun onHiddenChanged(hidden: Boolean) {
        if (!hidden) {
            bindData()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            dataUtil = instance!!.dataUtil
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onAttach(context: Context) {
        mContext = context
        isDetached = false
        super.onAttach(context)
    }

    private fun bindData() {
        try {
            mVpnGateConnection = dataUtil!!.lastVPNConnection
            binding.txtTotalUpload.text = OpenVPNService.humanReadableByteCount(
                PropertiesService.getUploaded(mContext),
                false,
                resources
            )
            binding.txtTotalDownload.text = OpenVPNService.humanReadableByteCount(
                PropertiesService.getDownloaded(mContext),
                false,
                resources
            )
            if (isFreeConnected) {
                binding.btnOnOff.isActivated = true
                binding.txtStatus.text =
                    String.format(resources.getString(R.string.tap_to_disconnect), connectionName)
            } else if (mVpnGateConnection != null) {
                binding.txtStatus.text =
                    String.format(resources.getString(R.string.tap_to_connect_last), connectionName)
            } else {
                binding.btnOnOff.isActivated = false
                binding.btnOnOff.isEnabled = false
                binding.txtStatus.setText(R.string.no_last_vpn_server)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopVpn() {
        //prepareStopVPN();
        ProfileManager.setConntectedVpnProfileDisconnected(context)
        if (mVPNService != null) {
            try {
                mVPNService!!.stopVPN(false)
            } catch (e: RemoteException) {
                e.printStackTrace()
            }
        }
    }

    private fun loadAdMob() {
        if (dataUtil!!.getBooleanSetting(DataUtil.USER_ALLOWED_VPN, false)) {
            val adRequest = AdRequest.Builder().build()
            InterstitialAd.load(
                mContext!!,
                resources.getString(R.string.admob_full_screen_status),
                adRequest,
                object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(interstitialAd: InterstitialAd) {
                        mInterstitialAd = interstitialAd
                        isFullScreenAdsLoaded = true
                    }

                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        mInterstitialAd = null
                    }
                })
        }
    }

    private fun showAds() {
        if (dataUtil!!.hasAds() && isFullScreenAdsLoaded) {
            if (mInterstitialAd != null) {
                mInterstitialAd!!.show(requireActivity())
            }
        }
    }



    override fun onClick(view: View) {
        if (view == binding.btnExcludeApps) {
            // Open exclude apps manager
            excludeAppsManager.openExcludeAppsManager(parentFragmentManager)
        }
        if (view == binding.btnClearStatistics) {
            TotalTraffic.clearTotal(mContext)
            Toast.makeText(context, "Statistics clear completed", Toast.LENGTH_SHORT).show()
            binding.txtTotalUpload.text = OpenVPNService.humanReadableByteCount(0, false, resources)
            binding.txtTotalDownload.text = OpenVPNService.humanReadableByteCount(0, false, resources)
        }
        if (view == binding.txtCheckIp) {
            val params = Bundle()
            params.putString("type", "check ip click")
            params.putString("hostname", mVpnGateConnection?.calculateHostName ?: "")
            params.putString("ip", mVpnGateConnection?.ip ?: "")
            params.putString("country", mVpnGateConnection?.countryLong ?: "")
            mContext?.let { FirebaseAnalytics.getInstance(it).logEvent("Click_Check_IP", params) }
            val browserIntent = Intent(
                Intent.ACTION_VIEW,
                FirebaseRemoteConfig.getInstance().getString("vpn_check_ip_url").toUri()
            )
            startActivity(browserIntent)
        }
        if (view == binding.btnOnOff) {
            if (mVpnGateConnection == null) {
                return
            }

            if (isAnyConnected) {
                if (isFreeConnected) {
                    // Free connected: just disconnect
                    val params = Bundle()
                    params.putString("type", "disconnect current")
                    params.putString("ip", mVpnGateConnection!!.ip)
                    params.putString("hostname", mVpnGateConnection!!.calculateHostName)
                    params.putString("country", mVpnGateConnection!!.countryLong)
                    FirebaseAnalytics.getInstance(mContext!!).logEvent("Disconnect_VPN", params)

                    if (checkStatus()) {
                        stopVpn()
                        isConnecting = false
                        binding.btnOnOff.isActivated = false
                        binding.txtStatus.setText(R.string.disconnecting)
                    } else {
                        startVpnSSTPService(ACTION_VPN_DISCONNECT)
                        binding.btnOnOff.isActivated = false
                        binding.txtStatus.setText(R.string.sstp_disconnecting)
                    }
                } else {
                    // Paid connected: disconnect paid and connect free
                    val params = Bundle()
                    params.putString("type", "disconnect paid and connect free")
                    params.putString("ip", mVpnGateConnection!!.ip)
                    params.putString("hostname", mVpnGateConnection!!.calculateHostName)
                    params.putString("country", mVpnGateConnection!!.countryLong)
                    FirebaseAnalytics.getInstance(mContext!!).logEvent("Disconnect_Paid_Connect_Free", params)

                    // Determine current connection type and disconnect
                    val currentMethod = if (checkStatus()) "openvpn" else "sstp"
                    if (currentMethod == "openvpn") {
                        stopVpn()
                    } else {
                        startVpnSSTPService(ACTION_VPN_DISCONNECT)
                    }

                    // Determine target connection method and connect
                    val targetMethod = dataUtil!!.getStringSetting(DataUtil.LAST_CONNECT_METHOD, "openvpn")
                    dataUtil!!.setBooleanSetting(DataUtil.IS_LAST_CONNECTED_PAID, false)
                    Handler(Looper.getMainLooper()).postDelayed({
                        if (targetMethod == "sstp") {
                            connectSSTPVPN()
                        } else {
                            showAds()
                            prepareVpn()
                        }
                        binding.txtStatus.text = getString(R.string.connecting)
                        binding.btnOnOff.isActivated = true
                        isConnecting = true
                    }, 1000) // Wait 1 second for disconnection to complete
                }
            } else {
                // No connected: create a new connect
                val method = dataUtil!!.getStringSetting(DataUtil.LAST_CONNECT_METHOD, "openvpn")
                if (method == "sstp") {
                    handleSSTPBtn()
                } else {
                    if (!isConnecting) {
                        dataUtil!!.setBooleanSetting(DataUtil.IS_LAST_CONNECTED_PAID, false)
                        showAds()
                        val params = Bundle()
                        params.putString("type", "connect from status")
                        params.putString("ip", mVpnGateConnection!!.ip)
                        params.putString("hostname", mVpnGateConnection!!.calculateHostName)
                        params.putString("country", mVpnGateConnection!!.countryLong)
                        FirebaseAnalytics.getInstance(mContext!!).logEvent("Connect_VPN", params)
                        prepareVpn()
                        binding.txtStatus.text = getString(R.string.connecting)
                        binding.btnOnOff.isActivated = true
                        isConnecting = true
                    }
                }
            }
        }
    }

    private fun prepareVpn() {
        if (loadVpnProfile()) {
            startVpn()
        } else {
            Toast.makeText(context, getString(R.string.error_load_profile), Toast.LENGTH_SHORT)
                .show()
        }
    }

    private fun loadVpnProfile(): Boolean {
        try {
            val useUdp = dataUtil!!.getBooleanSetting(DataUtil.LAST_CONNECT_USE_UDP, false)
            dataUtil!!.setBooleanSetting(DataUtil.LAST_CONNECT_USE_UDP, useUdp)
            val data = if (useUdp) {
                mVpnGateConnection!!.openVpnConfigDataUdp!!.toByteArray()
            } else {
                mVpnGateConnection!!.openVpnConfigData!!.toByteArray()
            }
            val cp = ConfigParser()
            val isr = InputStreamReader(ByteArrayInputStream(data))
            cp.parseConfig(isr)
            vpnProfile = cp.convertProfile()
            vpnProfile!!.mName = connectionName
            vpnProfile?.mCompatMode = App.VPN_PROFILE_COMPAT_MODE_24X

            // Configure split tunneling - exclude apps from VPN
            excludeAppsManager.configureSplitTunneling(vpnProfile)
            if (dataUtil!!.getBooleanSetting(DataUtil.SETTING_BLOCK_ADS, false)) {
                vpnProfile!!.mOverrideDNS = true
                vpnProfile!!.mDNS1 = FirebaseRemoteConfig.getInstance()
                    .getString(getString(R.string.dns_block_ads_primary_cfg_key))
                vpnProfile!!.mDNS2 = FirebaseRemoteConfig.getInstance()
                    .getString(getString(R.string.dns_block_ads_alternative_cfg_key))
            } else if (dataUtil!!.getBooleanSetting(DataUtil.USE_CUSTOM_DNS, false)) {
                vpnProfile!!.mOverrideDNS = true
                vpnProfile!!.mDNS1 =
                    dataUtil!!.getStringSetting(DataUtil.CUSTOM_DNS_IP_1, "8.8.8.8")
                val dns2 = dataUtil!!.getStringSetting(DataUtil.CUSTOM_DNS_IP_2, null)
                if (dns2 != null) {
                    vpnProfile!!.mDNS2 = dns2
                }
            }
            ProfileManager.setTemporaryProfile(activity, vpnProfile)
        } catch (e: IOException) {
            e.printStackTrace()
            return false
        } catch (e: ConfigParseError) {
            e.printStackTrace()
            return false
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }

        return true
    }

    private val startActivityIntentOpenVPN: ActivityResultLauncher<Intent> =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            if (it.resultCode == Activity.RESULT_OK) {
                VPNLaunchHelper.startOpenVpn(vpnProfile, mContext, null, true)
            }
        }

    private fun startVpn() {
        val intent = VpnService.prepare(context)

        if (intent != null) {
            VpnStatus.updateStateString(
                "USER_VPN_PERMISSION", "", R.string.state_user_vpn_permission,
                ConnectionStatus.LEVEL_WAITING_FOR_USER_INPUT
            )
            // Start the query
            try {
                startActivityIntentOpenVPN.launch(intent)
            } catch (ane: ActivityNotFoundException) {
                // Shame on you Sony! At least one user reported that
                // an official Sony Xperia Arc S image triggers this exception
                VpnStatus.logError(de.blinkt.openvpn.R.string.no_vpn_support_image)
            }
        } else {
            VPNLaunchHelper.startOpenVpn(vpnProfile, mContext, null, true)
        }
    }

    override fun onResume() {
        super.onResume()
        try {
            val intent = Intent(context, OpenVPNService::class.java)
            OpenVPNService.mDisplaySpeed =
                dataUtil!!.getBooleanSetting(DataUtil.SETTING_NOTIFY_SPEED, true)
            intent.setAction(OpenVPNService.START_SERVICE)
            requireActivity().bindService(intent, mConnection, Context.BIND_AUTO_CREATE)
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    override fun onPause() {
        super.onPause()
        try {
            TotalTraffic.saveTotal(mContext)
            requireActivity().unbindService(mConnection)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            VpnStatus.removeStateListener(this)
            VpnStatus.removeByteCountListener(this)
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun setConnectedVPN(uuid: String) {
        // Do nothing
    }

    override fun updateByteCount(`in`: Long, out: Long, diffIn: Long, diffOut: Long) {
        if (isDetached()) {
            return
        }
        requireActivity().runOnUiThread {
            if (isFreeConnected && !isDetached) {
                binding.txtDownloadSession.text =
                    OpenVPNService.humanReadableByteCount(`in`, false, resources)
                binding.txtDownloadSpeed.text = OpenVPNService.humanReadableByteCount(
                    diffIn / OpenVPNManagement.mBytecountInterval,
                    true,
                    resources
                )
                binding.txtUploadSession.text =
                    OpenVPNService.humanReadableByteCount(out, false, resources)
                binding.txtUploadSpeed.text = OpenVPNService.humanReadableByteCount(
                    diffOut / OpenVPNManagement.mBytecountInterval,
                    true,
                    resources
                )
                binding.txtTotalDownload.text =
                    OpenVPNService.humanReadableByteCount(TotalTraffic.inTotal, false, resources)
                binding.txtTotalUpload.text =
                    OpenVPNService.humanReadableByteCount(TotalTraffic.outTotal, false, resources)
            }
        }
    }

    override fun onDetach() {
        isDetached = true
        super.onDetach()
    }

    private fun checkStatus(): Boolean {
        try {
            return VpnStatus.isVPNActive()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return false
    }

    private val connectionName: String
        get() {
            val method = dataUtil!!.getStringSetting(DataUtil.LAST_CONNECT_METHOD, "openvpn")
            val useUdp = if (method == "sstp") {
                // SSTP only supports TCP
                false
            } else {
                dataUtil!!.getBooleanSetting(DataUtil.LAST_CONNECT_USE_UDP, false)
            }
            val baseName = mVpnGateConnection!!.getName(useUdp)
            return if (method == "sstp") {
                "MS-SSTP:$baseName"
            } else {
                baseName
            }
        }

    override fun updateState(
        state: String,
        logmessage: String,
        localizedResId: Int,
        status: ConnectionStatus,
        intent: Intent?
    ) {
        requireActivity().runOnUiThread {
            try {
                // Don't override status text if SSTP is connected or if it's a paid connection
                if (!isSSTPConnected && !dataUtil!!.getBooleanSetting(DataUtil.IS_LAST_CONNECTED_PAID, false)) {
                    binding.txtStatus.text = VpnStatus.getLastCleanLogMessage(mContext)
                }
                dataUtil!!.setBooleanSetting(DataUtil.USER_ALLOWED_VPN, true)
                when (status) {
                    ConnectionStatus.LEVEL_CONNECTED -> {
                        if (dataUtil!!.getBooleanSetting(DataUtil.IS_LAST_CONNECTED_PAID, false)) {
                            // Connected via paid server, treat as disconnected state
                            binding.btnOnOff.isActivated = false
                            binding.txtStatus.text =
                                String.format(getString(R.string.tap_to_connect_last), connectionName)
                            binding.txtCheckIp?.visibility = View.INVISIBLE
                        } else {
                            binding.btnOnOff.isActivated = true
                            isConnecting = false
                            isAuthFailed = false
                            binding.txtStatus.text = getString(R.string.connected_to, connectionName)
                            binding.txtCheckIp?.visibility = View.VISIBLE
                            val isStartUpDetail =
                                dataUtil!!.getIntSetting(DataUtil.SETTING_STARTUP_SCREEN, 0) == 0
                            OpenVPNService.setNotificationActivityClass(if (isStartUpDetail) DetailActivity::class.java else MainActivity::class.java)
                        }
                    }

                    ConnectionStatus.LEVEL_WAITING_FOR_USER_INPUT -> dataUtil!!.setBooleanSetting(
                        DataUtil.USER_ALLOWED_VPN,
                        false
                    )

                    ConnectionStatus.LEVEL_NOTCONNECTED -> if (!isConnecting && !isAuthFailed && !isSSTPConnected) {
                        binding.btnOnOff.isActivated = false
                        binding.txtStatus.text =
                            String.format(getString(R.string.tap_to_connect_last), connectionName)
                        binding.txtCheckIp?.visibility = View.INVISIBLE
                    }

                    ConnectionStatus.LEVEL_AUTH_FAILED -> {
                        isAuthFailed = true
                        binding.btnOnOff.isActivated = false
                        val params = Bundle()
                        params.putString("ip", mVpnGateConnection!!.ip)
                        params.putString("hostname", mVpnGateConnection!!.calculateHostName)
                        params.putString("country", mVpnGateConnection!!.countryLong)
                        FirebaseAnalytics.getInstance(mContext!!).logEvent("Connect_Error", params)
                        binding.txtStatus.text = resources.getString(R.string.vpn_auth_failure)
                        binding.txtCheckIp?.visibility = View.INVISIBLE
                        isConnecting = false
                    }

                    else -> binding.txtCheckIp?.visibility = View.INVISIBLE
                }
            } catch (e: Exception) {
                Log.e(TAG, "Status update error", e)
            }
        }
    }

    private fun initSSTP() {
        prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
        listener = SharedPreferences.OnSharedPreferenceChangeListener { _: SharedPreferences?, key: String? ->
            requireActivity().runOnUiThread {
                if (OscPrefKey.ROOT_STATE.toString() == key) {
                    val newState = prefs.getBoolean(OscPrefKey.ROOT_STATE.toString(), false)
                    if (!newState) {
                        if (isSSTPConnectOrDisconnecting) {
                            binding.txtStatus.setText(R.string.sstp_disconnected)
                        } else {
                            binding.txtStatus.setText(R.string.sstp_disconnected_by_error)
                        }
                        isSSTPConnected = false
                        binding.txtCheckIp?.visibility = View.INVISIBLE
                        bindData()
                    }
                    isSSTPConnectOrDisconnecting = false
                }
                if (OscPrefKey.HOME_CONNECTED_IP.toString() == key) {
                    val connectedIp =
                        prefs.getString(OscPrefKey.HOME_CONNECTED_IP.toString(), "")
                    if ("" != connectedIp) {
                        if (dataUtil!!.getBooleanSetting(DataUtil.IS_LAST_CONNECTED_PAID, false)) {
                            // Paid SSTP connected, treat as disconnected state
                            binding.txtStatus.text =
                                String.format(getString(R.string.tap_to_connect_last), connectionName)
                            binding.btnOnOff.isActivated = false
                            binding.txtCheckIp?.visibility = View.INVISIBLE
                        } else {
                            binding.txtStatus.text = getString(R.string.sstp_connected, connectedIp)
                            isSSTPConnected = true
                            binding.btnOnOff.isActivated = true
                            binding.txtCheckIp?.visibility = View.VISIBLE
                        }
                    }
                }
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)

        // Check initial SSTP state and update UI
        isSSTPConnected = prefs.getBoolean(OscPrefKey.ROOT_STATE.toString(), false)
        if (isSSTPConnected) {
            val connectedIp = prefs.getString(OscPrefKey.HOME_CONNECTED_IP.toString(), "")
            if (connectedIp!!.isNotEmpty()) {
                if (dataUtil!!.getBooleanSetting(DataUtil.IS_LAST_CONNECTED_PAID, false)) {
                    // Paid SSTP was already connected, treat as disconnected
                    binding.txtStatus.text = String.format(getString(R.string.tap_to_connect_last), connectionName)
                    binding.btnOnOff.isActivated = false
                    binding.txtCheckIp?.visibility = View.INVISIBLE
                    isSSTPConnected = false  // Treat as not connected
                } else {
                    binding.txtStatus.text = getString(R.string.sstp_connected, connectedIp)
                    binding.btnOnOff.isActivated = true
                    binding.txtCheckIp?.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun startVpnSSTPService(action: String) {
        val intent = Intent(requireContext(), SstpVpnService::class.java).setAction(action)
        if (action == ACTION_VPN_CONNECT && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            requireContext().startForegroundService(intent)
        } else {
            requireContext().startService(intent)
        }
    }

    private val startActivityIntentSSTPVPN: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        handleActivityResult(START_VPN_SSTP, it.resultCode)
    }

    private fun handleActivityResult(requestCode: Int, resultCode: Int) {
        try {
            if (resultCode == Activity.RESULT_OK) {
                if (requestCode == START_VPN_SSTP) {
                    connectSSTPVPN()
                }
                dataUtil!!.setBooleanSetting(DataUtil.USER_ALLOWED_VPN, true)
            } else {
                dataUtil!!.setBooleanSetting(DataUtil.USER_ALLOWED_VPN, false)
            }
        } catch (e: Exception) {
            Log.e(TAG, "handleActivityResult error", e)
        }
    }

    private fun startSSTPVPN() {
        if (checkStatus()) {
            stopVpn()
        }
        val intent = VpnService.prepare(requireContext())
        if (intent != null) {
            try {
                startActivityIntentSSTPVPN.launch(intent)
            } catch (_: ActivityNotFoundException) {
                Log.e(TAG, "OS does not support VPN")
            }
        } else {
            handleActivityResult(START_VPN_SSTP, Activity.RESULT_OK)
        }
    }

    private fun connectSSTPVPN() {
        if (mVpnGateConnection == null) return
        val excludedApps = App.instance?.excludedAppDao?.getAllExcludedApps() ?: emptyList()
        val excludedPackageNames = excludedApps.map { it.packageName }.toSet()
        prefs.edit {
            putString(
                OscPrefKey.HOME_HOSTNAME.toString(),
                mVpnGateConnection!!.calculateHostName
            )
            putString(
                OscPrefKey.HOME_COUNTRY.toString(),
                mVpnGateConnection!!.countryShort?.uppercase() ?: ""
            )
            putString(OscPrefKey.HOME_USERNAME.toString(), "vpn")
            putString(OscPrefKey.HOME_PASSWORD.toString(), "vpn")
            putString(OscPrefKey.SSL_PORT.toString(), mVpnGateConnection!!.tcpPort.toString())
            putStringSet(OscPrefKey.ROUTE_EXCLUDED_APPS.toString(), excludedPackageNames)
        }
        binding.btnOnOff.isActivated = true
        binding.txtStatus.setText(R.string.sstp_connecting)
        startVpnSSTPService(ACTION_VPN_CONNECT)
    }

    private fun handleSSTPBtn() {
        isSSTPConnectOrDisconnecting = true
        val params = Bundle()
        params.putString("hostname", mVpnGateConnection!!.calculateHostName)
        params.putString("ip", mVpnGateConnection!!.ip)
        params.putString("country", mVpnGateConnection!!.countryLong)
        val sstpHostName = prefs.getString(OscPrefKey.HOME_HOSTNAME.toString(), "")
        if (isSSTPConnected && sstpHostName != mVpnGateConnection!!.calculateHostName) {
            // Connected but not must disconnect old first
            startVpnSSTPService(ACTION_VPN_DISCONNECT)
            params.putString("type", "replace connect via MS-SSTP")
            Handler(Looper.getMainLooper()).postDelayed({ connectSSTPVPN() }, 100)
        } else if (!isSSTPConnected) {
            // Check if SSTP is actually running (paid SSTP case)
            if (prefs.getBoolean(OscPrefKey.ROOT_STATE.toString(), false)) {
                // Paid SSTP running, disconnect it first
                startVpnSSTPService(ACTION_VPN_DISCONNECT)
                Handler(Looper.getMainLooper()).postDelayed({
                    dataUtil!!.setBooleanSetting(DataUtil.IS_LAST_CONNECTED_PAID, false)
                    val connectParams = Bundle()
                    connectParams.putString("hostname", mVpnGateConnection!!.calculateHostName)
                    connectParams.putString("ip", mVpnGateConnection!!.ip)
                    connectParams.putString("country", mVpnGateConnection!!.countryLong)
                    connectParams.putString("type", "connect via MS-SSTP")
                    FirebaseAnalytics.getInstance(mContext!!).logEvent("Connect_Via_SSTP", connectParams)
                    dataUtil!!.lastVPNConnection = mVpnGateConnection
                    startSSTPVPN()
                }, 500)
            } else {
                dataUtil!!.setBooleanSetting(DataUtil.IS_LAST_CONNECTED_PAID, false)
                params.putString("type", "connect via MS-SSTP")
                FirebaseAnalytics.getInstance(mContext!!).logEvent("Connect_Via_SSTP", params)
                dataUtil!!.lastVPNConnection = mVpnGateConnection
                startSSTPVPN()
            }
        } else {
            params.putString("type", "cancel MS-SSTP")
            FirebaseAnalytics.getInstance(mContext!!).logEvent("Cancel_Via_SSTP", params)
            startVpnSSTPService(ACTION_VPN_DISCONNECT)
            binding.btnOnOff.isActivated = false
            binding.txtStatus.setText(R.string.sstp_disconnecting)
        }
    }
}
