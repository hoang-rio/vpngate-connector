package vn.unlimit.vpngate;

import android.app.Application;
import android.content.Context;

import com.crashlytics.android.Crashlytics;

import io.fabric.sdk.android.Fabric;

public class App extends Application {

    private static App instance;

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
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
    }

}
