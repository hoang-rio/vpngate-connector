package vn.unlimit.vpngate.activities.paid

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
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

    companion object {
        const val TAG = "PaidServerActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        bindViewModel()
        super.onCreate(savedInstanceState)
        binding = ActivityPaidServerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, windowInsets ->
            window.decorView.setBackgroundColor(resources.getColor(R.color.colorPaidServer, theme))
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            val statusBarInsets = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars())
            v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = statusBarInsets.top
                leftMargin = insets.left
                rightMargin = insets.right
            }
            v.setPadding(0, 0, 0, insets.bottom)

            // Return CONSUMED if you don't want the window insets to keep passing
            // down to descendant views.
            WindowInsetsCompat.CONSUMED
        }
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
}
