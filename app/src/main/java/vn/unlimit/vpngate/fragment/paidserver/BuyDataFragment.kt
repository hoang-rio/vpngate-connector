package vn.unlimit.vpngate.fragment.paidserver

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import de.blinkt.openvpn.core.OpenVPNService
import vn.unlimit.vpngate.App
import vn.unlimit.vpngate.R
import vn.unlimit.vpngate.activities.paid.LoginActivity
import vn.unlimit.vpngate.activities.paid.PaidServerActivity
import vn.unlimit.vpngate.adapter.OnItemClickListener
import vn.unlimit.vpngate.adapter.SkuDetailsAdapter
import vn.unlimit.vpngate.dialog.LoadingDialog
import vn.unlimit.vpngate.viewmodels.PurchaseViewModel
import vn.unlimit.vpngate.viewmodels.UserViewModel
import java.util.Collections

class BuyDataFragment : Fragment(), View.OnClickListener, OnItemClickListener {
    private var btnBack: ImageView? = null
    private var listSkus: Array<String>? = null
    private var dataUtil = App.getInstance().dataUtil
    private var paidServerUtil = App.getInstance().paidServerUtil
    private var billingClient: BillingClient? = null
    private var lnLoading: View? = null
    private var rcvSkuDetails: RecyclerView? = null
    private var skuDetailsAdapter: SkuDetailsAdapter? = null
    private var txtDataSize: TextView? = null
    private var isBillingDisconnected = false
    private var userViewModel: UserViewModel? = null
    private var buyingProductDetails: ProductDetails? = null
    private var isClickedBuyData = false
    private var isAttached = false
    private var loadingDialog: LoadingDialog? = null
    private var paidServerActivity: PaidServerActivity? = null
    private var purchaseViewModel: PurchaseViewModel? = null
    private val purchasesUpdatedListener =
        PurchasesUpdatedListener { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
                for (purchase in purchases) {
                    handlePurchase(purchase)
                }
            } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
                val params = Bundle()
                params.putString("username", paidServerUtil.getUserInfo()?.username)
                context?.let {
                    FirebaseAnalytics.getInstance(it)
                        .logEvent("Paid_Server_User_Cancel_Purchase", params)
                }
                Log.i(TAG, "User cancel purchase")
            } else {
                // Handle any other error codes.
                val params = Bundle()
                params.putString("username", paidServerUtil.getUserInfo()?.username)
                params.putString("errorCode", billingResult.responseCode.toString())
                context?.let {
                    FirebaseAnalytics.getInstance(it).logEvent("Paid_Server_Purchase_Error", params)
                }
                Log.e(
                    TAG,
                    "Error when process purchase with error code %s. Msg: %s".format(
                        billingResult.responseCode,
                        billingResult.debugMessage
                    )
                )
            }
        }

    companion object {
        const val TAG = "BuyDataFragment"
    }

    override fun onResume() {
        super.onResume()
        if (isBillingDisconnected) {
            initBilling()
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        isAttached = true
    }

    override fun onDetach() {
        super.onDetach()
        isAttached = false
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val root = inflater.inflate(R.layout.fragment_buy_data, container, false)
        txtDataSize = root.findViewById(R.id.txt_data_size)
        txtDataSize?.text = OpenVPNService.humanReadableByteCount(
            paidServerUtil.getUserInfo()!!.dataSize!!, false, resources
        )
        btnBack = root.findViewById(R.id.btn_back)
        btnBack?.setOnClickListener(this)
        lnLoading = root.findViewById(R.id.ln_loading)
        rcvSkuDetails = root.findViewById(R.id.rcv_sku_details)
        rcvSkuDetails!!.layoutManager = LinearLayoutManager(context)
        skuDetailsAdapter = SkuDetailsAdapter(context)
        skuDetailsAdapter!!.setOnItemClickListener(this)
        rcvSkuDetails!!.adapter = skuDetailsAdapter
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        billingClient = BillingClient.newBuilder(requireActivity())
            .setListener(purchasesUpdatedListener)
            .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
            .build()
        initBilling()
        bindViewModel()
    }

    private fun bindViewModel() {
        paidServerActivity = activity as PaidServerActivity
        userViewModel = paidServerActivity!!.userViewModel
        userViewModel?.userInfo?.observe(viewLifecycleOwner) { userInfo ->
            run {
                if (isAttached) {
                    txtDataSize?.text = OpenVPNService.humanReadableByteCount(
                        userInfo!!.dataSize!!,
                        false,
                        resources
                    )
                }
            }
        }
        purchaseViewModel = ViewModelProvider(this).get(PurchaseViewModel::class.java)
        purchaseViewModel?.isLoggedIn?.observe(viewLifecycleOwner) { isLoggedIn ->
            if (!isLoggedIn) {
                // Go to login screen if user login status is changed
                val intentLogin = Intent(paidServerActivity, LoginActivity::class.java)
                startActivity(intentLogin)
                paidServerActivity!!.finish()
            }
        }
        purchaseViewModel?.isLoading?.observe(viewLifecycleOwner) { isLoading ->
            if (!isClickedBuyData) {
                return@observe
            }
            if (!isLoading) {
                isClickedBuyData = false
                loadingDialog!!.dismiss()
                if (userViewModel?.errorCode == null) {
                    // Create purchase complete
                    Log.i(TAG, "Purchase product %s complete".format(buyingProductDetails?.productId))
                    // Force fetch user to update data size
                    userViewModel?.fetchUser(forceFetch = true)
                    Toast.makeText(
                        context,
                        getString(R.string.purchase_successful, buyingProductDetails?.title),
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    var errorMsg = R.string.invalid_purchase_request
                    if (userViewModel?.errorCode == 112) {
                        errorMsg = R.string.invalid_product_id
                    } else if (userViewModel?.errorCode == 111) {
                        errorMsg = R.string.duplicate_purchase_create_request
                    }
                    val params = Bundle()
                    params.putString(
                        "username",
                        paidServerUtil.getUserInfo()?.username
                    )
                    params.putString("packageId", buyingProductDetails?.productId)
                    params.putString("errorCode", userViewModel?.errorCode?.toString())
                    context?.let {
                        FirebaseAnalytics.getInstance(it)
                            .logEvent("Paid_Server_Create_Purchase_Error", params)
                    }
                    Log.e(
                        TAG,
                        "Purchase product %s error with errorCode: %s".format(
                            buyingProductDetails?.productId,
                            userViewModel?.errorCode
                        )
                    )
                    Toast.makeText(context, getString(errorMsg), Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onClick(v: View?) {
        when (v) {
            btnBack -> findNavController().popBackStack()
        }
    }

    private fun initBilling() {
        val listSkuStr: String = if (dataUtil.hasAds()) {
            FirebaseRemoteConfig.getInstance().getString(getString(R.string.cfg_paid_server_sku))
        } else {
            FirebaseRemoteConfig.getInstance()
                .getString(getString(R.string.cfg_paid_server_sku_pro_ver))
        }
        val gson = GsonBuilder().create()
        listSkus = gson.fromJson(listSkuStr, object : TypeToken<Array<String>>() {}.type)
        billingClient?.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    querySkuDetails()
                }
                isBillingDisconnected = false
            }

            override fun onBillingServiceDisconnected() {
                isBillingDisconnected = true
            }
        })
    }

    fun querySkuDetails() {
        val skuList = ArrayList<String>()
        skuList.addAll(listSkus!!)
        val productList = ArrayList<QueryProductDetailsParams.Product>()
        listSkus?.forEach { productList.add(QueryProductDetailsParams.Product.newBuilder()
            .setProductId(it)
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        )}
        val params = QueryProductDetailsParams.newBuilder().setProductList(productList)
        lnLoading?.visibility = View.VISIBLE
        rcvSkuDetails?.visibility = View.GONE
        billingClient?.queryProductDetailsAsync(params.build()) { result, listProductDetails ->
            if (isAttached) {
                requireActivity().runOnUiThread {
                    if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                        lnLoading?.visibility = View.GONE
                        rcvSkuDetails?.visibility = View.VISIBLE
                        Collections.sort(
                            listProductDetails,
                            Comparator { productDetails: ProductDetails, productDetails1: ProductDetails ->
                                return@Comparator productDetails.oneTimePurchaseOfferDetails!!.priceAmountMicros.compareTo(productDetails1.oneTimePurchaseOfferDetails!!.priceAmountMicros)
                            })
                        skuDetailsAdapter!!.initialize(listProductDetails)
                    } else {
                        Toast.makeText(
                            context,
                            getString(R.string.get_sku_list_error),
                            Toast.LENGTH_LONG
                        ).show()
                        FirebaseAnalytics.getInstance(requireContext())
                            .logEvent("Paid_Server_List_Package_Error", null)
                        findNavController().popBackStack()
                    }
                }
            }
        }
    }

    override fun onItemClick(o: Any?, position: Int) {
        if (o != null) {
            buyingProductDetails = o as ProductDetails
            val productDetailsParamsList =
            listOf(
                BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(buyingProductDetails!!)
                    .build()
            )
            val billingFlowParams =
                BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(productDetailsParamsList)
                    .build()
            isClickedBuyData = true
            val responseCode = billingClient?.launchBillingFlow(
                activity as PaidServerActivity,
                billingFlowParams
            )?.responseCode
            if (responseCode == BillingClient.BillingResponseCode.OK) {
                Log.i(TAG, "Launch purchase flow success")
            }
        } else {
            isClickedBuyData = false
            Toast.makeText(
                requireContext(),
                getString(R.string.sku_item_click_error),
                Toast.LENGTH_SHORT
            ).show()
            querySkuDetails()
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (isAttached) {
            loadingDialog =
                if (loadingDialog == null) LoadingDialog.newInstance(getString(R.string.processing_text)) else loadingDialog
            loadingDialog?.show(
                paidServerActivity!!.supportFragmentManager,
                LoadingDialog::class.java.name
            )
        }
        val consumeParams =
            ConsumeParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
        billingClient!!.consumeAsync(consumeParams) { billingResult, _ ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.i(
                    TAG,
                    "Purchase product %s success from Google Play. Continue with api process".format(
                        purchase.products[0]
                    )
                )
                requireActivity().runOnUiThread {
                    purchaseViewModel?.createPurchase(purchase, buyingProductDetails!!)
                }
            }
        }
    }

}