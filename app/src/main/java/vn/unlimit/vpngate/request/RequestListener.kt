package vn.unlimit.vpngate.request

/**
 * Created by dongh on 14/01/2018.
 */
interface RequestListener {
    fun onSuccess(result: Any?)

    fun onError(error: String?)
}
