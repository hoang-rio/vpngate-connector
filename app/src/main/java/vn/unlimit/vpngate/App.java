package vn.unlimit.vpngate;

import android.app.Application;
import android.util.Log;

import com.google.android.gms.security.ProviderInstaller;
import com.google.android.gms.tasks.Task;
import com.google.android.play.core.missingsplits.MissingSplitsManagerFactory;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;

import de.blinkt.openvpn.core.OpenVPNService;
import vn.unlimit.vpngate.activities.DetailActivity;
import vn.unlimit.vpngate.utils.DataUtil;

public class App extends Application {

    private static App instance;
    private static boolean isImportToOpenVPN = false;
    private DataUtil dataUtil;

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
        if (!BuildConfig.DEBUG) {
            // OPTIONAL: If crash reporting has been explicitly disabled previously, add:
            FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true);
        }
        // Make notification open DetailActivity
        OpenVPNService.setNotificationActivityClass(DetailActivity.class);
        instance = this;
        dataUtil = new DataUtil(this);
        FirebaseRemoteConfig.getInstance().fetchAndActivate().addOnCompleteListener((Task<Boolean> task) -> {
            if (task.isSuccessful()) {
                Boolean updated = task.getResult();
                Log.e("RemoteConfigUpdated", updated + "");
                isImportToOpenVPN = FirebaseRemoteConfig.getInstance().getBoolean("vpn_import_open_vpn");
            }
        });
        super.onCreate();
        try {
            ProviderInstaller.installIfNeeded(getApplicationContext());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
