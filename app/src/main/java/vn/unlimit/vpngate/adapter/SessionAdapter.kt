package vn.unlimit.vpngate.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import de.blinkt.openvpn.core.OpenVPNService
import vn.unlimit.vpngate.App
import vn.unlimit.vpngate.R
import vn.unlimit.vpngate.models.ConnectedSession
import java.text.DateFormat.getDateTimeInstance
import java.util.*

class SessionAdapter(context: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val mContext = context
    private val listSession = ArrayList<ConnectedSession>()
    private val layoutInflater: LayoutInflater = LayoutInflater.from(context)
    var onDisconnectListener: OnItemClickListener? = null

    @SuppressLint("NotifyDataSetChanged")
    fun initialize(list: LinkedHashSet<ConnectedSession>) {
        listSession.clear()
        listSession.addAll(list)
        notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        return (holder as VHTypeSession).bindViewHolder(listSession[position])
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return VHTypeSession(layoutInflater.inflate(R.layout.item_session, parent, false))
    }

    override fun getItemCount(): Int {
        return listSession.size
    }

    private fun getDateStr(time: Long): String {
        val cal: Calendar = Calendar.getInstance()
        cal.timeInMillis = time
        return getDateTimeInstance(java.text.DateFormat.SHORT, java.text.DateFormat.MEDIUM).format(
            cal.time
        )
    }

    inner class VHTypeSession(itemView: View) : RecyclerView.ViewHolder(itemView),
        View.OnClickListener {
        private val txtSessionId: TextView = itemView.findViewById(R.id.txt_session_id)
        private val txtServerName: TextView = itemView.findViewById(R.id.txt_server_name)
        private val txtClientPublicIp: TextView = itemView.findViewById(R.id.txt_client_public_ip)
        private val txtTransferredByte: TextView = itemView.findViewById(R.id.txt_transferbytes)
        private val lnCreated: View = itemView.findViewById(R.id.ln_created)
        private val lnCurrentSession: View = itemView.findViewById(R.id.ln_current_session)
        private val txtCreated: TextView = itemView.findViewById(R.id.txt_created)
        private val txtUpdate: TextView = itemView.findViewById(R.id.txt_updated)
        private val lnDisconnect: View = itemView.findViewById(R.id.ln_btn_disconnect)

        override fun onClick(v: View?) {
            onDisconnectListener?.onItemClick(listSession[adapterPosition], adapterPosition)
        }

        fun bindViewHolder(session: ConnectedSession) {
            txtSessionId.text = session.sessionId
            txtServerName.text = session.serverId?.serverName
            txtClientPublicIp.text = session.clientInfo?.ip
            txtTransferredByte.text = OpenVPNService.humanReadableByteCount(
                session.transferBytes,
                false,
                mContext.resources
            )
            txtCreated.text = getDateStr(session._created!!)
            if (session._updated != null) {
                txtUpdate.text = getDateStr(session._updated!!)
                lnCreated.visibility = View.VISIBLE
            } else {
                lnCreated.visibility = View.GONE
            }
            lnDisconnect.setOnClickListener(this)
            if (App.getInstance().paidServerUtil.isCurrentSession(
                    session.serverId!!._id,
                    session.clientIp
                )
            ) {
                lnCurrentSession.visibility = View.VISIBLE
                lnDisconnect.visibility = View.GONE
            } else {
                lnDisconnect.visibility = View.VISIBLE
                lnCurrentSession.visibility = View.GONE
            }
        }
    }
}