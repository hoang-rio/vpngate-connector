package vn.unlimit.vpngate.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.common.base.Strings
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
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
import vn.unlimit.vpngate.activities.MainActivity
import vn.unlimit.vpngate.api.DeviceApiService
import vn.unlimit.vpngate.models.DeviceInfo
import vn.unlimit.vpngate.models.request.DeviceAddRequest
import vn.unlimit.vpngate.utils.PaidServerUtil
import vn.unlimit.vpngate.viewmodels.BaseViewModel
import vn.unlimit.vpngate.viewmodels.DeviceViewModel.Companion.DEVICE_INFO_KEY

class PaidServerFCMService : FirebaseMessagingService() {
    private val paidServerUtil = App.instance!!.paidServerUtil!!

    private val httpClientBuilder: OkHttpClient.Builder = OkHttpClient.Builder().addInterceptor {
        val requestBuilder: Request.Builder = it.request().newBuilder()
        if (paidServerUtil.isLoggedIn()) {
            val sessionHeaderName = paidServerUtil.getSessionHeaderName()
            paidServerUtil.getStringSetting(PaidServerUtil.SESSION_ID_KEY)
                ?.let { it1 -> requestBuilder.addHeader(sessionHeaderName, it1) }
        }
        return@addInterceptor it.proceed(requestBuilder.build())
    }

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(
            FirebaseRemoteConfig.getInstance()
                .getString(App.getResourceString(R.string.cfg_paid_server_api_base_url))
        )
        .client(httpClientBuilder.build())
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val deviceApiService: DeviceApiService = retrofit.create(DeviceApiService::class.java)

    companion object {
        const val TAG = "PaidServerFCMService"
        val vibrationPattern = longArrayOf(100, 1000, 100, 1000, 100)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // Check if message contains a data payload.
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Message data payload: ${remoteMessage.data}")
            sendNotification(
                remoteMessage.data["title"].toString(),
                remoteMessage.data["body"].toString(),
            )
        }

        // Check if message contains a notification payload.
        remoteMessage.notification?.let {
            Log.d(TAG, "Message Notification Body: ${it.body}")
        }
    }

    private fun sendNotification(title: String, messageBody: String) {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val requestCode = 0
        val pendingIntent = PendingIntent.getActivity(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE,
        )

        val channelId = getString(R.string.notification_channel_id)
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setVibrate(vibrationPattern)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_MAX)

        createNotificationChannel()

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationId = 0
        notificationManager.notify(notificationId, notificationBuilder.build())
    }

    private fun createNotificationChannel() {
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                getString(R.string.notification_channel_id),
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH,
            )
            channel.vibrationPattern = vibrationPattern
            notificationManager.createNotificationChannel(channel)
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        if (paidServerUtil.isLoggedIn()) {
            //Send update fcmPushId to sever
            val sessionId = paidServerUtil.getStringSetting(PaidServerUtil.SESSION_ID_KEY)
            if (sessionId != null) {
                val json = paidServerUtil.getStringSetting(DEVICE_INFO_KEY)
                var notificationSetting: DeviceInfo.NotificationSetting =
                    DeviceInfo.NotificationSetting()
                notificationSetting.data = true
                notificationSetting.ticket = true
                try {
                    if (!Strings.isNullOrEmpty(json)) {
                        val deviceInfo: DeviceInfo = paidServerUtil.gson.fromJson(
                            json,
                            object : TypeToken<DeviceInfo>() {}.type
                        )
                        notificationSetting = deviceInfo.notificationSetting!!
                    }
                } catch (th: Throwable) {
                    Log.e(TAG, "Got exception when get device info from memory", th)
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