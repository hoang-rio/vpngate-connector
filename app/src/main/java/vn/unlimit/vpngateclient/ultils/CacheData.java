package vn.unlimit.vpngateclient.ultils;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;

import java.util.Date;

import vn.unlimit.vpngateclient.models.Cache;
import vn.unlimit.vpngateclient.models.VPNGateConnectionList;

/**
 * Created by hoangnd on 1/17/2018.
 */

public class CacheData {
    private Context mContext;
    private SharedPreferences sharedPreferencesCache;
    private Gson gson;
    private String CONNECTION_CACHE_KEY = "CONNECTION_CACHE_KEY";

    public CacheData(Context context) {
        sharedPreferencesCache = context.getSharedPreferences("vpn_cache_data", Context.MODE_PRIVATE);
        gson = new Gson();
        mContext = context;
    }

    public void setConnectionsCache(VPNGateConnectionList vpnGateConnectionList) {
        SharedPreferences.Editor editor = sharedPreferencesCache.edit();
        Cache cache = new Cache();
        cache.expires = new Date();
        //Cache in 3 hours
        cache.expires.setTime(cache.expires.getTime() + 3 * 60 * 60);
        cache.cacheData = vpnGateConnectionList;
        editor.putString(CONNECTION_CACHE_KEY, gson.toJson(cache));
        editor.apply();
    }

    public VPNGateConnectionList getConnectionsCache() {
        String json = sharedPreferencesCache.getString(CONNECTION_CACHE_KEY, null);
        if (json == null) {
            return null;
        } else {
            Cache cache = gson.fromJson(json, Cache.class);
            if (cache.expires.after(new Date())) {
                SharedPreferences.Editor editor = sharedPreferencesCache.edit();
                editor.remove(CONNECTION_CACHE_KEY);
                editor.apply();
                return null;
            } else {
                return cache.cacheData;
            }
        }
    }
}
