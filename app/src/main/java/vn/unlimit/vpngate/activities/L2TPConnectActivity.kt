package vn.unlimit.vpngate.activities

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.RelativeLayout
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
import vn.unlimit.vpngate.databinding.ActivityL2tpConnectBinding
import vn.unlimit.vpngate.models.PaidServer
import vn.unlimit.vpngate.models.VPNGateConnection
import vn.unlimit.vpngate.provider.BaseProvider
import vn.unlimit.vpngate.utils.DataUtil

class L2TPConnectActivity : AppCompatActivity(), View.OnClickListener {
    private var mVPNGateConnection: VPNGateConnection? = null
    private var dataUtil: DataUtil = App.instance!!.dataUtil!!
    private var adView: AdView? = null
    private lateinit var binding: ActivityL2tpConnectBinding

    companion object {
        const val TYPE_FREE = 0
        const val TYPE_PAID = 1
    }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityL2tpConnectBinding.inflate(layoutInflater)
        setContentView(binding.root)
        try {
            Glide.with(this)
                .load(R.drawable.add_vpn_connection)
                .placeholder(R.color.colorOverlay)
                .error(R.color.colorOverlay)
                .into(binding.ivStep1)
            Glide.with(this)
                .load(R.drawable.connected_vpn)
                .placeholder(R.color.colorOverlay)
                .error(R.color.colorOverlay)
                .into(binding.ivStep2)
            val typeServer: Int = intent.getIntExtra(BaseProvider.L2TP_SERVER_TYPE, TYPE_FREE)
            if (typeServer == TYPE_FREE) {
                mVPNGateConnection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BaseProvider.PASS_DETAIL_VPN_CONNECTION, VPNGateConnection::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(BaseProvider.PASS_DETAIL_VPN_CONNECTION)
                }
                binding.txtTitle.text =
                    getString(R.string.l2tp_connect_title, mVPNGateConnection?.hostName)
                binding.txtHint.text = getString(R.string.l2tp_connect_hint, mVPNGateConnection?.hostName)
                if (dataUtil.getBooleanSetting(DataUtil.USE_DOMAIN_TO_CONNECT, false)) {
                    binding.txtEndPoint.text = mVPNGateConnection?.hostName + ".opengw.net"
                } else {
                    binding.txtEndPoint.text = mVPNGateConnection?.ip
                }
                loadBannerAds()
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    window.statusBarColor = resources.getColor(R.color.colorPaidServer, theme)
                    binding.navDetail.setBackgroundColor(resources.getColor(R.color.colorPaidServer, theme))
                } else {
                    @Suppress("DEPRECATION")
                    window.statusBarColor = resources.getColor(R.color.colorPaidServer)
                    @Suppress("DEPRECATION")
                    binding.navDetail.setBackgroundColor(resources.getColor(R.color.colorPaidServer))
                }
                //Hide ad banner
                val paidServerUtil = App.instance!!.paidServerUtil!!
                binding.adContainerL2tp.visibility = View.GONE
                binding.txtVpnShareSecret.text = getString(R.string.vpn_paid_shared_secret)
                binding.txtVpnUser.text = paidServerUtil.getUserInfo()?.username
                binding.txtVpnPw.text = getString(R.string.vpn_pw_hint)
                val paidServer: PaidServer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(BaseProvider.PASS_DETAIL_VPN_CONNECTION, PaidServer::class.java)!!
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(BaseProvider.PASS_DETAIL_VPN_CONNECTION)!!
                }
                binding.txtTitle.text = getString(R.string.l2tp_connect_title, paidServer.serverName)
                binding.txtHint.text = getString(R.string.l2tp_connect_hint, paidServer.serverName)
                if (dataUtil.getBooleanSetting(DataUtil.USE_DOMAIN_TO_CONNECT, false)) {
                    binding.txtEndPoint.text = paidServer.serverDomain
                } else {
                    binding.txtEndPoint.text = paidServer.serverIp
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
                        binding.adContainerL2tp.visibility = View.GONE
                    }
                }
                val params = RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.WRAP_CONTENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT
                )
                params.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE)
                params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE)
                adView!!.layoutParams = params
                binding.adContainerL2tp.addView(adView)
                adView!!.loadAd(AdRequest.Builder().build())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onClick(view: View?) {
        if (view == binding.btnBack) {
            onBackPressedDispatcher.onBackPressed()
        }
    }

}
