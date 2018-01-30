package vn.unlimit.vpngate.models;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by dongh on 14/01/2018.
 */

public class VPNGateConnectionList implements Parcelable {
    public static final Parcelable.Creator<VPNGateConnectionList> CREATOR
            = new Parcelable.Creator<VPNGateConnectionList>() {
        public VPNGateConnectionList createFromParcel(Parcel in) {
            return new VPNGateConnectionList(in);
        }

        public VPNGateConnectionList[] newArray(int size) {
            return new VPNGateConnectionList[size];
        }
    };
    private List<VPNGateConnection> data;

    public VPNGateConnectionList() {
        data = new ArrayList<>();
    }

    private VPNGateConnectionList(Parcel in) {
        data = in.createTypedArrayList(VPNGateConnection.CREATOR);
    }

    public List<VPNGateConnection> getAll() {
        return data;
    }

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

    public void clear() {
        data.clear();
    }

    public void addAll(VPNGateConnectionList list) {
        data.addAll(list.data);
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

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeTypedList(data);
    }

    public final class ORDER {
        public final int ASC = 0;
        public final int DESC = 1;
    }

    public final class Property {
        public final String COUNTRY = "COUNTRY";
        public final String SPEED = "SPEED";
        public final String PING = "PING";
        public final String UPTIME = "UPTIME";
    }
}
