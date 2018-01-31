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

    public List<VPNGateConnection> getAll() {
        return data;
    }

    /**
     * Filter connection by keyword
     *
     * @param keyword keyword to filter
     * @return
     */
    public VPNGateConnectionList filter(String keyword) {
        VPNGateConnectionList result = new VPNGateConnectionList();
        for (VPNGateConnection vpnGateConnection : data) {
            if (vpnGateConnection.getCountryLong().toLowerCase().contains(keyword.toLowerCase())) {
                result.add(vpnGateConnection);
            }
        }
        return result;
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
    public void orderBy(final String property, final int type) {
        Collections.sort(data, new Comparator<VPNGateConnection>() {
            @Override
            public int compare(VPNGateConnection o1, VPNGateConnection o2) {
                if (type == ORDER.ASC) {
                    switch (property) {
                        case Property.COUNTRY:
                            return o1.getCountryLong().compareTo(o2.getCountryLong());
                        case Property.SPEED:
                            return Integer.valueOf(o1.getSpeed()).compareTo(o2.getSpeed());
                        case Property.PING:
                            return Integer.valueOf(o1.getPing()).compareTo(o2.getPing());
                        case Property.SCORE:
                            return Integer.valueOf(o1.getScore()).compareTo(o2.getScore());
                        case Property.UPTIME:
                            return Integer.valueOf(o1.getUptime()).compareTo(o2.getUptime());
                        default:
                            return 0;
                    }
                } else if (type == ORDER.DESC) {
                    switch (property) {
                        case Property.COUNTRY:
                            return o2.getCountryLong().compareTo(o1.getCountryLong());
                        case Property.SPEED:
                            return Integer.valueOf(o2.getSpeed()).compareTo(o1.getSpeed());
                        case Property.PING:
                            return Integer.valueOf(o2.getPing()).compareTo(o1.getPing());
                        case Property.SCORE:
                            return Integer.valueOf(o2.getScore()).compareTo(o1.getScore());
                        case Property.UPTIME:
                            return Integer.valueOf(o2.getUptime()).compareTo(o1.getUptime());
                        default:
                            return 0;
                    }
                }
                return 0;
            }
        });
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

    public final class Property {
        public static final String COUNTRY = "COUNTRY";
        public static final String SPEED = "SPEED";
        public static final String PING = "PING";
        public static final String SCORE = "SCORE";
        public static final String UPTIME = "UPTIME";
    }
}
