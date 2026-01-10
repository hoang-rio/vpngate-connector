package vn.unlimit.vpngate.activities

import android.Manifest
import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.content.pm.PackageManager
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.RemoteException
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import androidx.core.content.edit
import androidx.core.content.res.ResourcesCompat
import androidx.core.net.toUri
import androidx.preference.PreferenceManager
import com.bumptech.glide.Glide
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
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
import de.blinkt.openvpn.utils.TotalTraffic
import kittoku.osc.preference.OscPrefKey
import kittoku.osc.service.SstpVpnService
import vn.unlimit.vpngate.App
import vn.unlimit.vpngate.R
import vn.unlimit.vpngate.databinding.ActivityDetailBinding
import vn.unlimit.vpngate.dialog.ConnectionUseProtocol
import vn.unlimit.vpngate.dialog.MessageDialog
import vn.unlimit.vpngate.models.ExcludedApp
import vn.unlimit.vpngate.models.VPNGateConnection
import vn.unlimit.vpngate.provider.BaseProvider
import vn.unlimit.vpngate.utils.DataUtil
import vn.unlimit.vpngate.utils.NotificationUtil
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStreamReader

/**
 * Created by hoangnd on 2/5/2018.
 */
class DetailActivity : AppCompatActivity(), View.OnClickListener, VpnStatus.StateListener,
    ByteCountListener {
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
    private lateinit var dataUtil: DataUtil
    private var mVpnGateConnection: VPNGateConnection? = null
    private lateinit var vpnProfile: VpnProfile
    private var mInterstitialAd: InterstitialAd? = null
    private lateinit var adViewBellow: AdView
    private lateinit var prefs: SharedPreferences
    private lateinit var listener: OnSharedPreferenceChangeListener
    private var isConnecting = false
    private var isAuthFailed = false
    private var isShowAds = false
    private var isSSTPConnectOrDisconnecting = false
    private var isSSTPConnected = false
    private var isFullScreenAdLoaded = false
    private lateinit var binding: ActivityDetailBinding
    private lateinit var excludeAppsManager: vn.unlimit.vpngate.utils.ExcludeAppsManager

    private fun startVpnSSTPService(action: String) {
        val intent = Intent(applicationContext, SstpVpnService::class.java).setAction(action)
        if (action == ACTION_VPN_CONNECT && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            applicationContext.startForegroundService(intent)
        } else {
            applicationContext.startService(intent)
        }
    }

    private fun initSSTP() {
        prefs = PreferenceManager.getDefaultSharedPreferences(this)
        listener =
            OnSharedPreferenceChangeListener { _: SharedPreferences?, key: String? ->
                if (OscPrefKey.ROOT_STATE.toString() == key) {
                    val newState = prefs.getBoolean(OscPrefKey.ROOT_STATE.toString(), false)
                    if (!newState) {
                        binding.btnSstpConnect.background = ResourcesCompat.getDrawable(
                            resources, R.drawable.selector_paid_button, null
                        )
                        binding.btnSstpConnect.setText(R.string.connect_via_sstp)
                        if (isSSTPConnectOrDisconnecting) {
                            binding.txtStatus.setText(R.string.sstp_disconnected)
                        } else {
                            binding.txtStatus.setText(R.string.sstp_disconnected_by_error)
                        }
                        isSSTPConnected = false
                        binding.txtCheckIp.visibility = View.GONE
                    }
                    isSSTPConnectOrDisconnecting = false
                }
                if (OscPrefKey.HOME_CONNECTED_IP.toString() == key) {
                    val connectedIp = prefs.getString(OscPrefKey.HOME_CONNECTED_IP.toString(), "")
                    if (connectedIp!!.isNotEmpty()) {
                        binding.btnSstpConnect.background = ResourcesCompat.getDrawable(
                            resources, R.drawable.selector_red_button, null
                        )
                        binding.btnSstpConnect.setText(R.string.disconnect_sstp)
                        binding.txtStatus.text = getString(R.string.sstp_connected, connectedIp)
                        isSSTPConnected = true
                        binding.txtCheckIp.visibility = View.VISIBLE
                    }
                }
            }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        isSSTPConnected = prefs.getBoolean(OscPrefKey.ROOT_STATE.toString(), false)
        val sstpHostName = prefs.getString(OscPrefKey.HOME_HOSTNAME.toString(), "")
        if (isSSTPConnected) {
            binding.txtCheckIp.visibility = View.VISIBLE
            if (sstpHostName == mVpnGateConnection!!.calculateHostName) {
                binding.btnSstpConnect.background = ResourcesCompat.getDrawable(
                    resources, R.drawable.selector_red_button, null
                )
                binding.btnSstpConnect.setText(R.string.disconnect_sstp)
            }
        }
    }

    @SuppressLint("SetTextI18n")
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dataUtil = (application as App).dataUtil!!
        // Initialize exclude apps manager early to prevent crashes when loading VPN profile
        excludeAppsManager = vn.unlimit.vpngate.utils.ExcludeAppsManager(this)
        if (intent.getIntExtra(TYPE_START, TYPE_NORMAL) == TYPE_FROM_NOTIFY) {
            mVpnGateConnection = dataUtil.lastVPNConnection
            loadVpnProfile(dataUtil.getBooleanSetting(DataUtil.LAST_CONNECT_USE_UDP, false))
            try {
                val params = Bundle()
                params.putString("from", "Notification")
                params.putString("ip", mVpnGateConnection!!.ip)
                params.putString("hostname", mVpnGateConnection!!.calculateHostName)
                params.putString("country", mVpnGateConnection!!.countryLong)
                FirebaseAnalytics.getInstance(applicationContext).logEvent("Open_Detail", params)
            } catch (ex: NullPointerException) {
                Log.e(TAG, "onCreate error", ex)
            }
        } else {
            mVpnGateConnection = IntentCompat.getParcelableExtra(
                intent, BaseProvider.PASS_DETAIL_VPN_CONNECTION,
                VPNGateConnection::class.java
            )
        }
        binding = ActivityDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        excludeAppsManager.setCallback(object : vn.unlimit.vpngate.utils.ExcludeAppsManager.ExcludeAppsCallback {
            override fun updateButtonText(count: Int) {
                binding.btnExcludeApps.text = getString(R.string.exclude_apps_text, count)
            }

            override fun restartVpnIfRunning() {
                var vpnRestarted = false
                // Check if OpenVPN is currently running and restart it
                if (isCurrent && checkStatus()) {
                    // Disconnect first
                    stopVpn()
                    // Wait a bit then reconnect
                    Handler(mainLooper).postDelayed({
                        handleConnection(false) // Default to TCP, or could check current protocol
                    }, 500)
                    vpnRestarted = true
                }
                // Check if SSTP is currently running and restart it
                else if (isSSTPConnected) {
                    // Disconnect SSTP first
                    startVpnSSTPService(ACTION_VPN_DISCONNECT)
                    // Wait a bit then reconnect
                    Handler(mainLooper).postDelayed({
                        connectSSTPVPN()
                    }, 500)
                    vpnRestarted = true
                }

                if (vpnRestarted) {
                    Toast.makeText(this@DetailActivity, getString(R.string.vpn_restarted_for_settings), Toast.LENGTH_LONG).show()
                }
            }
        })

        binding.btnSaveConfigFile.setOnClickListener(this)
        binding.btnInstallOpenvpn.setOnClickListener(this)
        binding.btnBack.setOnClickListener(this)
        binding.btnConnect.setOnClickListener(this)
        binding.txtCheckIp.setOnClickListener(this)
        binding.btnL2tpConnect.setOnClickListener(this)
        binding.btnSstpConnect.setOnClickListener(this)
        binding.btnExcludeApps.setOnClickListener(this)
        excludeAppsManager.updateExcludeAppsButtonText { text ->
            binding.btnExcludeApps.text = text
        }
        bindData()
        initAdMob()
        initInterstitialAd()
        initSSTP()
        VpnStatus.addStateListener(this)
        VpnStatus.addByteCountListener(this)
        binding.txtStatus.text = ""
    }

    private fun initAdMob() {
        try {
            if (dataUtil.hasAds()) {
                MobileAds.initialize(this)
                //Banner bellow
                adViewBellow = AdView(applicationContext)
                adViewBellow.adUnitId = getString(R.string.admob_banner_bellow_detail)
                adViewBellow.setAdSize(AdSize.LARGE_BANNER)
                adViewBellow.adListener = object : AdListener() {
                    override fun onAdFailedToLoad(error: LoadAdError) {
                        adViewBellow.visibility = View.GONE
                    }
                }
                binding.lnContentDetail.addView(adViewBellow)
                adViewBellow.loadAd(AdRequest.Builder().build())
            }
        } catch (e: Exception) {
            Log.e(TAG, "initAdMob error", e)
        }
    }

    public override fun onDestroy() {
        super.onDestroy()
        VpnStatus.removeStateListener(this)
        VpnStatus.removeByteCountListener(this)
        prefs.unregisterOnSharedPreferenceChangeListener(listener)
    }

    override fun setConnectedVPN(uuid: String) {
        // Do nothing
    }

    override fun updateState(
        state: String,
        logmessage: String,
        localizedResId: Int,
        status: ConnectionStatus,
        intent: Intent?
    ) {
        runOnUiThread {
            try {
                binding.txtStatus.text = VpnStatus.getLastCleanLogMessage(this)
                when (status) {
                    ConnectionStatus.LEVEL_CONNECTED -> {
                        if (isCurrent) {
                            binding.btnConnect.background = ResourcesCompat.getDrawable(
                                resources, R.drawable.selector_red_button, null
                            )
                            binding.btnConnect.text = getString(R.string.disconnect)
                            binding.txtNetStats.visibility = View.VISIBLE
                            if (isConnecting && mVpnGateConnection!!.message!!.isNotEmpty() && dataUtil.getIntSetting(
                                    DataUtil.SETTING_HIDE_OPERATOR_MESSAGE_COUNT,
                                    0
                                ) == 0
                            ) {
                                val messageDialog = MessageDialog.newInstance(
                                    mVpnGateConnection!!.message, dataUtil
                                )
                                if (!isFinishing && !isDestroyed) {
                                    messageDialog.show(
                                        supportFragmentManager,
                                        MessageDialog::class.java.name
                                    )
                                } else if (!isFinishing) {
                                    messageDialog.show(
                                        supportFragmentManager,
                                        MessageDialog::class.java.name
                                    )
                                }
                            }
                            val isStartUpDetail =
                                dataUtil.getIntSetting(DataUtil.SETTING_STARTUP_SCREEN, 0) == 0
                            OpenVPNService.setNotificationActivityClass(if (isStartUpDetail) DetailActivity::class.java else MainActivity::class.java)
                            dataUtil.setBooleanSetting(DataUtil.IS_LAST_CONNECTED_PAID, false)
                        }
                        isConnecting = false
                        isAuthFailed = false
                        binding.txtCheckIp.visibility = View.VISIBLE
                    }

                    ConnectionStatus.LEVEL_NOTCONNECTED -> if (!isConnecting && !isAuthFailed) {
                        if (!isSSTPConnected) {
                            binding.txtCheckIp.visibility = View.GONE
                        }
                        binding.btnConnect.setText(R.string.connect_to_this_server)
                        binding.btnConnect.background = ResourcesCompat.getDrawable(
                            resources, R.drawable.selector_primary_button, null
                        )
                        binding.txtStatus.setText(R.string.disconnected)
                        binding.txtNetStats.visibility = View.GONE
                    }

                    ConnectionStatus.LEVEL_AUTH_FAILED -> {
                        isAuthFailed = true
                        binding.btnConnect.text = getString(R.string.retry_connect)
                        val params = Bundle()
                        params.putString("ip", mVpnGateConnection!!.ip)
                        params.putString("hostname", mVpnGateConnection!!.calculateHostName)
                        params.putString("country", mVpnGateConnection!!.countryLong)
                        FirebaseAnalytics.getInstance(applicationContext)
                            .logEvent("Connect_Error", params)
                        binding.btnConnect.background = ResourcesCompat.getDrawable(
                            resources, R.drawable.selector_primary_button, null
                        )
                        binding.txtStatus.text = resources.getString(R.string.vpn_auth_failure)
                        binding.txtCheckIp.visibility = View.GONE
                        isConnecting = false
                    }

                    else -> binding.txtCheckIp.visibility = View.GONE
                }
                if (dataUtil.getBooleanSetting(DataUtil.USER_ALLOWED_VPN, false) && !isShowAds) {
                    loadAds()
                }
            } catch (e: Exception) {
                Log.e(TAG, "UpdateState error", e)
            }
        }
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun bindData() {
        if (mVpnGateConnection != null) {
            try {
                Glide.with(this)
                    .load(dataUtil.baseUrl + "/images/flags/" + mVpnGateConnection!!.countryShort + ".png")
                    .placeholder(R.color.colorOverlay)
                    .error(R.color.colorOverlay)
                    .into(binding.imgFlag)
                binding.txtCountry.text = mVpnGateConnection!!.countryLong
                binding.txtIp.text = mVpnGateConnection!!.ip
                binding.txtHostname.text = mVpnGateConnection!!.calculateHostName
                binding.txtScore.text = mVpnGateConnection!!.scoreAsString
                binding.txtUptime.text = mVpnGateConnection!!.getCalculateUpTime(applicationContext)
                binding.txtSpeed.text = mVpnGateConnection!!.calculateSpeed
                binding.txtPing.text = mVpnGateConnection!!.pingAsString
                binding.txtSession.text = mVpnGateConnection!!.numVpnSessionAsString
                binding.txtOwner.text = mVpnGateConnection!!.operator
                binding.txtTotalUser.text = mVpnGateConnection!!.totalUser.toString()
                binding.txtTotalTraffic.text = mVpnGateConnection!!.calculateTotalTraffic
                binding.txtLogType.text = mVpnGateConnection!!.logType
                val isIncludeUDP =
                    dataUtil.getBooleanSetting(DataUtil.INCLUDE_UDP_SERVER, true)
                if (!isIncludeUDP || mVpnGateConnection!!.tcpPort == 0) {
                    binding.lnTcp.visibility = View.GONE
                } else {
                    binding.txtTcpPort.text = mVpnGateConnection!!.tcpPort.toString()
                }
                if (!isIncludeUDP || mVpnGateConnection!!.udpPort == 0) {
                    binding.lnUdp.visibility = View.GONE
                } else {
                    binding.txtUdpPort.text = mVpnGateConnection!!.udpPort.toString()
                }
                if (mVpnGateConnection!!.isL2TPSupport()) {
                    binding.lnTcp.visibility = View.VISIBLE
                    binding.lnL2tpBtn.visibility = View.VISIBLE
                } else {
                    binding.lnL2tp.visibility = View.GONE
                    binding.lnL2tpBtn.visibility = View.GONE
                }

                if (mVpnGateConnection!!.isSSTPSupport()) {
                    binding.lnSstp.visibility = View.VISIBLE
                    binding.lnSstpBtn.visibility = View.VISIBLE
                } else {
                    binding.lnSstp.visibility = View.GONE
                    binding.lnSstpBtn.visibility = View.GONE
                }

                if (isCurrent && checkStatus()) {
                    binding.btnConnect.text = resources.getString(R.string.disconnect)
                    binding.btnConnect.background =
                        resources.getDrawable(R.drawable.selector_apply_button, resources.newTheme())
                    binding.txtStatus.text = VpnStatus.getLastCleanLogMessage(this)
                    binding.txtNetStats.visibility = View.VISIBLE
                } else {
                    binding.txtNetStats.visibility = View.GONE
                }
                if (checkStatus()) {
                    binding.txtCheckIp.visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                Log.e(TAG, "bindData error", e)
            }
        }
    }

    private val isCurrent: Boolean
        get() {
            val vpnGateConnection = dataUtil.lastVPNConnection
            return vpnGateConnection != null && mVpnGateConnection != null && vpnGateConnection.name == mVpnGateConnection!!.name
        }

    public override fun onResume() {
        super.onResume()
        try {
            Handler(Looper.getMainLooper()).postDelayed({
                val intent = Intent(this, OpenVPNService::class.java)
                OpenVPNService.mDisplaySpeed =
                    dataUtil.getBooleanSetting(DataUtil.SETTING_NOTIFY_SPEED, true)
                intent.action = OpenVPNService.START_SERVICE
                bindService(intent, mConnection, BIND_AUTO_CREATE)
            }, 300)
            if (!App.isImportToOpenVPN) {
                binding.btnInstallOpenvpn.visibility = View.GONE
                binding.btnSaveConfigFile.visibility = View.GONE
                binding.btnConnect.visibility = View.VISIBLE
            } else {
                binding.btnConnect.visibility = View.GONE
                if (dataUtil.hasOpenVPNInstalled()) {
                    binding.btnSaveConfigFile.visibility = View.VISIBLE
                    binding.btnInstallOpenvpn.visibility = View.GONE
                } else {
                    binding.btnSaveConfigFile.visibility = View.GONE
                    binding.btnInstallOpenvpn.visibility = View.VISIBLE
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "onResume error", e)
        }
    }

    public override fun onPause() {
        try {
            super.onPause()
            TotalTraffic.saveTotal(this)
            unbindService(mConnection)
        } catch (e: Exception) {
            Log.e(TAG, "onPause error", e)
        }
    }

    private fun handleImport(useUdp: Boolean) {
        loadAds()
        val data = if (useUdp) {
            mVpnGateConnection!!.openVpnConfigDataUdp
        } else {
            mVpnGateConnection!!.getOpenVpnConfigDataString()
        }
        try {
            while (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    100
                )
            }
            val fileName = mVpnGateConnection!!.getName(useUdp) + ".ovpn"
            val writeFile = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                fileName
            )
            val fileOutputStream = FileOutputStream(writeFile)
            fileOutputStream.write(data!!.toByteArray())
            Toast.makeText(
                applicationContext,
                getString(R.string.saved_ovpn_file_in, "Download/$fileName"),
                Toast.LENGTH_LONG
            ).show()
            val handler = Handler(Looper.getMainLooper())
            handler.postDelayed({
                val packageManager = packageManager
                val intent = packageManager.getLaunchIntentForPackage("net.openvpn.openvpn")
                if (intent != null) {
                    intent.action = Intent.ACTION_VIEW
                    startActivity(intent)
                }
            }, 500)
        } catch (e: Exception) {
            Log.e(TAG, "handleImport error", e)
        }
    }

    private fun handleConnection(useUdp: Boolean) {
        loadAds()
        if (isSSTPConnected) {
            startVpnSSTPService(ACTION_VPN_DISCONNECT)
        }
        if (checkStatus()) {
            stopVpn()
            val params = Bundle()
            params.putString("type", "replace current")
            params.putString("hostname", mVpnGateConnection!!.calculateHostName)
            params.putString("ip", mVpnGateConnection!!.ip)
            params.putString("country", mVpnGateConnection!!.countryLong)
            FirebaseAnalytics.getInstance(applicationContext).logEvent("Connect_VPN", params)
            binding.txtCheckIp.visibility = View.GONE
            Handler(Looper.getMainLooper()).postDelayed({ prepareVpn(useUdp) }, 500)
        } else {
            val params = Bundle()
            params.putString("type", "connect new")
            params.putString("hostname", mVpnGateConnection!!.calculateHostName)
            params.putString("ip", mVpnGateConnection!!.ip)
            params.putString("country", mVpnGateConnection!!.countryLong)
            FirebaseAnalytics.getInstance(applicationContext).logEvent("Connect_VPN", params)
            prepareVpn(useUdp)
        }
        binding.btnConnect.background = ResourcesCompat.getDrawable(
            resources,
            R.drawable.selector_apply_button,
            null
        )
        binding.txtStatus.text = getString(R.string.connecting)
        isConnecting = true
        binding.btnConnect.setText(R.string.cancel)
        dataUtil.lastVPNConnection = mVpnGateConnection
        sendConnectVPN()
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    override fun onClick(view: View) {
        try {
            if (view == binding.btnBack) {
                finish()
                return
            }
            if (view == binding.btnConnect) {
                if (!isConnecting) {
                    if (checkStatus() && isCurrent) {
                        val params = Bundle()
                        params.putString("type", "disconnect current")
                        params.putString("hostname", mVpnGateConnection!!.calculateHostName)
                        params.putString("ip", mVpnGateConnection!!.ip)
                        params.putString("country", mVpnGateConnection!!.countryLong)
                        FirebaseAnalytics.getInstance(applicationContext)
                            .logEvent("Disconnect_VPN", params)
                        stopVpn()
                        binding.btnConnect.background =
                            resources.getDrawable(R.drawable.selector_primary_button, resources.newTheme())
                        binding.btnConnect.setText(R.string.connect_to_this_server)
                        binding.txtStatus.setText(R.string.disconnecting)
                    } else if (mVpnGateConnection!!.tcpPort > 0 && mVpnGateConnection!!.udpPort > 0) {
                        if (dataUtil.getIntSetting(DataUtil.SETTING_DEFAULT_PROTOCOL, 0) != 0) {
                            // Apply default protocol in setting
                            var protocol = "TCP"
                            if (dataUtil.getIntSetting(
                                    DataUtil.SETTING_DEFAULT_PROTOCOL,
                                    0
                                ) == 1
                            ) {
                                handleConnection(false)
                            } else {
                                protocol = "UDP"
                                handleConnection(true)
                            }
                            Toast.makeText(
                                this,
                                getString(R.string.connecting_use_protocol, protocol),
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            val connectionUseProtocol =
                                ConnectionUseProtocol.newInstance(mVpnGateConnection, object : ConnectionUseProtocol.ClickResult {
                                    override fun onResult(useUdp: Boolean) {
                                        handleConnection(useUdp)
                                    }
                                })
                            if (!isFinishing && !isDestroyed) {
                                connectionUseProtocol.show(
                                    supportFragmentManager,
                                    ConnectionUseProtocol::class.java.name
                                )
                            } else if (!isFinishing) {
                                connectionUseProtocol.show(
                                    supportFragmentManager,
                                    ConnectionUseProtocol::class.java.name
                                )
                            }
                        }
                    } else {
                        handleConnection(false)
                    }
                } else {
                    val params = Bundle()
                    params.putString("type", "cancel connect to vpn")
                    params.putString("hostname", mVpnGateConnection!!.calculateHostName)
                    params.putString("ip", mVpnGateConnection!!.ip)
                    params.putString("country", mVpnGateConnection!!.countryLong)
                    FirebaseAnalytics.getInstance(applicationContext).logEvent("Cancel_VPN", params)
                    stopVpn()
                    binding.btnConnect.background =
                        resources.getDrawable(R.drawable.selector_primary_button, resources.newTheme())
                    binding.btnConnect.setText(R.string.connect_to_this_server)
                    binding.txtStatus.text = getString(R.string.canceled)
                    isConnecting = false
                }
            } else if (view == binding.txtCheckIp) {
                val params = Bundle()
                params.putString("type", "check ip click")
                params.putString("hostname", mVpnGateConnection!!.calculateHostName)
                params.putString("ip", mVpnGateConnection!!.ip)
                params.putString("country", mVpnGateConnection!!.countryLong)
                FirebaseAnalytics.getInstance(applicationContext).logEvent("Click_Check_IP", params)
                val browserIntent = Intent(
                    Intent.ACTION_VIEW,
                    FirebaseRemoteConfig.getInstance().getString("vpn_check_ip_url").toUri()
                )
                startActivity(browserIntent)
            } else if (view == binding.btnL2tpConnect) {
                val params = Bundle()
                params.putString("type", "connect via L2TP")
                params.putString("hostname", mVpnGateConnection!!.calculateHostName)
                params.putString("ip", mVpnGateConnection!!.ip)
                params.putString("country", mVpnGateConnection!!.countryLong)
                FirebaseAnalytics.getInstance(applicationContext)
                    .logEvent("Connect_Via_L2TP", params)
                loadAds()
                val l2tpIntent = Intent(this, L2TPConnectActivity::class.java)
                l2tpIntent.putExtra(BaseProvider.PASS_DETAIL_VPN_CONNECTION, mVpnGateConnection)
                startActivity(l2tpIntent)
            }
            if (view == binding.btnSstpConnect) {
                handleSSTPBtn()
            }
            if (view == binding.btnInstallOpenvpn) {
                try {
                    startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            "market://details?id=net.openvpn.openvpn".toUri()
                        )
                    )
                } catch (_: ActivityNotFoundException) {
                    startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            "https://play.google.com/store/apps/details?id=net.openvpn.openvpn".toUri()
                        )
                    )
                }
            }
            if (view == binding.btnSaveConfigFile) {
                if (mVpnGateConnection!!.tcpPort > 0 && mVpnGateConnection!!.udpPort > 0) {
                    val connectionUseProtocol =
                        ConnectionUseProtocol.newInstance(mVpnGateConnection, object: ConnectionUseProtocol.ClickResult {
                            override fun onResult(useUdp: Boolean) {
                                handleImport(useUdp)
                            }
                        })
                    if (!isFinishing && !isDestroyed) {
                        connectionUseProtocol.show(
                            supportFragmentManager,
                            ConnectionUseProtocol::class.java.name
                        )
                    } else if (!isFinishing) {
                        connectionUseProtocol.show(
                            supportFragmentManager,
                            ConnectionUseProtocol::class.java.name
                        )
                    }
                } else {
                    handleImport(false)
                }
            }
            if (view == binding.btnExcludeApps) {
                excludeAppsManager.openExcludeAppsManager(supportFragmentManager)
            }
        } catch (e: Exception) {
            Log.e(TAG, "onClick error", e)
        }
    }

    private fun connectSSTPVPN() {
        val excludedApps = App.instance?.excludedAppDao?.getAllExcludedApps() ?: emptyList()
        val excludedPackageNames = excludedApps.map { it.packageName }.toSet()

        prefs.edit {
            putString(
                OscPrefKey.HOME_HOSTNAME.toString(),
                mVpnGateConnection!!.calculateHostName
            )
            putString(
                OscPrefKey.HOME_COUNTRY.toString(),
                mVpnGateConnection!!.countryShort!!.uppercase()
            )
            putString(OscPrefKey.HOME_USERNAME.toString(), "vpn")
            putString(OscPrefKey.HOME_PASSWORD.toString(), "vpn")
            putString(OscPrefKey.SSL_PORT.toString(), mVpnGateConnection!!.tcpPort.toString())
            putStringSet(OscPrefKey.ROUTE_EXCLUDED_APPS.toString(), excludedPackageNames)
        }
        binding.btnSstpConnect.background = ResourcesCompat.getDrawable(
            resources,
            R.drawable.selector_apply_button,
            null
        )
        binding.btnSstpConnect.setText(R.string.cancel_sstp)
        binding.txtStatus.setText(R.string.sstp_connecting)
        loadAds()
        startVpnSSTPService(ACTION_VPN_CONNECT)
    }

    private val startActivityIntentSSTPVPN: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        handleActivityResult(START_VPN_SSTP, it.resultCode)
    }

    private fun startSSTPVPN() {
        if (checkStatus()) {
            stopVpn()
        }
        val intent = VpnService.prepare(this)

        if (intent != null) {
            try {
                startActivityIntentSSTPVPN.launch(intent)
            } catch (_: ActivityNotFoundException) {
                Log.e(TAG, "OS does not support VPN")
            }
        } else {
            handleActivityResult(START_VPN_SSTP, RESULT_OK)
        }
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
            binding.txtCheckIp.visibility = View.GONE
            Handler(mainLooper).postDelayed({ this.connectSSTPVPN() }, 100)
        } else if (!isSSTPConnected) {
            params.putString("type", "connect via MS-SSTP")
            FirebaseAnalytics.getInstance(applicationContext).logEvent("Connect_Via_SSTP", params)
            dataUtil.lastVPNConnection = mVpnGateConnection
            startSSTPVPN()
        } else {
            params.putString("type", "cancel MS-SSTP")
            FirebaseAnalytics.getInstance(applicationContext).logEvent("Cancel_Via_SSTP", params)
            startVpnSSTPService(ACTION_VPN_DISCONNECT)
            binding.btnSstpConnect.background = ResourcesCompat.getDrawable(
                resources,
                R.drawable.selector_paid_button,
                null
            )
            binding.btnSstpConnect.setText(R.string.connect_via_sstp)
            binding.txtStatus.setText(R.string.sstp_disconnecting)
        }
    }

    private fun initInterstitialAd() {
        if (dataUtil.hasAds()) {
            try {
                val adRequest = AdRequest.Builder().build()
                InterstitialAd.load(
                    applicationContext,
                    getString(R.string.admob_full_screen_connect),
                    adRequest,
                    object : InterstitialAdLoadCallback() {
                        override fun onAdLoaded(interstitialAd: InterstitialAd) {
                            isFullScreenAdLoaded = true
                            mInterstitialAd = interstitialAd
                            Log.e(TAG, "Full screen ads loaded")
                        }

                        override fun onAdFailedToLoad(var1: LoadAdError) {
                            isFullScreenAdLoaded = false
                            mInterstitialAd = null
                            Log.e(TAG, String.format("Full screen ads failed to load %s", var1))
                        }
                    })
            } catch (e: Exception) {
                Log.e(TAG, "initInterstitialAd error", e)
            }
        }
    }

    private fun loadAds() {
        try {
            if (dataUtil.hasAds() && dataUtil.getBooleanSetting(
                    DataUtil.USER_ALLOWED_VPN,
                    false
                ) && isFullScreenAdLoaded
            ) {
                isShowAds = true
                if (mInterstitialAd != null) {
                    mInterstitialAd!!.show(this)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "loadAds error", e)
        }
    }

    private fun sendConnectVPN() {
        val intent = Intent(BaseProvider.ACTION.ACTION_CONNECT_VPN)
        sendBroadcast(intent)
    }

    private fun prepareVpn(useUdp: Boolean) {
        if (loadVpnProfile(useUdp)) {
            startOpenVpn()
        } else {
            Toast.makeText(this, getString(R.string.error_load_profile), Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadVpnProfile(useUDP: Boolean): Boolean {
        val data = if (useUDP) {
            mVpnGateConnection!!.openVpnConfigDataUdp!!.toByteArray()
        } else {
            mVpnGateConnection!!.getOpenVpnConfigDataString()!!.toByteArray()
        }
        dataUtil.setBooleanSetting(DataUtil.LAST_CONNECT_USE_UDP, useUDP)
        val cp = ConfigParser()
        val isr = InputStreamReader(ByteArrayInputStream(data))
        try {
            cp.parseConfig(isr)
            vpnProfile = cp.convertProfile()
            vpnProfile.mName = mVpnGateConnection!!.getName(useUDP)
            vpnProfile.mCompatMode = App.VPN_PROFILE_COMPAT_MODE_24X
            if (dataUtil.getBooleanSetting(DataUtil.SETTING_BLOCK_ADS, false)) {
                vpnProfile.mOverrideDNS = true
                vpnProfile.mDNS1 = FirebaseRemoteConfig.getInstance()
                    .getString(getString(R.string.dns_block_ads_primary_cfg_key))
                vpnProfile.mDNS2 = FirebaseRemoteConfig.getInstance()
                    .getString(getString(R.string.dns_block_ads_alternative_cfg_key))
            } else if (dataUtil.getBooleanSetting(DataUtil.USE_CUSTOM_DNS, false)) {
                vpnProfile.mOverrideDNS = true
                vpnProfile.mDNS1 = dataUtil.getStringSetting(DataUtil.CUSTOM_DNS_IP_1, "8.8.8.8")
                val dns2 = dataUtil.getStringSetting(DataUtil.CUSTOM_DNS_IP_2, null)
                if (dns2 != null) {
                    vpnProfile.mDNS2 = dns2
                }
            }
            // Configure split tunneling - exclude apps from VPN
            excludeAppsManager.configureSplitTunneling(vpnProfile)
            ProfileManager.setTemporaryProfile(applicationContext, vpnProfile)
        } catch (e: IOException) {
            Log.e(TAG, "loadVpnProfile error", e)
            return false
        } catch (e: ConfigParseError) {
            Log.e(TAG, "loadVpnProfile error", e)
            return false
        }

        return true
    }

    private fun checkStatus(): Boolean {
        try {
            return VpnStatus.isVPNActive()
        } catch (e: Exception) {
            Log.e(TAG, "checkStatus error", e)
        }

        return false
    }

    private fun stopVpn() {
        //prepareStopVPN();
        ProfileManager.setConntectedVpnProfileDisconnected(this)
        if (mVPNService != null) {
            try {
                mVPNService!!.stopVPN(false)
            } catch (e: RemoteException) {
                VpnStatus.logException(e)
            }
        }
    }

    private val startActivityIntentOpenVPN: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        handleActivityResult(START_VPN_PROFILE, it.resultCode)
    }

    private fun startOpenVpn() {
        val intent = VpnService.prepare(this)

        if (intent != null) {
            VpnStatus.updateStateString(
                "USER_VPN_PERMISSION", "", R.string.state_user_vpn_permission,
                ConnectionStatus.LEVEL_WAITING_FOR_USER_INPUT
            )
            // Start the query
            try {
                startActivityIntentOpenVPN.launch(intent)
            } catch (_: ActivityNotFoundException) {
                // Shame on you Sony! At least one user reported that
                // an official Sony Xperia Arc S image triggers this exception
                VpnStatus.logError(de.blinkt.openvpn.R.string.no_vpn_support_image)
            }
        } else {
            handleActivityResult(START_VPN_PROFILE, RESULT_OK)
        }
    }

    private fun handleActivityResult(requestCode: Int, resultCode: Int) {
        try {
            if (resultCode == RESULT_OK) {
                if (requestCode == START_VPN_PROFILE) {
                    VPNLaunchHelper.startOpenVpn(vpnProfile, baseContext, null, true)
                }
                if (requestCode == START_VPN_SSTP) {
                    connectSSTPVPN()
                }
                NotificationUtil(this).requestPermission()
                dataUtil.setBooleanSetting(DataUtil.USER_ALLOWED_VPN, true)
            } else {
                dataUtil.setBooleanSetting(DataUtil.USER_ALLOWED_VPN, false)
            }
        } catch (e: Exception) {
            Log.e(TAG, "handleActivityResult error", e)
        }
    }

    override fun updateByteCount(`in`: Long, out: Long, diffIn: Long, diffOut: Long) {
        if (!isCurrent) {
            return
        }
        runOnUiThread {
            val netstat = String.format(
                getString(de.blinkt.openvpn.R.string.statusline_bytecount),
                OpenVPNService.humanReadableByteCount(`in`, false, resources),
                OpenVPNService.humanReadableByteCount(
                    diffIn / OpenVPNManagement.mBytecountInterval,
                    true,
                    resources
                ),
                OpenVPNService.humanReadableByteCount(out, false, resources),
                OpenVPNService.humanReadableByteCount(
                    diffOut / OpenVPNManagement.mBytecountInterval,
                    true,
                    resources
                )
            )
            binding.txtNetStats.text = netstat
        }
    }

    companion object {
        const val TYPE_FROM_NOTIFY: Int = 1001
        const val TYPE_NORMAL: Int = 1000
        const val TYPE_START: String = "vn.ulimit.vpngate.TYPE_START"
        const val START_VPN_PROFILE: Int = 70
        const val START_VPN_SSTP: Int = 80
        const val ACTION_VPN_CONNECT: String = "kittoku.osc.connect"
        const val ACTION_VPN_DISCONNECT: String = "kittoku.osc.disconnect"
        private const val TAG = "DetailActivity"
        private var mVPNService: IOpenVPNServiceInternal? = null
    }
}
