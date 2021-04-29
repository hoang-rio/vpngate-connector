package vn.unlimit.vpngate.viewmodels

import android.app.Application
import androidx.lifecycle.MutableLiveData
import com.github.mikephil.charting.data.Entry
import org.json.JSONArray
import vn.unlimit.vpngate.api.ChartApiRequest
import vn.unlimit.vpngate.request.RequestListener

open class ChartViewModel(application: Application) : BaseViewModel(application) {
    enum class ChartType {
        HOURLY,
        DAILY,
        MONTHLY
    }

    var chartType: MutableLiveData<ChartType> = MutableLiveData(ChartType.HOURLY)
    private val chartApiRequest: ChartApiRequest = ChartApiRequest()
    var chartData: MutableLiveData<ArrayList<Entry>> = MutableLiveData(ArrayList())
    var xLabels: ArrayList<String> = ArrayList()
    var yLabels: ArrayList<String> = ArrayList()


    fun getChartData() {
        when (chartType.value) {
            ChartType.HOURLY -> this.getHourlyChart()
            ChartType.DAILY -> this.getDailyChart()
            ChartType.MONTHLY -> this.getMonthlyChart()
        }
    }

    private fun getHourlyChart() {
        if (isLoading.value!!) {
            return
        }
        isLoading.value = true
        chartApiRequest.getHourlyChart(object : RequestListener {
            override fun onError(error: String?) {
                baseErrorHandle(error)
                isLoading.value = false
            }

            override fun onSuccess(result: Any?) {
                val data = (result as JSONArray)
                val resChartData: ArrayList<Entry> = ArrayList()
                xLabels.clear()
                yLabels.clear()
                for (i in 1 until data.length()) {
                    val item = data.getJSONArray(i)
                    resChartData.add(Entry((i - 1).toFloat(), item.getDouble(1).toFloat()))
                    xLabels.add(if ((i - 1) > 9) (i - 1).toString() else "0${i - 1}")
                    yLabels.add(item.getDouble(1).toString() + " MB")
                }
                chartData.value = resChartData
                isLoading.value = false
            }
        })
    }

    private fun getDailyChart() {
        if (isLoading.value!!) {
            return
        }
        isLoading.value = true
        chartApiRequest.getDailyChart(object : RequestListener {
            override fun onError(error: String?) {
                baseErrorHandle(error)
                isLoading.value = false
            }

            override fun onSuccess(result: Any?) {
                val data = (result as JSONArray)
                val resChartData: ArrayList<Entry> = ArrayList()
                xLabels.clear()
                yLabels.clear()
                for (i in 1 until data.length()) {
                    val item = data.getJSONArray(i)
                    resChartData.add(Entry((i - 1).toFloat(), item.getDouble(1).toFloat()))
                    xLabels.add(item.getString(0))
                    yLabels.add(item.getDouble(1).toString() + " MB")
                }
                chartData.value = resChartData
                isLoading.value = false
            }
        })
    }

    private fun getMonthlyChart() {
        if (isLoading.value!!) {
            return
        }
        isLoading.value = true
        chartApiRequest.getMontlyChart(object : RequestListener {
            override fun onError(error: String?) {
                baseErrorHandle(error)
                isLoading.value = false
            }

            override fun onSuccess(result: Any?) {
                val data = (result as JSONArray)
                val resChartData: ArrayList<Entry> = ArrayList()
                xLabels.clear()
                yLabels.clear()
                for (i in 1 until data.length()) {
                    val item = data.getJSONArray(i)
                    resChartData.add(Entry((i - 1).toFloat(), item.getDouble(1).toFloat()))
                    xLabels.add(item.getString(0))
                    yLabels.add(item.getDouble(1).toString() + " MB")
                }
                chartData.value = resChartData
                isLoading.value = false
            }
        })
    }
}