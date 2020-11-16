package vn.unlimit.vpngate.api

import android.util.Log
import com.androidnetworking.error.ANError
import com.androidnetworking.interfaces.JSONObjectRequestListener
import org.json.JSONObject
import vn.unlimit.vpngate.activities.paid.PaidServerActivity
import vn.unlimit.vpngate.request.RequestListener

class ServerApiRequest: BaseApiRequest() {
    companion object{
        const val TAG = "ServerApiRequest"
        const val LIST_SERVER_URL = "/server/list"
    }

    fun loadServer(take: Int, skip: Int? = 0, requestListener: RequestListener, activity: PaidServerActivity) {
        get("$LIST_SERVER_URL?take=$take&skip=$skip", object :JSONObjectRequestListener {
            override fun onResponse(response: JSONObject?) {
                Log.d(TAG, "Server list from server: %s".format(response))
                requestListener.onSuccess(response)
            }

            override fun onError(anError: ANError?) = baseErrorHandle(anError, requestListener, activity)
        })
    }
}