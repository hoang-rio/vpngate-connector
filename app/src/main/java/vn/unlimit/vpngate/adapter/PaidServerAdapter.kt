package vn.unlimit.vpngate.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.View.*
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import vn.unlimit.vpngate.App
import vn.unlimit.vpngate.GlideApp
import vn.unlimit.vpngate.R
import vn.unlimit.vpngate.models.PaidServer

class PaidServerAdapter(context: Context?) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private var onItemClickListener: OnItemClickListener? = null
    private var onItemLongClickListener: OnItemLongClickListener? = null
    private var onScrollListener: OnScrollListener? = null
    private val layoutInflater: LayoutInflater = LayoutInflater.from(context)
    private var _list: ArrayList<PaidServer> = ArrayList()
    private var lastPosition = 0
    var mContext: Context? = context

    @SuppressLint("NotifyDataSetChanged")
    fun initialize(paidServerList: HashSet<PaidServer>?) {
        try {
            _list.clear()
            if (paidServerList != null) {
                _list.addAll(paidServerList)
            }
            notifyDataSetChanged()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    fun setOnItemClickListener(_onItemClickListener: OnItemClickListener?) {
        onItemClickListener = _onItemClickListener
    }

    fun setOnItemLongClickListener(_onItemLongPressListener: OnItemLongClickListener?) {
        onItemLongClickListener = _onItemLongPressListener
    }

    fun setOnScrollListener(_onScrollListener: OnScrollListener?) {
        onScrollListener = _onScrollListener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return VHTypeVPN(layoutInflater.inflate(R.layout.item_paid_vpn, parent, false))
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
        (viewHolder as VHTypeVPN).bindViewHolder(position)
        lastPosition = position
    }

    override fun getItemCount(): Int {
        return _list.size
    }

    inner class VHTypeVPN(itemView: View) : RecyclerView.ViewHolder(itemView), OnClickListener,
        OnLongClickListener {
        var imgFlag: ImageView = itemView.findViewById(R.id.img_flag)
        private var txtCountry: TextView = itemView.findViewById(R.id.txt_country)
        private var txtIp: TextView = itemView.findViewById(R.id.txt_ip)
        private var txtHostname: TextView = itemView.findViewById(R.id.txt_hostname)
        private var txtSession: TextView = itemView.findViewById(R.id.txt_session)
        private var txtOwner: TextView = itemView.findViewById(R.id.txt_owner)
        private var lnTCP: View = itemView.findViewById(R.id.ln_tcp)
        private var txtTCP: TextView = itemView.findViewById(R.id.txt_tcp_port)
        private var lnUDP: View = itemView.findViewById(R.id.ln_udp)
        private var txtUDP: TextView = itemView.findViewById(R.id.txt_udp_port)
        private var lnL2TP: View = itemView.findViewById(R.id.ln_l2tp)
        private var txtStatusColor: TextView = itemView.findViewById(R.id.txt_status_color)
        private var txtStatusText: TextView = itemView.findViewById(R.id.txt_status_text)
        private var txtDomain: TextView = itemView.findViewById(R.id.txt_domain)
        private var txtMaxSession: TextView = itemView.findViewById(R.id.txt_max_session)

        @Suppress("DEPRECATION")
        fun bindViewHolder(position: Int) {
            try {
                val paidServer: PaidServer = _list.get(getRealPosition(position))
                GlideApp.with(mContext!!)
                    .load(App.getInstance().dataUtil.baseUrl + "/images/flags/" + paidServer.serverCountryCode + ".png")
                    .placeholder(R.color.colorOverlay)
                    .error(R.color.colorOverlay)
                    .into(imgFlag)
                txtCountry.text = paidServer.serverLocation
                txtIp.text = paidServer.serverIp
                txtHostname.text = paidServer.serverName
                txtDomain.text = paidServer.serverDomain
                txtSession.text = paidServer.sessionCount.toString()
                txtMaxSession.text = paidServer.maxSession.toString()
                if (paidServer.serverStatus === "Full") {
                    txtStatusColor.setTextColor(mContext!!.resources.getColor(R.color.colorRed))
                    txtStatusText.text = mContext!!.getText(R.string.full)
                } else if (paidServer.serverStatus === "Medium") {
                    txtStatusColor.setTextColor(mContext!!.resources.getColor(R.color.colorAccent))
                    txtStatusText.text = mContext!!.getText(R.string.medium)
                } else {
                    txtStatusColor.setTextColor(mContext!!.resources.getColor(R.color.colorGoodStatus))
                    txtStatusText.text = mContext!!.getText(R.string.good)
                }
                if (paidServer.tcpPort > 0) {
                    lnTCP.visibility = VISIBLE
                    txtTCP.text = paidServer.tcpPort.toString()
                } else {
                    lnTCP.visibility = GONE
                }
                if (paidServer.udpPort > 0) {
                    lnUDP.visibility = VISIBLE
                    txtUDP.text = paidServer.udpPort.toString()
                } else {
                    lnUDP.visibility = GONE
                }
                if (paidServer.l2tpSupport == 1) {
                    lnL2TP.visibility = VISIBLE
                } else {
                    lnL2TP.visibility = GONE
                }
                if (paidServer.isCommunity) {
                    txtOwner.text = mContext!!.getText(R.string.community_server)
                }
            } catch (e: Exception) {
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
                    val item: PaidServer = _list.get(clickedPost)
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
                    val item: PaidServer = _list.get(clickedPost)
                    onItemClickListener!!.onItemClick(item, clickedPost)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        init {
            itemView.setOnLongClickListener(this)
            itemView.setOnClickListener(this)
        }
    }

}