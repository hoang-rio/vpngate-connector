package vn.unlimit.vpngate.activities

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks
import vn.unlimit.vpngate.App
import vn.unlimit.vpngate.R
import vn.unlimit.vpngate.activities.paid.ActivateActivity
import vn.unlimit.vpngate.activities.paid.LoginActivity
import vn.unlimit.vpngate.activities.paid.PaidServerActivity
import vn.unlimit.vpngate.provider.PaidServerProvider
import vn.unlimit.vpngate.utils.PaidServerUtil
import java.util.regex.Pattern

class SplashActivity : AppCompatActivity() {
    companion object {
        const val TAG = "SplashActivity"
        const val ACTIVATE_URL_REGEX = "/user/(\\w{24})/activate/(\\w{32})"
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
        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(actIntent)
            finish()
        }, 500)
    }

    private fun checkDynamicLink() {
        FirebaseDynamicLinks.getInstance().getDynamicLink(intent)
                .addOnSuccessListener {
                    // Get deep link from result (may be null if no link is found)
                    var deepLink: String?
                    if (it != null) {
                        deepLink = it.link.toString()
                        Log.w(TAG, "Got url from dynamic link: %s".format(deepLink))
                        val matcherActivate = Pattern.compile(ACTIVATE_URL_REGEX).matcher(deepLink)
                        if (matcherActivate.find()) {
                            val userId = matcherActivate.group(1)
                            val activateCode = matcherActivate.group(2)
                            val intentActivate = Intent(this, ActivateActivity::class.java)
                            intentActivate.putExtra(PaidServerProvider.USER_ID, userId)
                            intentActivate.putExtra(PaidServerProvider.ACTIVATE_CODE, activateCode)
                            startActivity(intentActivate)
                            finish()
                            return@addOnSuccessListener
                        }
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