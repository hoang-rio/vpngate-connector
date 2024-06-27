package vn.unlimit.vpngate.models.response

import vn.unlimit.vpngate.models.PaidServer

data class LoadServerResponse(
    val listServer: LinkedHashSet<PaidServer>,
    val countServer: Int
)
