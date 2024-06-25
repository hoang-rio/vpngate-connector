package vn.unlimit.vpngate.models.request

data class ResetPasswordRequest(
    val resetPassToken: String,
    val newPassword: String,
    val renewPassword: String
)
