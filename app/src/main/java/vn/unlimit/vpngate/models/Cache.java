package vn.unlimit.vpngate.models;

import java.util.Date;

/**
 * Created by hoangnd on 1/17/2018.
 */

public class Cache {
    public Date expires;
    public VPNGateConnectionList cacheData;

    /**
     * Check cache is expires or not
     *
     * @return boolean Expires check result
     */
    public boolean isExpires() {
        return expires != null && expires.before(new Date());
    }
}
