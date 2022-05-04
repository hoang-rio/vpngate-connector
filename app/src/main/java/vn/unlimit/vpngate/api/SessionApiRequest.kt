package vn.unlimit.vpngate.api

import android.util.Log
import com.androidnetworking.error.ANError
import com.androidnetworking.interfaces.JSONObjectRequestListener
import org.json.JSONObject
import vn.unlimit.vpngate.request.RequestListener

class SessionApiRequest : BaseApiRequest() {
    companion object {
        const val TAG = "SessionApiRequest"
        const val LIST_SESSION_URL = "/session/list"
        const val DEL_SESSION_URL = "/session/%s/delete"
    }

    fun getList(requestListener: RequestListener) {
        get("$LIST_SESSION_URL?take=100", object : JSONObjectRequestListener {
            override fun onResponse(response: JSONObject?) {
                Log.d(TAG, "List session success with response %s".format(response.toString()))
                requestListener.onSuccess(response)
            }

            override fun onError(anError: ANError?) {
                Log.e(TAG, "List session error with message %s".format(anError!!.errorBody))
                baseErrorHandle(anError, requestListener)
            }
        })
    }

    fun deleteSession(sessionId: String, requestListener: RequestListener) {
        delete(DEL_SESSION_URL.format(sessionId), object : JSONObjectRequestListener {
            override fun onResponse(response: JSONObject?) {
                Log.d(
                    TAG,
                    "Delete session %s success with response %s".format(
                        sessionId,
                        response.toString()
                    )
                )
                requestListener.onSuccess(response)
            }

            override fun onError(anError: ANError?) {
                Log.e(
                    TAG,
                    "Delete session %s error with message %s".format(sessionId, anError!!.errorBody)
                )
                requestListener.onError(anError.errorBody)
            }
        })
    }
}