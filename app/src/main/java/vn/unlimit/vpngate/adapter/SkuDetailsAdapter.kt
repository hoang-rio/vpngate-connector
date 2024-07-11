package vn.unlimit.vpngate.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.android.billingclient.api.ProductDetails
import vn.unlimit.vpngate.R

class SkuDetailsAdapter(context: Context?) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val mContext = context
    private var onItemClickListener: OnItemClickListener? = null
    private val layoutInflater: LayoutInflater = LayoutInflater.from(context)
    private var _list: ArrayList<ProductDetails> = ArrayList()
    private var lastPosition = 0

    @SuppressLint("NotifyDataSetChanged")
    fun initialize(skuList: List<ProductDetails>?) {
        try {
            _list.clear()
            if (skuList != null) {
                _list.addAll(skuList)
            }
            notifyDataSetChanged()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    fun setOnItemClickListener(inOnItemClickListener: OnItemClickListener?) {
        onItemClickListener = inOnItemClickListener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return VHTypeSkuDetails(layoutInflater.inflate(R.layout.item_sku_details, parent, false))
    }

    override fun onBindViewHolder(
        viewHolder: RecyclerView.ViewHolder,
        @SuppressLint("RecyclerView") position: Int
    ) {
        (viewHolder as VHTypeSkuDetails).bindViewHolder(position)
        lastPosition = position
    }

    override fun getItemCount(): Int {
        return _list.size
    }

    inner class VHTypeSkuDetails(itemView: View) : RecyclerView.ViewHolder(itemView),
        View.OnClickListener {
        private val txtTitle: TextView = itemView.findViewById(R.id.txt_title)
        private val txtDescription: TextView = itemView.findViewById(R.id.txt_description)
        private val btnBuy: Button = itemView.findViewById(R.id.btn_buy)
        override fun onClick(v: View?) {
            try {
                if (onItemClickListener != null) {
                    val item: ProductDetails = _list[adapterPosition]
                    onItemClickListener!!.onItemClick(item, adapterPosition)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun bindViewHolder(position: Int) {
            try {
                val productDetails: ProductDetails = _list[position]
                txtTitle.text = productDetails.title
                txtDescription.text = productDetails.description
                val priceToGb =
                    productDetails.oneTimePurchaseOfferDetails!!.priceAmountMicros / 1000000 / productDetails.productId.replace(
                        Regex("[^0-9]"),
                        ""
                    ).toInt()
                btnBuy.text = mContext!!.getString(
                    R.string.btn_buy_data,
                    productDetails.oneTimePurchaseOfferDetails!!.formattedPrice,
                    priceToGb.toString(),
                    productDetails.oneTimePurchaseOfferDetails!!.priceCurrencyCode
                )
            } catch (th: Throwable) {
                th.printStackTrace()
            }
        }

        init {
            btnBuy.setOnClickListener(this)
        }
    }
}