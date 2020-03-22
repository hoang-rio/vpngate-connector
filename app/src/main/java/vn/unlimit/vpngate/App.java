package vn.unlimit.vpngate;

import android.app.Application;

import com.crashlytics.android.Crashlytics;
import com.google.android.gms.security.ProviderInstaller;
import com.google.android.play.core.missingsplits.MissingSplitsManagerFactory;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;

import io.fabric.sdk.android.Fabric;
import vn.unlimit.vpngate.utils.DataUtil;

public class App extends Application {

    private static App instance;
    private static boolean isImportToOpenVPN = true;
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
        super.onCreate();
        if (!BuildConfig.DEBUG) {
            Fabric.with(this, new Crashlytics());
        }
        instance = this;
        dataUtil = new DataUtil(this);
        isImportToOpenVPN = FirebaseRemoteConfig.getInstance().getBoolean("vpn_import_open_vpn");
        try {
            ProviderInstaller.installIfNeeded(getApplicationContext());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
