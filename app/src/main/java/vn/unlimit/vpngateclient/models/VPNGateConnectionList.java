package vn.unlimit.vpngateclient.models;

import java.util.List;

/**
 * Created by dongh on 14/01/2018.
 */

public class VPNGateConnectionList {
    private List<VPNGateConnection> data;

    /**
     * Filter connection by keyword
     *
     * @param keyword  keyword to filter
     * @param property property to filter
     * @return
     */
    private List<VPNGateConnection> filter(String keyword, String property) {
        return data;
    }

    public void add(VPNGateConnection vpnGateConnection) {
        data.add(vpnGateConnection);
    }

    public void remove(int index) {
        data.remove(index);
    }

    public VPNGateConnection get(int index) {
        return data.get(index);
    }

    public int size() {
        return data.size();
    }


    /**
     * Get ordered list
     *
     * @param property
     * @param type     order type 0 = ASC, 1 = DESC
     * @return
     */
    public List<VPNGateConnection> orderBy(String property, int type) {
        return data;
    }

    public List<VPNGateConnection> getData() {
        return data;
    }

    public void setData(List<VPNGateConnection> data) {
        this.data = data;
    }
}
