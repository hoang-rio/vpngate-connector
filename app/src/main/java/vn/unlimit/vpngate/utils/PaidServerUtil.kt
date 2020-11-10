package vn.unlimit.vpngate.utils

import android.content.Context
import android.content.SharedPreferences
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.gson.Gson
import org.json.JSONObject
import vn.unlimit.vpngate.BuildConfig
import vn.unlimit.vpngate.R
import vn.unlimit.vpngate.models.PaidServer

/**
 * All process of paid server with device storage will do here
 * @param context Application Context
 */
class PaidServerUtil(context: Context) {

    companion object {
        private const val IS_LOGGED_IN = "IS_LOGGED_IN"
        const val SESSION_ID_KEY = "SESSION_ID_KEY"
        const val USER_INFO_KEY = "USER_INFO_KEY"
        const val LAST_USER_FETCH_TIME = "LAST_USER_FETCH_TIME"
        private const val STARTUP_SCREEN_KEY = "STARTUP_SCREEN_KEY"
        const val SAVED_VPN_PW = "SAVED_VPN_PW"
        private const val LAST_CONNECT_SERVER = "LAST_CONNECT_SERVER"
    }

    enum class StartUpScreen {
        FREE_SERVER,
        PAID_SERVER
    }

    private val sharedPreferencesSetting: SharedPreferences = context.getSharedPreferences("vpn_setting_paid_" + BuildConfig.FLAVOR, Context.MODE_PRIVATE)
    var mContext: Context = context
    private var userInfo: JSONObject? = if (getStringSetting(USER_INFO_KEY, "")!!.isEmpty()) null else JSONObject(getStringSetting(USER_INFO_KEY)!!)

    /**
     * Check paid user is logged in or not
     */
    fun isLoggedIn(): Boolean {
        return sharedPreferencesSetting.getBoolean(IS_LOGGED_IN, false)
    }

    /**
     * Get session header name from remote config
     */
    fun getSessionHeaderName(): String = FirebaseRemoteConfig.getInstance().getString(mContext.getString(R.string.cfg_paid_server_session_header_key))

    /**
     * Set user logged in information after login or login
     * @param isLoggedIn Logged In or not
     */
    fun setIsLoggedIn(isLoggedIn: Boolean) {
        val editor = sharedPreferencesSetting.edit()
        editor.putBoolean(IS_LOGGED_IN, isLoggedIn)
        if (!isLoggedIn) {
            // Remove user info
            editor.remove(USER_INFO_KEY)
        }
        editor.apply()
    }

    /**
     * Set user info after login success
     * @param userInfo user info from server or fetch user
     */
    fun setUserInfo(userInfo: JSONObject) {
        this.userInfo = userInfo
        setStringSetting(USER_INFO_KEY, this.userInfo!!.toString())
    }

    /**
     * Get logged in user info
     */
    fun getUserInfo(): JSONObject? {
        return this.userInfo
    }

    /**
     * Get boolean setting
     * @param key Setting key
     * @param defVal Default value if get null from storage
     */
    fun getBooleanSetting(key: String, defVal: Boolean = false): Boolean = sharedPreferencesSetting.getBoolean(key, defVal)

    /**
     * Set boolean setting to storage
     * @param key Setting key
     * @param value Setting value
     */
    fun setBooleanSetting(key: String, value: Boolean) {
        val editor = sharedPreferencesSetting.edit()
        editor.putBoolean(key, value)
        editor.apply()
    }

    /**
     * Get string setting
     * @param key Setting key
     * @param defVal Default value if get null from storage
     */
    fun getStringSetting(key: String, defVal: String = ""): String? = sharedPreferencesSetting.getString(key, defVal)

    /**
     * Set string setting to storage
     * @param key Setting key
     * @param value Setting value
     */
    fun setStringSetting(key: String, value: String) {
        val editor = sharedPreferencesSetting.edit()
        editor.putString(key, value)
        editor.apply()
    }

    /**
     * Get Long setting
     * @param key Setting key
     * @param defVal Default value if get null from storage
     */
    fun getLongSetting(key: String, defVal: Long = 0): Long? = sharedPreferencesSetting.getLong(key, defVal)

    /**
     * Set Long setting to storage
     * @param key Setting key
     * @param value Setting value
     */
    fun setLongSetting(key: String, value: Long) {
        val editor = sharedPreferencesSetting.edit()
        editor.putLong(key, value)
        editor.apply()
    }
    /**
     * Remove setting key
     * @param key Setting key want to remove
     */
    fun removeSetting(key: String) {
        val editor = sharedPreferencesSetting.edit()
        editor.remove(key)
        editor.apply()
    }

    /**
     * Get startup screen
     */
    fun getStartUpScreen(): StartUpScreen {
        return StartUpScreen.valueOf(getStringSetting(STARTUP_SCREEN_KEY, StartUpScreen.FREE_SERVER.toString()) as String)
    }

    /**
     * Set startup screen
     */
    fun setStartupScreen(startupScreen: StartUpScreen) {
        setStringSetting(STARTUP_SCREEN_KEY, startupScreen.toString())
    }

    /**
     * Set last connect server
     */
    fun setLastConnectServer(paidServer: PaidServer) {
        val gson = Gson()
        setStringSetting(LAST_CONNECT_SERVER, gson.toJson(paidServer))
    }

    /**
     * Get last connect server
     */
    fun getLastConnectServer(): PaidServer? {
        val jsonStr = getStringSetting(LAST_CONNECT_SERVER) ?: return null
        val gson = Gson()
        return gson.fromJson(jsonStr, PaidServer::class.java)
    }
}