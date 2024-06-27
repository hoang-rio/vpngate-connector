package vn.unlimit.vpngate.models.request

data class PurchaseCreateRequest(
    val packageId: String,
    val purchaseId: String,
    val platform: String,
    val paymentMethod: String,
    val currency: String,
    val currencyPrice: Double
)
