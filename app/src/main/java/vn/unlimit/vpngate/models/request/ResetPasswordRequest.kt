package vn.unlimit.vpngate.models.request

data class ResetPasswordRequest(
    val resetPassToken: String,
    val password: String,
    val repassword: String
)
