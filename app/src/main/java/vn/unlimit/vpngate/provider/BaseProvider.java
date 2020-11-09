package vn.unlimit.vpngate.provider;

/**
 * Created by hoangnd on 1/30/2018.
 */

public class BaseProvider {
    public final static String PASS_DETAIL_VPN_CONNECTION = "PASS_DETAIL_VPN_CONNECTION";
    public final static String L2TP_SERVER_TYPE = "L2TP_SERVER_TYPE";
    public final static String FROM_LOGIN = "FROM_LOGIN";

    public static class ACTION {
        public final static String ACTION_CLEAR_CACHE = "vn.unlimit.vpngate.ACTION_CLEAR_CACHE";
        public final static String ACTION_CONNECT_VPN = "vn.unlimit.vpngate.ACTION_CONNECT_VPN";
        public final static String ACTION_CHANGE_NETWORK_STATE = "android.net.conn.CONNECTIVITY_CHANGE";
    }
}
