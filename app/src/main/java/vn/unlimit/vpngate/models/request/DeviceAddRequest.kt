package vn.unlimit.vpngate.models.request

import vn.unlimit.vpngate.models.DeviceInfo

data class DeviceAddRequest(
    val fcmPushId: String,
    val sessionId: String,
    val platform: String,
    val notificationSetting: DeviceInfo.NotificationSetting
)