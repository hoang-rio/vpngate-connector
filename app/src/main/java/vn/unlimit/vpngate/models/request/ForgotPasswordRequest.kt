package vn.unlimit.vpngate.models.request

import vn.unlimit.vpngate.models.Captcha

data class ForgotPasswordRequest(
    val email: String,
    val captcha: Captcha
)
