package vn.unlimit.vpngate.models

import java.lang.reflect.Type
import java.util.Date

/**
 * Created by hoangnd on 1/17/2018.
 */
class Cache : Type {
    @JvmField
    var expires: Date? = null

    @JvmField
    var cacheData: VPNGateConnectionList? = null

    /**
     * Check cache is expires or not
     *
     * @return boolean Expires check result
     */
    fun isExpires(): Boolean {
        return expires != null && expires!!.before(Date())
    }
}