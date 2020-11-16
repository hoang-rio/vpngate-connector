package vn.unlimit.vpngate.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import vn.unlimit.vpngate.App
import vn.unlimit.vpngate.api.BaseApiRequest
import vn.unlimit.vpngate.utils.PaidServerUtil

open class BaseViewModel(application: Application): AndroidViewModel(application) {
    val paidServerUtil: PaidServerUtil = App.getInstance().paidServerUtil
    var isLoggedIn: MutableLiveData<Boolean> = MutableLiveData(paidServerUtil.isLoggedIn())
    var isLoading: MutableLiveData<Boolean> = MutableLiveData(false)
    fun baseErrorHandle(error: String?) {
        if (error === BaseApiRequest.ERROR_SESSION_EXPIRES) {
            isLoggedIn.value = false
            paidServerUtil.setIsLoggedIn(false)
        }
    }
}