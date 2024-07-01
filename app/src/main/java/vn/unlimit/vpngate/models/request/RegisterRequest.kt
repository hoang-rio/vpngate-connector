package vn.unlimit.vpngate.models.request

import vn.unlimit.vpngate.models.Captcha
import vn.unlimit.vpngate.models.UserRegister

data class RegisterRequest(
    val user: UserRegister,
    val captcha: Captcha
)