package vn.unlimit.vpngate.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.github.mikephil.charting.data.Entry
import kotlinx.coroutines.launch
import org.json.JSONArray
import vn.unlimit.vpngate.api.ChartApiService
import vn.unlimit.vpngate.request.RequestListener

open class ChartViewModel(application: Application) : BaseViewModel(application) {
    companion object {
        const val TAG = "ChartViewModel"
    }
    enum class ChartType {
        HOURLY,
        DAILY,
        MONTHLY
    }

    var chartType: MutableLiveData<ChartType> = MutableLiveData(ChartType.HOURLY)
    var isError: MutableLiveData<Boolean> = MutableLiveData(false)
    private val chartApiService: ChartApiService = retrofit.create(ChartApiService::class.java)
    var chartData: MutableLiveData<ArrayList<Entry>> = MutableLiveData(ArrayList())
    var xLabels: ArrayList<String> = ArrayList()


    fun getChartData() {
        if (isLoading.value!!) {
            return
        }
        isLoading.value = true
        isError.value = false
        when (chartType.value) {
            ChartType.HOURLY -> this.getHourlyChart()
            ChartType.DAILY -> this.getDailyChart()
            ChartType.MONTHLY -> this.getMonthlyChart()
            else -> {}
        }
    }

    private fun getHourlyChart() {
        viewModelScope.launch {
            try {
                val data = chartApiService.getHourlyChart()
                val resChartData: ArrayList<Entry> = ArrayList()
                xLabels.clear()
                for (i in 1 until data.size) {
                    val item = data[i]
                    resChartData.add(Entry((i - 1).toFloat(), (item[1] as Double).toFloat()))
                    xLabels.add((i - 1).toString())
                }
                isError.postValue(false)
                chartData.postValue(resChartData)
            } catch (e: Exception) {
                Log.e(TAG, "Got exception when get hourly chart", e)
                isError.postValue(true)
            } finally {
                isLoading.postValue(false)
            }
        }
    }

    private fun getDailyChart() {
        viewModelScope.launch {
            try {
                val data = chartApiService.getDailyChart()
                val resChartData: ArrayList<Entry> = ArrayList()
                xLabels.clear()
                for (i in 1 until data.size) {
                    val item = data[i]
                    resChartData.add(Entry((i - 1).toFloat(), (item[1] as Double).toFloat()))
                    xLabels.add(item[0] as String)
                }
                chartData.postValue(resChartData)
            } catch (e: Exception) {
                Log.e(TAG, "Got exception when get daily chart", e)
                isError.postValue(true)
            } finally {
                isLoading.postValue(false)
            }
        }
    }

    private fun getMonthlyChart() {
        viewModelScope.launch {
            try {
                val data = chartApiService.getMonthlyChart()
                val resChartData: ArrayList<Entry> = ArrayList()
                xLabels.clear()
                for (i in 1 until data.size) {
                    val item = data[i]
                    resChartData.add(Entry((i - 1).toFloat(), (item[1] as Double).toFloat()))
                    xLabels.add(item[0] as String)
                }
                chartData.postValue(resChartData)
            } catch (e: Exception) {
                Log.e(TAG, "Got exception when get daily chart", e)
                isError.postValue(true)
            } finally {
                isLoading.postValue(false)
            }
        }
    }
}