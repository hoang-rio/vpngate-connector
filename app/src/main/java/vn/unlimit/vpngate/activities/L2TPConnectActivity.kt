package vn.unlimit.vpngate.activities

import android.annotation.SuppressLint
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.ads.*
import vn.unlimit.vpngate.App
import vn.unlimit.vpngate.BuildConfig
import vn.unlimit.vpngate.GlideApp
import vn.unlimit.vpngate.R
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
    private var dataUtil: DataUtil = App.getInstance().dataUtil
    private var adContainer: RelativeLayout? = null
    private var adView: AdView? = null
    private var ivStep2: ImageView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_l2tp_connect)
        ivBack = findViewById(R.id.btn_back)
        txtTitle = findViewById(R.id.txt_title)
        txtHint = findViewById(R.id.txt_hint)
        ivStep1 = findViewById(R.id.iv_step1)
        txtEndPoint = findViewById(R.id.txt_end_point)
        adContainer = findViewById(R.id.ad_container_l2tp)
        ivStep2 = findViewById(R.id.iv_step2)
    }

    @SuppressLint("SetTextI18n")
    override fun onResume() {
        super.onResume()
        mVPNGateConnection = intent.getParcelableExtra(BaseProvider.PASS_DETAIL_VPN_CONNECTION)
        txtTitle?.text = getString(R.string.l2tp_connect_title, mVPNGateConnection?.hostName)
        txtHint?.text = getString(R.string.l2tp_connect_hint, mVPNGateConnection?.hostName)
        val step1Drawable: Drawable
        val step2Drawable: Drawable
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            step1Drawable = resources.getDrawable(R.drawable.add_vpn_connection, applicationContext.theme)
            step2Drawable = resources.getDrawable(R.drawable.connected_vpn, applicationContext.theme)
        } else {
            step1Drawable = resources.getDrawable(R.drawable.add_vpn_connection)
            step2Drawable = resources.getDrawable(R.drawable.connected_vpn)
        }
        GlideApp.with(this)
                .load(step1Drawable)
                .placeholder(R.color.colorOverlay)
                .error(R.color.colorOverlay)
                .into(ivStep1!!)
        GlideApp.with(this)
                .load(step2Drawable)
                .placeholder(R.color.colorOverlay)
                .error(R.color.colorOverlay)
                .into(ivStep2!!)
        if (dataUtil.getBooleanSetting(DataUtil.USE_DOMAIN_TO_CONNECT, false)) {
            txtEndPoint?.text = mVPNGateConnection?.hostName + ".opengw.net"
        } else {
            txtEndPoint?.text = mVPNGateConnection?.ip
        }
        loadBannerAds()
    }

    private fun loadBannerAds() {
        try {
            if (dataUtil.hasAds()) {
                MobileAds.initialize(this, dataUtil.adMobId)
                adView = AdView(applicationContext)
                adView!!.adSize = AdSize.LARGE_BANNER
                if (BuildConfig.DEBUG) { //Test
                    adView!!.adUnitId = "ca-app-pub-3940256099942544/6300978111"
                } else { //Real
                    adView!!.adUnitId = resources.getString(R.string.admob_banner_bottom_l2tp)
                }
                adView!!.adListener = object : AdListener() {
                    override fun onAdFailedToLoad(errorCode: Int) {
                        adContainer?.visibility = View.GONE
                    }
                }
                val params = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT)
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
            onBackPressed()
        }
    }

}
