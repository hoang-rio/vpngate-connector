package vn.unlimit.vpngate.api

import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import vn.unlimit.vpngate.models.response.ListSessionResponse

interface SessionApiService {
    @GET("session/list")
    suspend fun getList(@Query("take") take: Int = 100): ListSessionResponse

    @DELETE("session/{sessionId}/delete")
    suspend fun deleteSession(@Path("sessionId") sessionId: String)
}