package vn.unlimit.vpngate.models.request

import vn.unlimit.vpngate.models.Captcha

data class RegisterRequest(
    val username: String,
    val fullName: String,
    val email: String,
    val password: String,
    val repassword: String,
    val birthDay: String,
    val timeZone: String,
    val language: String,
    val userPlatform: String,
    val captcha: Captcha
)