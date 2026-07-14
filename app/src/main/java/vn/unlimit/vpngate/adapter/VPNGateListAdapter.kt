package vn.unlimit.vpngate.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnLongClickListener
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import vn.unlimit.vpngate.App.Companion.instance
import vn.unlimit.vpngate.R
import vn.unlimit.vpngate.models.VPNGateConnectionList
import vn.unlimit.vpngate.utils.DataUtil

/**
 * Created by hoangnd on 1/29/2018.
 */
class VPNGateListAdapter(private val mContext: Context) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val _list = VPNGateConnectionList()
    private val layoutInflater: LayoutInflater = LayoutInflater.from(mContext)
    private var onItemClickListener: OnItemClickListener? = null
    private var onItemLongClickListener: OnItemLongClickListener? = null
    private var onScrollListener: OnScrollListener? = null
    private var lastPosition = 0
    private var nativeAd: NativeAd? = null
    private var hasAds: Boolean = false
    private var adUnitId: String? = null

    @SuppressLint("NotifyDataSetChanged")
    fun initialize(vpnGateConnectionList: VPNGateConnectionList?) {
        try {
            Log.d(TAG, "initialize with: ${vpnGateConnectionList?.size()} items")
            _list.clear()
            if (vpnGateConnectionList != null) {
                _list.addAll(vpnGateConnectionList)
            }
            notifyDataSetChanged()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setOnItemClickListener(inOnItemClickListener: OnItemClickListener?) {
        this.onItemClickListener = inOnItemClickListener
    }

    fun setOnItemLongClickListener(inOnItemLongPressListener: OnItemLongClickListener?) {
        this.onItemLongClickListener = inOnItemLongPressListener
    }

    fun setOnScrollListener(inOnScrollListener: OnScrollListener?) {
        this.onScrollListener = inOnScrollListener
    }

    fun setHasAds(hasAds: Boolean) {
        this.hasAds = hasAds
    }

    fun setAdUnitId(adUnitId: String?) {
        this.adUnitId = adUnitId
    }

    private fun shouldShowAdAt(position: Int): Boolean {
        if (!hasAds || adUnitId == null) return false
        // First ad at position 2 (after 2 VPN items), then every 3 VPN items after that
        // Positions: 0=VPN, 1=VPN, 2=Ad, 3=VPN, 4=VPN, 5=VPN, 6=Ad, 7=VPN, 8=VPN, 9=VPN, 10=Ad, etc.
        return position > 0 && position % AD_INTERVAL == 2
    }

    private fun getRealPosition(position: Int): Int {
        if (!hasAds || adUnitId == null) return position
        // Calculate the actual data position accounting for ad positions
        // Ads are at positions 2, 6, 10, 14... (every 4th position starting from 2)
        // Subtract the number of ads that appear before this position: (position + 2) / 4
        return position - ((position + 2) / AD_INTERVAL)
    }

    private fun loadNativeAd(adViewHolder: VHTypeAd) {
        if (adUnitId == null) return

        val adLoader = AdLoader.Builder(mContext, adUnitId!!)
            .forNativeAd { nativeAd ->
                this.nativeAd = nativeAd
                populateNativeAdView(nativeAd, adViewHolder)
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.e(TAG, "Native ad failed to load: ${adError.message}")
                }

                override fun onAdLoaded() {
                    Log.d(TAG, "Native ad loaded successfully")
                }
            })
            .build()

        adLoader.loadAd(AdRequest.Builder().build())
    }

    private fun populateNativeAdView(nativeAd: NativeAd, adViewHolder: VHTypeAd) {
        // Hide loading container and show ad view
        adViewHolder.adLoadingContainer.visibility = View.GONE
        adViewHolder.nativeAdView.visibility = View.VISIBLE

        val adView = adViewHolder.nativeAdView

        // Set the media view
        adView.mediaView = adViewHolder.adMedia

        // Set other ad assets
        adView.headlineView = adViewHolder.adHeadline
        adView.bodyView = adViewHolder.adBody
        adView.callToActionView = adViewHolder.adCallToAction
        adView.iconView = adViewHolder.adAppIcon
        adView.priceView = adViewHolder.adPrice
        adView.starRatingView = adViewHolder.adStars
        adView.storeView = adViewHolder.adStore
        adView.advertiserView = adViewHolder.adAdvertiser

        // Populate the headline
        adViewHolder.adHeadline.text = nativeAd.headline
        adViewHolder.adHeadline.visibility = if (nativeAd.headline != null) View.VISIBLE else View.GONE

        // Populate the media view
        if (nativeAd.mediaContent != null) {
            adViewHolder.adMedia.mediaContent = nativeAd.mediaContent
            adViewHolder.adMedia.visibility = View.VISIBLE
        } else {
            adViewHolder.adMedia.visibility = View.GONE
        }

        // Populate the body
        if (nativeAd.body != null) {
            adViewHolder.adBody.text = nativeAd.body
            adViewHolder.adBody.visibility = View.VISIBLE
        } else {
            adViewHolder.adBody.visibility = View.GONE
        }

        // Populate the call to action
        if (nativeAd.callToAction != null) {
            adViewHolder.adCallToAction.text = nativeAd.callToAction
            adViewHolder.adCallToAction.visibility = View.VISIBLE
        } else {
            adViewHolder.adCallToAction.visibility = View.GONE
        }

        // Populate the icon
        if (nativeAd.icon != null) {
            adViewHolder.adAppIcon.setImageDrawable(nativeAd.icon!!.drawable)
            adViewHolder.adAppIcon.visibility = View.VISIBLE
        } else {
            adViewHolder.adAppIcon.visibility = View.GONE
        }

        // Populate the price
        if (nativeAd.price != null) {
            adViewHolder.adPrice.text = nativeAd.price
            adViewHolder.adPrice.visibility = View.VISIBLE
        } else {
            adViewHolder.adPrice.visibility = View.GONE
        }

        // Populate the star rating
        if (nativeAd.starRating != null) {
            adViewHolder.adStars.rating = nativeAd.starRating!!.toFloat()
            adViewHolder.adStars.visibility = View.VISIBLE
        } else {
            adViewHolder.adStars.visibility = View.GONE
        }

        // Populate the store
        if (nativeAd.store != null) {
            adViewHolder.adStore.text = nativeAd.store
            adViewHolder.adStore.visibility = View.VISIBLE
        } else {
            adViewHolder.adStore.visibility = View.GONE
        }

        // Populate the advertiser
        if (nativeAd.advertiser != null) {
            adViewHolder.adAdvertiser.text = nativeAd.advertiser
            adViewHolder.adAdvertiser.visibility = View.VISIBLE
        } else {
            adViewHolder.adAdvertiser.visibility = View.GONE
        }

        // Register the native ad view
        adView.setNativeAd(nativeAd)
    }

    override fun getItemViewType(position: Int): Int {
        return if (shouldShowAdAt(position)) {
            TYPE_AD
        } else {
            TYPE_NORMAL
        }
    }

    override fun onBindViewHolder(
        viewHolder: RecyclerView.ViewHolder,
        @SuppressLint("RecyclerView") position: Int
    ) {
        if (onScrollListener != null) {
            if (position > lastPosition || position == 0) {
                onScrollListener!!.onScrollDown()
            } else if (position < lastPosition) {
                onScrollListener!!.onScrollUp()
            }
        }
        when (viewHolder) {
            is VHTypeVPN -> viewHolder.bindViewHolder(getRealPosition(position))
            is VHTypeAd -> loadNativeAd(viewHolder)
        }
        lastPosition = position
    }

    override fun getItemCount(): Int {
        val itemCount = _list.size()
        if (!hasAds || adUnitId == null || itemCount == 0) {
            return itemCount
        }
        // Add ad positions (every 3 items means 1 ad for every 3 data items)
        return itemCount + (itemCount / AD_INTERVAL)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_AD -> {
                val view = layoutInflater.inflate(R.layout.item_native_ad, parent, false)
                VHTypeAd(view)
            }
            else -> {
                val view = layoutInflater.inflate(R.layout.item_vpn, parent, false)
                VHTypeVPN(view)
            }
        }
    }

    private inner class VHTypeVPN(itemView: View) : RecyclerView.ViewHolder(itemView),
        View.OnClickListener, OnLongClickListener {
        var imgFlag: ImageView = itemView.findViewById(R.id.img_flag)
        var txtCountry: TextView = itemView.findViewById(R.id.txt_country)
        var txtIp: TextView = itemView.findViewById(R.id.txt_ip)
        var txtHostname: TextView = itemView.findViewById(R.id.txt_hostname)
        var txtScore: TextView = itemView.findViewById(R.id.txt_score)
        var txtUptime: TextView = itemView.findViewById(R.id.txt_uptime)
        var txtSpeed: TextView = itemView.findViewById(R.id.txt_speed)
        var txtPing: TextView = itemView.findViewById(R.id.txt_ping)
        var txtSession: TextView = itemView.findViewById(R.id.txt_session)
        var txtOwner: TextView = itemView.findViewById(R.id.txt_owner)
        var lnTCP: View = itemView.findViewById(R.id.ln_tcp)
        var txtTCP: TextView = itemView.findViewById(R.id.txt_tcp_port)
        var lnUDP: View = itemView.findViewById(R.id.ln_udp)
        var txtUDP: TextView = itemView.findViewById(R.id.txt_udp_port)
        var lnL2TP: View = itemView.findViewById(R.id.ln_l2tp)
        var lnSSTP: View = itemView.findViewById(R.id.ln_sstp)

        init {
            itemView.setOnLongClickListener(this)
            itemView.setOnClickListener(this)
        }

        fun bindViewHolder(position: Int) {
            try {
                val vpnGateConnection = _list.get(position)
                Glide.with(mContext)
                    .load(instance!!.dataUtil!!.baseUrl + "/images/flags/" + vpnGateConnection.countryShort + ".png")
                    .placeholder(R.color.colorOverlay)
                    .error(R.color.colorOverlay)
                    .into(imgFlag)
                txtCountry.text = vpnGateConnection.countryLong
                txtIp.text = vpnGateConnection.ip
                txtHostname.text = vpnGateConnection.calculateHostName
                txtScore.text = vpnGateConnection.scoreAsString
                txtUptime.text = vpnGateConnection.getCalculateUpTime(mContext)
                txtSpeed.text = vpnGateConnection.calculateSpeed
                txtPing.text = vpnGateConnection.pingAsString
                txtSession.text = vpnGateConnection.numVpnSessionAsString
                txtOwner.text = vpnGateConnection.operator
                val dataUtil = instance!!.dataUtil
                val isIncludeUdp = dataUtil!!.getBooleanSetting(DataUtil.INCLUDE_UDP_SERVER, true)
                if (!isIncludeUdp || vpnGateConnection.tcpPort == 0) {
                    lnTCP.visibility = View.GONE
                } else {
                    lnTCP.visibility = View.VISIBLE
                    txtTCP.text = vpnGateConnection.tcpPort.toString()
                }
                if (!isIncludeUdp || vpnGateConnection.udpPort == 0) {
                    lnUDP.visibility = View.GONE
                } else {
                    lnUDP.visibility = View.VISIBLE
                    txtUDP.text = vpnGateConnection.udpPort.toString()
                }
                lnL2TP.visibility =
                    if (vpnGateConnection.isL2TPSupport()) View.VISIBLE else View.GONE
                lnSSTP.visibility =
                    if (vpnGateConnection.isSSTPSupport()) View.VISIBLE else View.GONE
            } catch (e: Exception) {
                Log.e(TAG, "bindViewHolder error", e)
                e.printStackTrace()
            }
        }

        override fun onLongClick(view: View): Boolean {
            try {
                if (onItemLongClickListener != null) {
                    val clickedPost = getRealPosition(adapterPosition)
                    val item = _list.get(clickedPost)
                    onItemLongClickListener!!.onItemLongClick(item, clickedPost)
                    return true
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return false
        }

        override fun onClick(view: View) {
            try {
                if (onItemClickListener != null) {
                    val clickedPost = getRealPosition(adapterPosition)
                    val item = _list.get(clickedPost)
                    onItemClickListener!!.onItemClick(item, clickedPost)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private inner class VHTypeAd(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nativeAdView: NativeAdView = itemView.findViewById(R.id.native_ad_view)
        val adMedia: com.google.android.gms.ads.nativead.MediaView = itemView.findViewById(R.id.ad_media)
        val adHeadline: TextView = itemView.findViewById(R.id.ad_headline)
        val adBody: TextView = itemView.findViewById(R.id.ad_body)
        val adCallToAction: Button = itemView.findViewById(R.id.ad_call_to_action)
        val adAppIcon: ImageView = itemView.findViewById(R.id.ad_app_icon)
        val adPrice: TextView = itemView.findViewById(R.id.ad_price)
        val adStars: RatingBar = itemView.findViewById(R.id.ad_stars)
        val adStore: TextView = itemView.findViewById(R.id.ad_store)
        val adAdvertiser: TextView = itemView.findViewById(R.id.ad_advertiser)
        val adLoadingContainer: com.facebook.shimmer.ShimmerFrameLayout = itemView.findViewById(R.id.ad_loading_container)
    }

    companion object {
        private const val TYPE_NORMAL = 100000
        private const val TYPE_AD = 100001
        private const val TAG = "VPNGateListAdapter"
        private const val AD_INTERVAL = 4 // Show ad after every 3 VPN items (every 4th position: 3, 7, 11...)
    }
}
