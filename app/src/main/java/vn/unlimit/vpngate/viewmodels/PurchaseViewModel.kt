package vn.unlimit.vpngate.viewmodels

import android.app.Application
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.SkuDetails
import org.json.JSONObject
import vn.unlimit.vpngate.api.PurchaseApiRequest
import vn.unlimit.vpngate.request.RequestListener

class PurchaseViewModel(application: Application): BaseViewModel(application) {
    var errorCode: Int? = null
    var purchaseApiRequest: PurchaseApiRequest = PurchaseApiRequest()

    fun createPurchase(purchase: Purchase, skuDetails: SkuDetails) {
        isLoading.value = true
        errorCode = null
        purchaseApiRequest.createPurchase(purchase, skuDetails, object: RequestListener {
            override fun onSuccess(result: Any?) {
                val resultJSon = result as JSONObject
                if (!resultJSon.getBoolean("result")) {
                    errorCode = resultJSon.getInt("errorCode")
                }
                isLoading.value = false
            }

            override fun onError(error: String?) {
                baseErrorHandle(error)
                errorCode = 1
                isLoading.value = false
            }
        })
    }
}