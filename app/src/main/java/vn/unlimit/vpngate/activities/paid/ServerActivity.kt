package vn.unlimit.vpngate.activities.paid

import android.Manifest
import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.net.Uri
import android.net.VpnService
import android.os.*
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.Keep
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import de.blinkt.openvpn.VpnProfile
import de.blinkt.openvpn.core.*
import de.blinkt.openvpn.core.ConfigParser.ConfigParseError
import de.blinkt.openvpn.utils.TotalTraffic
import vn.unlimit.vpngate.App
import vn.unlimit.vpngate.GlideApp
import vn.unlimit.vpngate.R
import vn.unlimit.vpngate.activities.DetailActivity
import vn.unlimit.vpngate.activities.L2TPConnectActivity
import vn.unlimit.vpngate.dialog.ConnectionUseProtocol
import vn.unlimit.vpngate.models.PaidServer
import vn.unlimit.vpngate.provider.BaseProvider
import vn.unlimit.vpngate.utils.DataUtil
import vn.unlimit.vpngate.utils.PaidServerUtil
import java.io.*

@Suppress("DEPRECATION")
class ServerActivity : AppCompatActivity(), View.OnClickListener, VpnStatus.StateListener, VpnStatus.ByteCountListener {
    @Keep companion object {
        const val TAG = "ServerActivity"
        private var mVPNService: IOpenVPNServiceInternal? = null
        const val TYPE_FROM_NOTIFY = 1001
        const val TYPE_NORMAL = 1000
        const val TYPE_START = "vn.ulimit.vpngate.TYPE_START"
    }

    private var ivBack: ImageView? = null
    private var ivFlag: ImageView? = null
    private var txtCountry: TextView? = null
    private var txtIp: TextView? = null
    private var txtHostname: TextView? = null
    private var txtSession: TextView? = null
    private var txtOwner: TextView? = null
    private var lnTCP: View? = null
    private var txtTCP: TextView? = null
    private var lnUDP: View? = null
    private var txtUDP: TextView? = null
    private var lnL2TP: View? = null
    private var txtStatusColor: TextView? = null
    private var txtStatusText: TextView? = null
    private var txtDomain: TextView? = null
    private var txtMaxSession: TextView? = null
    private var btnL2TPConnect: Button? = null
    private var btnConnect: Button? = null
    private var txtCheckIp: TextView? = null
    private var txtStatus: TextView? = null
    private var txtNetStats: TextView? = null
    private var mPaidServer: PaidServer? = null
    private val paidServerUtil: PaidServerUtil = App.getInstance().paidServerUtil
    private val dataUtil: DataUtil = App.getInstance().dataUtil
    private var isConnecting = false
    private var isAuthFailed = false
    private var vpnProfile: VpnProfile? = null
    private var btnInstallOpenVpn: Button? = null
    private var btnSaveConfigFile: Button? = null
    private val mConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName,
                                        service: IBinder) {
            mVPNService = IOpenVPNServiceInternal.Stub.asInterface(service)
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            mVPNService = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_server)
        supportActionBar!!.hide()
        ivBack = findViewById(R.id.btn_back)
        ivBack?.setOnClickListener(this)
        ivFlag = findViewById(R.id.img_flag)
        txtCountry = findViewById(R.id.txt_country)
        txtIp = findViewById(R.id.txt_ip)
        txtHostname = findViewById(R.id.txt_hostname)
        txtSession = findViewById(R.id.txt_session)
        txtMaxSession = findViewById(R.id.txt_max_session)
        txtOwner = findViewById(R.id.txt_owner)
        lnTCP = findViewById(R.id.ln_tcp)
        txtTCP = findViewById(R.id.txt_tcp_port)
        lnUDP = findViewById(R.id.ln_udp)
        txtUDP = findViewById(R.id.txt_udp_port)
        lnL2TP = findViewById(R.id.ln_l2tp)
        txtStatusColor = findViewById(R.id.txt_status_color)
        txtStatusText = findViewById(R.id.txt_status_text)
        txtNetStats = findViewById(R.id.txt_net_stats)
        txtDomain = findViewById(R.id.txt_domain)
        txtStatus = findViewById(R.id.txt_status)
        btnL2TPConnect = findViewById(R.id.btn_l2tp_connect)
        btnL2TPConnect?.setOnClickListener(this)
        btnConnect = findViewById(R.id.btn_connect)
        btnConnect?.setOnClickListener(this)
        txtCheckIp = findViewById(R.id.txt_check_ip)
        txtCheckIp?.setOnClickListener(this)
        btnSaveConfigFile = findViewById(R.id.btn_save_config_file)
        btnSaveConfigFile?.setOnClickListener(this)
        btnInstallOpenVpn = findViewById(R.id.btn_install_openvpn)
        btnInstallOpenVpn?.setOnClickListener(this)
        bindData()
        VpnStatus.addStateListener(this)
        VpnStatus.addByteCountListener(this)
        txtStatus!!.text = ""
    }

    override fun onDestroy() {
        super.onDestroy()
        VpnStatus.removeStateListener(this)
        VpnStatus.removeByteCountListener(this)
    }

    override fun onResume() {
        super.onResume()
        try {
            Handler(Looper.getMainLooper()).postDelayed({
                val intent = Intent(this, OpenVPNService::class.java)
                OpenVPNService.mDisplaySpeed = dataUtil.getBooleanSetting(DataUtil.SETTING_NOTIFY_SPEED, true)
                intent.action = OpenVPNService.START_SERVICE
                bindService(intent, mConnection, BIND_AUTO_CREATE)
            }, 300)
            if (!App.isIsImportToOpenVPN()) {
                btnInstallOpenVpn?.visibility = View.GONE
                btnSaveConfigFile?.visibility = View.GONE
                btnConnect!!.visibility = View.VISIBLE
            } else {
                btnConnect!!.visibility = View.GONE
                if (dataUtil.hasOpenVPNInstalled()) {
                    btnSaveConfigFile?.visibility = View.VISIBLE
                    btnInstallOpenVpn?.visibility = View.GONE
                } else {
                    btnSaveConfigFile?.visibility = View.GONE
                    btnInstallOpenVpn?.visibility = View.VISIBLE
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onPause() {
        try {
            super.onPause()
            TotalTraffic.saveTotal(this)
            unbindService(mConnection)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun checkStatus(): Boolean {
        try {
            return VpnStatus.isVPNActive()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }

    private fun sendConnectVPN() {
        val intent = Intent(BaseProvider.ACTION.ACTION_CONNECT_VPN)
        sendBroadcast(intent)
    }

    private fun prepareVpn(useUdp: Boolean) {
        if (loadVpnProfile(useUdp)) {
            startVpn()
        } else {
            Toast.makeText(this, getString(R.string.error_load_profile), Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun handleConnection(useUdp: Boolean) {
        if (checkStatus()) {
            stopVpn()
            val params = Bundle()
            params.putString("type", "replace current")
            params.putString("domain", mPaidServer!!.serverDomain)
            params.putString("ip", mPaidServer!!.serverIp)
            params.putString("country", mPaidServer!!.serverLocation)
            FirebaseAnalytics.getInstance(applicationContext).logEvent("Paid_Connect_VPN", params)
            txtCheckIp?.visibility = View.GONE
            Handler(Looper.getMainLooper()).postDelayed({ prepareVpn(useUdp) }, 500)
        } else {
            val params = Bundle()
            params.putString("type", "connect new")
            params.putString("domain", mPaidServer!!.serverDomain)
            params.putString("ip", mPaidServer!!.serverIp)
            params.putString("country", mPaidServer!!.serverLocation)
            FirebaseAnalytics.getInstance(applicationContext).logEvent("Paid_Connect_VPN", params)
            prepareVpn(useUdp)
        }
        if (Build.VERSION.SDK_INT >= 16) {
            btnConnect!!.background = resources.getDrawable(R.drawable.selector_apply_button)
            txtStatus!!.text = getString(R.string.connecting)
        }
        isConnecting = true
        btnConnect!!.setText(R.string.cancel)
        paidServerUtil.setLastConnectServer(mPaidServer!!)
        sendConnectVPN()
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    @Suppress("DEPRECATION")
    private fun bindData() {
        mPaidServer = if (intent.getIntExtra(TYPE_START, TYPE_NORMAL) == TYPE_FROM_NOTIFY) {
            paidServerUtil.getLastConnectServer()
        } else {
            intent.getParcelableExtra(BaseProvider.PASS_DETAIL_VPN_CONNECTION)
        }
        try {
            GlideApp.with(this)
                    .load(App.getInstance().dataUtil.baseUrl + "/images/flags/" + mPaidServer!!.serverCountryCode + ".png")
                    .placeholder(R.color.colorOverlay)
                    .error(R.color.colorOverlay)
                    .into(ivFlag!!)
            txtCountry?.text = mPaidServer!!.serverLocation
            txtIp?.text = mPaidServer!!.serverIp
            txtHostname?.text = mPaidServer!!.serverName
            txtDomain?.text = mPaidServer!!.serverDomain
            txtSession?.text = mPaidServer!!.sessionCount.toString()
            txtMaxSession?.text = mPaidServer!!.maxSession.toString()
            when {
                mPaidServer!!.serverStatus === "Full" -> {
                    txtStatusColor?.setTextColor(resources.getColor(R.color.colorRed))
                    txtStatusText?.text = getText(R.string.full)
                }
                mPaidServer!!.serverStatus === "Medium" -> {
                    txtStatusColor?.setTextColor(resources.getColor(R.color.colorAccent))
                    txtStatusText?.text = getText(R.string.medium)
                }
                else -> {
                    txtStatusColor?.setTextColor(resources.getColor(R.color.colorGoodStatus))
                    txtStatusText?.text = getText(R.string.good)
                }
            }
            if (mPaidServer!!.tcpPort > 0) {
                lnTCP?.visibility = View.VISIBLE
                txtTCP?.text = mPaidServer!!.tcpPort.toString()
            } else {
                lnTCP?.visibility = View.GONE
            }
            if (mPaidServer!!.udpPort > 0) {
                lnUDP?.visibility = View.VISIBLE
                txtUDP?.text = mPaidServer!!.udpPort.toString()
            } else {
                lnUDP?.visibility = View.GONE
            }
            if (mPaidServer!!.l2tpSupport == 1) {
                lnL2TP?.visibility = View.VISIBLE
                btnL2TPConnect?.visibility = View.VISIBLE
            } else {
                lnL2TP?.visibility = View.GONE
                btnL2TPConnect?.visibility = View.GONE
            }
            if (isCurrent() && checkStatus()) {
                btnConnect?.text = resources.getString(R.string.disconnect)
                if (Build.VERSION.SDK_INT >= 16) {
                    btnConnect?.background = resources.getDrawable(R.drawable.selector_apply_button)
                }
                txtStatus?.text = VpnStatus.getLastCleanLogMessage(this)
                txtNetStats?.visibility = View.VISIBLE
            } else {
                txtNetStats?.visibility = View.GONE
            }
            if (checkStatus()) {
                txtCheckIp?.visibility = View.VISIBLE
            }
        } catch (th: Throwable) {
            Log.e(TAG, "Bind data error", th)
            th.printStackTrace()
        }
    }

    private fun stopVpn() {
        //prepareStopVPN();
        ProfileManager.setConntectedVpnProfileDisconnected(this)
        if (mVPNService != null) {
            try {
                mVPNService?.stopVPN(false)
            } catch (e: RemoteException) {
                VpnStatus.logException(e)
            }
        }
    }

    private fun loadVpnProfile(useUDP: Boolean): Boolean {
        val data: ByteArray = if (useUDP) {
            mPaidServer!!.getOpenVpnConfigDataUdp()!!.toByteArray()
        } else {
            mPaidServer!!.getOpenVpnConfigData()!!.toByteArray()
        }
        dataUtil.setBooleanSetting(DataUtil.LAST_CONNECT_USE_UDP, useUDP)
        val cp = ConfigParser()
        val isr = InputStreamReader(ByteArrayInputStream(data))
        try {
            cp.parseConfig(isr)
            vpnProfile = cp.convertProfile()
            vpnProfile?.mName = mPaidServer!!.getName(useUDP)
            if (dataUtil.getBooleanSetting(DataUtil.SETTING_BLOCK_ADS, false)) {
                vpnProfile?.mOverrideDNS = true
                vpnProfile?.mDNS1 = FirebaseRemoteConfig.getInstance().getString(getString(R.string.dns_block_ads_primary_cfg_key))
                vpnProfile?.mDNS2 = FirebaseRemoteConfig.getInstance().getString(getString(R.string.dns_block_ads_alternative_cfg_key))
            } else if (dataUtil.getBooleanSetting(DataUtil.USE_CUSTOM_DNS, false)) {
                vpnProfile?.mOverrideDNS = true
                vpnProfile?.mDNS1 = dataUtil.getStringSetting(DataUtil.CUSTOM_DNS_IP_1, "8.8.8.8")
                val dns2 = dataUtil.getStringSetting(DataUtil.CUSTOM_DNS_IP_2, null)
                if (dns2 != null) {
                    vpnProfile?.mDNS2 = dns2
                }
            }
            vpnProfile?.mUsername = paidServerUtil.getUserInfo()!!.getString("username")
            vpnProfile?.mPassword = paidServerUtil.getStringSetting(PaidServerUtil.SAVED_VPN_PW)
            ProfileManager.setTemporaryProfile(applicationContext, vpnProfile)
        } catch (e: IOException) {
            e.printStackTrace()
            return false
        } catch (e: ConfigParseError) {
            e.printStackTrace()
            return false
        }
        return true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        try {
            if (resultCode == RESULT_OK) {
                if (requestCode == DetailActivity.START_VPN_PROFILE) {
                    VPNLaunchHelper.startOpenVpn(vpnProfile, baseContext)
                }
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    @Suppress("DEPRECATION")
    private fun startVpn() {
        val intent = VpnService.prepare(this)
        if (intent != null) {
            VpnStatus.updateStateString("USER_VPN_PERMISSION", "", R.string.state_user_vpn_permission,
                    ConnectionStatus.LEVEL_WAITING_FOR_USER_INPUT)
            // Start the query
            try {
                startActivityForResult(intent, DetailActivity.START_VPN_PROFILE)
            } catch (ane: ActivityNotFoundException) {
                // Shame on you Sony! At least one user reported that
                // an official Sony Xperia Arc S image triggers this exception
                VpnStatus.logError(R.string.no_vpn_support_image)
            }
        } else {
            onActivityResult(DetailActivity.START_VPN_PROFILE, RESULT_OK, null)
        }
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun connectVPNServer() {
        if (!isConnecting) {
            if (checkStatus() && isCurrent()) {
                val params = Bundle()
                params.putString("type", "disconnect current")
                params.putString("domain", mPaidServer!!.serverDomain)
                params.putString("ip", mPaidServer!!.serverIp)
                params.putString("country", mPaidServer!!.serverLocation)
                FirebaseAnalytics.getInstance(applicationContext).logEvent("Paid_Disconnect_VPN", params)
                stopVpn()
                if (Build.VERSION.SDK_INT >= 16) {
                    btnConnect!!.background = resources.getDrawable(R.drawable.selector_primary_button)
                }
                btnConnect!!.setText(R.string.connect_to_this_server)
                txtStatus!!.setText(R.string.disconnecting)
            } else if (mPaidServer!!.tcpPort > 0 && mPaidServer!!.udpPort > 0) {
                if (dataUtil.getIntSetting(DataUtil.SETTING_DEFAULT_PROTOCOL, 0) != 0) {
                    // Apply default protocol in setting
                    var protocol = "TCP"
                    if (dataUtil.getIntSetting(DataUtil.SETTING_DEFAULT_PROTOCOL, 0) == 1) {
                        handleConnection(false)
                    } else {
                        protocol = "UDP"
                        handleConnection(true)
                    }
                    Toast.makeText(this, getString(R.string.connecting_use_protocol, protocol), Toast.LENGTH_SHORT).show()
                } else {
                    val connectionUseProtocol = ConnectionUseProtocol.newInstance(mPaidServer) { useUdp: Boolean -> handleConnection(useUdp) }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && !isFinishing && !isDestroyed) {
                        connectionUseProtocol.show(supportFragmentManager, ConnectionUseProtocol::class.java.name)
                    } else if (!isFinishing) {
                        connectionUseProtocol.show(supportFragmentManager, ConnectionUseProtocol::class.java.name)
                    }
                }
            } else {
                handleConnection(false)
            }
        } else {
            val params = Bundle()
            params.putString("type", "cancel connect to vpn")
            params.putString("domain", mPaidServer!!.serverDomain)
            params.putString("ip", mPaidServer!!.serverIp)
            params.putString("country", mPaidServer!!.serverLocation)
            FirebaseAnalytics.getInstance(applicationContext).logEvent("Paid_Cancel_VPN", params)
            stopVpn()
            if (Build.VERSION.SDK_INT >= 16) {
                btnConnect!!.background = resources.getDrawable(R.drawable.selector_primary_button)
            }
            btnConnect!!.setText(R.string.connect_to_this_server)
            txtStatus!!.text = getString(R.string.canceled)
            isConnecting = false
        }
    }

    override fun onClick(v: View?) {
        when (v) {
            ivBack -> onBackPressed()
            btnL2TPConnect -> {
                val intentL2TP = Intent(this, L2TPConnectActivity::class.java)
                intentL2TP.putExtra(BaseProvider.L2TP_SERVER_TYPE, L2TPConnectActivity.TYPE_PAID)
                intentL2TP.putExtra(BaseProvider.PASS_DETAIL_VPN_CONNECTION, mPaidServer)
                startActivity(intentL2TP)
            }
            btnConnect -> connectVPNServer()
            txtCheckIp -> {
                val params = Bundle()
                params.putString("type", "check ip click")
                params.putString("domain", mPaidServer?.serverDomain)
                params.putString("ip", mPaidServer?.serverIp)
                params.putString("country", mPaidServer?.serverLocation)
                FirebaseAnalytics.getInstance(applicationContext).logEvent("Paid_Click_Check_IP", params)
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(FirebaseRemoteConfig.getInstance().getString("vpn_check_ip_url")))
                startActivity(browserIntent)
            }
            btnInstallOpenVpn -> {
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=net.openvpn.openvpn")))
                } catch (ex: ActivityNotFoundException) {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=net.openvpn.openvpn")))
                }
            }
            btnSaveConfigFile -> {
                if (mPaidServer!!.tcpPort > 0 && mPaidServer!!.udpPort > 0) {
                    val connectionUseProtocol: ConnectionUseProtocol = ConnectionUseProtocol.newInstance(mPaidServer) { useUdp: Boolean -> this.handleImport(useUdp) }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && !isFinishing && !isDestroyed) {
                        connectionUseProtocol.show(supportFragmentManager, ConnectionUseProtocol::class.java.name)
                    } else if (!isFinishing) {
                        connectionUseProtocol.show(supportFragmentManager, ConnectionUseProtocol::class.java.name)
                    }
                } else {
                    handleImport(false)
                }
            }
        }
    }

    private fun handleImport(useUdp: Boolean) {
        val data: String = if (useUdp) {
            mPaidServer!!.getOpenVpnConfigDataUdp()!!
        } else {
            mPaidServer!!.getOpenVpnConfigData()!!
        }
        try {
            while (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 100)
            }
            val fileName: String = mPaidServer!!.getName(useUdp) + ".ovpn"
            val writeFile = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName)
            val fileOutputStream = FileOutputStream(writeFile)
            fileOutputStream.write(data.toByteArray())
            Toast.makeText(applicationContext, getString(R.string.saved_ovpn_file_in, "Download/$fileName"), Toast.LENGTH_LONG).show()
            val handler = Handler(Looper.getMainLooper())
            handler.postDelayed({
                val packageManager = packageManager
                val intent = packageManager.getLaunchIntentForPackage("net.openvpn.openvpn")
                if (intent != null) {
                    intent.action = Intent.ACTION_VIEW
                    startActivity(intent)
                }
            }, 500)
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    override fun updateState(state: String?, logmessage: String?, localizedResId: Int, level: ConnectionStatus?, Intent: Intent?) {
        runOnUiThread {
            try {
                txtStatus!!.text = VpnStatus.getLastCleanLogMessage(this)
                dataUtil.setBooleanSetting(DataUtil.USER_ALLOWED_VPN, true)
                when (level) {
                    ConnectionStatus.LEVEL_CONNECTED -> {
                        if (isCurrent()) {
                            btnConnect!!.text = getString(R.string.disconnect)
                            txtNetStats!!.visibility = View.VISIBLE
                            OpenVPNService.setNotificationActivityClass(this::class.java)
                        }
                        isConnecting = false
                        isAuthFailed = false
                        txtCheckIp?.visibility = View.VISIBLE
                    }
                    ConnectionStatus.LEVEL_WAITING_FOR_USER_INPUT -> dataUtil.setBooleanSetting(DataUtil.USER_ALLOWED_VPN, false)
                    ConnectionStatus.LEVEL_NOTCONNECTED -> if (!isConnecting && !isAuthFailed) {
                        txtCheckIp?.visibility = View.GONE
                        btnConnect!!.setText(R.string.connect_to_this_server)
                        if (Build.VERSION.SDK_INT >= 16) {
                            btnConnect!!.background = resources.getDrawable(R.drawable.selector_paid_button)
                        }
                        txtStatus!!.setText(R.string.disconnected)
                        txtNetStats!!.visibility = View.GONE
                    }
                    ConnectionStatus.LEVEL_AUTH_FAILED -> {
                        isAuthFailed = true
                        btnConnect!!.text = getString(R.string.retry_connect)
                        val params = Bundle()
                        params.putString("ip", mPaidServer?.serverIp)
                        params.putString("domain", mPaidServer?.serverDomain)
                        params.putString("country", mPaidServer?.serverLocation)
                        FirebaseAnalytics.getInstance(applicationContext).logEvent("Paid_Connect_Error", params)
                        if (Build.VERSION.SDK_INT >= 16) {
                            btnConnect!!.background = resources.getDrawable(R.drawable.selector_paid_button)
                        }
                        txtStatus!!.text = resources.getString(R.string.vpn_auth_failure)
                        txtCheckIp?.visibility = View.GONE
                        isConnecting = false
                    }
                    else -> txtCheckIp?.visibility = View.GONE
                }
            } catch (th: Throwable) {
                Log.e(TAG, "UpdateState error", th)
                th.printStackTrace()
            }
        }
    }

    override fun setConnectedVPN(uuid: String?) {
        // Nothing here
    }

    private fun isCurrent(): Boolean {
        val lastServer = paidServerUtil.getLastConnectServer() ?: return false
        return lastServer.serverDomain == mPaidServer?.serverDomain
    }


    override fun updateByteCount(`in`: Long, out: Long, diffIn: Long, diffOut: Long) {
        if (!isCurrent()) {
            return
        }
        runOnUiThread {
            val netstat = String.format(getString(de.blinkt.openvpn.R.string.statusline_bytecount),
                    OpenVPNService.humanReadableByteCount(`in`, false, resources),
                    OpenVPNService.humanReadableByteCount(diffIn / OpenVPNManagement.mBytecountInterval, true, resources),
                    OpenVPNService.humanReadableByteCount(out, false, resources),
                    OpenVPNService.humanReadableByteCount(diffOut / OpenVPNManagement.mBytecountInterval, true, resources))
            txtNetStats!!.text = netstat
        }
    }
}