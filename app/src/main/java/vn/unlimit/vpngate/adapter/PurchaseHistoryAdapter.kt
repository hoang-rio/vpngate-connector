package vn.unlimit.vpngate.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import vn.unlimit.vpngate.R
import vn.unlimit.vpngate.models.PurchaseHistory

class PurchaseHistoryAdapter(context: Context?) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val mContext = context
    private val _list = ArrayList<PurchaseHistory>()
    private val layoutInflater: LayoutInflater = LayoutInflater.from(context)
    var onScrollListener: OnScrollListener? = null

    fun initialize(list: ArrayList<PurchaseHistory>?) {
        try {
            _list.clear()
            if (list != null) {
                _list.addAll(list)
            }
            notifyDataSetChanged()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return VHTypePurchaseHistory(layoutInflater.inflate(R.layout.item_purchase_history, parent, false))
    }

    override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder, position: Int) {
        if (onScrollListener != null) {
            if (position == _list.size - 1) {
                onScrollListener!!.onScrollDown()
            } else if (position == 0) {
                onScrollListener!!.onScrollUp()
            }
        }
        (viewHolder as VHTypePurchaseHistory).bindViewHolder(position)
    }

    override fun getItemCount(): Int {
        return _list.size
    }

    inner class VHTypePurchaseHistory(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val txtIndex: TextView = itemView.findViewById(R.id.txt_index)
        private val txtDataSize: TextView = itemView.findViewById(R.id.txt_data_size)
        private val txtDataSizeReceive: TextView = itemView.findViewById(R.id.txt_data_size_receive)
        private val txtPrice: TextView = itemView.findViewById(R.id.txt_price)
        private val txtDateTime: TextView = itemView.findViewById(R.id.txt_date_time)
        fun bindViewHolder(position: Int) {
            val purchaseHistory = _list.get(position)
            txtIndex.text = (position + 1).toString()
            txtDataSize.text = purchaseHistory.dataSizeStr
            txtDataSizeReceive.text = purchaseHistory.getDisplayDataSize(mContext!!.resources)
            txtPrice.text = "%s%s".format(purchaseHistory.currencyPrice, purchaseHistory.currency)
            txtDateTime.text = purchaseHistory.getCreated()
        }
    }

}