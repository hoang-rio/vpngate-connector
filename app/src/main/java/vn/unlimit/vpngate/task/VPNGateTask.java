package vn.unlimit.vpngate.task;

import android.os.AsyncTask;
import android.util.Log;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.common.ANResponse;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

import vn.unlimit.vpngate.App;
import vn.unlimit.vpngate.models.VPNGateConnection;
import vn.unlimit.vpngate.models.VPNGateConnectionList;
import vn.unlimit.vpngate.request.RequestListener;
import vn.unlimit.vpngate.utils.DataUtil;

/**
 * Created by dongh on 14/01/2018.
 */

public class VPNGateTask extends AsyncTask<Void, Void, VPNGateConnectionList> {
    private static final String TAG = "VPNGateTask";
    private RequestListener requestListener;
    private Boolean isRetried = false;

    @Override
    protected VPNGateConnectionList doInBackground(Void... voids) {
        DataUtil dataUtil = App.getInstance().getDataUtil();
        VPNGateConnectionList vpnGateConnectionList = new VPNGateConnectionList();
        try {
            String url;
            if (dataUtil.getBooleanSetting(DataUtil.INCLUDE_UDP_SERVER, true)) {
                url = FirebaseRemoteConfig.getInstance().getString("vpn_udp_api");
            } else {
                url = dataUtil.getBaseUrl() + "/api/iphone/";
            }
            if (!dataUtil.hasAds()) {
                url += "?version=pro";
            }
            ANResponse anResponse = AndroidNetworking.get(url).build().executeForString();
            if (anResponse.isSuccess()) {
                vpnGateConnectionList = getConnectionList((String) anResponse.getResult());
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, e.getMessage(), e);
        }
        if (vpnGateConnectionList.size() == 0 && !isRetried) {
            isRetried = true;
            dataUtil.setUseAlternativeServer(true);
            return this.doInBackground(voids);
        }
        return vpnGateConnectionList;
    }

    // convert InputStream to VPNGateConnectionList
    private VPNGateConnectionList getConnectionList(String str) {
        VPNGateConnectionList vpnGateConnectionList = new VPNGateConnectionList();
        BufferedReader br = null;
        try {
            br = new BufferedReader(new StringReader(str));
            String line;
            while ((line = br.readLine()) != null) {
                if (line.indexOf("*") != 0 && line.indexOf("#") != 0) {
                    VPNGateConnection vpnGateConnection = VPNGateConnection.fromCsv(line);
                    if (vpnGateConnection != null) {
                        vpnGateConnectionList.add(vpnGateConnection);
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, e.getMessage(), e);
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return vpnGateConnectionList;

    }

    @Override
    protected void onPostExecute(VPNGateConnectionList vpnGateConnections) {
        if (!isCancelled() && requestListener != null) {
            if (vpnGateConnections.size() > 0) {
                requestListener.onSuccess(vpnGateConnections);
            } else {
                requestListener.onError("unknown");
            }
        }
        super.onPostExecute(vpnGateConnections);
    }

    public void setRequestListener(RequestListener requestListener) {
        this.requestListener = requestListener;
    }

    public void stop() {
        this.cancel(true);
        requestListener = null;
    }
}
