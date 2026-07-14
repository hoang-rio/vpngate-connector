package vn.unlimit.vpngate.activities.paid

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.common.base.Strings
import vn.unlimit.vpngate.App
import vn.unlimit.vpngate.R
import vn.unlimit.vpngate.activities.MainActivity
import vn.unlimit.vpngate.databinding.ActivityPaidServerBinding
import vn.unlimit.vpngate.provider.BaseProvider
import vn.unlimit.vpngate.utils.NotificationUtil
import vn.unlimit.vpngate.utils.PaidServerUtil
import vn.unlimit.vpngate.viewmodels.DeviceViewModel
import vn.unlimit.vpngate.viewmodels.UserViewModel

class PaidServerActivity : AppCompatActivity() {

    private var isFromLogin = false
    var userViewModel: UserViewModel? = null
    private var deviceViewModel: DeviceViewModel? = null
    private var doubleBackToExitPressedOnce = false
    private var isPaused = false
    private lateinit var binding: ActivityPaidServerBinding
    private var lastSystemBarInsets: Insets = Insets.NONE

    companion object {
        const val TAG = "PaidServerActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        bindViewModel()
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = ActivityPaidServerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val navHostContainer: View = findViewById(R.id.nav_host_fragment)
        val initialNavHostLeft = navHostContainer.paddingLeft
        val initialNavHostTop = navHostContainer.paddingTop
        val initialNavHostRight = navHostContainer.paddingRight
        val initialNavHostBottom = navHostContainer.paddingBottom
        val initialNavViewLeft = binding.navView.paddingLeft
        val initialNavViewRight = binding.navView.paddingRight
        val initialNavViewBottom = binding.navView.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, windowInsets ->
            window.decorView.setBackgroundColor(resources.getColor(R.color.colorPaidServer, theme))
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            lastSystemBarInsets = insets
            navHostContainer.updatePadding(
                left = initialNavHostLeft + insets.left,
                right = initialNavHostRight + insets.right,
                bottom = initialNavHostBottom
            )
            binding.navView.updatePadding(
                left = initialNavViewLeft + insets.left,
                right = initialNavViewRight + insets.right,
                bottom = initialNavViewBottom + insets.bottom
            )
            applyInsetsToCurrentFragment(insets)
            windowInsets
        }
        ViewCompat.requestApplyInsets(binding.root)
        val paidServerUtil = (application as App).paidServerUtil
        paidServerUtil?.setStartupScreen(PaidServerUtil.StartUpScreen.PAID_SERVER)
        val navView: BottomNavigationView = findViewById(R.id.nav_view)
        navView.setOnItemSelectedListener {
            return@setOnItemSelectedListener onNavigationItemSelected(it)
        }
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home, R.id.navigation_server_list, R.id.navigation_free_server
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.id == R.id.navigation_free_server) {
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            } else {
                binding.root.post { applyInsetsToCurrentFragment(lastSystemBarInsets) }
            }
        }
        supportActionBar!!.hide()
        onBackPressedDispatcher.addCallback(object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                handleBackPress()
            }
        })
    }

    private fun onNavigationItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.navigation_free_server) {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
            return false
        }
        return true
    }

    private fun bindViewModel() {
        userViewModel = ViewModelProvider(this)[UserViewModel::class.java]
        deviceViewModel = ViewModelProvider(this)[DeviceViewModel::class.java]
        userViewModel!!.isLoggedIn.observe(this) { isLoggedIn ->
            if (!isLoggedIn!!) {
                // Go to login screen if user login status is changed
                val intentLogin = Intent(this@PaidServerActivity, LoginActivity::class.java)
                startActivity(intentLogin)
                finish()
            }
        }
        isFromLogin = intent.getBooleanExtra(BaseProvider.FROM_LOGIN, false)
        if (!isFromLogin) {
            userViewModel!!.fetchUser(true, this, true)
        } else {
            NotificationUtil(this).requestPermission()
        }
        if (isFromLogin || deviceViewModel?.deviceInfo?.value == null || Strings.isNullOrEmpty(
                deviceViewModel?.deviceInfo?.value?._id
            )
        ) {
            deviceViewModel!!.addDevice()
        }
    }

    override fun onPause() {
        super.onPause()
        isPaused = true
    }

    override fun onResume() {
        super.onResume()
        if (!isFromLogin && isPaused) {
            userViewModel!!.fetchUser(true, this)
        }
        isPaused = false
    }

    fun handleBackPress() {
        val navController =
            findNavController(R.id.nav_host_fragment)
        if (navController.currentDestination?.id == R.id.navigation_home) {
            if (doubleBackToExitPressedOnce) {
                finish()
                return
            }
            doubleBackToExitPressedOnce = true
            Toast.makeText(
                this,
                resources.getString(R.string.press_back_again_to_exit),
                Toast.LENGTH_SHORT
            ).show()
            Handler(mainLooper).postDelayed({ doubleBackToExitPressedOnce = false }, 2000)
        } else {
            navController.popBackStack()
        }
    }

    private fun applyInsetsToCurrentFragment(insets: Insets) {
        val currentFragmentView = (supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as? NavHostFragment)
            ?.childFragmentManager
            ?.primaryNavigationFragment
            ?.view ?: return

        val navDetail = currentFragmentView.findViewById<View?>(R.id.nav_detail)
        val navBar = currentFragmentView.findViewById<View?>(R.id.nav_bar)
        val homeHeaderBackground = currentFragmentView.findViewById<View?>(R.id.header_background)

        when {
            navDetail != null -> applyHeaderInsets(navDetail, insets)
            navBar != null -> applyHeaderInsets(navBar, insets)
            homeHeaderBackground != null -> applyHomeHeaderInsets(currentFragmentView, homeHeaderBackground, insets)
        }
    }

    private fun applyHeaderInsets(header: View, insets: Insets) {
        val initialState = (header.tag as? HeaderInsetsState) ?: HeaderInsetsState(
            height = header.layoutParams.height,
            paddingLeft = header.paddingLeft,
            paddingTop = header.paddingTop,
            paddingRight = header.paddingRight,
            paddingBottom = header.paddingBottom
        ).also { header.tag = it }

        header.updateLayoutParams<ViewGroup.LayoutParams> {
            height = if (initialState.height > 0) {
                initialState.height + insets.top
            } else {
                initialState.height
            }
        }
        header.updatePadding(
            left = initialState.paddingLeft + insets.left,
            top = initialState.paddingTop + insets.top,
            right = initialState.paddingRight + insets.right,
            bottom = initialState.paddingBottom
        )
    }

    private fun applyHomeHeaderInsets(root: View, headerBackground: View, insets: Insets) {
        val headerState = (headerBackground.tag as? HomeHeaderState) ?: HomeHeaderState(
            height = headerBackground.layoutParams.height,
            titleTopMargin = (root.findViewById<View>(R.id.text_home).layoutParams as ViewGroup.MarginLayoutParams).topMargin,
            titleLeftMargin = (root.findViewById<View>(R.id.text_home).layoutParams as ViewGroup.MarginLayoutParams).leftMargin,
            titleRightMargin = (root.findViewById<View>(R.id.text_home).layoutParams as ViewGroup.MarginLayoutParams).rightMargin,
            logoutTopMargin = (root.findViewById<View>(R.id.iv_logout).layoutParams as ViewGroup.MarginLayoutParams).topMargin,
            logoutRightMargin = (root.findViewById<View>(R.id.iv_logout).layoutParams as ViewGroup.MarginLayoutParams).rightMargin
        ).also { headerBackground.tag = it }

        val titleView = root.findViewById<View>(R.id.text_home)
        val logoutView = root.findViewById<View>(R.id.iv_logout)

        headerBackground.updateLayoutParams<ViewGroup.LayoutParams> {
            height = headerState.height + insets.top
        }
        titleView.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            topMargin = headerState.titleTopMargin + insets.top
            leftMargin = headerState.titleLeftMargin + insets.left
            rightMargin = headerState.titleRightMargin + insets.right
        }
        logoutView.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            topMargin = headerState.logoutTopMargin + insets.top
            rightMargin = headerState.logoutRightMargin + insets.right
        }
    }

    private data class HeaderInsetsState(
        val height: Int,
        val paddingLeft: Int,
        val paddingTop: Int,
        val paddingRight: Int,
        val paddingBottom: Int,
    )

    private data class HomeHeaderState(
        val height: Int,
        val titleTopMargin: Int,
        val titleLeftMargin: Int,
        val titleRightMargin: Int,
        val logoutTopMargin: Int,
        val logoutRightMargin: Int,
    )
}
