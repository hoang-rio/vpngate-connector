package vn.unlimit.vpngate.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import vn.unlimit.vpngate.App
import vn.unlimit.vpngate.api.VPNGateApiService
import vn.unlimit.vpngate.models.VPNGateConnection
import vn.unlimit.vpngate.models.VPNGateConnectionList
import vn.unlimit.vpngate.utils.DataUtil
import java.io.BufferedReader
import java.io.IOException
import java.io.StringReader

class ConnectionListViewModel(application: Application) : BaseViewModel(application) {
    companion object {
        const val TAG = "VPNGateViewModel"
    }

    var dataUtil: DataUtil = App.instance!!.dataUtil!!

    val vpnGateConnectionList = MutableLiveData<VPNGateConnectionList>()
    init {
        viewModelScope.launch(Dispatchers.IO) {
            val connectionCache = dataUtil.connectionsCache
            connectionCache?.let {
                withContext(Dispatchers.Main) {
                    vpnGateConnectionList.value = it
                }
            }
        }
    }
    private var isRetried = false
    var isError: MutableLiveData<Boolean> = MutableLiveData(false)
    private val vpnGateApiService: VPNGateApiService = retrofit.create(VPNGateApiService::class.java)

    private fun getConnectionList(str: String?): VPNGateConnectionList {
        val vpnGateConnectionList = VPNGateConnectionList()
        var br: BufferedReader? = null
        try {
            br = BufferedReader(StringReader(str))
            var line: String
            while ((br.readLine().also { line = it }) != null) {
                if (line.indexOf("*") != 0 && line.indexOf("#") != 0) {
                    val vpnGateConnection = VPNGateConnection.fromCsv(line)
                    if (vpnGateConnection != null) {
                        vpnGateConnectionList.add(vpnGateConnection)
                    }
                }
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            Log.e(TAG, "Error when get connection list from csv", e)
        } finally {
            if (br != null) {
                try {
                    br.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }

        return vpnGateConnectionList
    }

    fun getAPIData() {
        if (isLoading.value == true) {
            return
        }
        Log.d(TAG, "Start vpnItem from API")
        isLoading.postValue(true)
        isError.postValue(false)
        viewModelScope.launch {
            try {
                val connectionList: VPNGateConnectionList
                val csvString: String
                val version = if (!dataUtil.hasAds()) "pro" else null
                val url =
                    if (dataUtil.getBooleanSetting(DataUtil.INCLUDE_UDP_SERVER, true)) {
                        FirebaseRemoteConfig.getInstance().getString("vpn_udp_api")
                    } else {
                        dataUtil.baseUrl + "/api/iphone/"
                    }
                csvString = vpnGateApiService.getCsvString(url, version)
                connectionList = getConnectionList(csvString)
                if (connectionList.size() == 0 && !isRetried) {
                    isRetried = true
                    dataUtil.setUseAlternativeServer(true)
                    getAPIData()
                } else {
                    vpnGateConnectionList.value = connectionList
                    val items = connectionList.toVPNGateItems()
                    withContext(Dispatchers.IO) {
                        App.instance!!.vpnGateItemDao.deleteAll()
                        App.instance!!.vpnGateItemDao.insertAll(*items.toTypedArray())
                        val itemCount = App.instance!!.vpnGateItemDao.count()
                        Log.i(TAG, "Total item: ${items.size}. Total in database: $itemCount")
                        dataUtil.connectionsCache = connectionList
                    }
                }
            } catch (e: Throwable) {
                Log.e(TAG, "Got exception when get connection list", e)
                isError.postValue(true)
            } finally {
                isLoading.postValue(false)
            }
        }
    }
}