package vn.unlimit.vpngateclient;

import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import vn.unlimit.vpngateclient.models.VPNGateConnectionList;
import vn.unlimit.vpngateclient.request.RequestListener;
import vn.unlimit.vpngateclient.task.VPNGateTask;
import vn.unlimit.vpngateclient.ultils.CacheData;

public class MainActivity extends AppCompatActivity implements RequestListener {
    final String TAG = "Main";
    VPNGateConnectionList vpnGateConnectionList;
    VPNGateTask vpnGateTask;
    TextView txt;
    CacheData cacheData;
    boolean isLoading = true;

    //    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        cacheData = new CacheData(getApplicationContext());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        txt = this.findViewById(R.id.sample_text);
        txt.setText(stringFromJNI());
        vpnGateConnectionList = cacheData.getConnectionsCache();
        if (vpnGateConnectionList == null) {
            vpnGateTask = new VPNGateTask();
            vpnGateTask.setRequestListener(this);
            vpnGateTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } else {
            isLoading = false;
        }
    }

    @Override
    public void onComplete(Object o) {
        isLoading = false;
        vpnGateConnectionList = (VPNGateConnectionList) o;
        cacheData.setConnectionsCache(vpnGateConnectionList);
        System.out.print(o);
    }

    @Override
    public void onError(String error) {
        System.out.print(error);
    }

    private native String stringFromJNI();
}
