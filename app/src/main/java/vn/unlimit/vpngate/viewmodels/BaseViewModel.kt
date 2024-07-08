package vn.unlimit.vpngate.viewmodels

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import vn.unlimit.vpngate.App
import vn.unlimit.vpngate.R
import vn.unlimit.vpngate.activities.paid.LoginActivity
import vn.unlimit.vpngate.utils.PaidServerUtil
import java.net.HttpURLConnection

open class BaseViewModel(application: Application) : AndroidViewModel(application) {
    companion object {
        const val TAG = "BaseViewModel"
        const val ITEM_PER_PAGE = 30
        const val PARAMS_USER_PLATFORM = "Android"
        const val MAX_RETRY_COUNT = 3
    }

    val paidServerUtil: PaidServerUtil = App.getInstance().paidServerUtil
    var isLoggedIn: MutableLiveData<Boolean> = MutableLiveData(paidServerUtil.isLoggedIn())
    var isLoading: MutableLiveData<Boolean> = MutableLiveData(false)

    private fun processRequest(chain: Interceptor.Chain, retryCount: Int = 0) : okhttp3.Response {
        try {
            val requestBuilder: Request.Builder = chain.request().newBuilder()
            if (paidServerUtil.isLoggedIn()) {
                val sessionHeaderName = paidServerUtil.getSessionHeaderName()
                paidServerUtil.getStringSetting(PaidServerUtil.SESSION_ID_KEY)
                    ?.let { it1 -> requestBuilder.addHeader(sessionHeaderName, it1) }
            }
            return chain.proceed(requestBuilder.build())
        } catch (th: Throwable) {
            Log.e(TAG, "Got exception when process interceptor")
            if (retryCount < MAX_RETRY_COUNT) {
                return processRequest(chain, retryCount + 1)
            } else {
                throw th
            }
        }
    }

    private val httpClientBuilder: OkHttpClient.Builder = OkHttpClient.Builder().addInterceptor {
        return@addInterceptor processRequest(it)
    }

    var retrofit: Retrofit = Retrofit.Builder().baseUrl(
        FirebaseRemoteConfig.getInstance()
            .getString(App.getResourceString(R.string.cfg_paid_server_api_base_url))
    )
        .client(httpClientBuilder.build())
        .addConverterFactory(ScalarsConverterFactory.create())
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private fun handleExpiresError() {
        isLoggedIn.value = false
        paidServerUtil.setIsLoggedIn(false)
    }

    fun handleExpiresError(errorCode: Int?, activity: Activity?) {
        if (errorCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
            handleExpiresError()
            activity?.let {
                // Session expires
                paidServerUtil.setIsLoggedIn(false)
                paidServerUtil.removeSetting(PaidServerUtil.USER_INFO_KEY)
                // Redirect to login screen
                if (!activity.isFinishing) {
                    val intentLogin = Intent(activity, LoginActivity::class.java)
                    activity.startActivity(intentLogin)
                    activity.finish()
                    Toast.makeText(
                        activity,
                        activity.getText(R.string.session_expires),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
}