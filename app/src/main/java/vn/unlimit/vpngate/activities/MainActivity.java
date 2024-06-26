package vn.unlimit.vpngate.activities;

import android.annotation.SuppressLint;
import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.material.navigation.NavigationView;
import com.google.android.ump.ConsentDebugSettings;
import com.google.android.ump.ConsentInformation;
import com.google.android.ump.ConsentRequestParameters;
import com.google.android.ump.FormError;
import com.google.android.ump.UserMessagingPlatform;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.gson.Gson;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import vn.unlimit.vpngate.App;
import vn.unlimit.vpngate.BuildConfig;
import vn.unlimit.vpngate.R;
import vn.unlimit.vpngate.activities.paid.LoginActivity;
import vn.unlimit.vpngate.activities.paid.PaidServerActivity;
import vn.unlimit.vpngate.dialog.FilterBottomSheetDialog;
import vn.unlimit.vpngate.dialog.SortBottomSheetDialog;
import vn.unlimit.vpngate.fragment.AboutFragment;
import vn.unlimit.vpngate.fragment.HelpFragment;
import vn.unlimit.vpngate.fragment.HomeFragment;
import vn.unlimit.vpngate.fragment.PrivacyPolicyFragment;
import vn.unlimit.vpngate.fragment.SettingFragment;
import vn.unlimit.vpngate.fragment.StatusFragment;
import vn.unlimit.vpngate.models.VPNGateConnectionList;
import vn.unlimit.vpngate.provider.BaseProvider;
import vn.unlimit.vpngate.utils.DataUtil;
import vn.unlimit.vpngate.utils.PaidServerUtil;
import vn.unlimit.vpngate.viewmodels.ConnectionListViewModel;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, NavigationView.OnNavigationItemSelectedListener {
    public static final String TARGET_FRAGMENT = "TARGET_FRAGMENT";
    private static final String TAG = "MainActivity";
    private static final String SORT_PROPERTY_KEY = "SORT_PROPERTY_KEY";
    private static final String SORT_TYPE_KEY = "SORT_TYPE_KEY";
    private final AtomicBoolean isMobileAdsInitializeCalled = new AtomicBoolean(false);
    public ConnectionListViewModel connectionListViewModel;
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
    private boolean isInFront = false;
    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (Objects.requireNonNull(intent.getAction())) {
                case BaseProvider.ACTION.ACTION_CHANGE_NETWORK_STATE -> {
                    if (isInFront) {
                        initState();
                    }
                }
                case BaseProvider.ACTION.ACTION_CLEAR_CACHE -> setVpnGateConnectionList(null);
                case BaseProvider.ACTION.ACTION_CONNECT_VPN -> {
                    if (dataUtil != null && dataUtil.getLastVPNConnection() != null) {
                        try {
                            navigationView.getMenu().findItem(R.id.nav_status).setVisible(true);
                        } catch (Exception e) {
                            Log.e(TAG, "Got exception when handle broadcast receive", e);
                        }
                    }
                }
                default -> {
                }
            }
        }
    };
    private ConsentInformation consentInformation;
    private PaidServerUtil paidServerUtil;

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

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dataUtil = ((App) getApplication()).getDataUtil();
        connectionListViewModel = new ViewModelProvider(this).get(ConnectionListViewModel.class);
        connectionListViewModel.isLoading().observe(this, aBoolean -> {
            if (aBoolean) {
                lnLoading.setVisibility(View.VISIBLE);
            } else {
                onSuccess();
            }
        });
        connectionListViewModel.isError().observe(this, isError -> {
            if (isError) {
                onError("");
            } else {
                lnError.setVisibility(View.GONE);
            }
        });
        if (savedInstanceState != null) {
            isLoading = false;
            currentUrl = savedInstanceState.getString("currentUrl");
            currentTitle = savedInstanceState.getString("currentTitle");
            setVpnGateConnectionList(dataUtil.getConnectionsCache());
            disallowLoadHome = connectionListViewModel.getVpnGateConnectionList().getValue() != null && connectionListViewModel.getVpnGateConnectionList().getValue().size() > 0;
        }
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
        paidServerUtil = App.getInstance().getPaidServerUtil();
        // Set startup screen to free server when open MainActivity
        paidServerUtil.setStartupScreen(PaidServerUtil.StartUpScreen.FREE_SERVER);
        IntentFilter filter = new IntentFilter();
        filter.addAction(BaseProvider.ACTION.ACTION_CHANGE_NETWORK_STATE);
        filter.addAction(BaseProvider.ACTION.ACTION_CLEAR_CACHE);
        filter.addAction(BaseProvider.ACTION.ACTION_CONNECT_VPN);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(broadcastReceiver, filter, RECEIVER_EXPORTED);
        } else {
            registerReceiver(broadcastReceiver, filter);
        }
        try {
            Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
        } catch (Exception ex) {
            Log.e(TAG, "Got exception handle support action bar", ex);
        }

        checkUMP();
        if (consentInformation != null) {
            if (BuildConfig.DEBUG && consentInformation.getPrivacyOptionsRequirementStatus() == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED) {
                consentInformation.reset();
            }
            if (consentInformation.canRequestAds()) {
                initAdMob();
            }
        } else {
            hideAdContainer();
        }
        addBackPressedHandler();
    }

    private void checkStatusMenu() {
        navigationView.getMenu().findItem(R.id.nav_status).setVisible(dataUtil.getLastVPNConnection() != null);
    }

    private void checkUMP() {
        if (!dataUtil.hasAds()) {
            return;
        }
        ConsentDebugSettings debugSettings = new ConsentDebugSettings.Builder(this)
                .addTestDeviceHashedId("5A08C90645CF1173979B5320A03D1195")
                .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
                .build();
        // Set tag for under age of consent. false means users are not under age
        // of consent.
        ConsentRequestParameters params = new ConsentRequestParameters
                .Builder()
                .setConsentDebugSettings(debugSettings)
                .setTagForUnderAgeOfConsent(false)
                .build();

        consentInformation = UserMessagingPlatform.getConsentInformation(this);
        consentInformation.requestConsentInfoUpdate(
                this,
                params,
                () -> UserMessagingPlatform.loadAndShowConsentFormIfRequired(
                        this,
                        loadAndShowError -> {
                            if (loadAndShowError != null) {
                                // Consent gathering failed.
                                Log.w(TAG, String.format("%s: %s",
                                        loadAndShowError.getErrorCode(),
                                        loadAndShowError.getMessage()));
                                if (loadAndShowError.getErrorCode() == FormError.ErrorCode.INVALID_OPERATION) {
                                    initAdMob();
                                }
                            }
                            if (consentInformation.canRequestAds()) {
                                initAdMob();
                            } else if (!isMobileAdsInitializeCalled.get()) {
                                hideAdContainer();
                            }
                        }
                ),
                requestConsentError -> {
                    // Consent gathering failed.
                    Log.w(TAG, String.format("%s: %s",
                            requestConsentError.getErrorCode(),
                            requestConsentError.getMessage()));
                });
    }

    private void initAdMob() {
        try {
            if (dataUtil.hasAds()) {
                if (isMobileAdsInitializeCalled.getAndSet(true)) {
                    return;
                }
                adView = new AdView(getApplicationContext());
                adView.setAdSize(AdSize.LARGE_BANNER);
                adView.setAdUnitId(getResources().getString(R.string.admob_banner_bottom_home));
                adView.setAdListener(new AdListener() {
                    @Override
                    public void onAdFailedToLoad(@NotNull LoadAdError error) {
                        adView.setVisibility(View.GONE);
                        hideAdContainer();
                        Log.e(TAG, error.toString());
                    }
                });
                ((RelativeLayout) findViewById(R.id.ad_container_home)).addView(adView);
                adView.loadAd(new AdRequest.Builder().build());
            } else {
                hideAdContainer();
                navigationView.getMenu().setGroupVisible(R.id.menu_top, false);
            }
        } catch (Exception e) {
            Log.e(TAG, "Got exception when initAdMob", e);
        }
    }

    private void hideAdContainer() {
        try {
            findViewById(R.id.ad_container_home).setVisibility(View.GONE);
        } catch (Exception e) {
            Log.e(TAG, "Got exception when hideAdContainer", e);
        }
    }

    /**
     * Check network and process first state
     */
    private void initState() {
        checkStatusMenu();
        if (!dataUtil.isAcceptedPrivacyPolicy()) {
            replaceFragment("privacy-policy");
            return;
        }
        if (dataUtil.getIntSetting(DataUtil.SETTING_STARTUP_SCREEN, 0) == 1 && dataUtil.getLastVPNConnection() != null) {
            replaceFragment("status");
            navigationView.getMenu().findItem(R.id.nav_status).setChecked(true);
            return;
        }
        String targetFragment = this.getIntent().getStringExtra(TARGET_FRAGMENT);
        if (targetFragment != null) {
            replaceFragment(targetFragment);
            return;
        }
        this.loadData();
    }

    private void loadData() {
        if (!disallowLoadHome) {
            if (DataUtil.isOnline(getApplicationContext())) {
                lnNoNetwork.setVisibility(View.GONE);
                VPNGateConnectionList vpnGateConnectionList = dataUtil.getConnectionsCache();
                setVpnGateConnectionList(vpnGateConnectionList);
                if (vpnGateConnectionList == null || vpnGateConnectionList.size() == 0) {
                    getDataServer();
                } else {
                    connectionListViewModel.isLoading().postValue(false);
                }
                if (FirebaseRemoteConfig.getInstance().getBoolean(getString(R.string.cfg_invite_paid_server)) && !dataUtil.getBooleanSetting(DataUtil.INVITED_USE_PAID_SERVER, false)) {
                    AlertDialog.Builder alertDialogBuilder = getAlertDialogBuilder();
                    alertDialogBuilder.setNegativeButton(android.R.string.no, ((dialogInterface, i) -> {
                        dataUtil.setBooleanSetting(DataUtil.INVITED_USE_PAID_SERVER, true);
                        dialogInterface.dismiss();
                    }));
                    alertDialogBuilder.setCancelable(false);
                    AlertDialog alertDialog = alertDialogBuilder.create();
                    alertDialog.setTitle(R.string.invite_paid_server_title);
                    alertDialog.setMessage(getString(R.string.invite_paid_server_message));
                    alertDialog.show();
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

    private AlertDialog.Builder getAlertDialogBuilder() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setPositiveButton(android.R.string.ok, (dialogInterface, i) -> {
            // Allow invite => Redirect to paid screen
            dataUtil.setBooleanSetting(DataUtil.INVITED_USE_PAID_SERVER, true);
            Intent intentPaidServer = new Intent(this, LoginActivity.class);
            startActivity(intentPaidServer);
            finish();
        });
        return alertDialogBuilder;
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
        isInFront = false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            if (currentUrl.equals("home") && (connectionListViewModel.getVpnGateConnectionList().getValue() == null || connectionListViewModel.getVpnGateConnectionList().getValue().size() == 0)) {
                initState();
            }
            isInFront = true;
        } catch (Exception e) {
            Log.e(TAG, "Got exception when handle onResume", e);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(broadcastReceiver);
    }

    private void getDataServer() {
        isLoading = true;
        lnError.setVisibility(View.GONE);
        frameContent.setVisibility(View.GONE);
        lnNoNetwork.setVisibility(View.GONE);
        connectionListViewModel.getAPIData();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        drawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
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
            public boolean onMenuItemActionExpand(@NonNull MenuItem item) {
                HomeFragment currentFragment = (HomeFragment) getSupportFragmentManager().findFragmentByTag(HomeFragment.class.getName());
                if (currentFragment != null) {
                    currentFragment.filter("");
                }
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(@NonNull MenuItem item) {
                HomeFragment currentFragment = (HomeFragment) getSupportFragmentManager().findFragmentByTag(HomeFragment.class.getName());
                if (currentFragment != null) {
                    currentFragment.closeSearch();
                }
                return true;
            }
        });
        try {
            final SearchView searchView = (SearchView) menuSearch.getActionView();
            if (searchManager != null) {
                assert searchView != null;
                searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
            }
            assert searchView != null;
            searchView.setMaxWidth(Integer.MAX_VALUE);
            final EditText editText = searchView.findViewById(androidx.appcompat.R.id.search_src_text);
            editText.setImeOptions(EditorInfo.IME_ACTION_DONE);
            editText.setTextColor(getResources().getColor(R.color.colorWhite));
            editText.setHintTextColor(getResources().getColor(R.color.colorWhiteTransparent));
            searchView.setQueryHint(getString(R.string.search_hint));
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    return false;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    View closeBtn = searchView.findViewById(androidx.appcompat.R.id.search_close_btn);
                    if (closeBtn != null) {
                        closeBtn.setVisibility(View.GONE);
                    }
                    HomeFragment currentFragment = (HomeFragment) getSupportFragmentManager().findFragmentByTag(HomeFragment.class.getName());
                    if (currentFragment != null) {
                        Bundle params = new Bundle();
                        params.putString(FirebaseAnalytics.Param.SEARCH_TERM, newText);
                        FirebaseAnalytics.getInstance(getApplicationContext()).logEvent(FirebaseAnalytics.Event.SEARCH, params);
                        currentFragment.filter(newText);
                        return true;
                    }
                    return false;
                }
            });
            if (connectionListViewModel.getVpnGateConnectionList().getValue() != null && connectionListViewModel.getVpnGateConnectionList().getValue().getFilter() != null) {
                menu.findItem(R.id.action_filter).setIcon(R.drawable.ic_filter_active_white);
            }
        } catch (Exception e) {
            Log.e(TAG, "Got exception when handle search view", e);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (drawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        if (isLoading) {
            Toast.makeText(this, getResources().getText(R.string.feature_not_available), Toast.LENGTH_LONG).show();
            return true;
        }
        int itemId = item.getItemId();
        if (itemId == R.id.action_sort) {
            SortBottomSheetDialog sortBottomSheetDialog = SortBottomSheetDialog.newInstance(mSortProperty, mSortType);
            sortBottomSheetDialog.setOnApplyClickListener((sortProperty, sortType) -> {
                if (dataUtil.hasAds()) {
                    Toast.makeText(getApplicationContext(), getText(R.string.feature_available_in_pro), Toast.LENGTH_LONG).show();
                    return;
                }
                mSortProperty = sortProperty;
                mSortType = sortType;
                dataUtil.setStringSetting(SORT_PROPERTY_KEY, mSortProperty);
                dataUtil.setIntSetting(SORT_TYPE_KEY, mSortType);
                HomeFragment currentFragment = (HomeFragment) getSupportFragmentManager().findFragmentByTag(HomeFragment.class.getName());
                if (currentFragment != null) {
                    Bundle params = new Bundle();
                    params.putString("property", sortProperty);
                    params.putString("type", sortType == VPNGateConnectionList.ORDER.ASC ? "ASC" : "DESC");
                    FirebaseAnalytics.getInstance(getApplicationContext()).logEvent("Sort", params);
                    currentFragment.sort(sortProperty, sortType);
                }
            });
            if (!isFinishing() && !isDestroyed()) {
                sortBottomSheetDialog.show(getSupportFragmentManager(), sortBottomSheetDialog.getTag());
            } else if (!isFinishing()) {
                sortBottomSheetDialog.show(getSupportFragmentManager(), sortBottomSheetDialog.getTag());
            }
            return true;
        }

        if (itemId == R.id.action_filter && connectionListViewModel.getVpnGateConnectionList().getValue() != null) {
            FilterBottomSheetDialog filterBottomSheetDialog = FilterBottomSheetDialog.newInstance(connectionListViewModel.getVpnGateConnectionList().getValue().getFilter());
            filterBottomSheetDialog.setOnButtonClickListener(filter -> {
                mMenu.findItem(R.id.action_filter).setIcon(filter == null ? R.drawable.ic_filter_white : R.drawable.ic_filter_active_white);
                HomeFragment homeFragment = (HomeFragment) getSupportFragmentManager().findFragmentByTag(HomeFragment.class.getName());
                if (homeFragment != null) {
                    Bundle params = new Bundle();
                    params.putString("filterObj", new Gson().toJson(filter));
                    FirebaseAnalytics.getInstance(getApplicationContext()).logEvent("Filter", params);
                    connectionListViewModel.getVpnGateConnectionList().getValue().setFilter(filter);
                    homeFragment.advanceFilter(filter);
                }
            });
            if (!isFinishing() && !isDestroyed()) {
                filterBottomSheetDialog.show(getSupportFragmentManager(), filterBottomSheetDialog.getTag());
            } else if (!isFinishing()) {
                filterBottomSheetDialog.show(getSupportFragmentManager(), filterBottomSheetDialog.getTag());
            }
        }

        return super.onOptionsItemSelected(item);
    }

    public void onSuccess() {
        VPNGateConnectionList vpnGateConnectionList = connectionListViewModel.getVpnGateConnectionList().getValue();
        isLoading = false;
        lnLoading.setVisibility(View.GONE);
        frameContent.setVisibility(View.VISIBLE);
        if (vpnGateConnectionList != null && !"".equals(mSortProperty)) {
            vpnGateConnectionList.sort(mSortProperty, mSortType);
        }
        updateData();
        dataUtil.setConnectionsCache(vpnGateConnectionList);
    }

    private void updateData() {
        isLoading = false;
        lnLoading.setVisibility(View.GONE);
        replaceFragment("home");
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
        if (!dataUtil.isAcceptedPrivacyPolicy()) {
            Toast.makeText(this, getText(R.string.must_accept_privacy_policy), Toast.LENGTH_LONG).show();
            return true;
        }
        selectedMenuItem = menuItem;
        disallowLoadHome = true;
        Bundle params = new Bundle();
        params.putString("title", Objects.requireNonNull(menuItem.getTitle()).toString());
        FirebaseAnalytics.getInstance(getApplicationContext()).logEvent("Drawer_Select", params);
        switch (menuItem.getItemId()) {
            case R.id.nav_get_pro -> {
                if (dataUtil.hasAds() && dataUtil.hasProInstalled()) {
                    Intent launchIntent = getPackageManager().getLaunchIntentForPackage("vn.unlimit.vpngatepro");
                    if (launchIntent != null) {
                        startActivity(launchIntent);//null pointer check in case package name was not found
                    }
                    finish();
                } else {
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=vn.unlimit.vpngatepro")));
                    } catch (ActivityNotFoundException ex) {
                        try {
                            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=vn.unlimit.vpngatepro")));
                        } catch (ActivityNotFoundException exception) {
                            // No activity to handle this action
                            Log.e(TAG, "Got exception when handle onNavigationItemSelected", exception);
                        }
                    }
                }
                return false;
            }
            case R.id.nav_home -> {
                if (connectionListViewModel.getVpnGateConnectionList().getValue() == null) {
                    getDataServer();
                }
                replaceFragment("home");
                disallowLoadHome = false;
            }
            case R.id.nav_paid_server -> {
                if (paidServerUtil.isLoggedIn()) {
                    Intent intentPaidServer = new Intent(this, PaidServerActivity.class);
                    startActivity(intentPaidServer);
                    finish();
                } else {
                    Intent intentLogin = new Intent(this, LoginActivity.class);
                    startActivity(intentLogin);
                }
            }
            case R.id.nav_status -> {
                if (dataUtil.getLastVPNConnection() == null) {
                    Toast.makeText(getApplicationContext(), getString(R.string.connect_one_warning), Toast.LENGTH_LONG).show();
                    return false;
                }
                replaceFragment("status");
            }
            case R.id.nav_setting -> {
                replaceFragment("setting");
                stopRequest();
            }
            case R.id.nav_about -> {
                replaceFragment("about");
                stopRequest();
            }
            case R.id.nav_help -> {
                replaceFragment("help");
                stopRequest();
            }
            default -> {
            }
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
            lnError.setVisibility(View.GONE);
            lnLoading.setVisibility(View.GONE);
            lnNoNetwork.setVisibility(View.GONE);
        } catch (Exception e) {
            Log.e(TAG, "Got exception when handle stopRequest", e);
        }
    }

    private void replaceFragment(String url) {
        try {
            if (url != null && (!url.equals(currentUrl) || url.equals("home"))) {
                toggleAction(url.equals("home") && connectionListViewModel.getVpnGateConnectionList().getValue() != null);
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
                    case "privacy-policy" -> {
                        fragment = new PrivacyPolicyFragment();
                        tag = PrivacyPolicyFragment.class.getName();
                        title = getString(R.string.privacy_policy_title);
                    }
                    case "home" -> {
                        fragment = new HomeFragment();
                        tag = HomeFragment.class.getName();
                    }
                    case "status" -> {
                        fragment = new StatusFragment();
                        tag = StatusFragment.class.getName();
                        title = getResources().getString(R.string.status);
                    }
                    case "setting" -> {
                        fragment = new SettingFragment();
                        tag = SettingFragment.class.getName();
                        title = getResources().getString(R.string.setting);
                    }
                    case "help" -> {
                        fragment = new HelpFragment();
                        tag = HelpFragment.class.getName();
                        title = getResources().getString(R.string.help);
                    }
                    case "about" -> {
                        fragment = new AboutFragment();
                        tag = AboutFragment.class.getName();
                        title = getResources().getString(R.string.about);
                        navigationView.setCheckedItem(R.id.nav_about);
                    }
                    default -> {
                    }
                }
                if (fragment != null && !fragment.isAdded()) {
                    frameContent.setVisibility(View.VISIBLE);
                    setTitleActionbar(title);
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.frame_content, fragment, tag)
                            //.addToBackStack("home")
                            .commitAllowingStateLoss();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Got exception when handle replaceFragment", e);
        }
    }

    public void startHome() {
        this.loadData();
        replaceFragment("home");
    }

    public void restartApp() {
        try {
            Intent i = getBaseContext().getPackageManager()
                    .getLaunchIntentForPackage(getBaseContext().getPackageName());
            assert i != null;
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(i);
            this.finishAffinity();
        } catch (Exception e) {
            Log.e(TAG, "Got exception when handle restartApp", e);
            this.startHome();
        }
    }

    private void toggleAction(boolean visible) {
        if (mMenu != null) {
            mMenu.findItem(R.id.action_search).setVisible(visible);
            mMenu.findItem(R.id.action_sort).setVisible(visible);
            mMenu.findItem(R.id.action_filter).setVisible(visible);
        }
    }

    void addBackPressedHandler() {
        getOnBackPressedDispatcher().addCallback(new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                String startUpUrl = dataUtil.getIntSetting(DataUtil.SETTING_STARTUP_SCREEN, 0) == 0 ? "home" : "status";
                if (startUpUrl.equals(currentUrl)) {
                    if (doubleBackToExitPressedOnce) {
                        MainActivity.this.finish();
                        return;
                    }
                    doubleBackToExitPressedOnce = true;
                    Toast.makeText(MainActivity.this, getResources().getString(R.string.press_back_again_to_exit), Toast.LENGTH_SHORT).show();
                    new Handler(getMainLooper()).postDelayed(() -> doubleBackToExitPressedOnce = false, 2000);
                } else {
                    if (connectionListViewModel.getVpnGateConnectionList().getValue() == null) {
                        getDataServer();
                    }
                    navigationView.setCheckedItem(R.id.nav_home);
                    replaceFragment(startUpUrl);
                }
                drawerLayout.closeDrawer(GravityCompat.START);
            }
        });
    }

    private void setTitleActionbar(String title) {
        currentTitle = title;
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(title);
        }
    }

    public void onError(String error) {
        Bundle params = new Bundle();
        params.putString("screen", "home");
        FirebaseAnalytics.getInstance(getApplicationContext()).logEvent("Error", params);
        frameContent.setVisibility(View.GONE);
        lnLoading.setVisibility(View.GONE);
        lnError.setVisibility(View.VISIBLE);
        lnNoNetwork.setVisibility(View.GONE);
        System.out.print(error);
    }

    public VPNGateConnectionList getVpnGateConnectionList() {
        return connectionListViewModel.getVpnGateConnectionList().getValue();
    }

    public void setVpnGateConnectionList(VPNGateConnectionList _vpnGateConnectionList) {
        connectionListViewModel.getVpnGateConnectionList().postValue(_vpnGateConnectionList);
        if (mMenu != null) {
            mMenu.findItem(R.id.action_filter).setIcon(_vpnGateConnectionList != null && _vpnGateConnectionList.getFilter() != null ? R.drawable.ic_filter_active_white : R.drawable.ic_filter_white);
        }
    }
}
