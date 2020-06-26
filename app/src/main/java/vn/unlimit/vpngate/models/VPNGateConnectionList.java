package vn.unlimit.vpngate.models;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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

    /**
     * Filter connection by keyword
     *
     * @param keyword keyword to filter
     * @return
     */
    public VPNGateConnectionList filter(String keyword) {
        keyword = keyword.toLowerCase();
        VPNGateConnectionList result = new VPNGateConnectionList();
        for (VPNGateConnection vpnGateConnection : data) {
            if (
                    vpnGateConnection.getCountryLong().toLowerCase().contains(keyword) ||
                            vpnGateConnection.getHostName().toLowerCase().contains(keyword) ||
                            vpnGateConnection.getOperator().toLowerCase().contains(keyword) ||
                            vpnGateConnection.getIp().contains(keyword)
            ) {
                result.add(vpnGateConnection);
            }
        }
        return result;
    }

    public List<VPNGateConnection> getAll() {
        return data;
    }

    /**
     * Get ordered list
     *
     * @param property
     * @param type     order type 0 = ASC, 1 = DESC
     * @return
     */
    public void sort(final String property, final int type) {
        Collections.sort(data, new Comparator<VPNGateConnection>() {
            @Override
            public int compare(VPNGateConnection o1, VPNGateConnection o2) {
                if (type == ORDER.ASC) {
                    switch (property) {
                        case SortProperty.COUNTRY:
                            return o1.getCountryLong().compareTo(o2.getCountryLong());
                        case SortProperty.SPEED:
                            return Integer.compare(o1.getSpeed(), o2.getSpeed());
                        case SortProperty.PING:
                            return Integer.compare(o1.getPing(), o2.getPing());
                        case SortProperty.SCORE:
                            return Integer.compare(o1.getScore(), o2.getScore());
                        case SortProperty.UPTIME:
                            return Integer.compare(o1.getUptime(), o2.getUptime());
                        case SortProperty.SESSION:
                            return Integer.compare(o1.getNumVpnSession(), o2.getNumVpnSession());
                        default:
                            return 0;
                    }
                } else if (type == ORDER.DESC) {
                    switch (property) {
                        case SortProperty.COUNTRY:
                            return o2.getCountryLong().compareTo(o1.getCountryLong());
                        case SortProperty.SPEED:
                            return Integer.compare(o2.getSpeed(), o1.getSpeed());
                        case SortProperty.PING:
                            return Integer.compare(o2.getPing(), o1.getPing());
                        case SortProperty.SCORE:
                            return Integer.compare(o2.getScore(), o1.getScore());
                        case SortProperty.UPTIME:
                            return Integer.compare(o2.getUptime(), o1.getUptime());
                        case SortProperty.SESSION:
                            return Integer.compare(o2.getNumVpnSession(), o1.getNumVpnSession());
                        default:
                            return 0;
                    }
                }
                return 0;
            }
        });
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

    enum NumberFilterOperator {
        EQUAL,
        GREATER,
        GREATER_OR_EQUAL,
        LESS,
        LESS_OR_EQUAL
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
        public static final int ASC = 0;
        public static final int DESC = 1;
    }

    public final class SortProperty {
        public static final String COUNTRY = "COUNTRY";
        public static final String SPEED = "SPEED";
        public static final String PING = "PING";
        public static final String SCORE = "SCORE";
        public static final String UPTIME = "UPTIME";
        public static final String SESSION = "SESSION";
    }
}
