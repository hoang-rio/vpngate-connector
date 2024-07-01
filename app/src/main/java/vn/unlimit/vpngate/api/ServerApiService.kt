package vn.unlimit.vpngate.api

import retrofit2.http.GET
import retrofit2.http.Query
import vn.unlimit.vpngate.models.response.LoadServerResponse

interface ServerApiService {
    @GET("server/list")
    suspend fun loadServer(
        @Query("take") take: Int,
        @Query("skip") skip: Int? = 0
    ): LoadServerResponse
}