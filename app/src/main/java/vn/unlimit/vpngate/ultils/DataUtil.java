package vn.unlimit.vpngate.ultils;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.google.gson.Gson;

import java.util.Calendar;

import vn.unlimit.vpngate.models.Cache;
import vn.unlimit.vpngate.models.VPNGateConnectionList;

/**
 * Manager shared preferences data
 */

public class DataUtil {
    private Context mContext;
    private SharedPreferences sharedPreferencesCache;
    private SharedPreferences sharedPreferencesSetting;
    private Gson gson;
    private String CONNECTION_CACHE_KEY = "CONNECTION_CACHE_KEY";

    public DataUtil(Context context) {
        mContext = context;
        sharedPreferencesCache = mContext.getSharedPreferences("vpn_cache_data", Context.MODE_PRIVATE);
        sharedPreferencesSetting = mContext.getSharedPreferences("vpn_setting_data", Context.MODE_PRIVATE);
        gson = new Gson();
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
        String json = sharedPreferencesCache.getString(CONNECTION_CACHE_KEY, null);
        if (json == null) {
            return null;
        } else {
            Cache cache = gson.fromJson(json, Cache.class);
            if (cache.isExpires()) {
                SharedPreferences.Editor editor = sharedPreferencesCache.edit();
                editor.remove(CONNECTION_CACHE_KEY);
                editor.apply();
                return null;
            } else {
                return cache.cacheData;
            }
        }
    }

    /**
     * Set connection cache
     *
     * @param vpnGateConnectionList input VpnGateConnectionList
     */
    public void setConnectionsCache(VPNGateConnectionList vpnGateConnectionList) {
        SharedPreferences.Editor editor = sharedPreferencesCache.edit();
        Cache cache = new Cache();
        Calendar calendar = Calendar.getInstance();
        //Cache in 3 hours
        calendar.add(Calendar.HOUR, 3);
        cache.expires = calendar.getTime();
        cache.cacheData = vpnGateConnectionList;
        editor.putString(CONNECTION_CACHE_KEY, gson.toJson(cache));
        editor.apply();
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
        return sharedPreferencesSetting.getInt(key, defVal);
    }
}
