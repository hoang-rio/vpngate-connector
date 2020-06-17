package vn.unlimit.vpngate.task

import com.androidnetworking.error.ANError
import com.androidnetworking.interfaces.JSONObjectRequestListener
import org.json.JSONObject
import vn.unlimit.vpngate.request.RequestListener
import vn.unlimit.vpngate.utils.PaidServerUtil

class UserApiRequest : ApiRequest() {
    companion object {
        const val GET_USER_URL = "/user/get"
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