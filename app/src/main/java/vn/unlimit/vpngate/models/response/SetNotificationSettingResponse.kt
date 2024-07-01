package vn.unlimit.vpngate.models.response

import vn.unlimit.vpngate.models.DeviceInfo

data class SetNotificationSettingResponse(
    val saved: Boolean,
    val userDevice: DeviceInfo
)
