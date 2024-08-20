package vn.unlimit.vpngate.utils

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.common.reflect.TypeToken
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.google.gson.Gson
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import vn.unlimit.vpngate.App
import vn.unlimit.vpngate.BuildConfig
import vn.unlimit.vpngate.R
import vn.unlimit.vpngate.models.Cache
import vn.unlimit.vpngate.models.VPNGateConnection
import vn.unlimit.vpngate.models.VPNGateConnectionList
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.util.Calendar
import java.util.Date

/**
 * Manager shared preferences data
 */
class DataUtil(context: Context?) {
    private var mContext: Context? = null
    private var sharedPreferencesSetting: SharedPreferences? = null
    private var gson: Gson? = null

    init {
        try {
            mContext = context
            sharedPreferencesSetting = mContext!!.getSharedPreferences(
                "vpn_setting_data_" + BuildConfig.FLAVOR,
                Context.MODE_PRIVATE
            )
            gson = Gson()
            val mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance()
            val configSettings = FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds((if (BuildConfig.DEBUG || !this.isAcceptedPrivacyPolicy) 0 else 3600).toLong())
                .build()
            mFirebaseRemoteConfig.setDefaultsAsync(R.xml.remote_config_defaults)
            mFirebaseRemoteConfig.setConfigSettingsAsync(configSettings)
                .addOnCompleteListener { _: Task<Void?>? -> mFirebaseRemoteConfig.fetchAndActivate() }
        } catch (e: NullPointerException) {
            e.printStackTrace()
        }
    }

    var connectionsCache: VPNGateConnectionList?
        /**
         * Get connection cache
         *
         * @return VPNGateConnectionList
         */
        get() {
            try {
                val inFile = File(mContext!!.filesDir, CONNECTION_CACHE_KEY)
                if (!inFile.isFile) {
                    return null
                } else {
                    val fileInputStream = FileInputStream(inFile)
                    val reader = JsonReader(InputStreamReader(fileInputStream))
                    val cacheType = object : TypeToken<Cache?>() {
                    }.type
                    val cache = gson!!.fromJson<Cache>(reader, cacheType)
                    if (cache.isExpires()) {
                        reader.close()
                        return null
                    } else {
                        reader.close()
                        val items = App.instance!!.vpnGateItemDao.getAll()
                        return VPNGateConnectionList().fromVPNGateItems(items)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Got exception when get connection cache", e)
            }
            return null
        }
        /**
         * Set connection cache
         *
         */
        set(_) {
            try {
                val cache = Cache()
                val calendar = Calendar.getInstance()
                //Cache in minute get from setting
                val cacheTime = intArrayOf(15, 30, 60, 120, 240, 480, 960)
                val minute = cacheTime[getIntSetting(SETTING_CACHE_TIME_KEY, 0)]
                calendar.add(Calendar.MINUTE, minute)
                cache.expires = calendar.time
                val outFile = File(mContext!!.filesDir, CONNECTION_CACHE_KEY)
                val out = FileOutputStream(outFile)
                val writer = JsonWriter(OutputStreamWriter(out, StandardCharsets.UTF_8))
                gson!!.toJson(cache, Cache::class.java, writer)
                writer.close()
                setConnectionCacheExpire(cache.expires)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

    /**
     * Clear connection cache
     *
     * @return boolean
     */
    fun clearConnectionCache(): Boolean {
        val inFile = File(mContext!!.filesDir, CONNECTION_CACHE_KEY)
        return inFile.isFile && inFile.delete()
    }

    private fun setConnectionCacheExpire(expires: Date?) {
        val editor = sharedPreferencesSetting!!.edit()
        editor.putString("vpn_cache_time", gson!!.toJson(expires))
        editor.apply()
    }

    val connectionCacheExpires: Date?
        /**
         * Get connection cache from shared preferences
         *
         * @return
         */
        get() {
            try {
                val jsonString =
                    sharedPreferencesSetting!!.getString("vpn_cache_time", null) ?: return null
                return gson!!.fromJson(jsonString, Date::class.java)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return null
        }

    fun setStringSetting(key: String?, value: String?) {
        val editor = sharedPreferencesSetting!!.edit()
        editor.putString(key, value)
        editor.apply()
    }

    fun getStringSetting(key: String?, defVal: String?): String? {
        return sharedPreferencesSetting!!.getString(key, defVal)
    }

    /**
     * Set int setting value
     *
     * @param key   setting key
     * @param value setting value
     */
    fun setIntSetting(key: String?, value: Int) {
        val editor = sharedPreferencesSetting!!.edit()
        editor.putInt(key, value)
        editor.apply()
    }

    /**
     * Get int setting value
     *
     * @param key    setting key
     * @param defVal default value
     * @return int
     */
    fun getIntSetting(key: String, defVal: Int): Int {
        var retVal = sharedPreferencesSetting!!.getInt(key, defVal)
        if (key == SETTING_HIDE_OPERATOR_MESSAGE_COUNT && retVal > 0) {
            setIntSetting(key, retVal--)
        }
        return retVal
    }

    var lastVPNConnection: VPNGateConnection?
        get() {
            try {
                val jsonString =
                    sharedPreferencesSetting!!.getString("vpn_last_connection", null) ?: return null
                val vpnGateConnection = gson!!.fromJson(jsonString, VPNGateConnection::class.java)
                if (vpnGateConnection.hostName == null) {
                    return null
                }
                return vpnGateConnection
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return null
        }
        set(vpnGateConnection) {
            try {
                if (vpnGateConnection != null) {
                    val editor = sharedPreferencesSetting!!.edit()
                    editor.putString("vpn_last_connection", gson!!.toJson(vpnGateConnection))
                    editor.apply()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

    fun getBooleanSetting(key: String?, defVal: Boolean): Boolean {
        return sharedPreferencesSetting!!.getBoolean(key, defVal)
    }

    fun setBooleanSetting(key: String?, value: Boolean) {
        val editor = sharedPreferencesSetting!!.edit()
        editor.putBoolean(key, value)
        editor.apply()
    }

    fun hasAds(): Boolean {
        try {
            return this.isAcceptedPrivacyPolicy && (BuildConfig.FLAVOR == "free") && (adMobId != null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return true
    }

    private val adMobId: String?
        get() {
            try {
                val app = mContext!!.packageManager.getApplicationInfo(
                    mContext!!.packageName, PackageManager.GET_META_DATA
                )
                val bundle = app.metaData
                if (bundle != null) {
                    return bundle.getString("com.google.android.gms.ads.APPLICATION_ID")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Got exception when get admobId", e)
            }
            return null
        }

    fun hasProInstalled(): Boolean {
        try {
            val mPm = mContext!!.packageManager // 1
            val info = mPm.getPackageInfo("vn.unlimit.vpngatepro", 0) // 2,3
            return info != null
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }

    fun hasOpenVPNInstalled(): Boolean {
        try {
            val mPm = mContext!!.packageManager // 1
            val info = mPm.getPackageInfo("net.openvpn.openvpn", 0) // 2,3
            return info != null
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }

    val baseUrl: String
        get() {
            try {
                if (sharedPreferencesSetting!!.getBoolean(USE_ALTERNATIVE_SERVER, false)) {
                    return FirebaseRemoteConfig.getInstance()
                        .getString(mContext!!.getString(R.string.alternative_api_cfg_key))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return "https://www.vpngate.net"
        }

    fun setUseAlternativeServer(useAlternativeServer: Boolean?) {
        val editor = sharedPreferencesSetting!!.edit()
        editor.putBoolean(USE_ALTERNATIVE_SERVER, useAlternativeServer!!)
        editor.apply()
    }

    var isAcceptedPrivacyPolicy: Boolean
        get() = sharedPreferencesSetting!!.getBoolean(ACCEPTED_PRIVACY_POLICY, false)
        set(isAccepted) {
            val editor = sharedPreferencesSetting!!.edit()
            editor.putBoolean(ACCEPTED_PRIVACY_POLICY, isAccepted)
            editor.apply()
        }

    companion object {
        const val TAG = "DataUtil"
        const val SETTING_CACHE_TIME_KEY: String = "SETTING_CACHE_TIME_KEY"
        const val SETTING_HIDE_OPERATOR_MESSAGE_COUNT: String =
            "SETTING_HIDE_OPERATOR_MESSAGE_COUNT"
        const val USER_ALLOWED_VPN: String = "USER_ALLOWED_VPN"
        const val SETTING_BLOCK_ADS: String = "SETTING_BLOCK_ADS"
        const val INCLUDE_UDP_SERVER: String = "INCLUDE_UDP_SERVER"
        const val LAST_CONNECT_USE_UDP: String = "LAST_CONNECT_USE_UDP"
        const val USE_CUSTOM_DNS: String = "USE_CUSTOM_DNS"
        const val CUSTOM_DNS_IP_1: String = "CUSTOM_DNS_IP_1"
        const val CUSTOM_DNS_IP_2: String = "CUSTOM_DNS_IP_2"
        const val USE_DOMAIN_TO_CONNECT: String = "USE_DOMAIN_TO_CONNECT"
        const val SETTING_DEFAULT_PROTOCOL: String = "SETTING_DEFAULT_PROTOCOL"
        const val SETTING_STARTUP_SCREEN: String = "SETTING_STARTUP_SCREEN"
        const val SETTING_NOTIFY_SPEED: String = "SETTING_NOTIFY_SPEED"
        const val INVITED_USE_PAID_SERVER: String = "INVITED_USE_PAID_SERVER"
        const val IS_LAST_CONNECTED_PAID: String = "IS_LAST_CONNECTED_PAID"
        private const val USE_ALTERNATIVE_SERVER = "USE_ALTERNATIVE_SERVER"
        private const val ACCEPTED_PRIVACY_POLICY = "ACCEPTED_PRIVACY_POLICY"
        const val CONNECTION_CACHE_KEY = "CONNECTION_CACHE_KEY"

        /**
         * Check device connect to a network or not
         *
         * @param context
         * @return connect to network result
         */
        @JvmStatic
        fun isOnline(context: Context): Boolean {
            var result = false
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                cm?.run {
                    cm.getNetworkCapabilities(cm.activeNetwork)?.run {
                        result = when {
                            hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                            hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                            hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                            else -> false
                        }
                    }
                }
            } else {
                cm?.run {
                    @Suppress("DEPRECATION")
                    cm.activeNetworkInfo?.run {
                        if (type == ConnectivityManager.TYPE_WIFI) {
                            result = true
                        } else if (type == ConnectivityManager.TYPE_MOBILE) {
                            result = true
                        }
                    }
                }
            }
            return result
        }
    }
}
