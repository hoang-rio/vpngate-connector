package vn.unlimit.vpngate;

import android.app.SearchManager;
import android.content.Context;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import vn.unlimit.vpngate.adapter.OnItemClickListener;
import vn.unlimit.vpngate.adapter.VPNGateListAdapter;
import vn.unlimit.vpngate.models.VPNGateConnectionList;
import vn.unlimit.vpngate.request.RequestListener;
import vn.unlimit.vpngate.task.VPNGateTask;
import vn.unlimit.vpngate.ultils.DataUtil;

public class MainActivity extends AppCompatActivity implements RequestListener, SwipeRefreshLayout.OnRefreshListener, OnItemClickListener {
    final String TAG = "Main";
    VPNGateConnectionList vpnGateConnectionList;
    VPNGateTask vpnGateTask;
    DataUtil dataUtil;
    View lnLoading;
    SwipeRefreshLayout lnSwipeRefresh;
    boolean isLoading = true;
    private Toolbar toolbar;
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
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        lnLoading = findViewById(R.id.ln_loading);
        lnSwipeRefresh = findViewById(R.id.swipe_refresh);
        lnSwipeRefresh.setOnRefreshListener(this);
        recyclerViewVPN = findViewById(R.id.rcv_connection);
        vpnGateListAdapter = new VPNGateListAdapter(getApplicationContext());
        vpnGateListAdapter.setOnItemClickListener(this);
        recyclerViewVPN.setAdapter(vpnGateListAdapter);
        recyclerViewVPN.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
        drawerLayout = findViewById(R.id.activity_main_drawer);
        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
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
    public void onItemClick(Object o, int position) {
        Toast.makeText(this, "Select item at position: " + position, Toast.LENGTH_LONG).show();
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
            lnLoading.setVisibility(View.VISIBLE);
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
            lnLoading.setVisibility(View.VISIBLE);
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

        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        MenuItem menuSearch = menu.findItem(R.id.action_search);

        final SearchView searchView = (SearchView) menuSearch.getActionView();

        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setMaxWidth(Integer.MAX_VALUE);
//        searchView.setSubmitButtonEnabled(true);
        searchView.setQueryHint(getString(R.string.search_hint));
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (drawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        switch (item.getItemId()) {
            case R.id.action_search:
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
        lnLoading.setVisibility(View.GONE);
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
