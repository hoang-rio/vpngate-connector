package vn.unlimit.vpngate.models.request

data class ChangePasswordRequest(
    val password: String,
    val newpassword: String
)
