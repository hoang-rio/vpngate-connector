package vn.unlimit.vpngate.activities.paid

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import vn.unlimit.vpngate.R
import vn.unlimit.vpngate.activities.MainActivity
import vn.unlimit.vpngate.api.BaseApiRequest
import vn.unlimit.vpngate.api.UserApiRequest
import vn.unlimit.vpngate.provider.BaseProvider
import vn.unlimit.vpngate.request.RequestListener

class PaidServerActivity : AppCompatActivity() {

    var isFromLogin = false
    val userApiRequest = UserApiRequest()

    companion object {
        const val TAG = "PaidServerActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
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
    }

    override fun onResume() {
        isFromLogin = intent.getBooleanExtra(BaseProvider.FROM_LOGIN, false)
        if (!isFromLogin) {
            userApiRequest.fetchUser(object : RequestListener {
                override fun onSuccess(result: Any?) {
                    Log.d(TAG, "fetch user success")
                }

                override fun onError(error: String?) {
                    if (error!! == BaseApiRequest.ERROR_SESSION_EXPIRES) {
                        val loginIntent = Intent(this@PaidServerActivity, LoginActivity::class.java)
                        startActivity(loginIntent)
                        finish()
                    }
                    Log.e(TAG, "fetch user error with error {}".format(error))
                }
            })
        }
        super.onResume()
    }
}
