package vn.unlimit.vpngate.services

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import org.json.JSONObject
import vn.unlimit.vpngate.App
import vn.unlimit.vpngate.api.DeviceApiRequest
import vn.unlimit.vpngate.request.RequestListener
import vn.unlimit.vpngate.utils.PaidServerUtil
import vn.unlimit.vpngate.viewmodels.DeviceViewModel

class PaidServerFCMService : FirebaseMessagingService() {
    private val paidServerUtil = App.getInstance().paidServerUtil

    companion object {
        const val TAG = "PaidServerFCMService"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        if (paidServerUtil.isLoggedIn()) {
            //Send update fcmPushId to sever
            val deviceApiRequest = DeviceApiRequest()
            val sessionId = paidServerUtil.getStringSetting(PaidServerUtil.SESSION_ID_KEY)
            if (sessionId != null) {
                deviceApiRequest.addDevice(token, sessionId, object : RequestListener {
                    override fun onSuccess(result: Any?) {
                        Log.d(
                            TAG,
                            "Add device in FCMService success with message %s".format(result.toString())
                        )
                        val json = result as JSONObject
                        if (json.getBoolean("result")) {
                            paidServerUtil.setStringSetting(
                                DeviceViewModel.DEVICE_INFO_KEY,
                                paidServerUtil.gson.toJson(result.getString("userDevice"))
                            )
                        }
                    }

                    override fun onError(error: String?) {
                        Log.e(TAG, "Add device error with message %s".format(error))
                    }
                })
            }
        }
    }
}