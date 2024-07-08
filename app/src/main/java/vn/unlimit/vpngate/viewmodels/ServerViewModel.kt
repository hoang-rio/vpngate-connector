package vn.unlimit.vpngate.viewmodels

import android.app.Application
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.analytics.FirebaseAnalytics
import kotlinx.coroutines.launch
import retrofit2.HttpException
import vn.unlimit.vpngate.activities.paid.PaidServerActivity
import vn.unlimit.vpngate.api.ServerApiService
import vn.unlimit.vpngate.models.PaidServer

class ServerViewModel(application: Application) : BaseViewModel(application) {
    var serverList: MutableLiveData<LinkedHashSet<PaidServer>> =
        MutableLiveData(paidServerUtil.getServersCache())
    private val serverApiService = retrofit.create(ServerApiService::class.java)
    private var isOutOfData: Boolean = false

    companion object {
        const val TAG = "ServerViewModel"
    }

    fun loadServer(activity: PaidServerActivity, loadFromStart: Boolean = false) {
        if (isLoading.value!!) {
            return
        }
        if (!isOutOfData || loadFromStart) {
            isLoading.value = true
            var skip = serverList.value?.size
            if (loadFromStart) {
                skip = 0
                isOutOfData = false
            }
            viewModelScope.launch {
                try {
                    val loadServerResponse = serverApiService.loadServer(take = ITEM_PER_PAGE, skip)
                    if (loadFromStart) {
                        serverList.postValue(loadServerResponse.listServer)
                    } else {
                        serverList.value?.addAll(loadServerResponse.listServer)
                        serverList.postValue(serverList.value)
                    }
                    if (serverList.value != null) {
                        paidServerUtil.setServersCache(serverList.value!!)
                    }
                    isOutOfData = serverList.value?.size!! >= loadServerResponse.countServer
                } catch (e: HttpException) {
                    Log.e(TAG, "Got HttpException when load server", e)
                    handleExpiresError(e.code(), activity)
                    val params = Bundle()
                    params.putString(
                        "username",
                        paidServerUtil.getUserInfo()?.username
                    )
                    params.putString("errorInfo", e.message)
                    FirebaseAnalytics.getInstance(getApplication())
                        .logEvent("Paid_Server_List_Server_Error", params)
                } catch (e: Throwable) {
                    Log.e(TAG, "Got HttpException when load server", e)
                } finally {
                    isLoading.postValue(false)
                }
            }
        }
    }
}