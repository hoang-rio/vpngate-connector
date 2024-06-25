package vn.unlimit.vpngate.models.response

import vn.unlimit.vpngate.models.DeviceInfo

data class DeviceAddResponse(
    val result: Boolean,
    val exist: Boolean?,
    val userDevice: DeviceInfo?
)
