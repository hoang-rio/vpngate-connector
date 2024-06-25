package vn.unlimit.vpngate.api

import org.json.JSONArray
import retrofit2.http.GET

interface ChartApiService {
    @GET("data/hourly-chart")
    suspend fun getHourlyChart(): ArrayList<ArrayList<Any>>

    @GET("data/daily-chart")
    suspend fun getDailyChart(): ArrayList<ArrayList<Any>>

    @GET("data/monthly-chart")
    suspend fun getMonthlyChart(): ArrayList<ArrayList<Any>>
}