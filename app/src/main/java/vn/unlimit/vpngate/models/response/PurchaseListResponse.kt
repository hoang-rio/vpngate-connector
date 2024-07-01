package vn.unlimit.vpngate.models.response

import vn.unlimit.vpngate.models.PurchaseHistory

data class PurchaseListResponse(
    val listPurchase: ArrayList<PurchaseHistory>
)