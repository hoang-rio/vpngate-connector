package vn.unlimit.vpngate;

import android.app.Application;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.answers.Answers;
import com.google.android.gms.security.ProviderInstaller;

import io.fabric.sdk.android.Fabric;
import vn.unlimit.vpngate.request.RequestListener;
import vn.unlimit.vpngate.utils.DataUtil;

public class App extends Application {

    private static App instance;
    private static boolean isAdMobPrimary = true;
    private DataUtil dataUtil;

    public static String getResourceString(int resId) {
        return instance.getString(resId);
    }

    public static App getInstance() {
        return instance;
    }

    public static boolean isAdMobPrimary() {
        return isAdMobPrimary;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (!BuildConfig.DEBUG) {
            Fabric.with(this, new Crashlytics());
        }
        Fabric.with(this, new Answers());
        instance = this;
        dataUtil = new DataUtil(this);
        isAdMobPrimary = dataUtil.getBooleanSetting(DataUtil.CONFIG_ADMOB_PRIMARY, isAdMobPrimary);
        dataUtil.getIsAmobPrimary(new RequestListener() {
            @Override
            public void onSuccess(Object result) {
                isAdMobPrimary = (boolean) result;
            }

            @Override
            public void onError(String error) {

            }
        });
        try {
            ProviderInstaller.installIfNeeded(getApplicationContext());
        }catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public DataUtil getDataUtil() {
        return dataUtil;
    }
}
