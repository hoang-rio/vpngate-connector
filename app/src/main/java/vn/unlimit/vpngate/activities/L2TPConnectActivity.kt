package vn.unlimit.vpngate.activities

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import vn.unlimit.vpngate.App
import vn.unlimit.vpngate.R
import vn.unlimit.vpngate.models.PaidServer
import vn.unlimit.vpngate.models.VPNGateConnection
import vn.unlimit.vpngate.provider.BaseProvider
import vn.unlimit.vpngate.utils.DataUtil

class L2TPConnectActivity : AppCompatActivity(), View.OnClickListener {

    private var ivBack: ImageView? = null
    private var txtTitle: TextView? = null
    private var mVPNGateConnection: VPNGateConnection? = null
    private var txtHint: TextView? = null
    private var ivStep1: ImageView? = null
    private var txtEndPoint: TextView? = null
    private var dataUtil: DataUtil = App.instance!!.dataUtil!!
    private var adContainer: RelativeLayout? = null
    private var adView: AdView? = null
    private var ivStep2: ImageView? = null
    private var lnNavDetail: RelativeLayout? = null
    private var txtSharedSecret: TextView? = null
    private var txtVPNUser: TextView? = null
    private var txtVPNPw: TextView? = null

    companion object {
        const val TYPE_FREE = 0
        const val TYPE_PAID = 1
    }

    @Suppress("DEPRECATION")
    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_l2tp_connect)
        ivBack = findViewById(R.id.btn_back)
        ivBack!!.setOnClickListener(this)
        txtTitle = findViewById(R.id.txt_title)
        txtHint = findViewById(R.id.txt_hint)
        ivStep1 = findViewById(R.id.iv_step1)
        txtEndPoint = findViewById(R.id.txt_end_point)
        adContainer = findViewById(R.id.ad_container_l2tp)
        ivStep2 = findViewById(R.id.iv_step2)
        lnNavDetail = findViewById(R.id.nav_detail)
        txtSharedSecret = findViewById(R.id.txt_vpn_share_secret)
        txtVPNUser = findViewById(R.id.txt_vpn_user)
        txtVPNPw = findViewById(R.id.txt_vpn_pw)
        try {
            Glide.with(this)
                .load(R.drawable.add_vpn_connection)
                .placeholder(R.color.colorOverlay)
                .error(R.color.colorOverlay)
                .into(ivStep1!!)
            Glide.with(this)
                .load(R.drawable.connected_vpn)
                .placeholder(R.color.colorOverlay)
                .error(R.color.colorOverlay)
                .into(ivStep2!!)
            val typeServer: Int = intent.getIntExtra(BaseProvider.L2TP_SERVER_TYPE, TYPE_FREE)
            if (typeServer == TYPE_FREE) {
                mVPNGateConnection =
                    intent.getParcelableExtra(BaseProvider.PASS_DETAIL_VPN_CONNECTION)
                txtTitle?.text =
                    getString(R.string.l2tp_connect_title, mVPNGateConnection?.hostName)
                txtHint?.text = getString(R.string.l2tp_connect_hint, mVPNGateConnection?.hostName)
                if (dataUtil.getBooleanSetting(DataUtil.USE_DOMAIN_TO_CONNECT, false)) {
                    txtEndPoint?.text = mVPNGateConnection?.hostName + ".opengw.net"
                } else {
                    txtEndPoint?.text = mVPNGateConnection?.ip
                }
                loadBannerAds()
            } else {
                window.statusBarColor = resources.getColor(R.color.colorPaidServer)
                lnNavDetail?.setBackgroundColor(resources.getColor(R.color.colorPaidServer))
                //Hide ad banner
                val paidServerUtil = App.instance!!.paidServerUtil!!
                adContainer?.visibility = View.GONE
                txtSharedSecret?.text = getString(R.string.vpn_paid_shared_secret)
                txtVPNUser?.text = paidServerUtil.getUserInfo()?.username
                txtVPNPw?.text = getString(R.string.vpn_pw_hint)
                val paidServer: PaidServer =
                    intent.getParcelableExtra(BaseProvider.PASS_DETAIL_VPN_CONNECTION)!!
                txtTitle?.text = getString(R.string.l2tp_connect_title, paidServer.serverName)
                txtHint?.text = getString(R.string.l2tp_connect_hint, paidServer.serverName)
                if (dataUtil.getBooleanSetting(DataUtil.USE_DOMAIN_TO_CONNECT, false)) {
                    txtEndPoint?.text = paidServer.serverDomain
                } else {
                    txtEndPoint?.text = paidServer.serverIp
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadBannerAds() {
        try {
            if (dataUtil.hasAds()) {
                MobileAds.initialize(this)
                adView = AdView(applicationContext)
                adView!!.setAdSize(AdSize.LARGE_BANNER)
                adView!!.adUnitId = resources.getString(R.string.admob_banner_bottom_l2tp)
                adView!!.adListener = object : AdListener() {
                    override fun onAdFailedToLoad(errorCode: LoadAdError) {
                        adContainer?.visibility = View.GONE
                    }
                }
                val params = RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.WRAP_CONTENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT
                )
                params.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE)
                params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE)
                adView!!.layoutParams = params
                adContainer?.addView(adView)
                adView!!.loadAd(AdRequest.Builder().build())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onClick(view: View?) {
        if (view == ivBack) {
            onBackPressedDispatcher.onBackPressed()
        }
    }

}
