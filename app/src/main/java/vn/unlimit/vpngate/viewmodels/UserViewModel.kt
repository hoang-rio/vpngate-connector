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
    var userInfo: MutableLiveData<JSONObject?> = MutableLiveData(paidServerDataUtil.getUserInfo())
    var isLoggedIn: MutableLiveData<Boolean> = MutableLiveData(paidServerDataUtil.isLoggedIn())
    var isLoading: MutableLiveData<Boolean> = MutableLiveData(false)
    var isRegisterSuccess: MutableLiveData<Boolean> = MutableLiveData(false)
    var errorList: MutableLiveData<JSONObject> = MutableLiveData(JSONObject())
    var isUserActivated: MutableLiveData<Boolean> = MutableLiveData(false)
    var isForgotPassSuccess: MutableLiveData<Boolean> = MutableLiveData(false)
    var isPasswordReseted: MutableLiveData<Boolean> = MutableLiveData(false)
    var isValidResetPassToken: MutableLiveData<Boolean> = MutableLiveData(false)
    var errorCode: Int? = null

    fun login(username: String, password: String) {
        isLoading.value = true
        userApiRequest.login(username, password, object : RequestListener {
            override fun onSuccess(result: Any?) {
                Log.e(TAG, "Login success with response %s".format(result!!.toString()))
                val userInfoRes = (result as JSONObject).getJSONObject("user")
                userInfo.value = userInfoRes
                paidServerDataUtil.setUserInfo(userInfoRes)
                isLoggedIn.value = true
                paidServerDataUtil.setIsLoggedIn(true)
                isLoading.value = false
            }

            override fun onError(error: String) {
                Log.e(TAG, "Login failure with error %s".format(error))
                errorList.value = JSONObject(error)
                isLoading.value = false
            }
        })
    }

    fun fetchUser() {
        userApiRequest.fetchUser(object : RequestListener {
            override fun onSuccess(result: Any?) {
                Log.d(TAG, "fetch user success with response %s".format(result!!.toString()))
                val userInfoRes = (result as JSONObject)
                userInfo.value = userInfoRes
                paidServerDataUtil.setUserInfo(userInfoRes)
            }

            override fun onError(error: String?) {
                if (error!! == BaseApiRequest.ERROR_SESSION_EXPIRES) {
                    isLoggedIn.value = false
                    paidServerDataUtil.setIsLoggedIn(false)
                }
                Log.e(TAG, "fetch user error with error %s".format(error))
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

    fun forgotPassword(usernameOrEmail: String, captchaSecret: String, captchaAnswer: Int) {
        isLoading.value = true
        isForgotPassSuccess.value = false
        userApiRequest.forgotPassword(usernameOrEmail, captchaSecret, captchaAnswer, object : RequestListener {
            override fun onSuccess(result: Any?) {
                isLoading.value = false
                isForgotPassSuccess.value = true
            }

            override fun onError(error: String?) {
                isLoading.value = false
                isForgotPassSuccess.value = false
            }
        })
    }

    fun checkResetPassToken(resetPassToken: String) {
        userApiRequest.checkResetPassToken(resetPassToken, object : RequestListener {
            override fun onSuccess(result: Any?) {
                isValidResetPassToken.value = true
            }

            override fun onError(error: String?) {
                isValidResetPassToken.value = false
            }
        })
    }

    fun resetPassword(resetPassToken: String, newPassword: String, renewPassword: String) {
        isLoading.value = true
        isPasswordReseted.value = false
        userApiRequest.resetPassword(resetPassToken, newPassword, renewPassword, object : RequestListener {
            override fun onSuccess(result: Any?) {
                isPasswordReseted.value = true
                isLoading.value = false
            }

            override fun onError(error: String?) {
                val errorResponse = JSONObject(error!!.toString())
                if (errorResponse.has("errorList")) {
                    errorList.value = errorResponse.get("errorList") as JSONObject
                }
                isPasswordReseted.value = false
                isLoading.value = false
            }
        })
    }

}