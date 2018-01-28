package vn.unlimit.vpngate;

import android.content.res.Configuration;
import android.os.AsyncTask;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import vn.unlimit.vpngate.models.VPNGateConnectionList;
import vn.unlimit.vpngate.request.RequestListener;
import vn.unlimit.vpngate.task.VPNGateTask;
import vn.unlimit.vpngate.ultils.CacheData;

public class MainActivity extends AppCompatActivity implements RequestListener {
    final String TAG = "Main";
    VPNGateConnectionList vpnGateConnectionList;
    VPNGateTask vpnGateTask;
    TextView txt;
    CacheData cacheData;
    ProgressBar loadingProgressBar;
    View lnSwipeRefresh;
    boolean isLoading = true;
    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle drawerToggle;

    //    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        cacheData = new CacheData(getApplicationContext());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        loadingProgressBar = findViewById(R.id.loading);
        lnSwipeRefresh = findViewById(R.id.swiperefresh);
        drawerLayout = (DrawerLayout) findViewById(R.id.activity_main_drawer);
        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(drawerToggle);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        txt = this.findViewById(R.id.sample_text);
        txt.setText(stringFromJNI());
        vpnGateConnectionList = cacheData.getConnectionsCache();
        if(vpnGateConnectionList == null) {
            getDataServer();
        }else {
            onComplete(vpnGateConnectionList);
        }
    }
    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }
    private void getDataServer() {
        lnSwipeRefresh.setVisibility(View.GONE);
        isLoading = true;
        loadingProgressBar.setVisibility(View.VISIBLE);
        vpnGateTask = new VPNGateTask();
        vpnGateTask.setRequestListener(this);
        vpnGateTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        drawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        drawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (drawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        switch (item.getItemId()) {
            case R.id.search:
                Toast.makeText(this, "Search button selected", Toast.LENGTH_SHORT).show();
                return true;
            case R.id.menu_refresh:
                getDataServer();
                Toast.makeText(this, "Refresh button selected", Toast.LENGTH_SHORT).show();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onComplete(Object o) {
        isLoading = false;
        loadingProgressBar.setVisibility(View.GONE);
        lnSwipeRefresh.setVisibility(View.VISIBLE);
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
