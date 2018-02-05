package vn.unlimit.vpngate.ultils;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

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

import vn.unlimit.vpngate.models.Cache;
import vn.unlimit.vpngate.models.VPNGateConnection;
import vn.unlimit.vpngate.models.VPNGateConnectionList;

/**
 * Manager shared preferences data
 */

public class DataUtil {
    public static String SETTING_CACHE_TIME_KEY = "SETTING_CACHE_TIME_KEY";
    private Context mContext;
    private SharedPreferences sharedPreferencesSetting;
    private Gson gson;
    private String CONNECTION_CACHE_KEY = "CONNECTION_CACHE_KEY";

    public DataUtil(Context context) {
        mContext = context;
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
            //Cache in hours get from setting
            int[] cacheTime = new int[]{1, 3, 5, 7, 12};
            int hour = cacheTime[getIntSetting(SETTING_CACHE_TIME_KEY, 0)];
            calendar.add(Calendar.HOUR, hour);
            cache.expires = calendar.getTime();
            cache.cacheData = vpnGateConnectionList;
            File outFile = new File(mContext.getFilesDir(), CONNECTION_CACHE_KEY);
            FileOutputStream out = new FileOutputStream(outFile);
            JsonWriter writer = new JsonWriter(new OutputStreamWriter(out, "UTF-8"));
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
        return sharedPreferencesSetting.getInt(key, defVal);
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

}
