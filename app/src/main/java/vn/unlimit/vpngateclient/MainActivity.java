package vn.unlimit.vpngateclient;

import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import vn.unlimit.vpngateclient.models.VPNGateConnectionList;
import vn.unlimit.vpngateclient.request.RequestListener;
import vn.unlimit.vpngateclient.task.VPNGateTask;

public class MainActivity extends AppCompatActivity implements RequestListener {
    final String TAG = "Main";
    VPNGateConnectionList vpnGateConnectionList;
    VPNGateTask vpnGateTask;

//    // Used to load the 'native-lib' library on application startup.
//    static {
//        System.loadLibrary("native-lib");
//    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        vpnGateTask = new VPNGateTask();
        vpnGateTask.setRequestListener(this);
        vpnGateTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @Override
    public void onComplete(Object o) {
        vpnGateConnectionList = (VPNGateConnectionList) o;
        System.out.print(o);
    }

    @Override
    public void onError(String error) {
        System.out.print(error);
    }
}
