package vn.unlimit.vpngate.activities

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import vn.unlimit.vpngate.App
import vn.unlimit.vpngate.R
import vn.unlimit.vpngate.utils.PaidServerUtil

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
    }

    override fun onResume() {
        super.onResume()
        val actIntent: Intent
        val paidServerUtil: PaidServerUtil = App.getInstance().paidServerUtil
        if (paidServerUtil.getStartUpScreen() == PaidServerUtil.StartUpScreen.PAID_SERVER) {
            if (paidServerUtil.isLoggedIn()) {
                actIntent = Intent(this, PaidServerActivity::class.java)
            } else {
                actIntent = Intent(this, LoginActivity::class.java)
            }
        } else {
            actIntent = Intent(this, MainActivity::class.java)
        }
        Handler().postDelayed({
            startActivity(actIntent)
            finish()
        }, 800)
    }
}