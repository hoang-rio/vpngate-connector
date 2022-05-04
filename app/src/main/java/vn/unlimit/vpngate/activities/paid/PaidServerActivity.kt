package vn.unlimit.vpngate.activities.paid

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.android.synthetic.main.activity_paid_server.*
import vn.unlimit.vpngate.App
import vn.unlimit.vpngate.R
import vn.unlimit.vpngate.activities.MainActivity
import vn.unlimit.vpngate.provider.BaseProvider
import vn.unlimit.vpngate.utils.PaidServerUtil
import vn.unlimit.vpngate.viewmodels.DeviceViewModel
import vn.unlimit.vpngate.viewmodels.UserViewModel

class PaidServerActivity : AppCompatActivity() {

    private var isFromLogin = false
    var userViewModel: UserViewModel? = null
    var deviceViewModel: DeviceViewModel? = null
    private var doubleBackToExitPressedOnce = false
    private var isPaused = false
    var navController: NavController? = null

    companion object {
        const val TAG = "PaidServerActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        bindViewModel()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_paid_server)
        val paidServerUtil = (application as App).paidServerUtil
        paidServerUtil.setStartupScreen(PaidServerUtil.StartUpScreen.PAID_SERVER)
        val navView: BottomNavigationView = findViewById(R.id.nav_view)
        navView.setOnItemSelectedListener {
            return@setOnItemSelectedListener onNavigationItemSelected(it)
        }

        navController = findNavController(R.id.nav_host_fragment)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home, R.id.navigation_server_list, R.id.navigation_free_server
            )
        )
        setupActionBarWithNavController(navController!!, appBarConfiguration)
        navView.setupWithNavController(navController!!)
        navController!!.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.id == R.id.navigation_free_server) {
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            }
        }
        supportActionBar!!.hide()
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
        userViewModel = ViewModelProvider(this).get(UserViewModel::class.java)
        deviceViewModel = ViewModelProvider(this).get(DeviceViewModel::class.java)
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

    override fun onBackPressed() {
        val currentFragmentId =
            NavHostFragment.findNavController(nav_host_fragment).currentDestination?.id
        if (currentFragmentId == R.id.navigation_home) {
            if (doubleBackToExitPressedOnce) {
                super.onBackPressed()
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
            super.onBackPressed()
        }
    }
}
