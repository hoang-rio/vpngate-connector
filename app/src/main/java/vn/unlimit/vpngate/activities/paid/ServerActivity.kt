package vn.unlimit.vpngate.activities.paid

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import vn.unlimit.vpngate.App
import vn.unlimit.vpngate.GlideApp
import vn.unlimit.vpngate.R
import vn.unlimit.vpngate.activities.L2TPConnectActivity
import vn.unlimit.vpngate.models.PaidServer
import vn.unlimit.vpngate.provider.BaseProvider

class ServerActivity : AppCompatActivity(), View.OnClickListener {
    companion object {
        const val TAG = "ServerActivity"
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
    private var mPaidServer: PaidServer? = null

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
        txtDomain = findViewById(R.id.txt_domain)
        btnL2TPConnect = findViewById(R.id.btn_l2tp_connect)
        btnL2TPConnect?.setOnClickListener(this)
        bindData()
    }

    @Suppress("DEPRECATION")
    private fun bindData() {
        mPaidServer = intent.getParcelableExtra(BaseProvider.PASS_DETAIL_VPN_CONNECTION)
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
            if (mPaidServer!!.serverStatus === "Full") {
                txtStatusColor?.setTextColor(resources.getColor(R.color.colorRed))
                txtStatusText?.text = getText(R.string.full)
            } else if (mPaidServer!!.serverStatus === "Medium") {
                txtStatusColor?.setTextColor(resources.getColor(R.color.colorAccent))
                txtStatusText?.text = getText(R.string.medium)
            } else {
                txtStatusColor?.setTextColor(resources.getColor(R.color.colorGoodStatus))
                txtStatusText?.text = getText(R.string.good)
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
        } catch (th: Throwable) {
            Log.e(TAG, "Bind data error", th)
            th.printStackTrace()
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
        }
    }
}