package vn.unlimit.vpngate.services

import android.util.Log
import com.google.common.base.Strings
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import vn.unlimit.vpngate.App
import vn.unlimit.vpngate.R
import vn.unlimit.vpngate.api.DeviceApiService
import vn.unlimit.vpngate.models.DeviceInfo
import vn.unlimit.vpngate.models.request.DeviceAddRequest
import vn.unlimit.vpngate.utils.PaidServerUtil
import vn.unlimit.vpngate.viewmodels.BaseViewModel
import vn.unlimit.vpngate.viewmodels.DeviceViewModel.Companion.DEVICE_INFO_KEY

class PaidServerFCMService : FirebaseMessagingService() {
    private val paidServerUtil = App.getInstance().paidServerUtil

    private val httpClientBuilder: OkHttpClient.Builder = OkHttpClient.Builder().addInterceptor {
        val requestBuilder: Request.Builder = it.request().newBuilder()
        if (paidServerUtil.isLoggedIn()) {
            val sessionHeaderName = paidServerUtil.getSessionHeaderName()
            paidServerUtil.getStringSetting(PaidServerUtil.SESSION_ID_KEY)
                ?.let { it1 -> requestBuilder.addHeader(sessionHeaderName, it1) }
        }
        return@addInterceptor it.proceed(requestBuilder.build())
    }

    private var retrofit: Retrofit = Retrofit.Builder().baseUrl(
        FirebaseRemoteConfig.getInstance()
            .getString(App.getResourceString(R.string.cfg_paid_server_api_base_url))
    )
        .client(httpClientBuilder.build())
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val deviceApiService: DeviceApiService = retrofit.create(DeviceApiService::class.java)

    companion object {
        const val TAG = "PaidServerFCMService"
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        if (paidServerUtil.isLoggedIn()) {
            //Send update fcmPushId to sever
            val sessionId = paidServerUtil.getStringSetting(PaidServerUtil.SESSION_ID_KEY)
            if (sessionId != null) {
                val json = paidServerUtil.getStringSetting(DEVICE_INFO_KEY)
                val notificationSetting: DeviceInfo.NotificationSetting
                if (Strings.isNullOrEmpty(json)) {
                    notificationSetting = DeviceInfo.NotificationSetting()
                    notificationSetting.data = true
                    notificationSetting.ticket = true
                } else {
                    notificationSetting =
                        paidServerUtil.gson.fromJson(json, object : TypeToken<DeviceInfo>() {}.type)
                }
                GlobalScope.launch {
                    try {
                        val deviceAddResponse = deviceApiService.addDevice(
                            DeviceAddRequest(
                                fcmPushId = token,
                                sessionId = sessionId,
                                platform = BaseViewModel.PARAMS_USER_PLATFORM,
                                notificationSetting = notificationSetting
                            )
                        )
                        paidServerUtil.setStringSetting(
                            DEVICE_INFO_KEY,
                            paidServerUtil.gson.toJson(deviceAddResponse.userDevice)
                        )
                        Log.d(
                            TAG,
                            "Add device in FCMService success with message %s".format(
                                deviceAddResponse.toString()
                            )
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Add device error with message %s", e)
                    }
                }
            }
        }
    }
}