package vn.unlimit.vpngate.models

class DeviceInfo {
    class NotificationSetting {
        var data: Boolean? = null
        var ticket: Boolean? = null
    }

    var _id = ""
    val notificationSetting: NotificationSetting? = null
}