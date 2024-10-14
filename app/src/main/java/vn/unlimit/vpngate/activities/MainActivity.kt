package vn.unlimit.vpngate.activities

import android.annotation.SuppressLint
import android.app.SearchManager
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
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
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
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
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import vn.unlimit.vpngate.App
import vn.unlimit.vpngate.App.Companion.instance
import vn.unlimit.vpngate.BuildConfig
import vn.unlimit.vpngate.R
import vn.unlimit.vpngate.activities.paid.LoginActivity
import vn.unlimit.vpngate.activities.paid.PaidServerActivity
import vn.unlimit.vpngate.databinding.ActivityMainBinding
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
    var isLoading: Boolean = false
    var doubleBackToExitPressedOnce: Boolean = false
    private var selectedMenuItem: MenuItem? = null
    private var dataUtil: DataUtil? = null
    private var drawerToggle: ActionBarDrawerToggle? = null
    private var currentUrl: String? = ""
    private var currentFragmentTag: String? = null
    var sortProperty: String? = ""
        private set
    private var currentTitle: String? = ""
    private var mMenu: Menu? = null
    var sortType: Int = VPNGateConnectionList.ORDER.ASC
        private set
    private var disallowLoadHome = false
    private var adView: AdView? = null
    private var isInFront = false
    private lateinit var binding: ActivityMainBinding
    private val broadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (Objects.requireNonNull<String?>(intent.action)) {
                BaseProvider.ACTION.ACTION_CHANGE_NETWORK_STATE -> {
                    if (isInFront) {
                        initState()
                    }
                }

                BaseProvider.ACTION.ACTION_CLEAR_CACHE -> connectionListViewModel?.vpnGateConnectionList?.postValue(
                    null
                )

                BaseProvider.ACTION.ACTION_CONNECT_VPN -> {
                    if (dataUtil != null && dataUtil!!.lastVPNConnection != null) {
                        try {
                            binding.navMain.menu.findItem(R.id.nav_status).setVisible(true)
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
                binding.incLoading.lnLoading.visibility = View.VISIBLE
            } else if (intent.getStringExtra(TARGET_FRAGMENT) == null) {
                postVPNGateAPI()
            }
        }
        connectionListViewModel!!.isError.observe(this) { isError: Boolean ->
            if (isError) {
                onError("")
            } else {
                binding.incError.lnError.visibility = View.GONE
            }
        }
        if (savedInstanceState != null) {
            isLoading = false
            currentUrl = savedInstanceState.getString("currentUrl")
            currentTitle = savedInstanceState.getString("currentTitle")
        }
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        binding.incError.lnError.setOnClickListener(this)
        drawerToggle = ActionBarDrawerToggle(
            this,
            binding.activityMainDrawer,
            binding.toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        binding.activityMainDrawer.addDrawerListener(drawerToggle!!)
        binding.navMain.setNavigationItemSelectedListener(this)
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
        if (!dataUtil!!.hasAds()) {
            hideAdContainer()
            binding.navMain.menu.setGroupVisible(R.id.menu_top, false)
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
        lifecycleScope.launch(Dispatchers.IO) {
            disallowLoadHome =
                vpnGateConnectionList != null && vpnGateConnectionList!!
                    .size() > 0
            withContext(Dispatchers.Main) {
                initState()
            }
        }
    }

    private fun checkStatusMenu() {
        binding.navMain.menu.findItem(R.id.nav_status)
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
            }
        } catch (e: Exception) {
            Log.e(TAG, "Got exception when initAdMob", e)
        }
    }

    private fun hideAdContainer() {
        try {
            binding.adContainerHome.visibility = View.GONE
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
            binding.navMain.menu.findItem(R.id.nav_status).setChecked(true)
            return
        }
        val targetFragment = intent.getStringExtra(TARGET_FRAGMENT)
        if (targetFragment != null) {
            intent.removeExtra(TARGET_FRAGMENT)
            replaceFragment(targetFragment)
            return
        }
        this.loadData()
    }

    private fun loadData() {
        lifecycleScope.launch(Dispatchers.IO) {
            if (!disallowLoadHome) {
                if (isOnline(applicationContext)) {
                    runOnUiThread {
                        binding.incNoNetwork.lnNoNetwork.visibility = View.GONE
                    }
                    if (dataUtil?.connectionsCache == null) {
                        withContext(Dispatchers.Main) {
                            if (supportFragmentManager.isStateSaved.not()) {
                                callDataServer()
                            }
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            displayHome()
                        }
                    }
                } else {
                    runOnUiThread {
                        binding.incNoNetwork.lnNoNetwork.visibility = View.VISIBLE
                        binding.incError.lnError.visibility = View.GONE
                        binding.incLoading.lnLoading.visibility = View.GONE
                        binding.frameContent.visibility = View.GONE
                    }
                }
            } else {
                runOnUiThread {
                    setTitleActionbar(currentTitle)
                    binding.incError.lnError.visibility = View.GONE
                    binding.incLoading.lnLoading.visibility = View.GONE
                }
            }
        }
    }

    override fun onClick(view: View) {
        if (view == binding.incError.lnError) {
            binding.incError.lnError.visibility = View.GONE
            callDataServer()
        }
    }

    override fun onPause() {
        super.onPause()
        isInFront = false
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(broadcastReceiver)
    }

    fun callDataServer() {
        isLoading = true
        binding.incError.lnError.visibility = View.GONE
        binding.frameContent.visibility = View.GONE
        supportFragmentManager.findFragmentByTag(HomeFragment::class.java.name)?.let {
            supportFragmentManager.commit { hide(it) }
        }
        binding.incNoNetwork.lnNoNetwork.visibility = View.GONE
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
            val searchView =
                menuSearch.actionView as SearchView? ?: return super.onCreateOptionsMenu(menu)
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
            if (vpnGateConnectionList != null && vpnGateConnectionList!!.filter != null) {
                menu.findItem(R.id.action_filter).setIcon(R.drawable.ic_filter_active_white)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Got exception when handle search view", e)
        }
        return super.onCreateOptionsMenu(menu)
    }

    private fun postVPNGateAPI() {
        val vpnGateConnectionList = connectionListViewModel!!.vpnGateConnectionList.value
        isLoading = false
        binding.frameContent.visibility = View.VISIBLE
        lifecycleScope.launch(Dispatchers.IO) {
            if (vpnGateConnectionList != null && "" != sortProperty) {
                vpnGateConnectionList.sort(sortProperty, sortType, true)
            }
            if (dataUtil!!.isAcceptedPrivacyPolicy) {
                withContext(Dispatchers.Main) {
                    displayHome()
                }
            }
        }
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
            sortBottomSheetDialog.setOnApplyClickListener(object :
                SortBottomSheetDialog.OnApplyClickListener {
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
                        currentFragment.sort(
                            this@MainActivity.sortProperty,
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

        if (itemId == R.id.action_filter && vpnGateConnectionList != null) {
            val filterBottomSheetDialog = newInstance(
                vpnGateConnectionList!!.filter
            )
            filterBottomSheetDialog.onButtonClickListener =
                object : OnButtonClickListener {
                    override fun onButtonClick(filter: VPNGateConnectionList.Filter?) {
                        mMenu!!.findItem(R.id.action_filter)
                            .setIcon(if (filter == null) R.drawable.ic_filter_white else R.drawable.ic_filter_active_white)
                        val homeFragment = supportFragmentManager.findFragmentByTag(
                            HomeFragment::class.java.name
                        ) as HomeFragment?
                        if (homeFragment != null && vpnGateConnectionList != null) {
                            val params = Bundle()
                            params.putString("filterObj", Gson().toJson(filter))
                            FirebaseAnalytics.getInstance(applicationContext)
                                .logEvent("Filter", params)
                            vpnGateConnectionList!!.filter = filter
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

    private fun displayHome() {
        isLoading = false
        binding.incLoading.lnLoading.visibility = View.GONE
        if (vpnGateConnectionList != null) {
            replaceFragment("home")
        }
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
                if (vpnGateConnectionList == null || vpnGateConnectionList!!.size() == 0) {
                    callDataServer()
                }
                Log.d(TAG, "replaceFragment when click menu")
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
                    finish()
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
        binding.navMain.setCheckedItem(menuItem.itemId)
        binding.toolbar.collapseActionView()
        binding.activityMainDrawer.closeDrawers()
        return true
    }

    private fun stopRequest() {
        try {
            binding.incError.lnError.visibility = View.GONE
            binding.incLoading.lnLoading.visibility = View.GONE
            binding.incNoNetwork.lnNoNetwork.visibility = View.GONE
        } catch (e: Exception) {
            Log.e(TAG, "Got exception when handle stopRequest", e)
        }
    }

    private fun replaceFragment(url: String?) {
        try {
            Log.d(TAG, "replaceFragment: $url")
            if (url != null && (url != currentUrl || url == "home")) {
                toggleAction(url == "home" && vpnGateConnectionList != null)
                if (url != "home") {
                    binding.incLoading.lnLoading.visibility = View.GONE
                    binding.incNoNetwork.lnNoNetwork.visibility = View.GONE
                    binding.incError.lnError.visibility = View.GONE
                }
                currentUrl = url
                var fragment: Fragment? = null
                var tag: String? = ""
                var title = resources.getString(R.string.app_name)
                when (url) {
                    "privacy-policy" -> {
                        tag = PrivacyPolicyFragment::class.java.name
                        fragment = supportFragmentManager.findFragmentByTag(
                            tag
                        ) ?: PrivacyPolicyFragment()
                        title = getString(R.string.privacy_policy_title)
                    }

                    "home" -> {
                        tag = HomeFragment::class.java.name
                        fragment = supportFragmentManager.findFragmentByTag(
                            tag
                        ) ?: HomeFragment()
                    }

                    "status" -> {
                        tag = StatusFragment::class.java.name
                        fragment = supportFragmentManager.findFragmentByTag(
                            tag
                        ) ?: StatusFragment()
                        title = resources.getString(R.string.status)
                    }

                    "setting" -> {
                        tag = SettingFragment::class.java.name
                        fragment = supportFragmentManager.findFragmentByTag(
                            tag
                        ) ?: SettingFragment()
                        title = resources.getString(R.string.setting)
                    }

                    "help" -> {
                        tag = HelpFragment::class.java.name
                        fragment = supportFragmentManager.findFragmentByTag(
                            tag
                        ) ?: HelpFragment()
                        title = resources.getString(R.string.help)
                    }

                    "about" -> {
                        tag = AboutFragment::class.java.name
                        fragment = supportFragmentManager.findFragmentByTag(
                            tag
                        ) ?: AboutFragment()
                        title = resources.getString(R.string.about)
                        binding.navMain.setCheckedItem(R.id.nav_about)
                    }

                    else -> {}
                }
                if (fragment != null) {
                    binding.frameContent.visibility = View.VISIBLE
                    setTitleActionbar(title)
                    val transaction = supportFragmentManager.beginTransaction()
                    if (!fragment.isAdded) {
                        transaction.add(binding.frameContent.id, fragment, tag)
                    }
                    if (currentFragmentTag != null && currentFragmentTag != tag) {
                        supportFragmentManager.findFragmentByTag(currentFragmentTag)?.let {
                            transaction.hide(it)
                        }
                    }
                    transaction.show(fragment)
                    transaction.commitNow()
                    currentFragmentTag = tag
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Got exception when handle replaceFragment", e)
        }
    }

    private fun startHome() {
        this.loadData()
        Log.d(TAG, "replaceFragment startHome")
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
                    if (vpnGateConnectionList == null) {
                        callDataServer()
                    }
                    binding.navMain.setCheckedItem(R.id.nav_home)
                    replaceFragment(startUpUrl)
                }
                binding.activityMainDrawer.closeDrawer(GravityCompat.START)
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
        binding.frameContent.visibility = View.GONE
        binding.incLoading.lnLoading.visibility = View.GONE
        binding.incError.lnError.visibility = View.VISIBLE
        binding.incNoNetwork.lnNoNetwork.visibility = View.GONE
        Log.w(TAG, "Error on MainActivity.onError %s".format(error))
    }

    var vpnGateConnectionList: VPNGateConnectionList?
        get() = connectionListViewModel!!.vpnGateConnectionList.value
        set(inVpnGateConnectionList) {
            lifecycleScope.launch {
                connectionListViewModel!!.vpnGateConnectionList.value = inVpnGateConnectionList
                withContext(Dispatchers.Main) {
                    if (mMenu != null) {
                        mMenu!!.findItem(R.id.action_filter)
                            .setIcon(if (inVpnGateConnectionList?.filter != null) R.drawable.ic_filter_active_white else R.drawable.ic_filter_white)
                    }
                }
            }
        }

    companion object {
        const val TARGET_FRAGMENT: String = "TARGET_FRAGMENT"
        private const val TAG = "MainActivity"
        private const val SORT_PROPERTY_KEY = "SORT_PROPERTY_KEY"
        private const val SORT_TYPE_KEY = "SORT_TYPE_KEY"
    }
}
