package vn.unlimit.vpngate.api

import com.androidnetworking.error.ANError
import com.androidnetworking.interfaces.JSONObjectRequestListener
import org.json.JSONObject
import vn.unlimit.vpngate.request.RequestListener
import vn.unlimit.vpngate.utils.PaidServerUtil

class UserApiRequest : BaseApiRequest() {
    companion object {
        const val USER_LOGIN_URL = "/user/login"
        const val GET_USER_URL = "/user/get"
    }

    fun login(username: String, password: String, requestListener: RequestListener) {
        val loginData = HashMap<String, String>()
        loginData["username"] = username
        loginData["password"] = password
        post(USER_LOGIN_URL, loginData, object : JSONObjectRequestListener {
            override fun onResponse(response: JSONObject?) {
                paidServerUtil.setStringSetting(PaidServerUtil.SESSION_ID_KEY, response!!.get("sessionId").toString())
                paidServerUtil.setStringSetting(PaidServerUtil.USER_INFO_KEY, response.get("user").toString())
                paidServerUtil.setIsLoggedIn(true)
                requestListener.onSuccess(response)
            }

            override fun onError(anError: ANError?) = requestListener.onError(anError!!.toString())
        })
    }

    fun fetchUser(requestListener: RequestListener) {
        get(GET_USER_URL, object : JSONObjectRequestListener {
            override fun onResponse(response: JSONObject?) {
                paidServerUtil.setStringSetting(PaidServerUtil.USER_INFO_KEY, response!!.toString())
                requestListener.onSuccess(response)
            }

            override fun onError(anError: ANError?) = errorHandle(anError, requestListener)
        })
    }
}