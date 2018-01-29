package vn.unlimit.vpngate;

import android.content.res.Configuration;
import android.os.AsyncTask;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import vn.unlimit.vpngate.adapter.VPNGateListAdapter;
import vn.unlimit.vpngate.models.VPNGateConnectionList;
import vn.unlimit.vpngate.request.RequestListener;
import vn.unlimit.vpngate.task.VPNGateTask;
import vn.unlimit.vpngate.ultils.DataUtil;

public class MainActivity extends AppCompatActivity implements RequestListener, SwipeRefreshLayout.OnRefreshListener {
    final String TAG = "Main";
    VPNGateConnectionList vpnGateConnectionList;
    VPNGateTask vpnGateTask;
    DataUtil dataUtil;
    ProgressBar loadingProgressBar;
    SwipeRefreshLayout lnSwipeRefresh;
    boolean isLoading = true;
    private DrawerLayout drawerLayout;
    private RecyclerView recyclerViewVPN;
    private ActionBarDrawerToggle drawerToggle;
    private VPNGateListAdapter vpnGateListAdapter;

    //    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        dataUtil = new DataUtil(getApplicationContext());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        loadingProgressBar = findViewById(R.id.loading);
        lnSwipeRefresh = findViewById(R.id.swipe_refresh);
        lnSwipeRefresh.setOnRefreshListener(this);
        recyclerViewVPN = findViewById(R.id.rcv_connection);
        vpnGateListAdapter = new VPNGateListAdapter(getApplicationContext());
        recyclerViewVPN.setAdapter(vpnGateListAdapter);
        drawerLayout = findViewById(R.id.activity_main_drawer);
        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(drawerToggle);
        try {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        vpnGateConnectionList = dataUtil.getConnectionsCache();
        if (vpnGateConnectionList == null) {
            getDataServer();
        } else {
            onSuccess(vpnGateConnectionList);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        vpnGateConnectionList = dataUtil.getConnectionsCache();
        if (vpnGateConnectionList == null) {
            isLoading = true;
            loadingProgressBar.setVisibility(View.VISIBLE);
            lnSwipeRefresh.setVisibility(View.GONE);
        } else {
            onSuccess(vpnGateConnectionList);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        vpnGateTask.stop();
    }

    private void getDataServer() {
        getDataServer(false);
    }

    private void getDataServer(boolean isRefresh) {
        if (!isRefresh) {
            lnSwipeRefresh.setVisibility(View.GONE);
            loadingProgressBar.setVisibility(View.VISIBLE);
        }
        isLoading = true;
        vpnGateTask = new VPNGateTask();
        vpnGateTask.setRequestListener(this);
        vpnGateTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @Override
    public void onRefresh() {
        getDataServer(true);
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
//            case R.id.menu_refresh:
//                getDataServer();
//                Toast.makeText(this, "Refresh button selected", Toast.LENGTH_SHORT).show();
//                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSuccess(Object o) {
        isLoading = false;
        loadingProgressBar.setVisibility(View.GONE);
        lnSwipeRefresh.setVisibility(View.VISIBLE);
        vpnGateConnectionList = (VPNGateConnectionList) o;
        dataUtil.setConnectionsCache(vpnGateConnectionList);
        vpnGateListAdapter.initialize(vpnGateConnectionList);
        lnSwipeRefresh.setRefreshing(false);
    }

    @Override
    public void onError(String error) {
        System.out.print(error);
    }

    private native String stringFromJNI();
}
