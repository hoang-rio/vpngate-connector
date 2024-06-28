package vn.unlimit.vpngate.models

data class UserRegister(
    val username: String,
    val fullname: String,
    val email: String,
    val password: String,
    val repassword: String,
    val birthday: String,
    val timeZone: String,
    val language: String,
    val userPlatform: String,
)
