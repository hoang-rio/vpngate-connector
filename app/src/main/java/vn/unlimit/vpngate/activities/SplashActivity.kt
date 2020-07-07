package vn.unlimit.vpngate.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks
import vn.unlimit.vpngate.App
import vn.unlimit.vpngate.R
import vn.unlimit.vpngate.activities.paid.LoginActivity
import vn.unlimit.vpngate.activities.paid.PaidServerActivity
import vn.unlimit.vpngate.utils.PaidServerUtil

class SplashActivity : AppCompatActivity() {
    companion object {
        const val TAG = "SplashActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
    }

    private fun startStartUpActivity() {
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
        }, 500)
    }

    private fun checkDynamicLink() {
        FirebaseDynamicLinks.getInstance().getDynamicLink(intent)
                .addOnSuccessListener {
                    // Get deep link from result (may be null if no link is found)
                    var deepLink: Uri? = null
                    if (it != null) {
                        deepLink = it.link
                        Log.w(TAG, "Got url from dynamic link: %s".format(deepLink))
                        startStartUpActivity()
//                        TODO("Implement deep link check logic here (must remove startStartUpActivity cal above)")
                    } else {
                        Log.d(TAG, "No dynamic link found")
                        startStartUpActivity()
                    }


                    // Handle the deep link. For example, open the linked
                    // content, or apply promotional credit to the user's
                    // account.
                    // ...

                    // ...
                }
                .addOnFailureListener {
                    Log.w(TAG, "getDynamicLink:onFailure", it)
                    startStartUpActivity()
                }
    }

    override fun onResume() {
        super.onResume()
        checkDynamicLink()
    }
}