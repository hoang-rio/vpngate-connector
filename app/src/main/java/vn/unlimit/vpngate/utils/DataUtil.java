package vn.unlimit.vpngate.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;

import com.google.android.gms.tasks.Task;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Calendar;
import java.util.Date;

import vn.unlimit.vpngate.BuildConfig;
import vn.unlimit.vpngate.R;
import vn.unlimit.vpngate.models.Cache;
import vn.unlimit.vpngate.models.VPNGateConnection;
import vn.unlimit.vpngate.models.VPNGateConnectionList;

/**
 * Manager shared preferences data
 */

public class DataUtil {
    public static final String SETTING_CACHE_TIME_KEY = "SETTING_CACHE_TIME_KEY";
    public static final String SETTING_HIDE_OPERATOR_MESSAGE_COUNT = "SETTING_HIDE_OPERATOR_MESSAGE_COUNT";
    public static final String USER_ALLOWED_VPN = "USER_ALLOWED_VPN";
    public static final String SETTING_BLOCK_ADS = "SETTING_BLOCK_ADS";
    public static final String INCLUDE_UDP_SERVER = "INCLUDE_UDP_SERVER";
    public static final String LAST_CONNECT_USE_UDP = "LAST_CONNECT_USE_UDP";
    public static final String USE_CUSTOM_DNS = "USE_CUSTOM_DNS";
    public static final String CUSTOM_DNS_IP_1 = "CUSTOM_DNS_IP_1";
    public static final String CUSTOM_DNS_IP_2 = "CUSTOM_DNS_IP_2";
    public static final String USE_DOMAIN_TO_CONNECT = "USE_DOMAIN_TO_CONNECT";
    public static final String SETTING_DEFAULT_PROTOCOL = "SETTING_DEFAULT_PROTOCOL";
    public static final String SETTING_STARTUP_SCREEN = "SETTING_STARTUP_SCREEN";
    public static final String SETTING_NOTIFY_SPEED = "SETTING_NOTIFY_SPEED";
    public static final String INVITED_USE_PAID_SERVER = "INVITED_USE_PAID_SERVER";
    private static final String USE_ALTERNATIVE_SERVER = "USE_ALTERNATIVE_SERVER";
    private static final String ACCEPTED_PRIVACY_POLICY = "ACCEPTED_PRIVACY_POLICY";
    private Context mContext;
    private SharedPreferences sharedPreferencesSetting;
    private Gson gson;
    private final String CONNECTION_CACHE_KEY = "CONNECTION_CACHE_KEY";

    public DataUtil(Context context) {
        try {
            mContext = context;
            sharedPreferencesSetting = mContext.getSharedPreferences("vpn_setting_data_" + BuildConfig.FLAVOR, Context.MODE_PRIVATE);
            gson = new Gson();
            FirebaseRemoteConfig mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
            FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
                    .setMinimumFetchIntervalInSeconds(BuildConfig.DEBUG || !this.isAcceptedPrivacyPolicy() ? 0 : 3600)
                    .build();
            mFirebaseRemoteConfig.setDefaultsAsync(R.xml.remote_config_defaults);
            mFirebaseRemoteConfig.setConfigSettingsAsync(configSettings).addOnCompleteListener((Task<Void> task) -> mFirebaseRemoteConfig.fetchAndActivate());
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    /**
     * Check device connect to a network or not
     *
     * @param context
     * @return connect to network result
     */
    public static boolean isOnline(Context context) {
        try {
            ConnectivityManager cm =
                    (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo netInfo = cm.getActiveNetworkInfo();
            return netInfo != null && netInfo.isConnectedOrConnecting();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get connection cache
     *
     * @return VPNGateConnectionList
     */
    public VPNGateConnectionList getConnectionsCache() {
        try {
            File inFile = new File(mContext.getFilesDir(), CONNECTION_CACHE_KEY);
            if (!inFile.isFile()) {
                return null;
            } else {
                FileInputStream fileInputStream = new FileInputStream(inFile);
                JsonReader reader = new JsonReader(new InputStreamReader(fileInputStream));
                Cache cache = gson.fromJson(reader, Cache.class);
                if (cache.isExpires()) {
                    reader.close();
                    return null;
                } else {
                    reader.close();
                    return cache.cacheData;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Set connection cache
     *
     * @param vpnGateConnectionList input VpnGateConnectionList
     */
    public void setConnectionsCache(VPNGateConnectionList vpnGateConnectionList) {
        try {
            Cache cache = new Cache();
            Calendar calendar = Calendar.getInstance();
            //Cache in minute get from setting
            int[] cacheTime = new int[]{15, 30, 60, 120, 240, 480, 960};
            int minute = cacheTime[getIntSetting(SETTING_CACHE_TIME_KEY, 0)];
            calendar.add(Calendar.MINUTE, minute);
            cache.expires = calendar.getTime();
            vpnGateConnectionList.setFilter(null);
            cache.cacheData = vpnGateConnectionList;
            File outFile = new File(mContext.getFilesDir(), CONNECTION_CACHE_KEY);
            FileOutputStream out = new FileOutputStream(outFile);
            JsonWriter writer = new JsonWriter(new OutputStreamWriter(out, "utf8"));
            gson.toJson(cache, Cache.class, writer);
            writer.close();
            setConnectionCacheExpire(cache.expires);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Clear connection cache
     *
     * @return boolean
     */
    public boolean clearConnectionCache() {
        File inFile = new File(mContext.getFilesDir(), CONNECTION_CACHE_KEY);
        return inFile.isFile() && inFile.delete();
    }

    private void setConnectionCacheExpire(Date expires) {
        SharedPreferences.Editor editor = sharedPreferencesSetting.edit();
        editor.putString("vpn_cache_time", gson.toJson(expires));
        editor.apply();
    }

    /**
     * Get connection cache from shared preferences
     *
     * @return
     */
    public Date getConnectionCacheExpires() {
        try {
            String jsonString = sharedPreferencesSetting.getString("vpn_cache_time", null);
            if (jsonString == null) {
                return null;
            }
            return gson.fromJson(jsonString, Date.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public void setStringSetting(String key, String value) {
        SharedPreferences.Editor editor = sharedPreferencesSetting.edit();
        editor.putString(key, value);
        editor.apply();
    }

    public String getStringSetting(String key, String defVal) {
        return sharedPreferencesSetting.getString(key, defVal);
    }

    /**
     * Set int setting value
     *
     * @param key   setting key
     * @param value setting value
     */
    public void setIntSetting(String key, int value) {
        SharedPreferences.Editor editor = sharedPreferencesSetting.edit();
        editor.putInt(key, value);
        editor.apply();
    }

    /**
     * Get int setting value
     *
     * @param key    setting key
     * @param defVal default value
     * @return int
     */
    public int getIntSetting(String key, int defVal) {
        int retVal = sharedPreferencesSetting.getInt(key, defVal);
        if (key.equals(SETTING_HIDE_OPERATOR_MESSAGE_COUNT) && retVal > 0) {
            setIntSetting(key, retVal--);
        }
        return retVal;
    }

    public VPNGateConnection getLastVPNConnection() {
        try {
            String jsonString = sharedPreferencesSetting.getString("vpn_last_connection", null);
            if (jsonString == null) {
                return null;
            }
            return gson.fromJson(jsonString, VPNGateConnection.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public void setLastVPNConnection(VPNGateConnection vpnGateConnection) {
        try {
            if (vpnGateConnection != null) {
                SharedPreferences.Editor editor = sharedPreferencesSetting.edit();
                editor.putString("vpn_last_connection", gson.toJson(vpnGateConnection));
                editor.apply();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public boolean getBooleanSetting(String key, boolean defVal) {
        return sharedPreferencesSetting.getBoolean(key, defVal);
    }

    public void setBooleanSetting(String key, boolean value) {
        SharedPreferences.Editor editor = sharedPreferencesSetting.edit();
        editor.putBoolean(key, value);
        editor.apply();
    }

    public boolean hasAds() {
        try {
            return this.isAcceptedPrivacyPolicy() && BuildConfig.FLAVOR.equals("free") && getAdMobId() != null;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    public String getAdMobId() {
        try {
            ApplicationInfo app = mContext.getPackageManager().getApplicationInfo(mContext.getPackageName(), PackageManager.GET_META_DATA);
            Bundle bundle = app.metaData;
            if (bundle != null) {
                return bundle.getString("com.google.android.gms.ads.APPLICATION_ID");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean hasProInstalled() {
        try {
            android.content.pm.PackageManager mPm = mContext.getPackageManager();  // 1
            PackageInfo info = mPm.getPackageInfo("vn.unlimit.vpngatepro", 0);  // 2,3
            return info != null;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean hasOpenVPNInstalled() {
        try {
            android.content.pm.PackageManager mPm = mContext.getPackageManager();  // 1
            PackageInfo info = mPm.getPackageInfo("net.openvpn.openvpn", 0);  // 2,3
            return info != null;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public String getBaseUrl() {
        try {
            if (sharedPreferencesSetting.getBoolean(USE_ALTERNATIVE_SERVER, false)) {
                return FirebaseRemoteConfig.getInstance().getString(mContext.getString(R.string.alternative_api_cfg_key));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "https://www.vpngate.net";
    }

    public void setUseAlternativeServer(Boolean useAlternativeServer) {
        SharedPreferences.Editor editor = sharedPreferencesSetting.edit();
        editor.putBoolean(USE_ALTERNATIVE_SERVER, useAlternativeServer);
        editor.apply();
    }

    public boolean isAcceptedPrivacyPolicy() {
        return sharedPreferencesSetting.getBoolean(ACCEPTED_PRIVACY_POLICY, false);
    }

    public void setAcceptedPrivacyPolicy(boolean isAccepted) {
        SharedPreferences.Editor editor = sharedPreferencesSetting.edit();
        editor.putBoolean(ACCEPTED_PRIVACY_POLICY, isAccepted);
        editor.apply();
    }

}
