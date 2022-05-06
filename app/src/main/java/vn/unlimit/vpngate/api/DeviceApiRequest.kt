package vn.unlimit.vpngate.api

import android.util.Log
import com.androidnetworking.error.ANError
import com.androidnetworking.interfaces.JSONObjectRequestListener
import org.json.JSONObject
import vn.unlimit.vpngate.request.RequestListener

class DeviceApiRequest : BaseApiRequest() {
    companion object {
        const val TAG = "DeviceApiRequest"
        const val USER_DEVICE_ADD = "/user/device/add"
        const val DEVICE_NOTIFICATION_SETTING = "/user/device/%s/notification-setting"
    }

    fun addDevice(fcmPushId: String, sessionId: String, requestListener: RequestListener) {
        val data = HashMap<String, Any>()
        data["fcmPushId"] = fcmPushId
        data["sessionId"] = sessionId
        data["platform"] = PARAMS_USER_PLATFORM
        val notificationSetting = HashMap<String, Boolean>()
        notificationSetting["ticket"] = true
        notificationSetting["data"] = true
        data["notificationSetting"] = notificationSetting
        post(USER_DEVICE_ADD, data, object : JSONObjectRequestListener {
            override fun onResponse(response: JSONObject?) {
                Log.d(TAG, "Add device success with message %s".format(response.toString()))
                requestListener.onSuccess(response)
            }

            override fun onError(anError: ANError?) {
                Log.e(TAG, "Add device error with message %s".format(anError!!.errorBody))
                requestListener.onError(anError.errorBody)
                baseErrorHandle(anError = anError)
            }
        })
    }

    fun getNotificationSetting(deviceId: String, requestListener: RequestListener) {
        get(DEVICE_NOTIFICATION_SETTING.format(deviceId), object :
            JSONObjectRequestListener {
            override fun onResponse(response: JSONObject?) {
                Log.d(
                    TAG,
                    "Get notification setting of device %s success with response %s".format(
                        deviceId,
                        response.toString()
                    )
                )
                requestListener.onSuccess(response)
            }

            override fun onError(anError: ANError?) {
                Log.e(
                    TAG,
                    "Get notification setting of device %s error with message %s".format(
                        deviceId,
                        anError!!.errorBody
                    )
                )
                baseErrorHandle(anError)
            }
        })
    }

    fun setNotificationSetting(
        deviceId: String,
        isEnableNotification: Boolean,
        requestListener: RequestListener
    ) {
        val data = HashMap<String, Any>()
        data["data"] = isEnableNotification
        data["ticket"] = true
        post(
            DEVICE_NOTIFICATION_SETTING.format(deviceId),
            data,
            object : JSONObjectRequestListener {
                override fun onResponse(response: JSONObject?) {
                    Log.d(
                        TAG,
                        "Set notification setting of device %s success with response %s".format(
                            deviceId,
                            response.toString()
                        )
                    )
                    requestListener.onSuccess(response)
                }

                override fun onError(anError: ANError?) {
                    Log.e(
                        TAG,
                        "Set notification for device %s error with message %s".format(
                            deviceId,
                            anError!!.errorBody
                        )
                    )
                    baseErrorHandle(anError)
                }
            })
    }
}