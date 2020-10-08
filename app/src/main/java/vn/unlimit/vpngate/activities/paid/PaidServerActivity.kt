package vn.unlimit.vpngate.activities.paid

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import vn.unlimit.vpngate.R
import vn.unlimit.vpngate.activities.MainActivity
import vn.unlimit.vpngate.provider.BaseProvider
import vn.unlimit.vpngate.viewmodels.UserViewModel

class PaidServerActivity : AppCompatActivity() {

    private var isFromLogin = false
    var userViewModel: UserViewModel? = null

    companion object {
        const val TAG = "PaidServerActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        bindViewModel()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_paid_server)
        val navView: BottomNavigationView = findViewById(R.id.nav_view)

        val navController = findNavController(R.id.nav_host_fragment)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(setOf(
                R.id.navigation_home, R.id.navigation_server_list, R.id.navigation_free_server))
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
    }

    private fun bindViewModel() {
        userViewModel = ViewModelProvider(this).get(UserViewModel::class.java)
        userViewModel!!.isLoggedIn.observe(this, { isLoggedIn ->
            if (!isLoggedIn!!) {
                // Go to login screen if user login status is changed
                val intentLogin = Intent(this@PaidServerActivity, LoginActivity::class.java)
                startActivity(intentLogin)
                finish()
            }
        })
    }

    override fun onResume() {
        isFromLogin = intent.getBooleanExtra(BaseProvider.FROM_LOGIN, false)
        if (!isFromLogin) {
            userViewModel!!.fetchUser(true, this)
        } else {
            userViewModel!!.addDevice()
        }
        super.onResume()
    }

    override fun onBackPressed() {
        finish()
    }
}
