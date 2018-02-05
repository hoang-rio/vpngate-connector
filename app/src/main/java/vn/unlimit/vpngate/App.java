package vn.unlimit.vpngate;

import android.app.Application;
import android.content.Context;

import com.crashlytics.android.Crashlytics;

import io.fabric.sdk.android.Fabric;
import vn.unlimit.vpngate.ultils.DataUtil;

public class App extends Application {

    private static App instance;
    private DataUtil dataUtil;

    public static String getResourceString(int resId) {
        return instance.getString(resId);
    }

    public static App getInstance() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (!BuildConfig.DEBUG)
            Fabric.with(this, new Crashlytics());

        instance = this;
        dataUtil = new DataUtil(this);
    }

    public DataUtil getDataUtil() {
        return dataUtil;
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
    }

}
