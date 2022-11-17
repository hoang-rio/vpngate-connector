package vn.unlimit.vpngate.utils

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Lifecycle.Event.ON_START
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.appopen.AppOpenAd.AppOpenAdLoadCallback
import vn.unlimit.vpngate.App
import vn.unlimit.vpngate.R
import vn.unlimit.vpngate.activities.SplashActivity
import java.util.*


class AppOpenManager(myApplication: App?) : Application.ActivityLifecycleCallbacks{
    private val LOG_TAG = "AppOpenManager"
    private var AD_UNIT_ID: String? = null
    private var appOpenAd: AppOpenAd? = null

    private var loadCallback: AppOpenAdLoadCallback? = null

    private var myApplication: App? = null

    private var currentActivity: Activity? = null

    private var loadTime: Long = 0

    private val lifeCycleEventObserver = LifecycleEventObserver { _, event ->
        run {
            when (event) {
                ON_START -> {
                    showAdIfAvailable()
                    Log.d(LOG_TAG, "onStart")
                }
                else -> {}
            }
        }
    }


    companion object {
        var isShowingAd = false
        var splashActivity: SplashActivity? = null
    }


    /** Constructor  */
    init {
        this.myApplication = myApplication
        AD_UNIT_ID = this.myApplication?.resources!!.getString(R.string.admob_open_app)
        this.myApplication?.registerActivityLifecycleCallbacks(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(lifeCycleEventObserver)
    }


    /** Request an ad  */
    fun fetchAd() {
        // Have unused ad, no need to fetch another.
        if (isAdAvailable()) {
            return
        }

        loadCallback = object : AppOpenAdLoadCallback() {
            /**
             * Called when an app open ad has loaded.
             *
             * @param ad the loaded app open ad.
             */
            override fun onAdLoaded(ad: AppOpenAd) {
                appOpenAd = ad
                loadTime = Date().time
            }

            /**
             * Called when an app open ad has failed to load.
             *
             * @param loadAdError the error.
             */
            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                Log.e(LOG_TAG, loadAdError.toString())
            }
        }
        val request = getAdRequest()
        AppOpenAd.load(
            myApplication!!, AD_UNIT_ID!!, request!!,
            AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT, loadCallback!!
        )

    }

    /** Creates and returns ad request.  */
    private fun getAdRequest(): AdRequest? {
        return AdRequest.Builder().build()
    }

    /** Utility method to check if ad was loaded more than n hours ago.  */
    private fun wasLoadTimeLessThanNHoursAgo(numHours: Long): Boolean {
        val dateDifference = Date().time - loadTime
        val numMilliSecondsPerHour: Long = 3600000
        return dateDifference < numMilliSecondsPerHour * numHours
    }

    /** Utility method that checks if ad exists and can be shown.  */
    private fun isAdAvailable(): Boolean {
        return appOpenAd != null && wasLoadTimeLessThanNHoursAgo(4)
    }

    /** ActivityLifecycleCallback methods  */
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

    override fun onActivityStarted(activity: Activity) {
        currentActivity = activity
    }

    override fun onActivityResumed(activity: Activity) {
        currentActivity = activity
    }

    override fun onActivityStopped(activity: Activity) {}

    override fun onActivityPaused(activity: Activity) {}

    override fun onActivitySaveInstanceState(activity: Activity, bundle: Bundle) {}

    override fun onActivityDestroyed(activity: Activity) {
        currentActivity = null
    }

    /** Shows the ad if one isn't already showing.  */
    fun showAdIfAvailable() {
        // Only show ad if there is not already an app open ad currently showing
        // and an ad is available.
        if (!isShowingAd && isAdAvailable()) {
            Log.d(LOG_TAG, "Will show ad.")
            val fullScreenContentCallback: FullScreenContentCallback =
                object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        // Set the reference to null so isAdAvailable() returns false.
                        appOpenAd = null
                        isShowingAd = false
                        fetchAd()
                        splashActivity?.startStartUpActivity(0)
                        splashActivity = null
                    }

                    override fun onAdFailedToShowFullScreenContent(adError: AdError) {}
                    override fun onAdShowedFullScreenContent() {
                        isShowingAd = true
                    }
                }
            appOpenAd!!.fullScreenContentCallback = fullScreenContentCallback
            appOpenAd!!.show(currentActivity!!)
        } else {
            Log.d(LOG_TAG, "Can not show ad.")
            fetchAd()
        }
    }


}