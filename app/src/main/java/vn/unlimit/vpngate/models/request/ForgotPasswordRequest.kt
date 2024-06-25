package vn.unlimit.vpngate.models.request

data class ForgotPasswordRequest(
    val usernameOrEmail: String,
    val captchaSecret: String,
    val captchaAnswer: Int
)
