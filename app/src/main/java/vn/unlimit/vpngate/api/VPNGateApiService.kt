package vn.unlimit.vpngate.api

import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url

interface VPNGateApiService {
    @GET
    suspend fun getCsvString(
        @Url url: String,
        @Query("version") version: String?
    ): String
}