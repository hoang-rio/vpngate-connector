package vn.unlimit.vpngate.viewmodels

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import com.google.firebase.analytics.FirebaseAnalytics
import org.json.JSONObject
import vn.unlimit.vpngate.R
import vn.unlimit.vpngate.activities.paid.LoginActivity
import vn.unlimit.vpngate.api.UserApiRequest
import vn.unlimit.vpngate.request.RequestListener
import vn.unlimit.vpngate.utils.PaidServerUtil
import java.util.*


class UserViewModel(application: Application) : BaseViewModel(application) {
    companion object {
        const val TAG = "UserViewModel"
        const val USER_CACHE_TIME = 10 * 60 * 1000 // 10 Minute
    }

    private val userApiRequest = UserApiRequest()
    var userInfo: MutableLiveData<JSONObject?> = MutableLiveData(paidServerUtil.getUserInfo())

    var isRegisterSuccess: MutableLiveData<Boolean> = MutableLiveData(false)
    var errorList: MutableLiveData<JSONObject> = MutableLiveData(JSONObject())
    var isUserActivated: MutableLiveData<Boolean> = MutableLiveData(false)
    var isForgotPassSuccess: MutableLiveData<Boolean> = MutableLiveData(false)
    var isPasswordReset: MutableLiveData<Boolean> = MutableLiveData(false)
    var isValidResetPassToken: MutableLiveData<Boolean> = MutableLiveData(false)
    var errorCode: Int? = null
    var isProfileUpdate = false

    fun login(username: String, password: String) {
        isLoading.value = true
        userApiRequest.login(username, password, object : RequestListener {
            override fun onSuccess(result: Any?) {
                Log.d(TAG, "Login success with response %s".format(result!!.toString()))
                val userInfoRes = (result as JSONObject).getJSONObject("user")
                userInfo.value = userInfoRes
                paidServerUtil.setUserInfo(userInfoRes)
                isLoggedIn.value = true
                paidServerUtil.setIsLoggedIn(true)
                isLoading.value = false
            }

            override fun onError(error: String) {
                Log.e(TAG, "Login failure with error %s".format(error))
                try {
                    isLoggedIn.value = false
                    errorList.value = JSONObject(error)
                    isLoading.value = false
                } catch (e: Exception) {
                    isLoading.value = false
                }
            }
        })
    }

    fun localLogout(activity: Activity?) {
        isLoggedIn.value = false
        paidServerUtil.setIsLoggedIn(false)
        paidServerUtil.removeSetting(PaidServerUtil.USER_INFO_KEY)
        userInfo.value = null
        if (activity != null && !activity.isFinishing) {
            val intentLogin = Intent(activity, LoginActivity::class.java)
            activity.startActivity(intentLogin)
            activity.finish()
        }
    }

    fun logout(activity: Activity?) {
        userApiRequest.logout(object : RequestListener {
            override fun onSuccess(result: Any?) {
                localLogout(activity)
            }

            override fun onError(error: String?) {
                //Nothing todo here
                localLogout(activity)
            }
        })
    }

    fun fetchUser(
        updateLoading: Boolean = false,
        activity: Activity? = null,
        forceFetch: Boolean = false
    ) {
        val lastFetchTime = paidServerUtil.getLongSetting(PaidServerUtil.LAST_USER_FETCH_TIME)
        var date = Calendar.getInstance().time
        var nowInMs = date.time
        if (!forceFetch && lastFetchTime + USER_CACHE_TIME > nowInMs || updateLoading && isLoading.value!!) {
            // Skip fetch user user in cache
            return
        }
        if (updateLoading) {
            isLoading.value = true
        }
        userApiRequest.fetchUser(object : RequestListener {
            override fun onSuccess(result: Any?) {
                Log.d(TAG, "fetch user success with response %s".format(result!!.toString()))
                val userInfoRes = (result as JSONObject)
                userInfo.value = userInfoRes
                paidServerUtil.setUserInfo(userInfoRes)
                if (updateLoading) {
                    isLoading.value = false
                }
                date = Calendar.getInstance().time
                nowInMs = date.time
                paidServerUtil.setLongSetting(PaidServerUtil.LAST_USER_FETCH_TIME, nowInMs)
                isProfileUpdate = false
            }

            override fun onError(error: String?) {
                val params = Bundle()
                params.putString("username", paidServerUtil.getUserInfo()?.getString("username"))
                params.putString("errorInfo", error)
                FirebaseAnalytics.getInstance(getApplication())
                    .logEvent("Paid_Server_Fetch_User_Error", params)
                baseErrorHandle(error)
                Log.e(TAG, "fetch user error with error %s".format(error))
                if (updateLoading) {
                    isLoading.value = false
                }
            }
        }, activity)
    }

    fun register(
        username: String,
        fullName: String,
        email: String,
        password: String,
        repassword: String,
        birthDay: String,
        timeZone: String,
        captchaAnswer: Int,
        captchaSecret: String
    ) {
        isLoading.value = true
        userApiRequest.register(
            username,
            fullName,
            email,
            password,
            repassword,
            birthDay,
            timeZone,
            captchaAnswer,
            captchaSecret,
            object : RequestListener {
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
                isUserActivated.value =
                    !((result as JSONObject).has("result") && !result.getBoolean("result"))
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
        userApiRequest.forgotPassword(
            usernameOrEmail,
            captchaSecret,
            captchaAnswer,
            object : RequestListener {
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
        isPasswordReset.value = false
        userApiRequest.resetPassword(
            resetPassToken,
            newPassword,
            renewPassword,
            object : RequestListener {
                override fun onSuccess(result: Any?) {
                    isPasswordReset.value = true
                    isLoading.value = false
                }

                override fun onError(error: String?) {
                    val errorResponse = JSONObject(error!!.toString())
                    if (errorResponse.has("errorList")) {
                        errorList.value = errorResponse.get("errorList") as JSONObject
                    }
                    isPasswordReset.value = false
                    isLoading.value = false
                }
            })
    }

    fun changePass(password: String, newPassword: String, activity: Activity) {
        isLoading.value = true
        userApiRequest.changePass(password, newPassword, object : RequestListener {
            override fun onSuccess(result: Any?) {
                localLogout(activity)
                Toast.makeText(
                    activity,
                    activity.getText(R.string.password_changed),
                    Toast.LENGTH_LONG
                ).show()
                isLoading.value = false
            }

            override fun onError(error: String?) {
                Toast.makeText(
                    activity,
                    activity.getText(R.string.incorrect_current_password),
                    Toast.LENGTH_LONG
                ).show()
                isLoading.value = false
            }
        })
    }

    fun updateProfile(fullName: String, birthDay: String, timeZone: String, requestListener: RequestListener) {
        isLoading.value = true
        userApiRequest.updateProfile(fullName, birthDay, timeZone, object : RequestListener {
            override fun onSuccess(result: Any?) {
                val json = result as JSONObject
                if (json.has("user")) {
                    userInfo.value = json.getJSONObject("user")
                    paidServerUtil.setUserInfo(json.getJSONObject("user"))
                    isProfileUpdate = true
                    requestListener.onSuccess(json.getJSONObject("user"))
                } else {
                    requestListener.onError("")
                }
                isLoading.value = false
            }

            override fun onError(error: String?) {
                requestListener.onError("")
                isLoading.value = false
            }
        })
    }

    fun deleteAccount(requestListener: RequestListener) {
        isLoading.value = true
        userApiRequest.deleteAccount(object: RequestListener{
            override fun onSuccess(result: Any?) {
                isLoading.value = false
                requestListener.onSuccess(true)
            }
            override fun onError(error: String?) {
                isLoading.value = false
                requestListener.onSuccess(false)
            }
        })
    }
}
