package vn.unlimit.vpngate.viewmodels

import android.app.Application
import androidx.lifecycle.MutableLiveData
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.SkuDetails
import com.google.gson.reflect.TypeToken
import org.json.JSONObject
import vn.unlimit.vpngate.api.PurchaseApiRequest
import vn.unlimit.vpngate.models.PurchaseHistory
import vn.unlimit.vpngate.request.RequestListener

class PurchaseViewModel(application: Application) : BaseViewModel(application) {
    var errorCode: Int? = null
    var purchaseApiRequest: PurchaseApiRequest = PurchaseApiRequest()
    var purchaseList: MutableLiveData<ArrayList<PurchaseHistory>> = MutableLiveData(ArrayList())
    var isOutOfData = false

    fun createPurchase(purchase: Purchase, skuDetails: SkuDetails) {
        isLoading.value = true
        errorCode = null
        purchaseApiRequest.createPurchase(purchase, skuDetails, object : RequestListener {
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

    fun listPurchase(fromStart: Boolean? = false) {
        if (isOutOfData && !fromStart!! || isLoading.value!!) {
            return
        }
        isLoading.value = true
        val take = ITEM_PER_PAGE
        val skip = if (fromStart!!) 0 else purchaseList.value!!.size
        purchaseApiRequest.listPurchase(take, skip, object : RequestListener {
            override fun onSuccess(result: Any?) {
                val resultObj = result as JSONObject
                var purchaseList = ArrayList<PurchaseHistory>()
                if (skip != 0) {
                    purchaseList = this@PurchaseViewModel.purchaseList.value!!
                }
                val resListPurchase: ArrayList<PurchaseHistory> = paidServerUtil.gson.fromJson(
                        resultObj.getString("listPurchase"),
                        object : TypeToken<ArrayList<PurchaseHistory>>() {}.type
                )
                purchaseList.addAll(resListPurchase)
                this@PurchaseViewModel.purchaseList.value = purchaseList
                isOutOfData = resultObj.getJSONArray("listPurchase").length() < take
                isLoading.value = false
            }

            override fun onError(error: String?) {
                baseErrorHandle(error)
                isLoading.value = false
            }
        })
    }
}