package vn.unlimit.vpngate.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnLongClickListener
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
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

    @SuppressLint("NotifyDataSetChanged")
    fun initialize(vpnGateConnectionList: VPNGateConnectionList?) {
        try {
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

    override fun getItemViewType(position: Int): Int {
        return TYPE_NORMAL
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
        if (viewHolder is VHTypeVPN) {
            viewHolder.bindViewHolder(position)
        }
        lastPosition = position
    }

    override fun getItemCount(): Int {
        return _list.size()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return VHTypeVPN(layoutInflater.inflate(R.layout.item_vpn, parent, false))
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
                val vpnGateConnection = _list.get(getRealPosition(position))
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

        private fun getRealPosition(position: Int): Int {
            return position
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

    companion object {
        private const val TYPE_NORMAL = 100000
        private const val TAG = "VPNGateListAdapter"
    }
}
