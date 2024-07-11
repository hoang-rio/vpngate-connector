package vn.unlimit.vpngate.activities

import android.annotation.SuppressLint
import android.app.SearchManager
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.material.navigation.NavigationView
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.FormError
import com.google.android.ump.UserMessagingPlatform
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.gson.Gson
import vn.unlimit.vpngate.App
import vn.unlimit.vpngate.App.Companion.instance
import vn.unlimit.vpngate.BuildConfig
import vn.unlimit.vpngate.R
import vn.unlimit.vpngate.activities.paid.LoginActivity
import vn.unlimit.vpngate.activities.paid.PaidServerActivity
import vn.unlimit.vpngate.dialog.FilterBottomSheetDialog.Companion.newInstance
import vn.unlimit.vpngate.dialog.FilterBottomSheetDialog.OnButtonClickListener
import vn.unlimit.vpngate.dialog.SortBottomSheetDialog
import vn.unlimit.vpngate.fragment.AboutFragment
import vn.unlimit.vpngate.fragment.HelpFragment
import vn.unlimit.vpngate.fragment.HomeFragment
import vn.unlimit.vpngate.fragment.PrivacyPolicyFragment
import vn.unlimit.vpngate.fragment.SettingFragment
import vn.unlimit.vpngate.fragment.StatusFragment
import vn.unlimit.vpngate.models.VPNGateConnectionList
import vn.unlimit.vpngate.provider.BaseProvider
import vn.unlimit.vpngate.utils.DataUtil
import vn.unlimit.vpngate.utils.DataUtil.Companion.isOnline
import vn.unlimit.vpngate.utils.PaidServerUtil
import vn.unlimit.vpngate.viewmodels.ConnectionListViewModel
import java.util.Objects
import java.util.concurrent.atomic.AtomicBoolean

class MainActivity : AppCompatActivity(), View.OnClickListener,
    NavigationView.OnNavigationItemSelectedListener {
    private val isMobileAdsInitializeCalled = AtomicBoolean(false)
    var connectionListViewModel: ConnectionListViewModel? = null
    private var lnLoading: View? = null
    private var frameContent: View? = null
    var isLoading: Boolean = true
    var doubleBackToExitPressedOnce: Boolean = false
    private var selectedMenuItem: MenuItem? = null
    private var dataUtil: DataUtil? = null
    private var toolbar: Toolbar? = null
    private var drawerLayout: DrawerLayout? = null
    private var navigationView: NavigationView? = null
    private var drawerToggle: ActionBarDrawerToggle? = null
    private var lnError: View? = null
    private var lnNoNetwork: View? = null
    private var currentUrl: String? = ""
    var sortProperty: String? = ""
        private set
    private var currentTitle: String? = ""
    private var mMenu: Menu? = null
    var sortType: Int = VPNGateConnectionList.ORDER.ASC
        private set
    private var disallowLoadHome = false
    private var adView: AdView? = null
    private var isInFront = false
    private val broadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (Objects.requireNonNull<String?>(intent.action)) {
                BaseProvider.ACTION.ACTION_CHANGE_NETWORK_STATE -> {
                    if (isInFront) {
                        initState()
                    }
                }

                BaseProvider.ACTION.ACTION_CLEAR_CACHE -> connectionListViewModel?.vpnGateConnectionList?.postValue(null)
                BaseProvider.ACTION.ACTION_CONNECT_VPN -> {
                    if (dataUtil != null && dataUtil!!.lastVPNConnection != null) {
                        try {
                            navigationView!!.menu.findItem(R.id.nav_status).setVisible(true)
                        } catch (e: Exception) {
                            Log.e(TAG, "Got exception when handle broadcast receive", e)
                        }
                    }
                }

                else -> {}
            }
        }
    }
    private var consentInformation: ConsentInformation? = null
    private var paidServerUtil: PaidServerUtil? = null

    public override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("currentUrl", currentUrl)
        outState.putString("currentTitle", currentTitle)
        super.onSaveInstanceState(outState)
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dataUtil = (application as App).dataUtil
        connectionListViewModel = ViewModelProvider(this)[ConnectionListViewModel::class.java]
        connectionListViewModel!!.isLoading.observe(this) { aBoolean: Boolean ->
            if (aBoolean) {
                lnLoading!!.visibility = View.VISIBLE
            } else {
                onSuccess()
            }
        }
        connectionListViewModel!!.isError.observe(this) { isError: Boolean ->
            if (isError) {
                onError("")
            } else {
                lnError!!.visibility = View.GONE
            }
        }
        if (savedInstanceState != null) {
            isLoading = false
            currentUrl = savedInstanceState.getString("currentUrl")
            currentTitle = savedInstanceState.getString("currentTitle")
            vpnGateConnectionList = dataUtil!!.connectionsCache
            disallowLoadHome =
                connectionListViewModel!!.vpnGateConnectionList.value != null && connectionListViewModel!!.vpnGateConnectionList.value!!
                    .size() > 0
        }
        setContentView(R.layout.activity_main)
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        lnLoading = findViewById(R.id.ln_loading)
        lnError = findViewById(R.id.ln_error)
        lnNoNetwork = findViewById(R.id.ln_no_network)
        lnError!!.setOnClickListener(this)
        frameContent = findViewById(R.id.frame_content)
        drawerLayout = findViewById(R.id.activity_main_drawer)
        drawerToggle = ActionBarDrawerToggle(
            this,
            drawerLayout,
            toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawerLayout!!.addDrawerListener(drawerToggle!!)
        navigationView = findViewById(R.id.nav_main)
        navigationView!!.setNavigationItemSelectedListener(this)
        sortProperty = dataUtil!!.getStringSetting(SORT_PROPERTY_KEY, "")
        sortType = dataUtil!!.getIntSetting(SORT_TYPE_KEY, VPNGateConnectionList.ORDER.ASC)
        paidServerUtil = instance!!.paidServerUtil!!
        // Set startup screen to free server when open MainActivity
        checkNotNull(paidServerUtil)
        paidServerUtil!!.setStartupScreen(PaidServerUtil.StartUpScreen.FREE_SERVER)
        val filter = IntentFilter()
        filter.addAction(BaseProvider.ACTION.ACTION_CHANGE_NETWORK_STATE)
        filter.addAction(BaseProvider.ACTION.ACTION_CLEAR_CACHE)
        filter.addAction(BaseProvider.ACTION.ACTION_CONNECT_VPN)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(broadcastReceiver, filter, RECEIVER_EXPORTED)
        } else {
            registerReceiver(broadcastReceiver, filter)
        }
        try {
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            supportActionBar!!.setHomeButtonEnabled(true)
        } catch (ex: Exception) {
            Log.e(TAG, "Got exception handle support action bar", ex)
        }

        checkUMP()
        if (consentInformation != null) {
            if (BuildConfig.DEBUG && consentInformation!!.privacyOptionsRequirementStatus == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED) {
                consentInformation!!.reset()
            }
            if (consentInformation!!.canRequestAds()) {
                initAdMob()
            }
        } else {
            hideAdContainer()
        }
        addBackPressedHandler()
    }

    private fun checkStatusMenu() {
        navigationView!!.menu.findItem(R.id.nav_status)
            .setVisible(dataUtil!!.lastVPNConnection != null)
    }

    private fun checkUMP() {
        if (!dataUtil!!.hasAds()) {
            return
        }
//        val debugSettings = ConsentDebugSettings.Builder(this)
//            .addTestDeviceHashedId("5A08C90645CF1173979B5320A03D1195")
//            .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
//            .build()
        // Set tag for under age of consent. false means users are not under age
        // of consent.
        val params = ConsentRequestParameters.Builder()
//            .setConsentDebugSettings(debugSettings)
            .setTagForUnderAgeOfConsent(false)
            .build()

        consentInformation = UserMessagingPlatform.getConsentInformation(this)
        consentInformation!!.requestConsentInfoUpdate(
            this,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(
                    this
                ) { loadAndShowError: FormError? ->
                    if (loadAndShowError != null) {
                        // Consent gathering failed.
                        Log.w(
                            TAG, String.format(
                                "%s: %s",
                                loadAndShowError.errorCode,
                                loadAndShowError.message
                            )
                        )
                        if (loadAndShowError.errorCode == FormError.ErrorCode.INVALID_OPERATION) {
                            initAdMob()
                        }
                    }
                    if (consentInformation!!.canRequestAds()) {
                        initAdMob()
                    } else if (!isMobileAdsInitializeCalled.get()) {
                        hideAdContainer()
                    }
                }
            },
            { requestConsentError: FormError ->
                // Consent gathering failed.
                Log.w(
                    TAG, String.format(
                        "%s: %s",
                        requestConsentError.errorCode,
                        requestConsentError.message
                    )
                )
            })
    }

    private fun initAdMob() {
        try {
            if (dataUtil!!.hasAds()) {
                if (isMobileAdsInitializeCalled.getAndSet(true)) {
                    return
                }
                adView = AdView(applicationContext)
                adView!!.setAdSize(AdSize.LARGE_BANNER)
                adView!!.adUnitId = resources.getString(R.string.admob_banner_bottom_home)
                adView!!.adListener = object : AdListener() {
                    override fun onAdFailedToLoad(error: LoadAdError) {
                        adView!!.visibility = View.GONE
                        hideAdContainer()
                        Log.e(TAG, error.toString())
                    }
                }
                (findViewById<View>(R.id.ad_container_home) as RelativeLayout).addView(adView)
                adView!!.loadAd(AdRequest.Builder().build())
            } else {
                hideAdContainer()
                navigationView!!.menu.setGroupVisible(R.id.menu_top, false)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Got exception when initAdMob", e)
        }
    }

    private fun hideAdContainer() {
        try {
            findViewById<View>(R.id.ad_container_home).visibility = View.GONE
        } catch (e: Exception) {
            Log.e(TAG, "Got exception when hideAdContainer", e)
        }
    }

    /**
     * Check network and process first state
     */
    private fun initState() {
        checkStatusMenu()
        if (!dataUtil!!.isAcceptedPrivacyPolicy) {
            replaceFragment("privacy-policy")
            return
        }
        if (dataUtil!!.getIntSetting(
                DataUtil.SETTING_STARTUP_SCREEN,
                0
            ) == 1 && dataUtil!!.lastVPNConnection != null
        ) {
            replaceFragment("status")
            navigationView!!.menu.findItem(R.id.nav_status).setChecked(true)
            return
        }
        val targetFragment = this.intent.getStringExtra(TARGET_FRAGMENT)
        if (targetFragment != null) {
            replaceFragment(targetFragment)
            return
        }
        this.loadData()
    }

    private fun loadData() {
        if (!disallowLoadHome) {
            if (isOnline(applicationContext)) {
                lnNoNetwork!!.visibility = View.GONE
                val vpnGateConnectionList = dataUtil!!.connectionsCache
                this.vpnGateConnectionList = vpnGateConnectionList
                if (vpnGateConnectionList == null || vpnGateConnectionList.size() == 0) {
                    callDataServer()
                } else {
                    connectionListViewModel!!.isLoading.postValue(false)
                }
                if (FirebaseRemoteConfig.getInstance()
                        .getBoolean(getString(R.string.cfg_invite_paid_server)) && !dataUtil!!.getBooleanSetting(
                        DataUtil.INVITED_USE_PAID_SERVER,
                        false
                    )
                ) {
                    val alertDialogBuilder = alertDialogBuilder
                    alertDialogBuilder.setNegativeButton(
                        android.R.string.cancel,
                        (DialogInterface.OnClickListener { dialogInterface: DialogInterface, _: Int ->
                            dataUtil!!.setBooleanSetting(DataUtil.INVITED_USE_PAID_SERVER, true)
                            dialogInterface.dismiss()
                        })
                    )
                    alertDialogBuilder.setCancelable(false)
                    val alertDialog = alertDialogBuilder.create()
                    alertDialog.setTitle(R.string.invite_paid_server_title)
                    alertDialog.setMessage(getString(R.string.invite_paid_server_message))
                    alertDialog.show()
                }
            } else {
                lnNoNetwork!!.visibility = View.VISIBLE
                lnError!!.visibility = View.GONE
                lnLoading!!.visibility = View.GONE
                frameContent!!.visibility = View.GONE
            }
        } else {
            setTitleActionbar(currentTitle)
            lnError!!.visibility = View.GONE
            lnLoading!!.visibility = View.GONE
        }
    }

    private val alertDialogBuilder: AlertDialog.Builder
        get() {
            val alertDialogBuilder = AlertDialog.Builder(this)
            alertDialogBuilder.setPositiveButton(android.R.string.ok) { _: DialogInterface?, _: Int ->
                // Allow invite => Redirect to paid screen
                dataUtil!!.setBooleanSetting(DataUtil.INVITED_USE_PAID_SERVER, true)
                val intentPaidServer = Intent(this, LoginActivity::class.java)
                startActivity(intentPaidServer)
                finish()
            }
            return alertDialogBuilder
        }

    override fun onClick(view: View) {
        if (view == lnError) {
            lnError!!.visibility = View.GONE
            callDataServer()
        }
    }

    override fun onPause() {
        super.onPause()
        isInFront = false
    }

    override fun onResume() {
        super.onResume()
        try {
            if (currentUrl == "home" && (connectionListViewModel!!.vpnGateConnectionList.value == null || connectionListViewModel!!.vpnGateConnectionList.value!!
                    .size() == 0)
            ) {
                initState()
            }
            isInFront = true
        } catch (e: Exception) {
            Log.e(TAG, "Got exception when handle onResume", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(broadcastReceiver)
    }

    fun callDataServer() {
        isLoading = true
        lnError!!.visibility = View.GONE
        frameContent!!.visibility = View.GONE
        lnNoNetwork!!.visibility = View.GONE
        connectionListViewModel!!.getAPIData()
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        // Sync the toggle state after onRestoreInstanceState has occurred.
        drawerToggle!!.syncState()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        drawerToggle!!.onConfigurationChanged(newConfig)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        mMenu = menu
        toggleAction(currentUrl == "home")
        val searchManager = getSystemService(SEARCH_SERVICE) as SearchManager
        val menuSearch = menu.findItem(R.id.action_search)
        menuSearch.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem): Boolean {
                val currentFragment = supportFragmentManager.findFragmentByTag(
                    HomeFragment::class.java.name
                ) as HomeFragment?
                currentFragment?.filter("")
                return true
            }

            override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                val currentFragment = supportFragmentManager.findFragmentByTag(
                    HomeFragment::class.java.name
                ) as HomeFragment?
                currentFragment?.closeSearch()
                return true
            }
        })
        try {
            val searchView = menuSearch.actionView as SearchView? ?: return super.onCreateOptionsMenu(menu)
            searchView.setSearchableInfo(searchManager.getSearchableInfo(componentName))
            searchView.maxWidth = Int.MAX_VALUE
            val editText =
                searchView.findViewById<EditText>(androidx.appcompat.R.id.search_src_text)
            editText.imeOptions = EditorInfo.IME_ACTION_DONE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                editText.setTextColor(resources.getColor(R.color.colorWhite, theme))
                editText.setHintTextColor(resources.getColor(R.color.colorWhiteTransparent, theme))
            } else {
                @Suppress("DEPRECATION")
                editText.setTextColor(resources.getColor(R.color.colorWhite))
                @Suppress("DEPRECATION")
                editText.setHintTextColor(resources.getColor(R.color.colorWhiteTransparent))
            }
            searchView.queryHint = getString(R.string.search_hint)
            searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String): Boolean {
                    return false
                }

                override fun onQueryTextChange(newText: String): Boolean {
                    val closeBtn =
                        searchView.findViewById<View>(androidx.appcompat.R.id.search_close_btn)
                    if (closeBtn != null) {
                        closeBtn.visibility = View.GONE
                    }
                    val currentFragment = supportFragmentManager.findFragmentByTag(
                        HomeFragment::class.java.name
                    ) as HomeFragment?
                    if (currentFragment != null) {
                        val params = Bundle()
                        params.putString(FirebaseAnalytics.Param.SEARCH_TERM, newText)
                        FirebaseAnalytics.getInstance(applicationContext)
                            .logEvent(FirebaseAnalytics.Event.SEARCH, params)
                        currentFragment.filter(newText)
                        return true
                    }
                    return false
                }
            })
            if (connectionListViewModel!!.vpnGateConnectionList.value != null && connectionListViewModel!!.vpnGateConnectionList.value!!.filter != null) {
                menu.findItem(R.id.action_filter).setIcon(R.drawable.ic_filter_active_white)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Got exception when handle search view", e)
        }
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (drawerToggle!!.onOptionsItemSelected(item)) {
            return true
        }
        if (isLoading) {
            Toast.makeText(
                this,
                resources.getText(R.string.feature_not_available),
                Toast.LENGTH_LONG
            ).show()
            return true
        }
        val itemId = item.itemId
        if (itemId == R.id.action_sort) {
            val sortBottomSheetDialog = SortBottomSheetDialog.newInstance(
                sortProperty, sortType
            )
            sortBottomSheetDialog.setOnApplyClickListener(object : SortBottomSheetDialog.OnApplyClickListener {
                override fun onApplyClick(sortProperty: String?, sortType: Int) {
                    if (dataUtil!!.hasAds()) {
                        Toast.makeText(
                            applicationContext,
                            getText(R.string.feature_available_in_pro),
                            Toast.LENGTH_LONG
                        ).show()
                        return
                    }
                    this@MainActivity.sortProperty = sortProperty
                    this@MainActivity.sortType = sortType
                    dataUtil!!.setStringSetting(SORT_PROPERTY_KEY, this@MainActivity.sortProperty)
                    dataUtil!!.setIntSetting(SORT_TYPE_KEY, this@MainActivity.sortType)
                    val currentFragment = supportFragmentManager.findFragmentByTag(
                        HomeFragment::class.java.name
                    ) as HomeFragment?
                    if (currentFragment != null) {
                        val params = Bundle()
                        params.putString("property", this@MainActivity.sortProperty)
                        params.putString(
                            "type",
                            if (this@MainActivity.sortType == VPNGateConnectionList.ORDER.ASC) "ASC" else "DESC"
                        )
                        FirebaseAnalytics.getInstance(applicationContext).logEvent("Sort", params)
                        currentFragment.sort(this@MainActivity.sortProperty,
                            this@MainActivity.sortType
                        )
                    }
                }

            })
            if (!isFinishing && !isDestroyed) {
                sortBottomSheetDialog.show(supportFragmentManager, sortBottomSheetDialog.tag)
            } else if (!isFinishing) {
                sortBottomSheetDialog.show(supportFragmentManager, sortBottomSheetDialog.tag)
            }
            return true
        }

        if (itemId == R.id.action_filter && connectionListViewModel!!.vpnGateConnectionList.value != null) {
            val filterBottomSheetDialog = newInstance(
                connectionListViewModel!!.vpnGateConnectionList.value!!.filter
            )
            filterBottomSheetDialog.onButtonClickListener =
                object: OnButtonClickListener {
                    override fun onButtonClick(filter: VPNGateConnectionList.Filter?) {
                        mMenu!!.findItem(R.id.action_filter)
                            .setIcon(if (filter == null) R.drawable.ic_filter_white else R.drawable.ic_filter_active_white)
                        val homeFragment = supportFragmentManager.findFragmentByTag(
                            HomeFragment::class.java.name
                        ) as HomeFragment?
                        if (homeFragment != null && connectionListViewModel!!.vpnGateConnectionList.value != null) {
                            val params = Bundle()
                            params.putString("filterObj", Gson().toJson(filter))
                            FirebaseAnalytics.getInstance(applicationContext).logEvent("Filter", params)
                            connectionListViewModel!!.vpnGateConnectionList.value!!.filter = filter
                            homeFragment.advanceFilter(filter)
                        }
                    }
                }
            if (!isFinishing && !isDestroyed) {
                filterBottomSheetDialog.show(supportFragmentManager, filterBottomSheetDialog.tag)
            } else if (!isFinishing) {
                filterBottomSheetDialog.show(supportFragmentManager, filterBottomSheetDialog.tag)
            }
        }

        return super.onOptionsItemSelected(item)
    }

    fun onSuccess() {
        val vpnGateConnectionList = connectionListViewModel!!.vpnGateConnectionList.value
        isLoading = false
        lnLoading!!.visibility = View.GONE
        frameContent!!.visibility = View.VISIBLE
        if (vpnGateConnectionList != null && "" != sortProperty) {
            vpnGateConnectionList.sort(sortProperty, sortType)
        }
        updateData()
        dataUtil!!.connectionsCache = vpnGateConnectionList
    }

    private fun updateData() {
        isLoading = false
        lnLoading!!.visibility = View.GONE
        replaceFragment("home")
    }

    @SuppressLint("NonConstantResourceId")
    override fun onNavigationItemSelected(menuItem: MenuItem): Boolean {
        if (!dataUtil!!.isAcceptedPrivacyPolicy) {
            Toast.makeText(this, getText(R.string.must_accept_privacy_policy), Toast.LENGTH_LONG)
                .show()
            return true
        }
        selectedMenuItem = menuItem
        disallowLoadHome = true
        val params = Bundle()
        params.putString("title", Objects.requireNonNull(menuItem.title).toString())
        FirebaseAnalytics.getInstance(applicationContext).logEvent("Drawer_Select", params)
        when (menuItem.itemId) {
            R.id.nav_get_pro -> {
                if (dataUtil!!.hasAds() && dataUtil!!.hasProInstalled()) {
                    val launchIntent =
                        packageManager.getLaunchIntentForPackage("vn.unlimit.vpngatepro")
                    if (launchIntent != null) {
                        startActivity(launchIntent) //null pointer check in case package name was not found
                    }
                    finish()
                } else {
                    try {
                        startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("market://details?id=vn.unlimit.vpngatepro")
                            )
                        )
                    } catch (ex: ActivityNotFoundException) {
                        try {
                            startActivity(
                                Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://play.google.com/store/apps/details?id=vn.unlimit.vpngatepro")
                                )
                            )
                        } catch (exception: ActivityNotFoundException) {
                            // No activity to handle this action
                            Log.e(
                                TAG,
                                "Got exception when handle onNavigationItemSelected",
                                exception
                            )
                        }
                    }
                }
                return false
            }

            R.id.nav_home -> {
                if (connectionListViewModel!!.vpnGateConnectionList.value == null) {
                    callDataServer()
                }
                replaceFragment("home")
                disallowLoadHome = false
            }

            R.id.nav_paid_server -> {
                if (paidServerUtil!!.isLoggedIn()) {
                    val intentPaidServer = Intent(this, PaidServerActivity::class.java)
                    startActivity(intentPaidServer)
                    finish()
                } else {
                    val intentLogin = Intent(this, LoginActivity::class.java)
                    startActivity(intentLogin)
                }
            }

            R.id.nav_status -> {
                if (dataUtil!!.lastVPNConnection == null) {
                    Toast.makeText(
                        applicationContext,
                        getString(R.string.connect_one_warning),
                        Toast.LENGTH_LONG
                    ).show()
                    return false
                }
                replaceFragment("status")
            }

            R.id.nav_setting -> {
                replaceFragment("setting")
                stopRequest()
            }

            R.id.nav_about -> {
                replaceFragment("about")
                stopRequest()
            }

            R.id.nav_help -> {
                replaceFragment("help")
                stopRequest()
            }

            else -> {}
        }
        navigationView!!.setCheckedItem(menuItem.itemId)
        if (toolbar != null) {
            toolbar!!.collapseActionView()
        }
        drawerLayout!!.closeDrawers()
        return true
    }

    private fun stopRequest() {
        try {
            lnError!!.visibility = View.GONE
            lnLoading!!.visibility = View.GONE
            lnNoNetwork!!.visibility = View.GONE
        } catch (e: Exception) {
            Log.e(TAG, "Got exception when handle stopRequest", e)
        }
    }

    private fun replaceFragment(url: String?) {
        try {
            if (url != null && (url != currentUrl || url == "home")) {
                toggleAction(url == "home" && connectionListViewModel!!.vpnGateConnectionList.value != null)
                if (url != "home") {
                    lnLoading!!.visibility = View.GONE
                    lnNoNetwork!!.visibility = View.GONE
                    lnError!!.visibility = View.GONE
                }
                currentUrl = url
                var fragment: Fragment? = null
                var tag: String? = ""
                var title = resources.getString(R.string.app_name)
                when (url) {
                    "privacy-policy" -> {
                        fragment = PrivacyPolicyFragment()
                        tag = PrivacyPolicyFragment::class.java.name
                        title = getString(R.string.privacy_policy_title)
                    }

                    "home" -> {
                        fragment = HomeFragment()
                        tag = HomeFragment::class.java.name
                    }

                    "status" -> {
                        fragment = StatusFragment()
                        tag = StatusFragment::class.java.name
                        title = resources.getString(R.string.status)
                    }

                    "setting" -> {
                        fragment = SettingFragment()
                        tag = SettingFragment::class.java.name
                        title = resources.getString(R.string.setting)
                    }

                    "help" -> {
                        fragment = HelpFragment()
                        tag = HelpFragment::class.java.name
                        title = resources.getString(R.string.help)
                    }

                    "about" -> {
                        fragment = AboutFragment()
                        tag = AboutFragment::class.java.name
                        title = resources.getString(R.string.about)
                        navigationView!!.setCheckedItem(R.id.nav_about)
                    }

                    else -> {}
                }
                if (fragment != null && !fragment.isAdded) {
                    frameContent!!.visibility = View.VISIBLE
                    setTitleActionbar(title)
                    val transaction = supportFragmentManager.beginTransaction()
                    transaction.replace(R.id.frame_content, fragment, tag)
                    transaction.commitAllowingStateLoss()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Got exception when handle replaceFragment", e)
        }
    }

    private fun startHome() {
        this.loadData()
        replaceFragment("home")
    }

    fun restartApp() {
        try {
            val i = checkNotNull(
                baseContext.packageManager
                    .getLaunchIntentForPackage(baseContext.packageName)
            )
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(i)
            this.finishAffinity()
        } catch (e: Exception) {
            Log.e(TAG, "Got exception when handle restartApp", e)
            this.startHome()
        }
    }

    private fun toggleAction(visible: Boolean) {
        if (mMenu != null) {
            mMenu!!.findItem(R.id.action_search).setVisible(visible)
            mMenu!!.findItem(R.id.action_sort).setVisible(visible)
            mMenu!!.findItem(R.id.action_filter).setVisible(visible)
        }
    }

    private fun addBackPressedHandler() {
        onBackPressedDispatcher.addCallback(object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val startUpUrl = if (dataUtil!!.getIntSetting(
                        DataUtil.SETTING_STARTUP_SCREEN,
                        0
                    ) == 0
                ) "home" else "status"
                if (startUpUrl == currentUrl) {
                    if (doubleBackToExitPressedOnce) {
                        this@MainActivity.finish()
                        return
                    }
                    doubleBackToExitPressedOnce = true
                    Toast.makeText(
                        this@MainActivity,
                        resources.getString(R.string.press_back_again_to_exit),
                        Toast.LENGTH_SHORT
                    ).show()
                    Handler(mainLooper).postDelayed({ doubleBackToExitPressedOnce = false }, 2000)
                } else {
                    if (connectionListViewModel!!.vpnGateConnectionList.value == null) {
                        callDataServer()
                    }
                    navigationView!!.setCheckedItem(R.id.nav_home)
                    replaceFragment(startUpUrl)
                }
                drawerLayout!!.closeDrawer(GravityCompat.START)
            }
        })
    }

    private fun setTitleActionbar(title: String?) {
        currentTitle = title
        if (supportActionBar != null) {
            supportActionBar!!.title = title
        }
    }

    fun onError(error: String?) {
        val params = Bundle()
        params.putString("screen", "home")
        FirebaseAnalytics.getInstance(applicationContext).logEvent("Error", params)
        frameContent!!.visibility = View.GONE
        lnLoading!!.visibility = View.GONE
        lnError!!.visibility = View.VISIBLE
        lnNoNetwork!!.visibility = View.GONE
        print(error)
    }

    var vpnGateConnectionList: VPNGateConnectionList?
        get() = connectionListViewModel!!.vpnGateConnectionList.value
        set(inVpnGateConnectionList) {
            connectionListViewModel!!.vpnGateConnectionList.postValue(inVpnGateConnectionList)
            if (mMenu != null) {
                mMenu!!.findItem(R.id.action_filter)
                    .setIcon(if (inVpnGateConnectionList?.filter != null) R.drawable.ic_filter_active_white else R.drawable.ic_filter_white)
            }
        }

    companion object {
        const val TARGET_FRAGMENT: String = "TARGET_FRAGMENT"
        private const val TAG = "MainActivity"
        private const val SORT_PROPERTY_KEY = "SORT_PROPERTY_KEY"
        private const val SORT_TYPE_KEY = "SORT_TYPE_KEY"
    }
}
