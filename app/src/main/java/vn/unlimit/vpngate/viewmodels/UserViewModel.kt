package vn.unlimit.vpngate.viewmodels

import android.app.Activity
import android.app.Application
import android.util.Log
import androidx.annotation.NonNull
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.SkuDetails
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.messaging.FirebaseMessaging
import org.json.JSONObject
import vn.unlimit.vpngate.App
import vn.unlimit.vpngate.api.BaseApiRequest
import vn.unlimit.vpngate.api.UserApiRequest
import vn.unlimit.vpngate.request.RequestListener
import vn.unlimit.vpngate.utils.PaidServerUtil
import java.util.*


class UserViewModel(application: Application) : AndroidViewModel(application) {
    companion object {
        const val TAG = "UserViewModel"
        const val USER_CACHE_TIME = 10 * 60 * 1000 // 10 Minute
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
                try {
                    errorList.value = JSONObject(error)
                    isLoading.value = false
                } catch (e: Exception) {
                    isLoading.value = false
                }
            }
        })
    }

    fun fetchUser(updateLoading: Boolean = false, activity: Activity? = null, forceFetch: Boolean = false) {
        val lastFetchTime = paidServerDataUtil.getLongSetting(PaidServerUtil.LAST_USER_FETCH_TIME)
        var date = Calendar.getInstance().time
        var nowInMs = date.time
        if (!forceFetch && lastFetchTime!! + USER_CACHE_TIME > nowInMs || updateLoading && isLoading.value!!) {
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
                paidServerDataUtil.setUserInfo(userInfoRes)
                if (updateLoading) {
                    isLoading.value = false
                }
                date = Calendar.getInstance().time
                nowInMs = date.time
                paidServerDataUtil.setLongSetting(PaidServerUtil.LAST_USER_FETCH_TIME, nowInMs)
            }

            override fun onError(error: String?) {
                if (error!! == BaseApiRequest.ERROR_SESSION_EXPIRES) {
                    isLoggedIn.value = false
                    paidServerDataUtil.setIsLoggedIn(false)
                }
                Log.e(TAG, "fetch user error with error %s".format(error))
                if (updateLoading) {
                    isLoading.value = false
                }
            }
        }, activity)
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


    fun addDevice() {
        FirebaseMessaging.getInstance().token
                .addOnCompleteListener(object : OnCompleteListener<String?> {
                    override fun onComplete(@NonNull task: Task<String?>) {
                        if (!task.isSuccessful) {
                            Log.w(TAG, "Fetching FCM registration token failed", task.exception)
                            return
                        }

                        // Get new FCM registration token
                        val token: String? = task.result

                        // Log and toast
                        Log.d(TAG, "After login addDevice with fcmId: %s".format(token))
                        if (token != null) {
                            val sessionId = paidServerDataUtil.getStringSetting(PaidServerUtil.SESSION_ID_KEY, "")
                            if (sessionId != null) {
                                userApiRequest.addDevice(token, sessionId)
                            }
                        }
                    }
                })
    }

    fun createPurchase(purchase: Purchase, skuDetails: SkuDetails) {
        isLoading.value = true
        errorCode = null
        userApiRequest.createPurchase(purchase, skuDetails, object: RequestListener {
            override fun onSuccess(result: Any?) {
                val resultJSon = result as JSONObject
                if (resultJSon.getBoolean("result")) {
                    // Purchase success => update user data info
                    fetchUser(forceFetch = true)
                } else {
                    errorCode = resultJSon.getInt("errorCode")
                }
                isLoading.value = false
            }

            override fun onError(error: String?) {
                errorCode = 1
                isLoading.value = false
            }

        })
    }


}