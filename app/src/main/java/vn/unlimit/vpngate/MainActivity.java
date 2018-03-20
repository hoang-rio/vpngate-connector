package vn.unlimit.vpngate;

import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;
import com.crashlytics.android.answers.SearchEvent;
import com.facebook.ads.Ad;
import com.facebook.ads.AdError;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;

import io.fabric.sdk.android.Fabric;
import vn.unlimit.vpngate.dialog.SortBottomSheetDialog;
import vn.unlimit.vpngate.fragment.AboutFragment;
import vn.unlimit.vpngate.fragment.HelpFragment;
import vn.unlimit.vpngate.fragment.HomeFragment;
import vn.unlimit.vpngate.fragment.SettingFragment;
import vn.unlimit.vpngate.fragment.StatusFragment;
import vn.unlimit.vpngate.models.VPNGateConnectionList;
import vn.unlimit.vpngate.provider.BaseProvider;
import vn.unlimit.vpngate.request.RequestListener;
import vn.unlimit.vpngate.task.VPNGateTask;
import vn.unlimit.vpngate.ultils.DataUtil;

public class MainActivity extends AppCompatActivity implements RequestListener, View.OnClickListener, NavigationView.OnNavigationItemSelectedListener {
    //    // Used to load the 'native-lib' library on application startup.
//    static {
//        System.loadLibrary("native-lib");
//    }

    private static String SORT_PROPERTY_KEY = "SORT_PROPERTY_KEY";
    private static String SORT_TYPE_KEY = "SORT_TYPE_KEY";
    final String TAG = "Main";
    VPNGateConnectionList vpnGateConnectionList;
    VPNGateTask vpnGateTask;
    View lnLoading;
    View frameContent;
    boolean isLoading = true;
    boolean doubleBackToExitPressedOnce = false;
    MenuItem selectedMenuItem = null;
    private DataUtil dataUtil;
    private Toolbar toolbar;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ActionBarDrawerToggle drawerToggle;
    private View lnError;
    private View lnNoNetwork;
    private String currentUrl = "";
    private String mSortProperty = "";
    private String currentTitle = "";
    private Menu mMenu;
    private int mSortType = VPNGateConnectionList.ORDER.ASC;
    private boolean disallowLoadHome = false;
    private AdView adView;
    private com.facebook.ads.AdView fAdView;
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case BaseProvider.ACTION.ACTION_CHANGE_NETWORK_STATE:
                    initState();
                    break;
                case BaseProvider.ACTION.ACTION_CLEAR_CACHE:
                    vpnGateConnectionList = null;
                    break;
                case BaseProvider.ACTION.ACTION_CONNECT_VPN:
                    if (dataUtil != null && dataUtil.getLastVPNConnection() != null) {
                        try {
                            navigationView.getMenu().findItem(R.id.nav_status).setVisible(true);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString("currentUrl", currentUrl);
        outState.putString("currentTitle", currentTitle);
        super.onSaveInstanceState(outState);
    }

    public String getSortProperty() {
        return mSortProperty;
    }

    public int getSortType() {
        return mSortType;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dataUtil = ((App) getApplication()).getDataUtil();
        if (savedInstanceState != null) {
            isLoading = false;
            currentUrl = savedInstanceState.getString("currentUrl");
            currentTitle = savedInstanceState.getString("currentTitle");
            vpnGateConnectionList = dataUtil.getConnectionsCache();
            disallowLoadHome = vpnGateConnectionList != null && vpnGateConnectionList.size() > 0;
        }
        Fabric.with(this, new Crashlytics());
        Fabric.with(this, new Answers());
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
        navigationView = findViewById(R.id.nav_main);
        navigationView.setNavigationItemSelectedListener(this);
        mSortProperty = dataUtil.getStringSetting(SORT_PROPERTY_KEY, "");
        mSortType = dataUtil.getIntSetting(SORT_TYPE_KEY, VPNGateConnectionList.ORDER.ASC);
        IntentFilter filter = new IntentFilter();
        filter.addAction(BaseProvider.ACTION.ACTION_CHANGE_NETWORK_STATE);
        filter.addAction(BaseProvider.ACTION.ACTION_CLEAR_CACHE);
        filter.addAction(BaseProvider.ACTION.ACTION_CONNECT_VPN);
        registerReceiver(broadcastReceiver, filter);
        try {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        if (App.isAdMobPrimary()) {
            initAdMob();
        } else {
            initFan();
        }
    }

    private void checkStatusMenu() {
        if (dataUtil.getLastVPNConnection() != null) {
            navigationView.getMenu().findItem(R.id.nav_status).setVisible(true);
        } else {
            navigationView.getMenu().findItem(R.id.nav_status).setVisible(false);
        }
    }

    private void initAdMob() {
        try {
            if (dataUtil.hasAds()) {
                MobileAds.initialize(this, dataUtil.getAdMobId());
                adView = new AdView(getApplicationContext());
                adView.setAdSize(AdSize.BANNER);
                if (BuildConfig.DEBUG) {
                    adView.setAdUnitId("ca-app-pub-3940256099942544/6300978111");
                } else {
                    adView.setAdUnitId(getResources().getString(R.string.admob_banner_bottom_home));
                }
                adView.setAdListener(new AdListener() {
                    @Override
                    public void onAdFailedToLoad(int errorCode) {
                        adView.setVisibility(View.GONE);
                        fAdView = new com.facebook.ads.AdView(MainActivity.this, getString(R.string.fan_banner_bottom_home), com.facebook.ads.AdSize.BANNER_HEIGHT_50);
                        fAdView.setAdListener(new com.facebook.ads.AdListener() {
                            @Override
                            public void onError(Ad ad, AdError adError) {
                                hideAdContainer();
                            }

                            @Override
                            public void onAdLoaded(Ad ad) {
                            }

                            @Override
                            public void onAdClicked(Ad ad) {

                            }

                            @Override
                            public void onLoggingImpression(Ad ad) {

                            }
                        });
                        ((RelativeLayout) findViewById(R.id.ad_container_home)).addView(fAdView);
                        fAdView.loadAd();
                    }
                });
                ((RelativeLayout) findViewById(R.id.ad_container_home)).addView(adView);
                adView.loadAd(new AdRequest.Builder().build());
            } else {
                hideAdContainer();
                navigationView.getMenu().setGroupVisible(R.id.menu_top, false);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initFan() {
        try {
            if (dataUtil.hasAds()) {
                MobileAds.initialize(this, dataUtil.getAdMobId());
                fAdView = new com.facebook.ads.AdView(MainActivity.this, getString(R.string.fan_banner_bottom_home), com.facebook.ads.AdSize.BANNER_HEIGHT_50);
                fAdView.setAdListener(new com.facebook.ads.AdListener() {
                    @Override
                    public void onError(Ad ad, AdError adError) {
                        fAdView.setVisibility(View.GONE);
                        adView = new AdView(getApplicationContext());
                        adView.setAdSize(AdSize.BANNER);
                        if (BuildConfig.DEBUG) {
                            adView.setAdUnitId("ca-app-pub-3940256099942544/6300978111");
                        } else {
                            adView.setAdUnitId(getResources().getString(R.string.admob_banner_bottom_home));
                        }
                        adView.setAdListener(new AdListener() {
                            @Override
                            public void onAdFailedToLoad(int errorCode) {
                                hideAdContainer();
                            }
                        });
                        ((RelativeLayout) findViewById(R.id.ad_container_home)).addView(adView);
                        adView.loadAd(new AdRequest.Builder().build());
                    }

                    @Override
                    public void onAdLoaded(Ad ad) {
                    }

                    @Override
                    public void onAdClicked(Ad ad) {

                    }

                    @Override
                    public void onLoggingImpression(Ad ad) {

                    }
                });
                ((RelativeLayout) findViewById(R.id.ad_container_home)).addView(fAdView);
                fAdView.loadAd();
            } else {
                hideAdContainer();
                navigationView.getMenu().setGroupVisible(R.id.menu_top, false);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void hideAdContainer() {
        try {
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
            ViewGroup.MarginLayoutParams marginLayoutParams = (ViewGroup.MarginLayoutParams) frameContent.getLayoutParams();
            params.setMargins(marginLayoutParams.leftMargin, marginLayoutParams.topMargin, marginLayoutParams.rightMargin, 0);
            if (adView != null) {
                adView.setVisibility(View.GONE);
            }
            if (fAdView != null) {
                fAdView.setVisibility(View.GONE);
            }
            findViewById(R.id.ad_container_home).setVisibility(View.GONE);
            frameContent.setLayoutParams(params);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Check network and process first state
     */
    private void initState() {
        checkStatusMenu();
        if (!disallowLoadHome) {
            if (DataUtil.isOnline(getApplicationContext())) {
                lnNoNetwork.setVisibility(View.GONE);
                vpnGateConnectionList = dataUtil.getConnectionsCache();
                if (vpnGateConnectionList == null || vpnGateConnectionList.size() == 0) {
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
        } else {
            setTitleActionbar(currentTitle);
            lnError.setVisibility(View.GONE);
            lnLoading.setVisibility(View.GONE);
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
        if (currentUrl.equals("home") && (vpnGateConnectionList == null || vpnGateConnectionList.size() == 0)) {
            initState();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (vpnGateTask != null) {
            vpnGateTask.stop();
        }
        unregisterReceiver(broadcastReceiver);
    }

    private void getDataServer() {
        isLoading = true;
        lnLoading.setVisibility(View.VISIBLE);
        lnError.setVisibility(View.GONE);
        frameContent.setVisibility(View.GONE);
        lnNoNetwork.setVisibility(View.GONE);
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
        mMenu = menu;
        toggleAction(currentUrl.equals("home"));
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
        if (searchManager != null) {
            searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        }
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
                    Answers.getInstance().logSearch(new SearchEvent()
                            .putQuery(newText));
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
        if (isLoading) {
            Toast.makeText(this, getResources().getText(R.string.feature_not_available), Toast.LENGTH_LONG).show();
            return true;
        }
        switch (item.getItemId()) {
            case R.id.action_sort:
                SortBottomSheetDialog sortBottomSheetDialog = SortBottomSheetDialog.newInstance(mSortProperty, mSortType);
                sortBottomSheetDialog.setOnApplyClickListener(new SortBottomSheetDialog.OnApplyClickListener() {
                    @Override
                    public void onApplyClick(String sortProperty, int sortType) {
                        mSortProperty = sortProperty;
                        mSortType = sortType;
                        dataUtil.setStringSetting(SORT_PROPERTY_KEY, mSortProperty);
                        dataUtil.setIntSetting(SORT_TYPE_KEY, mSortType);
                        HomeFragment currentFragment = (HomeFragment) getSupportFragmentManager().findFragmentByTag(HomeFragment.class.getName());
                        if (currentFragment != null) {
                            Answers.getInstance().logCustom(new CustomEvent("Sort")
                                    .putCustomAttribute("property", sortProperty)
                                    .putCustomAttribute("type", sortType == VPNGateConnectionList.ORDER.ASC ? "ASC" : "DESC"));
                            currentFragment.sort(sortProperty, sortType);
                        }
                    }
                });
                sortBottomSheetDialog.show(getSupportFragmentManager(), sortBottomSheetDialog.getTag());
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSuccess(Object o) {
        isLoading = false;
        lnLoading.setVisibility(View.GONE);
        frameContent.setVisibility(View.VISIBLE);
        if (!"".equals(mSortProperty)) {
            ((VPNGateConnectionList) o).sort(mSortProperty, mSortType);
        }
        updateData((VPNGateConnectionList) o);
        dataUtil.setConnectionsCache(vpnGateConnectionList);
    }

    private void updateData(VPNGateConnectionList o) {
        isLoading = false;
        vpnGateConnectionList = o;
        lnLoading.setVisibility(View.GONE);
        replaceFragment("home");
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
        selectedMenuItem = menuItem;
        disallowLoadHome = true;
        Answers.getInstance().logCustom(new CustomEvent("Drawer select")
                .putCustomAttribute("title", menuItem.getTitle().toString()));
        switch (menuItem.getItemId()) {
            case R.id.nav_get_pro:
                if (dataUtil.hasAds() && dataUtil.hasProInstalled()) {
                    Intent launchIntent = getPackageManager().getLaunchIntentForPackage("vn.unlimit.vpngatepro");
                    if (launchIntent != null) {
                        startActivity(launchIntent);//null pointer check in case package name was not found
                    }
                    finish();
                } else {
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=vn.unlimit.vpngatepro")));
                    } catch (android.content.ActivityNotFoundException ex) {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=vn.unlimit.vpngatepro")));
                    }
                }

                return false;
            case R.id.nav_home:
                if (vpnGateConnectionList == null) {
                    getDataServer();
                }
                replaceFragment("home");
                disallowLoadHome = false;
                break;
            case R.id.nav_status:
                if (dataUtil.getLastVPNConnection() == null) {
                    Toast.makeText(getApplicationContext(), getString(R.string.connect_one_warning), Toast.LENGTH_LONG).show();
                    return false;
                }
                replaceFragment("status");
                break;
            case R.id.nav_setting:
                replaceFragment("setting");
                stopRequest();
                break;
            case R.id.nav_about:
                replaceFragment("about");
                stopRequest();
                break;
            case R.id.nav_help:
                replaceFragment("help");
                stopRequest();
                break;
            default:
                break;
        }
        navigationView.setCheckedItem(menuItem.getItemId());
        if (toolbar != null) {
            toolbar.collapseActionView();
        }
        drawerLayout.closeDrawers();
        return true;
    }

    private void stopRequest() {
        try {
            if (vpnGateTask != null && !vpnGateTask.isCancelled()) {
                vpnGateTask.stop();
            }
            lnError.setVisibility(View.GONE);
            lnLoading.setVisibility(View.GONE);
            lnNoNetwork.setVisibility(View.GONE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void replaceFragment(String url) {
        try {
            if (url != null && (!url.equals(currentUrl) || url.equals("home"))) {
                toggleAction(url.equals("home") && vpnGateConnectionList != null);
                if (!url.equals("home")) {
                    lnLoading.setVisibility(View.GONE);
                    lnNoNetwork.setVisibility(View.GONE);
                    lnError.setVisibility(View.GONE);
                }
                currentUrl = url;
                Fragment fragment = null;
                String tag = "";
                String title = getResources().getString(R.string.app_name);
                switch (url) {
                    case "home":
                        fragment = new HomeFragment();
                        tag = HomeFragment.class.getName();
                        break;
                    case "status":
                        fragment = new StatusFragment();
                        tag = StatusFragment.class.getName();
                        title = getResources().getString(R.string.status);
                        break;
                    case "setting":
                        fragment = new SettingFragment();
                        tag = SettingFragment.class.getName();
                        title = getResources().getString(R.string.setting);
                        break;
                    case "help":
                        fragment = new HelpFragment();
                        tag = HelpFragment.class.getName();
                        title = getResources().getString(R.string.help);
                        break;
                    case "about":
                        fragment = new AboutFragment();
                        tag = AboutFragment.class.getName();
                        title = getResources().getString(R.string.about);
                        break;
                    default:
                        break;
                }
                if (fragment != null) {
                    frameContent.setVisibility(View.VISIBLE);
                    setTitleActionbar(title);
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.frame_content, fragment, tag)
                            //.addToBackStack("home")
                            .commitAllowingStateLoss();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void toggleAction(boolean visible) {
        if (mMenu != null) {
            mMenu.findItem(R.id.action_search).setVisible(visible);
            mMenu.findItem(R.id.action_sort).setVisible(visible);
        }
    }

    @Override
    public void onBackPressed() {
        switch (currentUrl) {
            case "home":
                if (doubleBackToExitPressedOnce) {
                    super.onBackPressed();
                    return;
                }
                this.doubleBackToExitPressedOnce = true;
                Toast.makeText(this, getResources().getString(R.string.press_back_again_to_exit), Toast.LENGTH_SHORT).show();
                new Handler().postDelayed(new Runnable() {

                    @Override
                    public void run() {
                        doubleBackToExitPressedOnce = false;
                    }
                }, 2000);
                break;
            default:
                if (vpnGateConnectionList == null) {
                    getDataServer();
                }
                navigationView.setCheckedItem(R.id.nav_home);
                replaceFragment("home");
                break;
        }
        drawerLayout.closeDrawer(Gravity.START);
    }

    private void setTitleActionbar(String title) {
        currentTitle = title;
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(title);
        }
    }

    @Override
    public void onError(String error) {
        Answers.getInstance().logCustom(new CustomEvent("Error").putCustomAttribute("screen", "home"));
        frameContent.setVisibility(View.GONE);
        lnLoading.setVisibility(View.GONE);
        lnError.setVisibility(View.VISIBLE);
        lnNoNetwork.setVisibility(View.GONE);
        System.out.print(error);
    }

    public VPNGateConnectionList getVpnGateConnectionList() {
        return vpnGateConnectionList;
    }

    public void setVpnGateConnectionList(VPNGateConnectionList _vpnGateConnectionList) {
        vpnGateConnectionList = _vpnGateConnectionList;
    }

//    private native String stringFromJNI();
}
