package vn.unlimit.vpngate.api

import android.app.Activity
import android.content.Intent
import com.androidnetworking.AndroidNetworking
import com.androidnetworking.error.ANError
import com.androidnetworking.interfaces.JSONArrayRequestListener
import com.androidnetworking.interfaces.JSONObjectRequestListener
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import vn.unlimit.vpngate.App
import vn.unlimit.vpngate.R
import vn.unlimit.vpngate.activities.paid.LoginActivity
import vn.unlimit.vpngate.request.RequestListener
import vn.unlimit.vpngate.utils.PaidServerUtil

open class BaseApiRequest {
    companion object {
        val jsonHeaders = HashMap<String, String>().put("Content-Type", "application/json")
        const val ERROR_SESSION_EXPIRES = "ERROR_SESSION_EXPIRES"
        const val PARAMS_USER_FLAT_FORM = "Android"
    }

    val paidServerUtil: PaidServerUtil = App.getInstance().paidServerUtil
    val isPro = !App.getInstance().dataUtil.hasAds()
    private val sessionHeaderName = paidServerUtil.getSessionHeaderName()

    private val apiEndPoint: String = FirebaseRemoteConfig.getInstance()
        .getString(App.getResourceString(R.string.cfg_paid_server_api_base_url))

    fun get(url: String, requestListener: JSONObjectRequestListener) {
        val networkRequest = AndroidNetworking.get("$apiEndPoint$url")
        if (paidServerUtil.isLoggedIn()) {
            networkRequest.addHeaders(
                sessionHeaderName,
                paidServerUtil.getStringSetting(PaidServerUtil.SESSION_ID_KEY)
            )
        }
        networkRequest
            .build()
            .getAsJSONObject(requestListener)
    }

    fun get(url: String, requestListener: JSONArrayRequestListener) {
        val networkRequest = AndroidNetworking.get("$apiEndPoint$url")
        if (paidServerUtil.isLoggedIn()) {
            networkRequest.addHeaders(
                sessionHeaderName,
                paidServerUtil.getStringSetting(PaidServerUtil.SESSION_ID_KEY)
            )
        }
        networkRequest.build().getAsJSONArray(requestListener)
    }

    fun post(url: String, data: Map<String, Any>, requestListener: JSONObjectRequestListener) {
        val networkRequest = AndroidNetworking.post("$apiEndPoint$url")
            .addHeaders(jsonHeaders)
            .addApplicationJsonBody(data)
        if (paidServerUtil.isLoggedIn()) {
            networkRequest.addHeaders(
                sessionHeaderName,
                paidServerUtil.getStringSetting(PaidServerUtil.SESSION_ID_KEY)
            )
        }
        networkRequest.build().getAsJSONObject(requestListener)
    }

    fun post(url: String, data: Map<String, Any>, requestListener: JSONArrayRequestListener) {
        val networkRequest = AndroidNetworking.post("$apiEndPoint$url")
            .addHeaders(jsonHeaders)
            .addApplicationJsonBody(data)
        if (paidServerUtil.isLoggedIn()) {
            networkRequest.addHeaders(
                sessionHeaderName,
                paidServerUtil.getStringSetting(PaidServerUtil.SESSION_ID_KEY)
            )
        }
        networkRequest.build().getAsJSONArray(requestListener)
    }

    fun delete(url: String, requestListener: JSONObjectRequestListener) {
        val networkRequest = AndroidNetworking.delete("$apiEndPoint$url")
            .addHeaders(jsonHeaders)
        if (paidServerUtil.isLoggedIn()) {
            networkRequest.addHeaders(
                sessionHeaderName,
                paidServerUtil.getStringSetting(PaidServerUtil.SESSION_ID_KEY)
            )
        }
        networkRequest.build().getAsJSONObject(requestListener)
    }

    fun baseErrorHandle(
        anError: ANError?,
        requestListener: RequestListener? = null,
        activity: Activity? = null
    ) {
        if (anError?.errorCode == 401) {
            // Session expires
            paidServerUtil.setIsLoggedIn(false)
            paidServerUtil.removeSetting(PaidServerUtil.USER_INFO_KEY)
            requestListener?.onError(ERROR_SESSION_EXPIRES)
            // Redirect to login screen
            if (activity != null && !activity.isFinishing) {
                val intentLogin = Intent(activity, LoginActivity::class.java)
                activity.startActivity(intentLogin)
                activity.finish()
            }
        } else {
            requestListener?.onError(anError?.errorBody)
        }
    }
}