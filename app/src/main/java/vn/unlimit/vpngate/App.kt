package vn.unlimit.vpngate

import android.annotation.SuppressLint
import android.app.Application
import android.util.Log
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.security.ProviderInstaller
import com.google.android.gms.tasks.Task
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import de.blinkt.openvpn.core.OpenVPNService
import vn.unlimit.vpngate.activities.DetailActivity
import vn.unlimit.vpngate.activities.MainActivity
import vn.unlimit.vpngate.utils.AppOpenManager
import vn.unlimit.vpngate.utils.DataUtil
import vn.unlimit.vpngate.utils.PaidServerUtil

class App : Application() {
    var dataUtil: DataUtil? = null
        private set
    @JvmField
    var paidServerUtil: PaidServerUtil? = null

    override fun onCreate() {
        super.onCreate()
        if (!BuildConfig.DEBUG) {
            // OPTIONAL: If crash reporting has been explicitly disabled previously, add:
            FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)
        }
        instance = this
        dataUtil = DataUtil(this)
        if (dataUtil!!.hasAds()) {
            MobileAds.initialize(this)
            if (dataUtil!!.isAcceptedPrivacyPolicy && dataUtil!!.getBooleanSetting(
                    DataUtil.INVITED_USE_PAID_SERVER,
                    false
                )
            ) {
                appOpenManager = AppOpenManager(this)
            }
        }
        // Make notification open DetailActivity
        OpenVPNService.setNotificationActivityClass(
            if (dataUtil!!.getIntSetting(
                    DataUtil.SETTING_STARTUP_SCREEN,
                    0
                ) == 0
            ) DetailActivity::class.java else MainActivity::class.java
        )
        paidServerUtil = PaidServerUtil(this)
        FirebaseRemoteConfig.getInstance().fetchAndActivate()
            .addOnCompleteListener { task: Task<Boolean> ->
                if (task.isSuccessful) {
                    val updated = task.result
                    Log.e(TAG, "RemoteConfigUpdated:$updated")
                    isImportToOpenVPN =
                        FirebaseRemoteConfig.getInstance().getBoolean("vpn_import_open_vpn")
                }
            }
        try {
            ProviderInstaller.installIfNeeded(applicationContext)
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    companion object {
        private const val TAG = "VpnGateApp"

        @SuppressLint("StaticFieldLeak")
        var appOpenManager: AppOpenManager? = null
        @JvmStatic
        var instance: App? = null
            private set
        var isImportToOpenVPN: Boolean = false
            private set

        fun getResourceString(resId: Int): String {
            return instance!!.getString(resId)
        }
    }
}
