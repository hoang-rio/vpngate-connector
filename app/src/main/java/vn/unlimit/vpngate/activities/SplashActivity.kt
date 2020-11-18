package vn.unlimit.vpngate.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks
import vn.unlimit.vpngate.App
import vn.unlimit.vpngate.R
import vn.unlimit.vpngate.activities.paid.ActivateActivity
import vn.unlimit.vpngate.activities.paid.LoginActivity
import vn.unlimit.vpngate.activities.paid.PaidServerActivity
import vn.unlimit.vpngate.activities.paid.ResetPassActivity
import vn.unlimit.vpngate.provider.PaidServerProvider
import vn.unlimit.vpngate.utils.PaidServerUtil
import java.util.regex.Pattern

class SplashActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "SplashActivity"
        private const val ACTIVATE_URL_REGEX = "/user/(\\w{24})/activate/(\\w{32})"
        private const val PASS_RESET_URL_REGEX = "/user/password-reset/(\\w{20})"
        private const val REQUEST_UPDATE_CODE = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        checkDynamicLink()
    }

    private fun checkAppUpdateAndStartActivity() {
        // Creates instance of the manager.
        val appUpdateManager = AppUpdateManagerFactory.create(this)

        // Returns an intent object that you use to check for an update.
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo

        // Checks that the platform will allow the specified type of update.
        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                    // For a flexible update, use AppUpdateType.FLEXIBLE
                    && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
            ) {
                appUpdateManager.startUpdateFlowForResult(appUpdateInfo, AppUpdateType.IMMEDIATE, this, REQUEST_UPDATE_CODE)
            } else {
                startStartUpActivity(100)
            }
        }
        appUpdateInfoTask.addOnFailureListener{ e ->
            Log.e(TAG, "Update check failure", e)
            startStartUpActivity()
        }
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_UPDATE_CODE && resultCode != Activity.RESULT_OK) {
            checkAppUpdateAndStartActivity()
        }
    }

    private fun startStartUpActivity(delay: Long = 300) {
        val paidServerUtil: PaidServerUtil = App.getInstance().paidServerUtil
        val actIntent: Intent = if (paidServerUtil.getStartUpScreen() == PaidServerUtil.StartUpScreen.PAID_SERVER) {
            if (paidServerUtil.isLoggedIn()) {
                Intent(this, PaidServerActivity::class.java)
            } else {
                Intent(this, LoginActivity::class.java)
            }
        } else {
            Intent(this, MainActivity::class.java)
        }
        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(actIntent)
            finish()
        }, delay)
    }

    private fun checkDynamicLink() {
        FirebaseDynamicLinks.getInstance().getDynamicLink(intent)
                .addOnSuccessListener {
                    // Get deep link from result (may be null if no link is found)
                    if (it != null) {
                        val deepLink = it.link.toString()
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
                        val matcherResetPass = Pattern.compile(PASS_RESET_URL_REGEX).matcher(deepLink)
                        if (matcherResetPass.find()) {
                            val token = matcherResetPass.group(1)
                            val intentResetPass = Intent(this, ResetPassActivity::class.java)
                            intentResetPass.putExtra(PaidServerProvider.RESET_PASS_TOKEN, token)
                            startActivity(intentResetPass)
                            finish()
                            return@addOnSuccessListener
                        }
                        checkAppUpdateAndStartActivity()
                    } else {
                        Log.d(TAG, "No dynamic link found")
                        checkAppUpdateAndStartActivity()
                    }
                }
                .addOnFailureListener {
                    Log.e(TAG, "getDynamicLink:onFailure", it)
                    checkAppUpdateAndStartActivity()
                }
    }
}