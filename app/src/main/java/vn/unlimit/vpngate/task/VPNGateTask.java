package vn.unlimit.vpngate.task;

import android.os.AsyncTask;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import vn.unlimit.vpngate.models.VPNGateConnection;
import vn.unlimit.vpngate.models.VPNGateConnectionList;
import vn.unlimit.vpngate.request.RequestListener;

/**
 * Created by dongh on 14/01/2018.
 */

public class VPNGateTask extends AsyncTask<Void, Void, VPNGateConnectionList> {
    private static String VPN_GATE_API_URL = "http://www.vpngate.net/api/iphone/";
    final Integer TYPE_ERROR = 0;
    final Integer TYPE_NOT_RELOAD = 1;

    @Override
    protected VPNGateConnectionList doInBackground(Void... voids) {
        VPNGateConnectionList vpnGateConnectionList = new VPNGateConnectionList();
        HttpURLConnection connection = null;
        InputStreamReader inputStreamReader = null;
        try {
            URL url = new URL(VPN_GATE_API_URL);
            connection = (HttpURLConnection) url.openConnection();
            connection.setReadTimeout(5000);
            connection.setConnectTimeout(5000);
            connection.setRequestProperty("Accept-Encoding", "identity");
            connection.connect();
            String csv = getStringFromInputStream(connection.getInputStream());
            vpnGateConnectionList = getConnectionList(csv);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                connection.disconnect();
                inputStreamReader.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return vpnGateConnectionList;
    }

    private VPNGateConnectionList getConnectionList(String csv) {
        VPNGateConnectionList vpnGateConnectionList = new VPNGateConnectionList();
        try {
            String[] lines = csv.split("\n");
            for (String line : lines) {
                if (line.indexOf("*") != 0 && line.indexOf("#") != 0) {
                    VPNGateConnection vpnGateConnection = VPNGateConnection.fromCsv(line);
                    if (vpnGateConnection != null) {
                        vpnGateConnectionList.add(vpnGateConnection);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return vpnGateConnectionList;
    }

    // convert InputStream to String
    private static String getStringFromInputStream(InputStream is) {

        BufferedReader br = null;
        StringBuilder sb = new StringBuilder();

        String line;
        try {

            br = new BufferedReader(new InputStreamReader(is));
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return sb.toString();

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

    private RequestListener requestListener;

    public void setRequestListener(RequestListener requestListener) {
        this.requestListener = requestListener;
    }

    public void stop() {
        this.cancel(true);
        requestListener = null;
    }
}
