package vn.unlimit.vpngate.viewmodels

import android.app.Application
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.json.JSONArray
import org.json.JSONObject
import vn.unlimit.vpngate.activities.paid.PaidServerActivity
import vn.unlimit.vpngate.api.ServerApiRequest
import vn.unlimit.vpngate.models.PaidServer
import vn.unlimit.vpngate.request.RequestListener
import java.lang.reflect.Type

class ServerViewModel(application: Application) : BaseViewModel(application) {
    var serverList: MutableLiveData<LinkedHashSet<PaidServer>> =
        MutableLiveData(paidServerUtil.getServersCache())
    private val serverApiRequest = ServerApiRequest()
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
            serverApiRequest.loadServer(ITEM_PER_PAGE, skip, object : RequestListener {
                override fun onSuccess(result: Any?) {
                    val type: Type = object : TypeToken<LinkedHashSet<PaidServer?>?>() {}.type
                    val listServerArray = (result as JSONObject).get("listServer") as JSONArray
                    val listServer: LinkedHashSet<PaidServer> =
                        Gson().fromJson(listServerArray.toString(), type)
                    if (loadFromStart) {
                        serverList.value = listServer
                    } else {
                        serverList.value?.addAll(listServer)
                        serverList.value = serverList.value
                    }
                    if (serverList.value != null) {
                        paidServerUtil.setServersCache(serverList.value!!)
                    }
                    isOutOfData = serverList.value?.size!! >= result.get("countServer") as Int
                    isLoading.value = false
                }

                override fun onError(error: String?) {
                    baseErrorHandle(error)
                    isLoading.value = false
                    val params = Bundle()
                    params.putString(
                        "username",
                        paidServerUtil.getUserInfo()?.getString("username")
                    )
                    params.putString("errorInfo", error)
                    FirebaseAnalytics.getInstance(getApplication())
                        .logEvent("Paid_Server_List_Server_Error", params)
                    Log.e(TAG, "Load paid server error with message: %s".format(error))
                }
            }, activity)
        }
    }
}