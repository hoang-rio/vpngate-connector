package vn.unlimit.vpngate;

import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;

import io.fabric.sdk.android.Fabric;
import vn.unlimit.vpngate.fragment.HomeFragment;
import vn.unlimit.vpngate.models.VPNGateConnectionList;
import vn.unlimit.vpngate.request.RequestListener;
import vn.unlimit.vpngate.task.VPNGateTask;
import vn.unlimit.vpngate.ultils.DataUtil;

public class MainActivity extends AppCompatActivity implements RequestListener, View.OnClickListener {
    //    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    final String TAG = "Main";
    VPNGateConnectionList vpnGateConnectionList;
    VPNGateTask vpnGateTask;
    View lnLoading;
    View frameContent;
    private DataUtil dataUtil;
    private Toolbar toolbar;
    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle drawerToggle;
    private View lnError;
    private View lnNoNetwork;
    private String currentUrl = "home";
    private BroadcastReceiver connectionChangeReceive = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            initState();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        dataUtil = new DataUtil(getApplicationContext());
        super.onCreate(savedInstanceState);
        Fabric.with(this, new Crashlytics());
        setContentView(R.layout.activity_main);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        lnLoading = findViewById(R.id.ln_loading);
        lnError = findViewById(R.id.ln_error);
        lnNoNetwork = findViewById(R.id.ln_no_network);
        lnError.setOnClickListener(this);
        frameContent = findViewById(R.id.frame_content);
        drawerLayout = findViewById(R.id.activity_main_drawer);
        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(drawerToggle);
        IntentFilter filter = new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE");
        registerReceiver(connectionChangeReceive, filter);
        try {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Check network and process first state
     */
    private void initState() {
        if (DataUtil.isOnline(getApplicationContext())) {
            lnNoNetwork.setVisibility(View.GONE);
            vpnGateConnectionList = dataUtil.getConnectionsCache();
            if (vpnGateConnectionList == null) {
                getDataServer();
            } else {
                updateData(vpnGateConnectionList);
            }
        } else {
            lnNoNetwork.setVisibility(View.VISIBLE);
            lnError.setVisibility(View.GONE);
            lnLoading.setVisibility(View.GONE);
            frameContent.setVisibility(View.GONE);
        }
    }

    @Override
    public void onClick(View view) {
        if (view.equals(lnError)) {
            lnError.setVisibility(View.GONE);
            getDataServer();
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
    protected void onDestroy() {
        super.onDestroy();
        if (vpnGateTask != null) {
            vpnGateTask.stop();
        }
        unregisterReceiver(connectionChangeReceive);
    }

    private void getDataServer() {
        lnLoading.setVisibility(View.VISIBLE);
        if (vpnGateTask != null && vpnGateTask.isCancelled()) {
            vpnGateTask.stop();
        }
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

        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        MenuItem menuSearch = menu.findItem(R.id.action_search);
        menuSearch.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                HomeFragment currentFragment = (HomeFragment) getSupportFragmentManager().findFragmentByTag(HomeFragment.class.getName());
                if (currentFragment != null) {
                    currentFragment.filter("");
                }
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                HomeFragment currentFragment = (HomeFragment) getSupportFragmentManager().findFragmentByTag(HomeFragment.class.getName());
                if (currentFragment != null) {
                    currentFragment.closeSearch();
                }
                return true;
            }
        });
        final SearchView searchView = (SearchView) menuSearch.getActionView();

        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setMaxWidth(Integer.MAX_VALUE);
        final EditText editText = searchView.findViewById(android.support.v7.appcompat.R.id.search_src_text);
        editText.setImeOptions(EditorInfo.IME_ACTION_DONE);
        editText.setTextColor(getResources().getColor(R.color.colorWhite));
        editText.setHintTextColor(getResources().getColor(R.color.colorWhiteTransparent));
//        searchView.setSubmitButtonEnabled(true);
        searchView.setQueryHint(getString(R.string.search_hint));
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                HomeFragment currentFragment = (HomeFragment) getSupportFragmentManager().findFragmentByTag(HomeFragment.class.getName());
                if (currentFragment != null) {
                    currentFragment.filter(newText);
                    return true;
                }
                return false;
            }
        });
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (drawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        switch (item.getItemId()) {
            case R.id.action_sort:
                Toast.makeText(this, "Sort button selected", Toast.LENGTH_SHORT).show();
                return true;
            case R.id.action_search:
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSuccess(Object o) {
        lnLoading.setVisibility(View.GONE);
        updateData(o);
        dataUtil.setConnectionsCache(vpnGateConnectionList);
    }

    private void updateData(Object o) {
        vpnGateConnectionList = (VPNGateConnectionList) o;
        lnLoading.setVisibility(View.GONE);
        replaceFragment(currentUrl);
    }

    private void replaceFragment(String url) {
        try {
            if (url != null) {
                currentUrl = url;
                Fragment fragment = null;
                String tag = "";
                String title = getResources().getString(R.string.app_name);
                if (url.equals("home")) {
                    fragment = HomeFragment.newInstance(vpnGateConnectionList);
                    tag = HomeFragment.class.getName();
                }
                if (fragment != null) {
                    frameContent.setVisibility(View.VISIBLE);
                    setTitleActionbar(title);
                    if (url.equals("home")) {
                        getSupportFragmentManager().beginTransaction()
                                .replace(R.id.frame_content, fragment, tag)
                                //.addToBackStack("home")
                                .commitAllowingStateLoss();
                    } else {
                        getSupportFragmentManager().beginTransaction()
                                .replace(R.id.frame_content, fragment, tag)
                                .addToBackStack("home")
                                .commitAllowingStateLoss();
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setTitleActionbar(String title) {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(title);
        }
    }

    @Override
    public void onError(String error) {
        frameContent.setVisibility(View.GONE);
        lnLoading.setVisibility(View.GONE);
        lnError.setVisibility(View.VISIBLE);
        System.out.print(error);
    }

    public DataUtil getDataUtil() {
        return dataUtil;
    }

    private native String stringFromJNI();
}
