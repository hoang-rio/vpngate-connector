package vn.unlimit.vpngate.provider

/**
 * Created by hoangnd on 1/30/2018.
 */
object BaseProvider {
    const val PASS_DETAIL_VPN_CONNECTION: String = "PASS_DETAIL_VPN_CONNECTION"
    const val L2TP_SERVER_TYPE: String = "L2TP_SERVER_TYPE"
    const val FROM_LOGIN: String = "FROM_LOGIN"

    object ACTION {
        const val ACTION_CLEAR_CACHE: String = "vn.unlimit.vpngate.ACTION_CLEAR_CACHE"
        const val ACTION_CONNECT_VPN: String = "vn.unlimit.vpngate.ACTION_CONNECT_VPN"
        const val ACTION_CHANGE_NETWORK_STATE: String = "android.net.conn.CONNECTIVITY_CHANGE"
    }
}
