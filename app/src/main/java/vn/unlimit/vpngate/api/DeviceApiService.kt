package vn.unlimit.vpngate.api

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import vn.unlimit.vpngate.models.DeviceInfo
import vn.unlimit.vpngate.models.request.DeviceAddRequest
import vn.unlimit.vpngate.models.response.DeviceAddResponse
import vn.unlimit.vpngate.models.response.SetNotificationSettingResponse

interface DeviceApiService {
    @POST("user/device/add")
    suspend fun addDevice(@Body deviceAddRequest: DeviceAddRequest): DeviceAddResponse

    @GET("user/device/{deviceId}/notification-setting")
    suspend fun getNotificationSetting(@Path("deviceId") deviceId: String): DeviceInfo

    @POST("user/device/{deviceId}/notification-setting")
    suspend fun setNotificationSetting(
        @Path("deviceId") deviceId: String,
        @Body notificationSetting: DeviceInfo.NotificationSetting
    ): SetNotificationSettingResponse
}