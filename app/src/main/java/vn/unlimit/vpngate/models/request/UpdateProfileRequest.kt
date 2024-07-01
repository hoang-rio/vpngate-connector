package vn.unlimit.vpngate.models.request

data class UpdateProfileRequest(
    val fullname: String,
    val birthday: String,
    val timeZone: String
)
