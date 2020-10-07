package vn.unlimit.vpngate.api

import android.app.Activity
import android.util.Log
import com.androidnetworking.error.ANError
import com.androidnetworking.interfaces.JSONObjectRequestListener
import org.json.JSONObject
import vn.unlimit.vpngate.BuildConfig
import vn.unlimit.vpngate.request.RequestListener
import vn.unlimit.vpngate.utils.PaidServerUtil

class UserApiRequest : BaseApiRequest() {
    companion object {
        const val TAG = "UserApiRequest"
        const val USER_LOGIN_URL = "/user/login"
        const val GET_USER_URL = "/user/get"
        const val GET_CAPTCHA_URL = "/user/captcha"
        const val USER_REGISTER_URL = "/user/register"
    }

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
        val isPro = BuildConfig.FLAVOR == "pro"
        val user = HashMap<String, String>()
        user["username"] = username
        user["fullname"] = fullName
        user["email"] = email
        user["birthday"] = birthDay
        user["timezone"] = timeZone
        user["password"] = password
        user["repassword"] = repassword
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
        post("/user/password/forgot", data, object : JSONObjectRequestListener {
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
        get("/user/password-reset/${resetPassToken}", object : JSONObjectRequestListener {
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
        post("/user/password-reset", data, object: JSONObjectRequestListener {
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
}