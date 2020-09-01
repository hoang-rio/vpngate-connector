package vn.unlimit.vpngate.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import org.json.JSONObject
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
    var isLoading: MutableLiveData<Boolean> = MutableLiveData(false)
    var isRegisterSuccess: MutableLiveData<Boolean> = MutableLiveData(false)
    var errorList: MutableLiveData<JSONObject> = MutableLiveData(JSONObject())
    var isUserActivated: MutableLiveData<Boolean> = MutableLiveData(false)
    var isPasswordReseted: MutableLiveData<Boolean> = MutableLiveData(false)
    var errorCode: Int? = null

    fun login(username: String, password: String) {
        isLoading.value = true
        userApiRequest.login(username, password, object : RequestListener {
            override fun onSuccess(result: Any?) {
                Log.e(TAG, result.toString())
                isLoggedIn.value = true
                paidServerDataUtil.setIsLoggedIn(true)
                isLoading.value = false
            }

            override fun onError(error: String) {
                Log.e(TAG, error)
                errorList.value = JSONObject(error)
                isLoading.value = false
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

    fun register(username: String, fullName: String, email: String, password: String, repassword: String, birthDay: String, timeZone: String, captchaAnswer: Int, captchaSecret: String) {
        isLoading.value = true
        userApiRequest.register(username, fullName, email, password, repassword, birthDay, timeZone, captchaAnswer, captchaSecret, object : RequestListener {
            override fun onSuccess(result: Any?) {
                isLoading.value = true
                isRegisterSuccess.value = true
            }

            override fun onError(error: String?) {
                val errorResponse = JSONObject(error!!.toString())
                if (errorResponse.has("errorList")) {
                    errorList.value = errorResponse.get("errorList") as JSONObject
                }
                isLoading.value = false
                isRegisterSuccess.value = false
            }
        })
    }

    fun activateUser(userId: String, activateCode: String) {
        isLoading.value = true
        isUserActivated.value = false
        userApiRequest.activateUser(userId, activateCode, object : RequestListener {
            override fun onSuccess(result: Any?) {
                isUserActivated.value = !((result as JSONObject).has("result") && !result.getBoolean("result"))
                if (!isUserActivated.value!!) {
                    errorCode = result.getInt("errorCode")
                }
                isLoading.value = false
            }

            override fun onError(error: String?) {
                isLoading.value = false
                isUserActivated.value = true
            }
        })
    }

    fun resetPassword(resetPassToken: String, newPassword: String) {
        isLoading.value = true
        isPasswordReseted.value = false

    }

}