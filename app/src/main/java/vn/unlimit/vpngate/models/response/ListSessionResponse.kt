package vn.unlimit.vpngate.models.response

import vn.unlimit.vpngate.models.ConnectedSession

data class ListSessionResponse(
    val listSession: LinkedHashSet<ConnectedSession>
)