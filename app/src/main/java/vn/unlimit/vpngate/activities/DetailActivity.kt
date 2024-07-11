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
import android.net.Uri
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
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
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
import vn.unlimit.vpngate.dialog.ConnectionUseProtocol
import vn.unlimit.vpngate.dialog.MessageDialog
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
    private lateinit var imgFlag: ImageView
    private lateinit var txtCountry: TextView
    private lateinit var txtIp: TextView
    private lateinit var txtHostname: TextView
    private lateinit var txtScore: TextView
    private lateinit var txtUptime: TextView
    private lateinit var txtSpeed: TextView
    private lateinit var txtPing: TextView
    private lateinit var txtSession: TextView
    private lateinit var txtOwner: TextView
    private lateinit var txtTotalUser: TextView
    private lateinit var txtTotalTraffic: TextView
    private lateinit var txtLogType: TextView
    private lateinit var txtStatus: TextView
    private lateinit var linkCheckIp: View
    private lateinit var lnContentDetail: LinearLayout
    private lateinit var lnTCP: View
    private lateinit var txtTCP: TextView
    private lateinit var lnUDP: View
    private lateinit var txtUDP: TextView
    private lateinit var lnL2TP: View
    private lateinit var getLnL2TPBtn: View
    private lateinit var btnConnectL2TP: Button
    private lateinit var lnSSTP: View
    private lateinit var lnSTTPBtn: View
    private lateinit var btnConnectSSTP: Button
    private lateinit var dataUtil: DataUtil
    private var mVpnGateConnection: VPNGateConnection? = null
    private lateinit var btnConnect: Button
    private lateinit var btnBack: View
    private lateinit var vpnProfile: VpnProfile
    private var mInterstitialAd: InterstitialAd? = null
    private lateinit var adView: AdView
    private lateinit var adViewBellow: AdView
    private lateinit var btnInstallOpenVpn: View
    private lateinit var btnSaveConfigFile: View
    private lateinit var txtNetStats: TextView
    private lateinit var prefs: SharedPreferences
    private lateinit var listener: OnSharedPreferenceChangeListener
    private var isConnecting = false
    private var isAuthFailed = false
    private var isShowAds = false
    private var isSSTPConnectOrDisconnecting = false
    private var isSSTPConnected = false
    private var isFullScreenAdLoaded = false

    private fun checkConnectionData() {
        if (mVpnGateConnection == null) {
            //Start main
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

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
                        btnConnectSSTP.background = ResourcesCompat.getDrawable(
                            resources, R.drawable.selector_paid_button, null
                        )
                        btnConnectSSTP.setText(R.string.connect_via_sstp)
                        if (isSSTPConnectOrDisconnecting) {
                            txtStatus.setText(R.string.sstp_disconnected)
                        } else {
                            txtStatus.setText(R.string.sstp_disconnected_by_error)
                        }
                        isSSTPConnected = false
                        linkCheckIp.visibility = View.GONE
                    }
                    isSSTPConnectOrDisconnecting = false
                }
                if (OscPrefKey.HOME_CONNECTED_IP.toString() == key) {
                    val connectedIp = prefs.getString(OscPrefKey.HOME_CONNECTED_IP.toString(), "")
                    if (connectedIp!!.isNotEmpty()) {
                        btnConnectSSTP.background = ResourcesCompat.getDrawable(
                            resources, R.drawable.selector_red_button, null
                        )
                        btnConnectSSTP.setText(R.string.disconnect_sstp)
                        txtStatus.text = getString(R.string.sstp_connected, connectedIp)
                        isSSTPConnected = true
                        linkCheckIp.visibility = View.VISIBLE
                    }
                }
            }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        isSSTPConnected = prefs.getBoolean(OscPrefKey.ROOT_STATE.toString(), false)
        val sstpHostName = prefs.getString(OscPrefKey.HOME_HOSTNAME.toString(), "")
        if (isSSTPConnected) {
            linkCheckIp.visibility = View.VISIBLE
            if (sstpHostName == mVpnGateConnection!!.calculateHostName) {
                btnConnectSSTP.background = ResourcesCompat.getDrawable(
                    resources, R.drawable.selector_red_button, null
                )
                btnConnectSSTP.setText(R.string.disconnect_sstp)
            }
        }
    }

    @SuppressLint("SetTextI18n")
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dataUtil = (application as App).dataUtil!!
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
            mVpnGateConnection = (if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(
                    BaseProvider.PASS_DETAIL_VPN_CONNECTION
                )
            } else intent.getParcelableExtra(
                BaseProvider.PASS_DETAIL_VPN_CONNECTION,
                VPNGateConnection::class.java
            ))
        }
        checkConnectionData()
        setContentView(R.layout.activity_detail)
        btnConnect = findViewById(R.id.btn_connect)
        btnSaveConfigFile = findViewById(R.id.btn_save_config_file)
        btnSaveConfigFile.setOnClickListener(this)
        btnInstallOpenVpn = findViewById(R.id.btn_install_openvpn)
        btnInstallOpenVpn.setOnClickListener(this)
        btnBack = findViewById(R.id.btn_back)
        btnBack.setOnClickListener(this)
        btnConnect.setOnClickListener(this)
        imgFlag = findViewById(R.id.img_flag)
        txtCountry = findViewById(R.id.txt_country)
        txtIp = findViewById(R.id.txt_ip)
        txtHostname = findViewById(R.id.txt_hostname)
        txtScore = findViewById(R.id.txt_score)
        txtUptime = findViewById(R.id.txt_uptime)
        txtSpeed = findViewById(R.id.txt_speed)
        txtPing = findViewById(R.id.txt_ping)
        txtSession = findViewById(R.id.txt_session)
        txtOwner = findViewById(R.id.txt_owner)
        txtTotalUser = findViewById(R.id.txt_total_user)
        txtTotalTraffic = findViewById(R.id.txt_total_traffic)
        txtLogType = findViewById(R.id.txt_log_type)
        txtStatus = findViewById(R.id.txt_status)
        linkCheckIp = findViewById(R.id.txt_check_ip)
        linkCheckIp.setOnClickListener(this)
        lnContentDetail = findViewById(R.id.ln_content_detail)
        lnTCP = findViewById(R.id.ln_tcp)
        txtTCP = findViewById(R.id.txt_tcp_port)
        lnUDP = findViewById(R.id.ln_udp)
        txtUDP = findViewById(R.id.txt_udp_port)
        lnL2TP = findViewById(R.id.ln_l2tp)
        getLnL2TPBtn = findViewById(R.id.ln_l2tp_btn)
        btnConnectL2TP = findViewById(R.id.btn_l2tp_connect)
        btnConnectL2TP.setOnClickListener(this)
        lnSSTP = findViewById(R.id.ln_sstp)
        lnSTTPBtn = findViewById(R.id.ln_sstp_btn)
        btnConnectSSTP = findViewById(R.id.btn_sstp_connect)
        btnConnectSSTP.setOnClickListener(this)
        txtNetStats = findViewById(R.id.txt_net_stats)
        bindData()
        initAdMob()
        initInterstitialAd()
        initSSTP()
        VpnStatus.addStateListener(this)
        VpnStatus.addByteCountListener(this)
        txtStatus.text = ""
    }

    private fun initAdMob() {
        try {
            if (dataUtil.hasAds()) {
                MobileAds.initialize(this)
                adView = AdView(applicationContext)
                adView.setAdSize(AdSize.LARGE_BANNER)
                adView.adUnitId = resources.getString(R.string.admob_banner_bottom_detail)
                adView.adListener = object : AdListener() {
                    override fun onAdFailedToLoad(error: LoadAdError) {
                        hideAdContainer()
                        Log.e(TAG, error.toString())
                    }
                }
                val params = RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.WRAP_CONTENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT
                )
                params.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE)
                params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE)
                adView.layoutParams = params
                (findViewById<View>(R.id.ad_container_detail) as RelativeLayout).addView(adView)
                adView.loadAd(AdRequest.Builder().build())
                //Banner bellow
                adViewBellow = AdView(applicationContext)
                adViewBellow.adUnitId = getString(R.string.admob_banner_bellow_detail)
                adViewBellow.setAdSize(AdSize.MEDIUM_RECTANGLE)
                adViewBellow.adListener = object : AdListener() {
                    override fun onAdFailedToLoad(error: LoadAdError) {
                        adViewBellow.visibility = View.GONE
                    }
                }
                lnContentDetail.addView(adViewBellow)
                adViewBellow.loadAd(AdRequest.Builder().build())
            } else {
                hideAdContainer()
            }
        } catch (e: Exception) {
            Log.e(TAG, "initAdMob error", e)
        }
    }

    private fun hideAdContainer() {
        try {
            findViewById<View>(R.id.ad_container_detail).visibility = View.GONE
            adView.visibility = View.GONE
        } catch (e: Exception) {
            Log.e(TAG, "hideAdContainer error", e)
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
                txtStatus.text = VpnStatus.getLastCleanLogMessage(this)
                when (status) {
                    ConnectionStatus.LEVEL_CONNECTED -> {
                        if (isCurrent) {
                            btnConnect.background = ResourcesCompat.getDrawable(
                                resources, R.drawable.selector_red_button, null
                            )
                            btnConnect.text = getString(R.string.disconnect)
                            txtNetStats.visibility = View.VISIBLE
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
                        linkCheckIp.visibility = View.VISIBLE
                    }

                    ConnectionStatus.LEVEL_NOTCONNECTED -> if (!isConnecting && !isAuthFailed) {
                        if (!isSSTPConnected) {
                            linkCheckIp.visibility = View.GONE
                        }
                        btnConnect.setText(R.string.connect_to_this_server)
                        btnConnect.background = ResourcesCompat.getDrawable(
                            resources, R.drawable.selector_primary_button, null
                        )
                        txtStatus.setText(R.string.disconnected)
                        txtNetStats.visibility = View.GONE
                    }

                    ConnectionStatus.LEVEL_AUTH_FAILED -> {
                        isAuthFailed = true
                        btnConnect.text = getString(R.string.retry_connect)
                        val params = Bundle()
                        params.putString("ip", mVpnGateConnection!!.ip)
                        params.putString("hostname", mVpnGateConnection!!.calculateHostName)
                        params.putString("country", mVpnGateConnection!!.countryLong)
                        FirebaseAnalytics.getInstance(applicationContext)
                            .logEvent("Connect_Error", params)
                        btnConnect.background = ResourcesCompat.getDrawable(
                            resources, R.drawable.selector_primary_button, null
                        )
                        txtStatus.text = resources.getString(R.string.vpn_auth_failure)
                        linkCheckIp.visibility = View.GONE
                        isConnecting = false
                    }

                    else -> linkCheckIp.visibility = View.GONE
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
                    .into(imgFlag)
                txtCountry.text = mVpnGateConnection!!.countryLong
                txtIp.text = mVpnGateConnection!!.ip
                txtHostname.text = mVpnGateConnection!!.calculateHostName
                txtScore.text = mVpnGateConnection!!.scoreAsString
                txtUptime.text = mVpnGateConnection!!.getCalculateUpTime(applicationContext)
                txtSpeed.text = mVpnGateConnection!!.calculateSpeed
                txtPing.text = mVpnGateConnection!!.pingAsString
                txtSession.text = mVpnGateConnection!!.numVpnSessionAsString
                txtOwner.text = mVpnGateConnection!!.operator
                txtTotalUser.text = mVpnGateConnection!!.totalUser.toString()
                txtTotalTraffic.text = mVpnGateConnection!!.calculateTotalTraffic
                txtLogType.text = mVpnGateConnection!!.logType
                val isIncludeUDP =
                    dataUtil.getBooleanSetting(DataUtil.INCLUDE_UDP_SERVER, true)
                if (!isIncludeUDP || mVpnGateConnection!!.tcpPort == 0) {
                    lnTCP.visibility = View.GONE
                } else {
                    txtTCP.text = mVpnGateConnection!!.tcpPort.toString()
                }
                if (!isIncludeUDP || mVpnGateConnection!!.udpPort == 0) {
                    lnUDP.visibility = View.GONE
                } else {
                    txtUDP.text = mVpnGateConnection!!.udpPort.toString()
                }
                if (mVpnGateConnection!!.isL2TPSupport()) {
                    lnL2TP.visibility = View.VISIBLE
                    getLnL2TPBtn.visibility = View.VISIBLE
                } else {
                    lnL2TP.visibility = View.GONE
                    getLnL2TPBtn.visibility = View.GONE
                }

                if (mVpnGateConnection!!.isSSTPSupport()) {
                    lnSSTP.visibility = View.VISIBLE
                    lnSTTPBtn.visibility = View.VISIBLE
                } else {
                    lnSSTP.visibility = View.GONE
                    lnSTTPBtn.visibility = View.GONE
                }

                if (isCurrent && checkStatus()) {
                    btnConnect.text = resources.getString(R.string.disconnect)
                    btnConnect.background =
                        resources.getDrawable(R.drawable.selector_apply_button, resources.newTheme())
                    txtStatus.text = VpnStatus.getLastCleanLogMessage(this)
                    txtNetStats.visibility = View.VISIBLE
                } else {
                    txtNetStats.visibility = View.GONE
                }
                if (checkStatus()) {
                    linkCheckIp.visibility = View.VISIBLE
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
                intent.setAction(OpenVPNService.START_SERVICE)
                bindService(intent, mConnection, BIND_AUTO_CREATE)
            }, 300)
            if (!App.isImportToOpenVPN) {
                btnInstallOpenVpn.visibility = View.GONE
                btnSaveConfigFile.visibility = View.GONE
                btnConnect.visibility = View.VISIBLE
            } else {
                btnConnect.visibility = View.GONE
                if (dataUtil.hasOpenVPNInstalled()) {
                    btnSaveConfigFile.visibility = View.VISIBLE
                    btnInstallOpenVpn.visibility = View.GONE
                } else {
                    btnSaveConfigFile.visibility = View.GONE
                    btnInstallOpenVpn.visibility = View.VISIBLE
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
                    intent.setAction(Intent.ACTION_VIEW)
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
            linkCheckIp.visibility = View.GONE
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
        btnConnect.background = ResourcesCompat.getDrawable(
            resources,
            R.drawable.selector_apply_button,
            null
        )
        txtStatus.text = getString(R.string.connecting)
        isConnecting = true
        btnConnect.setText(R.string.cancel)
        dataUtil.lastVPNConnection = mVpnGateConnection
        sendConnectVPN()
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    override fun onClick(view: View) {
        try {
            if (view == btnBack) {
                finish()
                return
            }
            if (view == btnConnect) {
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
                        btnConnect.background =
                            resources.getDrawable(R.drawable.selector_primary_button, resources.newTheme())
                        btnConnect.setText(R.string.connect_to_this_server)
                        txtStatus.setText(R.string.disconnecting)
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
                    btnConnect.background =
                        resources.getDrawable(R.drawable.selector_primary_button, resources.newTheme())
                    btnConnect.setText(R.string.connect_to_this_server)
                    txtStatus.text = getString(R.string.canceled)
                    isConnecting = false
                }
            } else if (view == linkCheckIp) {
                val params = Bundle()
                params.putString("type", "check ip click")
                params.putString("hostname", mVpnGateConnection!!.calculateHostName)
                params.putString("ip", mVpnGateConnection!!.ip)
                params.putString("country", mVpnGateConnection!!.countryLong)
                FirebaseAnalytics.getInstance(applicationContext).logEvent("Click_Check_IP", params)
                val browserIntent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(FirebaseRemoteConfig.getInstance().getString("vpn_check_ip_url"))
                )
                startActivity(browserIntent)
            } else if (view == btnConnectL2TP) {
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
            if (view == btnConnectSSTP) {
                handleSSTPBtn()
            }
            if (view == btnInstallOpenVpn) {
                try {
                    startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("market://details?id=net.openvpn.openvpn")
                        )
                    )
                } catch (ex: ActivityNotFoundException) {
                    startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://play.google.com/store/apps/details?id=net.openvpn.openvpn")
                        )
                    )
                }
            }
            if (view == btnSaveConfigFile) {
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
        } catch (e: Exception) {
            Log.e(TAG, "onClick error", e)
        }
    }

    private fun connectSSTPVPN() {
        val editor = prefs.edit()
        editor.putString(
            OscPrefKey.HOME_HOSTNAME.toString(),
            mVpnGateConnection!!.calculateHostName
        )
        editor.putString(
            OscPrefKey.HOME_COUNTRY.toString(),
            mVpnGateConnection!!.countryShort!!.uppercase()
        )
        editor.putString(OscPrefKey.HOME_USERNAME.toString(), "vpn")
        editor.putString(OscPrefKey.HOME_PASSWORD.toString(), "vpn")
        editor.putString(OscPrefKey.SSL_PORT.toString(), mVpnGateConnection!!.tcpPort.toString())
        editor.apply()
        btnConnectSSTP.background = ResourcesCompat.getDrawable(
            resources,
            R.drawable.selector_apply_button,
            null
        )
        btnConnectSSTP.setText(R.string.cancel_sstp)
        txtStatus.setText(R.string.sstp_connecting)
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
            } catch (e: ActivityNotFoundException) {
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
            linkCheckIp.visibility = View.GONE
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
            btnConnectSSTP.background = ResourcesCompat.getDrawable(
                resources,
                R.drawable.selector_paid_button,
                null
            )
            btnConnectSSTP.setText(R.string.connect_via_sstp)
            txtStatus.setText(R.string.sstp_disconnecting)
        }
    }

    private fun initInterstitialAd() {
        if (dataUtil.hasAds()) {
            try {
                val adRequest = AdRequest.Builder().build()
                InterstitialAd.load(
                    applicationContext,
                    getString(R.string.admob_full_screen),
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
            } catch (ane: ActivityNotFoundException) {
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
                    VPNLaunchHelper.startOpenVpn(vpnProfile, baseContext)
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
            txtNetStats.text = netstat
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
