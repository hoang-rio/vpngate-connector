package vn.unlimit.vpngate.activities

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
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
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = ActivityL2tpConnectBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val initialRootLeft = binding.root.paddingLeft
        val initialRootRight = binding.root.paddingRight
        val initialNavHeight = binding.navDetail.layoutParams.height
        val initialNavTop = binding.navDetail.paddingTop
        val initialNavLeft = binding.navDetail.paddingLeft
        val initialNavRight = binding.navDetail.paddingRight
        val initialNavBottom = binding.navDetail.paddingBottom
        val initialBackTopMargin = (binding.btnBack.layoutParams as RelativeLayout.LayoutParams).topMargin
        val initialScrollBottom = binding.scrollView.paddingBottom
        val initialAdBottom = binding.adContainerL2tp.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.root.updatePadding(
                left = initialRootLeft + insets.left,
                right = initialRootRight + insets.right
            )
            binding.navDetail.updateLayoutParams {
                height = initialNavHeight + insets.top
            }
            binding.navDetail.updatePadding(
                top = initialNavTop,
                left = initialNavLeft + insets.left,
                right = initialNavRight + insets.right,
                bottom = initialNavBottom
            )
            binding.btnBack.updateLayoutParams<RelativeLayout.LayoutParams> {
                addRule(RelativeLayout.CENTER_VERTICAL, 0)
                addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE)
                topMargin = initialBackTopMargin + initialNavTop + insets.top
            }
            binding.scrollView.updatePadding(bottom = initialScrollBottom + insets.bottom)
            binding.adContainerL2tp.updatePadding(bottom = initialAdBottom + insets.bottom)
            windowInsets
        }
        ViewCompat.requestApplyInsets(binding.root)
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
            binding.btnBack.setOnClickListener(this)
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
                binding.navDetail.setBackgroundColor(resources.getColor(R.color.colorPaidServer, theme))
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
