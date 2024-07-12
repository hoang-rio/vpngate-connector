package vn.unlimit.vpngate.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import vn.unlimit.vpngate.api.SessionApiService
import vn.unlimit.vpngate.models.ConnectedSession
import vn.unlimit.vpngate.request.RequestListener

class SessionViewModel(application: Application) : BaseViewModel(application) {
    companion object {
        const val TAG = "SessionViewModel"
    }

    var sessionList: MutableLiveData<LinkedHashSet<ConnectedSession>> =
        MutableLiveData(LinkedHashSet())
    private var sessionApiService = retrofit.create(SessionApiService::class.java)
    var isError = MutableLiveData(false)

    fun getListSession() {
        if (isLoading.value!!) {
            return
        }
        isLoading.value = true
        isError.value = false
        viewModelScope.launch {
            try {
                val listSessionResponse = sessionApiService.getList()
                sessionList.postValue(listSessionResponse.listSession)
            } catch (e: Throwable) {
                Log.e(TAG, "Got exception when get list session", e)
                isError.postValue(true)
            } finally {
                isLoading.postValue(false)
            }
        }
    }

    fun deleteSession(sessionId: String, requestListener: RequestListener) {
        viewModelScope.launch {
            try {
                sessionApiService.deleteSession(sessionId)
                Log.d(TAG, "Deleted session with sessionId %s".format(sessionId))
                requestListener.onSuccess("")
            } catch (e: Throwable) {
                Log.e(TAG, "Got exception when delete session", e)
                requestListener.onError(e.message)
            }
        }
    }
}