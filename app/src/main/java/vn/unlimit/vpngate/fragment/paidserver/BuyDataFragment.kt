package vn.unlimit.vpngate.fragment.paidserver

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.billingclient.api.*
import vn.unlimit.vpngate.App
import vn.unlimit.vpngate.R
import vn.unlimit.vpngate.adapter.OnItemClickListener
import vn.unlimit.vpngate.adapter.SkuDetailsAdapter
import java.util.*
import kotlin.collections.ArrayList

/**
 * A simple [Fragment] subclass.
 * Use the [BuyDataFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class BuyDataFragment : Fragment(), View.OnClickListener, OnItemClickListener {
    private var btnBack: ImageView? = null
    private var listSkus: Array<String>? = null
    private var dataUtil = App.getInstance().dataUtil
    private var billingClient: BillingClient? = null
    private var lnLoading: View? = null
    private var rcvSkuDetail: RecyclerView? = null
    private var skuDetailsAdapter: SkuDetailsAdapter? = null
    private val purchasesUpdatedListener =
            PurchasesUpdatedListener { billingResult, purchases ->
                // To be implemented in a later section.
            }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        billingClient = BillingClient.newBuilder(requireActivity())
                .setListener(purchasesUpdatedListener)
                .enablePendingPurchases()
                .build()
        initBilling()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val root = inflater.inflate(R.layout.fragment_buy_data, container, false)
        btnBack = root.findViewById(R.id.btn_back)
        btnBack?.setOnClickListener(this)
        lnLoading = root.findViewById(R.id.ln_loading_wrap)
        rcvSkuDetail = root.findViewById(R.id.rcv_sku_details)
        rcvSkuDetail!!.layoutManager = LinearLayoutManager(context)
        skuDetailsAdapter = SkuDetailsAdapter(context)
        skuDetailsAdapter!!.setOnItemClickListener(this)
        rcvSkuDetail!!.adapter = skuDetailsAdapter
        return root
    }

    companion object {
        const val TAG = "BuyDataFragment"
    }

    override fun onClick(v: View?) {
        when (v) {
            btnBack -> findNavController().popBackStack()
        }
    }

    private fun initBilling() {
        if (dataUtil.hasAds()) {
            listSkus = resources.getStringArray(R.array.free_paid_skus)
        } else {
            listSkus = resources.getStringArray(R.array.pro_paid_skus)
        }
        billingClient?.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    querySkuDetails()
                }
            }

            override fun onBillingServiceDisconnected() {
                // Try to restart the connection on the next request to
                // Google Play by calling the startConnection() method.
            }
        })
    }

    fun querySkuDetails() {
        val skuList = ArrayList<String>()
        skuList.addAll(listSkus!!)
        val params = SkuDetailsParams.newBuilder()
        params.setSkusList(skuList).setType(BillingClient.SkuType.INAPP)
        billingClient?.querySkuDetailsAsync(params.build()) { result, listSkuDetails ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                lnLoading?.visibility = View.GONE
                rcvSkuDetail?.visibility = View.VISIBLE
                Collections.sort(listSkuDetails!!, Comparator { skuDetails: SkuDetails, skuDetails1: SkuDetails ->
                    return@Comparator skuDetails.priceAmountMicros.compareTo(skuDetails1.priceAmountMicros)
                })
                skuDetailsAdapter!!.initialize(listSkuDetails)
            } else {
                Toast.makeText(context, getString(R.string.get_sku_list_error), Toast.LENGTH_LONG).show()
                findNavController().popBackStack()
            }
        }
    }

    override fun onItemClick(o: Any?, position: Int) {
        val skuDetails = o as SkuDetails
        Toast.makeText(context, "Click buy item %s at %d".format(skuDetails.sku, position), Toast.LENGTH_SHORT).show()
    }
}