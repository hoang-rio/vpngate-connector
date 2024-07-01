package vn.unlimit.vpngate.models.response

import vn.unlimit.vpngate.models.User

data class UserLoginResponse(
    val status: Int,
    val code: Int?,
    val sessionId: String?,
    val user: User?
)
