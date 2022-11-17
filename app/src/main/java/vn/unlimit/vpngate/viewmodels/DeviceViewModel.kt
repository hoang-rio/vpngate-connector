package vn.unlimit.vpngate.viewmodels

import android.app.Application
import android.util.Log
import androidx.annotation.NonNull
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.common.base.Strings
import com.google.firebase.messaging.FirebaseMessaging
import com.google.gson.reflect.TypeToken
import org.json.JSONObject
import vn.unlimit.vpngate.api.DeviceApiRequest
import vn.unlimit.vpngate.models.DeviceInfo
import vn.unlimit.vpngate.request.RequestListener
import vn.unlimit.vpngate.utils.PaidServerUtil

class DeviceViewModel(application: Application) : BaseViewModel(application) {
    val deviceApiRequest: DeviceApiRequest = DeviceApiRequest()
    var deviceInfo: MutableLiveData<DeviceInfo> = MutableLiveData(getDeviceInfo())

    companion object {
        const val TAG = "DeviceViewModel"
        const val DEVICE_INFO_KEY = "DEVICE_INFO_KEY"
    }

    fun addDevice() {
        FirebaseMessaging.getInstance().token
            .addOnCompleteListener(object : OnCompleteListener<String?> {
                override fun onComplete(@NonNull task: Task<String?>) {
                    if (!task.isSuccessful) {
                        Log.w(
                            UserViewModel.TAG,
                            "Fetching FCM registration token failed",
                            task.exception
                        )
                        return
                    }

                    // Get new FCM registration token
                    val token: String? = task.result

                    // Log and toast
                    Log.d(UserViewModel.TAG, "After login addDevice with fcmId: %s".format(token))
                    if (token != null) {
                        val sessionId =
                            paidServerUtil.getStringSetting(PaidServerUtil.SESSION_ID_KEY, "")
                        if (sessionId != null) {
                            deviceApiRequest.addDevice(token, sessionId, object : RequestListener {
                                override fun onSuccess(result: Any?) {
                                    val json = result as JSONObject
                                    if (json.getBoolean("result")) {
                                        val dInfo: DeviceInfo = paidServerUtil.gson.fromJson(
                                            result.getString("userDevice"),
                                            object : TypeToken<DeviceInfo>() {}.type
                                        )
                                        setDeviceInfo(dInfo)
                                    }
                                }

                                override fun onError(error: String?) {
                                    //Nothing to do here
                                }
                            })
                        }
                    }
                }
            })
    }

    fun getNotificationSetting() {
        if (deviceInfo.value == null) {
            Log.e(TAG, "No device information")
            return
        }
        isLoading.value = true
        deviceApiRequest.getNotificationSetting(deviceInfo.value!!._id, object : RequestListener {
            override fun onSuccess(result: Any?) {
                val dInfo: DeviceInfo = paidServerUtil.gson.fromJson(
                    result.toString(),
                    object : TypeToken<DeviceInfo>() {}.type
                )
                setDeviceInfo(dInfo)
                isLoading.value = false
            }

            override fun onError(error: String?) {
                //Nothing to do here
                isLoading.value = false
            }
        })
    }

    fun setNotificationSetting(isEnableNotification: Boolean) {
        if (deviceInfo.value == null) {
            Log.e(TAG, "No device information")
            return
        }
        if (deviceInfo.value!!.notificationSetting?.data == isEnableNotification) {
            Log.w(TAG, "Device notification change same as current setting. Skip update API")
            return
        }
        isLoading.value = true
        deviceApiRequest.setNotificationSetting(
            deviceInfo.value!!._id,
            isEnableNotification,
            object : RequestListener {
                override fun onSuccess(result: Any?) {
                    val resJson = result as JSONObject
                    val dInfo: DeviceInfo = paidServerUtil.gson.fromJson(
                        resJson.getString("saved"),
                        object : TypeToken<DeviceInfo>() {}.type
                    )
                    setDeviceInfo(dInfo)
                    isLoading.value = false
                }

                override fun onError(error: String?) {
                    //Nothing to do here
                    isLoading.value = false
                }
            })
    }

    private fun setDeviceInfo(dInfo: DeviceInfo) {
        paidServerUtil.setStringSetting(DEVICE_INFO_KEY, paidServerUtil.gson.toJson(dInfo))
        deviceInfo.value = dInfo
    }

    private fun getDeviceInfo(): DeviceInfo? {
        val json = paidServerUtil.getStringSetting(DEVICE_INFO_KEY)
        if (Strings.isNullOrEmpty(json)) {
            return null
        }
        return paidServerUtil.gson.fromJson(json, object : TypeToken<DeviceInfo>() {}.type)
    }
}