package vn.unlimit.vpngate.viewmodels

import android.app.Application
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.json.JSONObject
import vn.unlimit.vpngate.api.SessionApiRequest
import vn.unlimit.vpngate.models.ConnectedSession
import vn.unlimit.vpngate.request.RequestListener
import java.lang.reflect.Type

open class SessionViewModel(application: Application) : BaseViewModel(application) {
    var sessionList: MutableLiveData<LinkedHashSet<ConnectedSession>> = MutableLiveData(LinkedHashSet())
    var sessionApiRequest = SessionApiRequest()
    var isError = MutableLiveData(false)

    fun getListSession() {
        if (isLoading.value!!) {
            return
        }
        isLoading.value = true
        isError.value = false
        sessionApiRequest.getList(object : RequestListener {
            override fun onSuccess(result: Any?) {
                val type: Type = object : TypeToken<LinkedHashSet<ConnectedSession?>?>() {}.type
                val listSessionArray = (result as JSONObject).getJSONArray("listSession")
                sessionList.value = Gson().fromJson(listSessionArray.toString(), type)
                isLoading.value = false
            }

            override fun onError(error: String?) {
                isError.value = true
                isLoading.value = false
            }
        })
    }

    fun deleteSession(sessionId: String, requestListener: RequestListener) {
        sessionApiRequest.deleteSession(sessionId, requestListener)
    }
}