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
    }

    fun createPurchase(purchase: Purchase, skuDetails: SkuDetails, requestListener: RequestListener) {
        val purchaseInfo = HashMap<String, Any>()
        purchaseInfo["packageId"] = purchase.sku
        purchaseInfo["purchaseId"] = purchase.orderId
        purchaseInfo["platform"] = PARAMS_USER_FLAT_FORM
        purchaseInfo["paymentMethod"] = PARAMS_USER_FLAT_FORM + "_IAP"
        purchaseInfo["currency"] = skuDetails.priceCurrencyCode
        purchaseInfo["currencyPrice"] = skuDetails.price.replace(Regex("[^0-9]"), "").toDouble()
        post("$USER_CREATE_PURCHASE_URL${if (isPro) "?version=pro" else ""}", purchaseInfo, object : JSONObjectRequestListener {
            override fun onResponse(response: JSONObject?) {
                Log.i(UserApiRequest.TAG, "Create purchase success with message %s".format(response))
                requestListener.onSuccess(response)
            }

            override fun onError(anError: ANError?) {
                Log.e(UserApiRequest.TAG, "Create purchase error with error %s".format(anError!!.errorBody))
                baseErrorHandle(anError, requestListener)
            }
        })
    }
}