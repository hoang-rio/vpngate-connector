package vn.unlimit.vpngate.api

import android.util.Log
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.SkuDetails
import com.androidnetworking.error.ANError
import com.androidnetworking.interfaces.JSONObjectRequestListener
import org.json.JSONObject
import vn.unlimit.vpngate.request.RequestListener

class PurchaseApiRequest : BaseApiRequest() {
    companion object {
        const val USER_CREATE_PURCHASE_URL = "/purchase/create"
        const val USER_PURCHASE_HISTORY_URL = "/purchase/list"
        private const val TAG = "PurchaseApiRequest"
    }

    fun createPurchase(
        purchase: Purchase,
        skuDetails: SkuDetails,
        requestListener: RequestListener
    ) {
        val purchaseInfo = HashMap<String, Any>()
        purchaseInfo["packageId"] = purchase.sku
        purchaseInfo["purchaseId"] = purchase.orderId
        purchaseInfo["platform"] = PARAMS_USER_PLATFORM
        purchaseInfo["paymentMethod"] = PARAMS_USER_PLATFORM + "_IAP"
        purchaseInfo["currency"] = skuDetails.priceCurrencyCode
        purchaseInfo["currencyPrice"] = skuDetails.priceAmountMicros.toDouble() / 1000000
        post(
            "$USER_CREATE_PURCHASE_URL${if (isPro) "?version=pro" else ""}",
            purchaseInfo,
            object : JSONObjectRequestListener {
                override fun onResponse(response: JSONObject?) {
                    Log.i(TAG, "Create purchase success with message %s".format(response))
                    requestListener.onSuccess(response)
                }

                override fun onError(anError: ANError?) {
                    Log.e(TAG, "Create purchase error with message %s".format(anError!!.errorBody))
                    baseErrorHandle(anError, requestListener)
                }
            })
    }

    fun listPurchase(take: Int, skip: Int, requestListener: RequestListener) {
        get("$USER_PURCHASE_HISTORY_URL?take=$take&skip=$skip", object : JSONObjectRequestListener {
            override fun onResponse(response: JSONObject?) {
                Log.i(TAG, "List purchase success with message %s".format(response))
                requestListener.onSuccess(response)
            }

            override fun onError(anError: ANError?) {
                Log.e(TAG, "List purchase error with message %s".format(anError!!.errorBody))
                baseErrorHandle(anError, requestListener)
            }
        })
    }
}