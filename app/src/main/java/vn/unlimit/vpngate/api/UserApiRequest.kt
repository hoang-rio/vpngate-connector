package vn.unlimit.vpngate.api

import android.app.Activity
import android.util.Log
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.SkuDetails
import com.androidnetworking.error.ANError
import com.androidnetworking.interfaces.JSONObjectRequestListener
import org.json.JSONObject
import vn.unlimit.vpngate.App
import vn.unlimit.vpngate.request.RequestListener
import vn.unlimit.vpngate.utils.PaidServerUtil
import java.util.*
import kotlin.collections.HashMap

class UserApiRequest : BaseApiRequest() {
    companion object {
        const val TAG = "UserApiRequest"
        const val USER_LOGIN_URL = "/user/login"
        const val GET_USER_URL = "/user/get"
        const val GET_CAPTCHA_URL = "/user/captcha"
        const val USER_REGISTER_URL = "/user/register"
        const val USER_DEVICE_ADD = "/user/device/add"
        const val USER_PASSWORD_RESET = "/user/password-reset"
        const val USER_PASSWORD_FORGOT = "/user/password/forgot"
        const val PARAMS_USER_FLAT_FORM = "Android"
        const val USER_CREATE_PURCHASE_URL = "/purchase/create"
    }

    private val isPro = !App.getInstance().dataUtil.hasAds()

    fun login(username: String, password: String, requestListener: RequestListener) {
        val loginData = HashMap<String, String>()
        loginData["username"] = username
        loginData["password"] = password
        post(USER_LOGIN_URL, loginData, object : JSONObjectRequestListener {
            override fun onResponse(response: JSONObject?) {
                try {
                    paidServerUtil.setStringSetting(PaidServerUtil.SESSION_ID_KEY, response!!.get("sessionId").toString())
                    paidServerUtil.setStringSetting(PaidServerUtil.USER_INFO_KEY, response.get("user").toString())
                    paidServerUtil.setIsLoggedIn(true)
                    requestListener.onSuccess(response)
                } catch (ex: Exception) {
                    Log.e(TAG, "Process login error", ex)
                }
            }

            override fun onError(anError: ANError?) = requestListener.onError(anError!!.errorBody)
        })
    }

    fun fetchUser(requestListener: RequestListener, activity: Activity? = null) {
        get(GET_USER_URL, object : JSONObjectRequestListener {
            override fun onResponse(response: JSONObject?) {
                paidServerUtil.setStringSetting(PaidServerUtil.USER_INFO_KEY, response!!.toString())
                requestListener.onSuccess(response)
            }

            override fun onError(anError: ANError?) = errorHandle(anError, requestListener, activity)
        })
    }

    fun getCaptcha(requestListener: RequestListener, width: Int? = 120, height: Int? = 90, fontSize: Int? = 60) {
        get("$GET_CAPTCHA_URL?width=$width&height=$height&fontSize=$fontSize", object : JSONObjectRequestListener {
            override fun onResponse(response: JSONObject?) {
                requestListener.onSuccess(response)
            }

            override fun onError(anError: ANError?) {
                Log.e(TAG, "Get captcha error: {}".format(anError!!.errorDetail.toString()))
                requestListener.onError(anError.errorDetail.toString())
            }
        })
    }

    fun register(username: String, fullName: String, email: String, password: String, repassword: String, birthDay: String, timeZone: String, captchaAnswer: Int, captchaSecret: String, requestListener: RequestListener) {
        val user = HashMap<String, String>()
        user["username"] = username
        user["fullname"] = fullName
        user["email"] = email
        user["birthday"] = birthDay
        user["timezone"] = timeZone
        user["password"] = password
        user["repassword"] = repassword
        user["userPlatform"] = PARAMS_USER_FLAT_FORM
        user["language"] = Locale.getDefault().language
        val captcha = HashMap<String, String>()
        captcha["answer"] = captchaAnswer.toString()
        captcha["secret"] = captchaSecret
        val data = HashMap<String, Any>()
        data["user"] = user
        data["captcha"] = captcha
        post("$USER_REGISTER_URL${if (isPro) "?version=pro" else ""}", data, object : JSONObjectRequestListener {
            override fun onResponse(response: JSONObject?) {
                Log.d(TAG, "Register success with email: %s".format(email))
                requestListener.onSuccess(response)
            }

            override fun onError(anError: ANError?) {
                Log.e(TAG, "Register error with detail: %s".format(anError!!.errorBody))
                requestListener.onError(anError.errorBody)
            }
        })
    }

    fun activateUser(userId: String, activateCode: String, requestListener: RequestListener) {
        get("/user/$userId/activate/$activateCode", object : JSONObjectRequestListener {
            override fun onResponse(response: JSONObject?) {
                Log.d(TAG, "User activate success with response %s".format(response!!.toString()))
                requestListener.onSuccess(response)
            }

            override fun onError(anError: ANError?) {
                Log.e(TAG, "User activate error with detail: %s".format(anError!!.errorBody))
                requestListener.onError(anError.errorBody)
            }
        })
    }

    fun forgotPassword(usernameOrEmail: String, captchaSecret: String, captchaAnswer: Int, requestListener: RequestListener) {
        val data = HashMap<String, Any>()
        val captcha = HashMap<String, Any>()
        captcha["secret"] = captchaSecret
        captcha["answer"] = captchaAnswer
        data["captcha"] = captcha
        data["email"] = usernameOrEmail
        post(USER_PASSWORD_FORGOT, data, object : JSONObjectRequestListener {
            override fun onResponse(response: JSONObject?) {
                Log.d(TAG, "Forgot password success with response: %s".format(response!!.toString()))
                requestListener.onSuccess(response)
            }

            override fun onError(anError: ANError?) {
                Log.e(TAG, "Forgot password error with detail: %s".format(anError!!.errorBody))
                requestListener.onError(anError.errorBody)
            }
        })
    }

    fun checkResetPassToken(resetPassToken: String, requestListener: RequestListener) {
        get("$USER_PASSWORD_RESET/${resetPassToken}", object : JSONObjectRequestListener {
            override fun onResponse(response: JSONObject?) {
                Log.d(TAG, "Valid reset pass token with response %s".format(response!!.toString()))
                requestListener.onSuccess(response)
            }

            override fun onError(anError: ANError?) {
                Log.e(TAG, "Invalid reset pass token with detail: %s".format(anError!!.errorBody))
                requestListener.onError(anError.errorBody)
            }
        })
    }

    fun resetPassword(resetPassToken: String, newPassword: String, reNewPassword: String, requestListener: RequestListener) {
        val data = HashMap<String, Any>()
        data["resetPassToken"] = resetPassToken
        data["password"] = newPassword
        data["repassword"] = reNewPassword
        post(USER_PASSWORD_RESET, data, object: JSONObjectRequestListener {
            override fun onResponse(response: JSONObject?) {
                Log.d(TAG, "Reset pass success with response %s".format(response!!.toString()))
                requestListener.onSuccess(response)
            }

            override fun onError(anError: ANError?) {
                Log.e(TAG, "Invalid reset pass request %s".format(anError!!.errorBody))
                requestListener.onError(anError.errorBody)
            }
        })
    }

    fun addDevice(fcmPushId: String, sessionId: String) {
        val data = HashMap<String, Any>()
        data["fcmPushId"] = fcmPushId
        data["sessionId"] = sessionId
        data["platform"] = PARAMS_USER_FLAT_FORM
        val notificationSetting = HashMap<String, Boolean>()
        notificationSetting["ticket"] = true
        notificationSetting["data"] = true
        data["notificationSetting"] = notificationSetting
        post(USER_DEVICE_ADD, data, object : JSONObjectRequestListener{
            override fun onResponse(response: JSONObject?) {
                Log.d(TAG, "Add device success with message %s".format(response.toString()))
            }

            override fun onError(anError: ANError?) {
                Log.e(TAG, "Add device error with message %s".format(anError!!.errorBody))
            }
        })
    }

    fun createPurchase(purchase: Purchase, skuDetails: SkuDetails, requestListener: RequestListener) {
        val purchaseInfo = HashMap<String, Any>()
        purchaseInfo["packageId"] = purchase.sku
        purchaseInfo["purchaseId"] = purchase.orderId
        purchaseInfo["platform"] = PARAMS_USER_FLAT_FORM
        purchaseInfo["paymentMethod"] = PARAMS_USER_FLAT_FORM + "_IAP"
        purchaseInfo["currency"] = skuDetails.priceCurrencyCode
        purchaseInfo["currencyPrice"] = skuDetails.price.replace(Regex("[^0-9]"), "").toDouble()
        post("$USER_CREATE_PURCHASE_URL${if (isPro) "?version=pro" else ""}", purchaseInfo, object: JSONObjectRequestListener {
            override fun onResponse(response: JSONObject?) {
                Log.i(TAG, "Create purchase success with message %s".format(response))
                requestListener.onSuccess(response)
            }

            override fun onError(anError: ANError?) {
                Log.e(TAG, "Create purchase error with error %s".format(anError!!.errorBody))
                requestListener.onSuccess(anError.errorBody)
            }

        })
    }
}