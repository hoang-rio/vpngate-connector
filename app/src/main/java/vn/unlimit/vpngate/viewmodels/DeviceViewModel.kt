package vn.unlimit.vpngate.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.common.base.Strings
import com.google.firebase.messaging.FirebaseMessaging
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import vn.unlimit.vpngate.api.DeviceApiService
import vn.unlimit.vpngate.models.DeviceInfo
import vn.unlimit.vpngate.models.request.DeviceAddRequest
import vn.unlimit.vpngate.utils.PaidServerUtil

class DeviceViewModel(application: Application) : BaseViewModel(application) {
    val deviceApiService: DeviceApiService = retrofit.create(DeviceApiService::class.java)
    var deviceInfo: MutableLiveData<DeviceInfo> = MutableLiveData(getDeviceInfo())

    companion object {
        const val TAG = "DeviceViewModel"
        const val DEVICE_INFO_KEY = "DEVICE_INFO_KEY"
    }

    fun addDevice() {
        FirebaseMessaging.getInstance().token
            .addOnCompleteListener(object : OnCompleteListener<String?> {
                override fun onComplete(task: Task<String?>) {
                    if (!task.isSuccessful) {
                        Log.w(
                            TAG,
                            "Fetching FCM registration token failed",
                            task.exception
                        )
                        return
                    }

                    // Get new FCM registration token
                    val token: String? = task.result

                    // Log and toast
                    Log.d(TAG, "After login addDevice with fcmId: %s".format(token))
                    if (token != null) {
                        val sessionId =
                            paidServerUtil.getStringSetting(PaidServerUtil.SESSION_ID_KEY, "")
                        if (sessionId != null) {
                            viewModelScope.launch {
                                try {
                                    val defaultNotificationSetting =
                                        DeviceInfo.NotificationSetting()
                                    defaultNotificationSetting.data = true
                                    defaultNotificationSetting.ticket = true
                                    val deviceAddResponse = deviceApiService.addDevice(
                                        DeviceAddRequest(
                                            fcmPushId = token,
                                            sessionId = sessionId,
                                            platform = PARAMS_USER_PLATFORM,
                                            notificationSetting = defaultNotificationSetting
                                        )
                                    )
                                    if (deviceAddResponse.result) {
                                        setDeviceInfo(deviceAddResponse.userDevice!!)
                                    }
                                    Log.d(TAG, "Add device result %s".format(deviceAddResponse.toString()))
                                } catch (e: Exception) {
                                    Log.e(TAG, "Got exception when add device after login", e)
                                }
                            }
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
        viewModelScope.launch {
            try {
                val deviceInfo = deviceApiService.getNotificationSetting(deviceInfo.value!!._id)
                withContext(Dispatchers.Main) {
                    setDeviceInfo(deviceInfo)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Got exception when get notification setting", e)
            } finally {
                isLoading.postValue(false)
            }
        }
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
        val notificationSetting = DeviceInfo.NotificationSetting()
        notificationSetting.data = isEnableNotification
        viewModelScope.launch {
            try {
                val setNotificationSettingResponse = deviceApiService.setNotificationSetting(
                    deviceInfo.value!!._id,
                    notificationSetting
                )
                if (setNotificationSettingResponse.saved) {
                    withContext(Dispatchers.Main) {
                        setDeviceInfo(setNotificationSettingResponse.userDevice)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Got exception when set notification setting", e)
            } finally {
                isLoading.postValue(false)
            }
        }
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