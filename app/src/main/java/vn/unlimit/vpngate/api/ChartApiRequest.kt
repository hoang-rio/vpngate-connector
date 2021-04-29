package vn.unlimit.vpngate.api

import android.util.Log
import com.androidnetworking.error.ANError
import com.androidnetworking.interfaces.JSONArrayRequestListener
import org.json.JSONArray
import vn.unlimit.vpngate.request.RequestListener

open class ChartApiRequest : BaseApiRequest() {
    companion object {
        const val TAG = "ChartApiRequest"
        const val HOURLY_CHART_URL = "/data/hourly-chart"
        const val DAILY_CHART_URL = "/data/daily-chart"
        const val MONTHLY_CHART_URL = "/data/monthly-chart"
    }

    fun getHourlyChart(requestListener: RequestListener) {
        get(HOURLY_CHART_URL, object : JSONArrayRequestListener {
            override fun onResponse(response: JSONArray?) {
                requestListener.onSuccess(response)
            }

            override fun onError(anError: ANError?) {
                Log.e(TAG, "Get hourly chart error with message %s".format(anError!!.errorBody))
                baseErrorHandle(anError, requestListener)
            }
        })
    }

    fun getDailyChart(requestListener: RequestListener) {
        get(DAILY_CHART_URL, object : JSONArrayRequestListener {
            override fun onResponse(response: JSONArray?) {
                requestListener.onSuccess(response)
            }

            override fun onError(anError: ANError?) {
                Log.e(TAG, "Get daily chart error with message %s".format(anError!!.errorBody))
                baseErrorHandle(anError, requestListener)
            }
        })
    }

    fun getMontlyChart(requestListener: RequestListener) {
        get(MONTHLY_CHART_URL, object : JSONArrayRequestListener {
            override fun onResponse(response: JSONArray?) {
                requestListener.onSuccess(response)
            }

            override fun onError(anError: ANError?) {
                Log.e(TAG, "Get monthly chart error with message %s".format(anError!!.errorBody))
                baseErrorHandle(anError, requestListener)
            }
        })
    }
}