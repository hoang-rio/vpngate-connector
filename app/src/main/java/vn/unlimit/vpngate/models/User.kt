package vn.unlimit.vpngate.models

data class User(
    val _id: String,
    val username: String,
    val fullname: String,
    val timeZone: String,
    val email: String,
    val dataSize: Long?,
    val birthday: String?,
    val registerDate: String?,
    val lastLogin: String?
)
