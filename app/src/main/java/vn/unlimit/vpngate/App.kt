package vn.unlimit.vpngate

import android.annotation.SuppressLint
import android.app.Application
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.room.Room
import com.google.android.libraries.ads.mobile.sdk.MobileAds
import com.google.android.libraries.ads.mobile.sdk.initialization.InitializationConfig
import com.google.android.gms.security.ProviderInstaller
import com.google.android.gms.tasks.Task
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import de.blinkt.openvpn.core.OpenVPNService
import vn.unlimit.vpngate.activities.DetailActivity
import vn.unlimit.vpngate.activities.MainActivity
import vn.unlimit.vpngate.db.AppDatabase
import vn.unlimit.vpngate.db.ExcludedAppDao
import vn.unlimit.vpngate.db.VPNGateItemDao
import vn.unlimit.vpngate.models.ExcludedApp
import vn.unlimit.vpngate.utils.AppOpenManager
import vn.unlimit.vpngate.utils.DataUtil
import vn.unlimit.vpngate.utils.PaidServerUtil
import java.util.concurrent.atomic.AtomicBoolean

class App : Application() {
    var dataUtil: DataUtil? = null
        private set
    @JvmField
    var paidServerUtil: PaidServerUtil? = null
    private lateinit var appDatabase: AppDatabase
    lateinit var vpnGateItemDao: VPNGateItemDao
    lateinit var excludedAppDao: ExcludedAppDao

    override fun onCreate() {
        super.onCreate()
        appDatabase = Room.databaseBuilder(applicationContext, AppDatabase::class.java, "vpn_gate_connector")
            .addMigrations(object : androidx.room.migration.Migration(1, 2) {
                override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                    // Migration from version 1 to 2: create excluded_apps table
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS `excluded_apps` (" +
                                "`packageName` TEXT NOT NULL, " +
                                "`appName` TEXT NOT NULL, " +
                                "PRIMARY KEY(`packageName`))"
                    )
                }
            })
            .addMigrations(AppDatabase.MIGRATION_2_3)
            .allowMainThreadQueries() // Allow main thread queries for VPN profile configuration
            .build()
        vpnGateItemDao = appDatabase.vpnGateItemDao()
        excludedAppDao = appDatabase.excludedAppDao()

        // Initialize default excluded apps off the main thread: this performs the
        // first Room open (plus any pending migration) and used to run synchronously
        // on main, lengthening process start. When the VPN service is started cold
        // via startForegroundService() (e.g. the keepVPNAlive job) that delay eats
        // into the 5s foreground-service budget.
        Thread { initializeDefaultExcludedApps() }.start()
        if (!BuildConfig.DEBUG) {
            // OPTIONAL: If crash reporting has been explicitly disabled previously, add:
            FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)
        }
        instance = this
        dataUtil = DataUtil(this)
        if (dataUtil!!.hasAds()) {
            ensureMobileAdsInitialized()
            if (dataUtil!!.isAcceptedPrivacyPolicy) {
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
            ProviderInstaller.installIfNeededAsync(applicationContext,
                object : ProviderInstaller.ProviderInstallListener {
                    override fun onProviderInstalled() {
                        Log.d(TAG, "Security provider installed")
                    }

                    override fun onProviderInstallFailed(errorCode: Int, recoveryIntent: Intent?) {
                        Log.e(TAG, "Security provider install failed: $errorCode")
                    }
                })
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    private fun initializeDefaultExcludedApps() {
        // Always exclude self — hidden from user, cannot be removed
        val selfPackage = ExcludedApp(
            packageName = packageName,
            appName = "Self"
        )
        try {
            val existingSelf = excludedAppDao.isAppExcluded(selfPackage.packageName)
            if (existingSelf == 0) {
                excludedAppDao.insertExcludedApp(selfPackage)
                Log.d(TAG, "Added self as default excluded app")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing self excluded app", e)
        }

        // Add Android Auto as default excluded app
        val androidAuto = ExcludedApp(
            packageName = "com.google.android.projection.gearhead",
            appName = "Android Auto"
        )

        // Check if Android Auto is already added
        try {
            val existing = excludedAppDao.isAppExcluded(androidAuto.packageName)
            if (existing == 0) {
                // First time - add synchronously to ensure it's available immediately
                excludedAppDao.insertExcludedApp(androidAuto)
                Log.d(TAG, "Added Android Auto as default excluded app")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing default excluded apps", e)
            // Try to add on background thread as fallback
            Thread {
                try {
                    val existing = excludedAppDao.isAppExcluded(androidAuto.packageName)
                    if (existing == 0) {
                        excludedAppDao.insertExcludedApp(androidAuto)
                        Log.d(TAG, "Added Android Auto as default excluded app (fallback)")
                    }
                } catch (e2: Exception) {
                    Log.e(TAG, "Error in fallback initialization", e2)
                }
            }.start()
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
        @Volatile
        var isMobileAdsInitialized: Boolean = false
            private set

        private val adsInitStarted = AtomicBoolean(false)

        private val mainHandler = Handler(Looper.getMainLooper())
        private val pendingCallbacks = mutableListOf<Runnable>()

        @Synchronized
        private fun drainPendingCallbacks() {
            val callbacks = ArrayList(pendingCallbacks)
            pendingCallbacks.clear()
            callbacks.forEach { it.run() }
        }

        @Synchronized
        fun runWhenInitialized(runnable: Runnable) {
            if (isMobileAdsInitialized) {
                runnable.run()
            } else {
                pendingCallbacks.add(runnable)
                /* First run: the privacy policy is accepted after Application.onCreate()
                 * has already run, when hasAds() was still false, so nothing kicked off
                 * initialization for this process. Starting it here is what makes the
                 * queued ad loads actually run instead of waiting forever. */
                ensureMobileAdsInitialized()
            }
        }

        /**
         * Starts GMA Next-Gen SDK initialization once, on a background thread: the SDK
         * blocks internally while initializing, and WebView Chromium startup on the main
         * thread can cause an ANR.
         *
         * Idempotent and safe to call after Application.onCreate(): on a first run the
         * privacy policy is accepted later (and restartApp() only recreates the activity,
         * not the process), so this is the only chance to initialize for that process.
         */
        @JvmStatic
        @Synchronized
        fun ensureMobileAdsInitialized() {
            val app = instance ?: return
            if (isMobileAdsInitialized) return
            // hasAds() includes the privacy-policy consent check, so the SDK is never
            // initialized before the user has accepted it.
            if (app.dataUtil?.hasAds() != true) return
            if (!adsInitStarted.compareAndSet(false, true)) return
            Thread {
                MobileAds.initialize(app, InitializationConfig.Builder(app.getString(R.string.admob_app_id)).build()) {
                    mainHandler.post {
                        isMobileAdsInitialized = true
                        Log.d(TAG, "MobileAds initialized successfully")
                        drainPendingCallbacks()
                    }
                }
            }.start()
        }

        @Synchronized
        fun cancelPendingCallbacks(runnable: Runnable) {
            pendingCallbacks.remove(runnable)
        }

        fun getResourceString(resId: Int): String {
            return instance!!.getString(resId)
        }
        // Default VpnProfileCompat mode for openvpn2.4.x compatibility with softether vpn server
        const val VPN_PROFILE_COMPAT_MODE_24X = 20400
    }
}
