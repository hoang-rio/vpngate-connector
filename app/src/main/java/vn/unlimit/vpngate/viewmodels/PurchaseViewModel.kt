package vn.unlimit.vpngate.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch
import org.json.JSONObject
import retrofit2.HttpException
import vn.unlimit.vpngate.App
import vn.unlimit.vpngate.api.PurchaseApiService
import vn.unlimit.vpngate.models.PurchaseHistory
import vn.unlimit.vpngate.models.request.PurchaseCreateRequest
import vn.unlimit.vpngate.request.RequestListener

class PurchaseViewModel(application: Application) : BaseViewModel(application) {
    companion object {
        const val TAG = "PurchaseViewModel"
    }
    var errorCode: Int? = null
    private var purchaseApiService: PurchaseApiService = retrofit.create(PurchaseApiService::class.java)
    var purchaseList: MutableLiveData<ArrayList<PurchaseHistory>> = MutableLiveData(ArrayList())
    var isOutOfData = false

    fun createPurchase(purchase: Purchase, productDetails: ProductDetails) {
        isLoading.value = true
        errorCode = null
        viewModelScope.launch {
            try {
                val isPro = !App.getInstance().dataUtil.hasAds()
                val res = purchaseApiService.createPurchase(PurchaseCreateRequest(
                    packageId = purchase.products[0],
                    purchaseId = purchase.orderId!!,
                    platform = PARAMS_USER_PLATFORM,
                    paymentMethod = PARAMS_USER_PLATFORM + "_IAP",
                    currency = productDetails.oneTimePurchaseOfferDetails?.priceCurrencyCode!!,
                    currencyPrice = productDetails.oneTimePurchaseOfferDetails?.priceAmountMicros!!.toDouble() / 1000000
                ), version = if (isPro) "pro" else null)
                if (!res.result) {
                    errorCode = res.errorCode
                }
            } catch (e: Exception) {
                Log.e(TAG, "Got exception when create purchase", e)
                errorCode = 1
            } finally {
                isLoading.postValue(false)
            }
        }
    }

    fun listPurchase(fromStart: Boolean? = false) {
        if (isOutOfData && !fromStart!! || isLoading.value!!) {
            return
        }
        isLoading.value = true
        val take = ITEM_PER_PAGE
        val skip = if (fromStart!!) 0 else purchaseList.value!!.size
        viewModelScope.launch {
            try {
                var purchases = ArrayList<PurchaseHistory>()
                if (skip != 0) {
                    purchases = purchaseList.value!!
                }
                val res = purchaseApiService.listPurchase(take, skip)
                purchases.addAll(res.listPurchase)
                purchaseList.postValue(purchases)
                isOutOfData = res.listPurchase.size < take
            } catch (e: Exception) {
                Log.e(TAG, "Got exception when get purchase list", e)
            } finally {
                isLoading.postValue(false)
            }
        }
    }
}