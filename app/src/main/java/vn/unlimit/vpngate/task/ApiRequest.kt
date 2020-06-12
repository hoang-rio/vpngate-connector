package vn.unlimit.vpngate.task

import com.androidnetworking.AndroidNetworking
import com.androidnetworking.interfaces.JSONArrayRequestListener
import com.androidnetworking.interfaces.JSONObjectRequestListener
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import vn.unlimit.vpngate.R
import java.util.*

open class ApiRequest {
    companion object {
        val jsonHeaders = HashMap<String, String>().put("Content-Type", "application/json")
        const val USER_LOGIN_URL = "/user/login"
    }

    private val apiEndPoint: String = FirebaseRemoteConfig.getInstance().getString(vn.unlimit.vpngate.App.getResourceString(R.string.cfg_paid_server_api_base_url))

    fun get(url: String, requestListener: JSONObjectRequestListener) {
        AndroidNetworking.get("$apiEndPoint$url")
                .build()
                .getAsJSONObject(requestListener)
    }

    fun get(url: String, requestListener: JSONArrayRequestListener) {
        AndroidNetworking.get("$apiEndPoint$url")
                .build()
                .getAsJSONArray(requestListener)
    }

    fun post(url: String, data: Map<String, String>, requestListener: JSONObjectRequestListener) {
        AndroidNetworking.post("$apiEndPoint$url")
                .addHeaders(jsonHeaders)
                .addApplicationJsonBody(data)
                .build()
                .getAsJSONObject(requestListener)
    }

    fun post(url: String, data: Map<String, String>, requestListener: JSONArrayRequestListener) {
        AndroidNetworking.post("$apiEndPoint$url")
                .addHeaders(jsonHeaders)
                .addApplicationJsonBody(data)
                .build()
                .getAsJSONArray(requestListener)
    }
}