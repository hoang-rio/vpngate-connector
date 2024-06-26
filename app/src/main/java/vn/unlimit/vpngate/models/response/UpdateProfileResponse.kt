package vn.unlimit.vpngate.models.response

import vn.unlimit.vpngate.models.User

data class UpdateProfileResponse(
    val result: Boolean,
    val errorCode: Int?,
    val user: User?
)
