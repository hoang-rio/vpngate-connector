package vn.unlimit.vpngate.models

data class Captcha(
    val answer: Int,
    val secret: String,
)