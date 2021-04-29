package vn.unlimit.vpngate.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import de.blinkt.openvpn.core.OpenVPNService
import vn.unlimit.vpngate.R
import vn.unlimit.vpngate.models.ConnectedSession
import java.text.DateFormat.getDateTimeInstance
import java.util.*
import kotlin.collections.ArrayList

class SessionAdapter(context: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val mContext = context
    private val listSession = ArrayList<ConnectedSession>()
    private val layoutInflater: LayoutInflater = LayoutInflater.from(context)
    var onDetailClickListener: OnItemClickListener? = null
    var onDeleteCLickListener: OnItemClickListener? = null
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
        return getDateTimeInstance(java.text.DateFormat.SHORT, java.text.DateFormat.MEDIUM).format(cal.time)
    }

    inner class VHTypeSession(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
        private val txtSessionId: TextView = itemView.findViewById(R.id.txt_session_id)
        private val txtServerName: TextView = itemView.findViewById(R.id.txt_server_name)
        private val txtTransferredByte: TextView = itemView.findViewById(R.id.txt_transferbytes)
        private val txtCreated: TextView = itemView.findViewById(R.id.txt_created)
        private val txtUpdate: TextView = itemView.findViewById(R.id.txt_updated)
        private val lnDetail: LinearLayout = itemView.findViewById(R.id.ln_btn_detail)
        private val lnDelete: LinearLayout = itemView.findViewById(R.id.ln_btn_delete)

        override fun onClick(v: View?) {
            when (v) {
                lnDetail -> onDetailClickListener?.onItemClick(listSession[adapterPosition], adapterPosition)
                lnDelete -> onDeleteCLickListener?.onItemClick(listSession[adapterPosition], adapterPosition)
            }
        }

        fun bindViewHolder(session: ConnectedSession) {
            txtSessionId.text = session.sessionId
            txtServerName.text = session.serverId?.serverName
            txtTransferredByte.text = OpenVPNService.humanReadableByteCount(session.transferBytes, false, mContext.resources)
            txtCreated.text = getDateStr(session._created!!)
            txtUpdate.text = getDateStr(session._updated!!)
            lnDetail.setOnClickListener(this)
            lnDelete.setOnClickListener(this)
        }
    }
}