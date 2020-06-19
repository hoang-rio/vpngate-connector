package vn.unlimit.vpngate.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import vn.unlimit.vpngate.App
import vn.unlimit.vpngate.api.BaseApiRequest
import vn.unlimit.vpngate.api.UserApiRequest
import vn.unlimit.vpngate.request.RequestListener

class UserViewModel(application: Application) : AndroidViewModel(application) {
    companion object {
        const val TAG = "PaidServerViewModel"
    }

    private val paidServerDataUtil = App.getInstance().paidServerUtil
    private val userApiRequest = UserApiRequest()
    var isLoggedIn: MutableLiveData<Boolean> = MutableLiveData(paidServerDataUtil.isLoggedIn())
    var isLoggingIn: MutableLiveData<Boolean> = MutableLiveData(false)

    fun login(username: String, password: String) {
        userApiRequest.login(username, password, object : RequestListener {
            override fun onSuccess(result: Any?) {
                Log.e(TAG, result.toString())
                isLoggingIn.value = false
                isLoggedIn.value = true
                paidServerDataUtil.setIsLoggedIn(true)
            }

            override fun onError(error: String) {
                Log.e(TAG, error)
                isLoggingIn.value = false
            }
        })
    }

    fun fetchUser() {
        userApiRequest.fetchUser(object : RequestListener {
            override fun onSuccess(result: Any?) {
                Log.d(TAG, "fetch user success")
            }

            override fun onError(error: String?) {
                if (error!! == BaseApiRequest.ERROR_SESSION_EXPIRES) {
                    isLoggedIn.value = false
                    paidServerDataUtil.setIsLoggedIn(false)
                }
                Log.e(TAG, "fetch user error with error {}".format(error))
            }
        })
    }

}