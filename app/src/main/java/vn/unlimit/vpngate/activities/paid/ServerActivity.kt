package vn.unlimit.vpngate.activities.paid

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import vn.unlimit.vpngate.App
import vn.unlimit.vpngate.GlideApp
import vn.unlimit.vpngate.R
import vn.unlimit.vpngate.models.PaidServer
import vn.unlimit.vpngate.provider.BaseProvider

class ServerActivity : AppCompatActivity(),View.OnClickListener {
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
        bindData()
    }

    @Suppress("DEPRECATION")
    private fun bindData() {
        val paidServer: PaidServer? = intent.getParcelableExtra(BaseProvider.PASS_DETAIL_VPN_CONNECTION)
        if (paidServer!=null) {
            try {
                GlideApp.with(this)
                        .load(App.getInstance().dataUtil.baseUrl + "/images/flags/" + paidServer.serverCountryCode + ".png")
                        .placeholder(R.color.colorOverlay)
                        .error(R.color.colorOverlay)
                        .into(ivFlag!!)
                txtCountry?.text = paidServer.serverLocation
                txtIp?.text = paidServer.serverIp
                txtHostname?.text = paidServer.serverName
                txtDomain?.text = paidServer.serverDomain
                txtSession?.text = paidServer.sessionCount.toString()
                txtMaxSession?.text = paidServer.maxSession.toString()
                if (paidServer.serverStatus === "Full") {
                    txtStatusColor?.setTextColor(resources.getColor(R.color.colorRed))
                    txtStatusText?.text = getText(R.string.full)
                } else if (paidServer.serverStatus === "Medium"){
                    txtStatusColor?.setTextColor(resources.getColor(R.color.colorAccent))
                    txtStatusText?.text = getText(R.string.medium)
                } else {
                    txtStatusColor?.setTextColor(resources.getColor(R.color.colorGoodStatus))
                    txtStatusText?.text = getText(R.string.good)
                }
                if (paidServer.tcpPort > 0) {
                    lnTCP?.visibility = View.VISIBLE
                    txtTCP?.text = paidServer.tcpPort.toString()
                } else {
                    lnTCP?.visibility = View.GONE
                }
                if (paidServer.udpPort > 0) {
                    lnUDP?.visibility = View.VISIBLE
                    txtUDP?.text = paidServer.udpPort.toString()
                } else {
                    lnUDP?.visibility = View.GONE
                }
                if (paidServer.l2tpSupport == 1) {
                    lnL2TP?.visibility = View.VISIBLE
                } else {
                    lnL2TP?.visibility = View.GONE
                }
            } catch (th: Throwable) {
                Log.e(TAG, "Bind data error", th)
                th.printStackTrace()
            }
        }
    }

    override fun onClick(v: View?) {
        when(v) {
            ivBack -> onBackPressed()
        }
    }
}