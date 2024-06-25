package vn.unlimit.vpngate.models.response

data class CaptchaResponse(
    val image: String,
    val secret: String
)
