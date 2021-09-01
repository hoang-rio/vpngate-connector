package vn.unlimit.vpngate;

import android.app.Application;
import android.util.Log;

import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.security.ProviderInstaller;
import com.google.android.gms.tasks.Task;
import com.google.android.play.core.missingsplits.MissingSplitsManagerFactory;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;

import de.blinkt.openvpn.core.OpenVPNService;
import vn.unlimit.vpngate.activities.DetailActivity;
import vn.unlimit.vpngate.activities.MainActivity;
import vn.unlimit.vpngate.utils.AppOpenManager;
import vn.unlimit.vpngate.utils.DataUtil;
import vn.unlimit.vpngate.utils.PaidServerUtil;

public class App extends Application {

    private static App instance;
    private static boolean isImportToOpenVPN = false;
    private DataUtil dataUtil;
    private PaidServerUtil paidServerUtil;
    private static final String TAG = "VpnGateApp";

    public static AppOpenManager appOpenManager;


    public static String getResourceString(int resId) {
        return instance.getString(resId);
    }

    public static App getInstance() {
        return instance;
    }

    public static boolean isIsImportToOpenVPN() {
        return isImportToOpenVPN;
    }

    public DataUtil getDataUtil() {
        return dataUtil;
    }

    @Override
    public void onCreate() {
        if (MissingSplitsManagerFactory.create(this).disableAppIfMissingRequiredSplits()) {
            // Skip app initialization.
            return;
        }
        super.onCreate();
        if (!BuildConfig.DEBUG) {
            // OPTIONAL: If crash reporting has been explicitly disabled previously, add:
            FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true);
        }
        instance = this;
        dataUtil = new DataUtil(this);
        if (dataUtil.hasAds()) {
            MobileAds.initialize(this);
            if (dataUtil.isAcceptedPrivacyPolicy() && dataUtil.getBooleanSetting(DataUtil.INVITED_USE_PAID_SERVER, false)) {
                appOpenManager = new AppOpenManager(this);
            }
        }
        // Make notification open DetailActivity
        OpenVPNService.setNotificationActivityClass(dataUtil.getIntSetting(DataUtil.SETTING_STARTUP_SCREEN, 0) == 0 ? DetailActivity.class : MainActivity.class);
        paidServerUtil = new PaidServerUtil(this);
        FirebaseRemoteConfig.getInstance().fetchAndActivate().addOnCompleteListener((Task<Boolean> task) -> {
            if (task.isSuccessful()) {
                Boolean updated = task.getResult();
                Log.e(TAG, "RemoteConfigUpdated:" + updated);
                isImportToOpenVPN = FirebaseRemoteConfig.getInstance().getBoolean("vpn_import_open_vpn");
            }
        });
        try {
            ProviderInstaller.installIfNeeded(getApplicationContext());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public PaidServerUtil getPaidServerUtil() {
        return paidServerUtil;
    }

    public void setPaidServerUtil(PaidServerUtil paidServerUtil) {
        this.paidServerUtil = paidServerUtil;
    }
}
