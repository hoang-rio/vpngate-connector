package vn.unlimit.vpngate.models.request

data class UpdateProfileRequest(
    val fullName: String,
    val birthDay: String,
    val timeZone: String
)
