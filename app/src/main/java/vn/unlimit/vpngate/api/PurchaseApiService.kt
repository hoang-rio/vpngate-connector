package vn.unlimit.vpngate.api

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import vn.unlimit.vpngate.models.PurchaseHistory
import vn.unlimit.vpngate.models.request.PurchaseCreateRequest
import vn.unlimit.vpngate.models.response.GeneralResponse
import vn.unlimit.vpngate.models.response.PurchaseListResponse

interface PurchaseApiService {
    @POST("purchase/create")
    suspend fun createPurchase(@Body purchaseCreateRequest: PurchaseCreateRequest, @Query("version") version: String?) : GeneralResponse

    @GET("purchase/list")
    suspend fun listPurchase(@Query("take") take: Int, @Query("skip") skip: Int?): PurchaseListResponse
}