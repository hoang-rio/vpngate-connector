package vn.unlimit.vpngate.models;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.Collections;
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
    private Filter filter;
    private List<VPNGateConnection> data;

    public VPNGateConnectionList() {
        data = new ArrayList<>();
    }

    private VPNGateConnectionList(Parcel in) {
        data = in.createTypedArrayList(VPNGateConnection.CREATOR);
    }

    public Filter getFilter() {
        return filter;
    }

    public void setFilter(Filter filter) {
        this.filter = filter;
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
        if (filter != null) {
            return result.advancedFilter(filter);
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
        Collections.sort(data, (o1, o2) -> {
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

    public VPNGateConnectionList advancedFilter(Filter filter) {
        this.setFilter(filter);
        return advancedFilter();
    }

    public VPNGateConnectionList advancedFilter() {
        if (filter == null) {
            return this;
        }
        VPNGateConnectionList dataWithFilter = new VPNGateConnectionList();
        // Apply or filter conditional
        for (VPNGateConnection vpnGateConnection : data) {
            if (filter.ping != null) {
                switch (filter.pingFilterOperator) {
                    case EQUAL:
                        if (vpnGateConnection.getPing() != filter.ping) {
                            continue;
                        }
                        break;
                    case GREATER:
                        if (vpnGateConnection.getPing() <= filter.ping) {
                            continue;
                        }
                        break;
                    case GREATER_OR_EQUAL:
                        if (vpnGateConnection.getPing() < filter.ping) {
                            continue;
                        }
                        break;
                    case LESS:
                        if (vpnGateConnection.getPing() >= filter.ping) {
                            continue;
                        }
                        break;
                    case LESS_OR_EQUAL:
                        if (vpnGateConnection.getPing() > filter.ping) {
                            continue;
                        }
                        break;
                    default:
                        break;
                }
            }
            if (filter.speed != null) {
                int speedInMb = filter.speed * 1024 * 1024;
                switch (filter.speedFilterOperator) {
                    case EQUAL:
                        if (vpnGateConnection.getSpeed() != speedInMb) {
                            continue;
                        }
                        break;
                    case GREATER:
                        if (vpnGateConnection.getSpeed() <= speedInMb) {
                            continue;
                        }
                        break;
                    case GREATER_OR_EQUAL:
                        if (vpnGateConnection.getSpeed() < speedInMb) {
                            continue;
                        }
                        break;
                    case LESS:
                        if (vpnGateConnection.getSpeed() >= speedInMb) {
                            continue;
                        }
                        break;
                    case LESS_OR_EQUAL:
                        if (vpnGateConnection.getSpeed() > speedInMb) {
                            continue;
                        }
                        break;
                    default:
                        break;
                }
            }
            if (filter.sessionCount != null) {
                switch (filter.sessionCountFilterOperator) {
                    case EQUAL:
                        if (vpnGateConnection.getNumVpnSession() != filter.sessionCount) {
                            continue;
                        }
                        break;
                    case GREATER:
                        if (vpnGateConnection.getNumVpnSession() <= filter.sessionCount) {
                            continue;
                        }
                        break;
                    case GREATER_OR_EQUAL:
                        if (vpnGateConnection.getNumVpnSession() < filter.sessionCount) {
                            continue;
                        }
                        break;
                    case LESS:
                        if (vpnGateConnection.getNumVpnSession() >= filter.sessionCount) {
                            continue;
                        }
                        break;
                    case LESS_OR_EQUAL:
                        if (vpnGateConnection.getNumVpnSession() > filter.sessionCount) {
                            continue;
                        }
                        break;
                    default:
                        break;
                }
            }
            if (filter.isShowTCP && vpnGateConnection.getTcpPort() > 0) {
                dataWithFilter.add(vpnGateConnection);
                continue;
            }
            if (filter.isShowUDP && vpnGateConnection.getUdpPort() > 0) {
                dataWithFilter.add(vpnGateConnection);
                continue;
            }
            if (filter.isShowL2TP && vpnGateConnection.isL2TPSupport()) {
                dataWithFilter.add(vpnGateConnection);
                continue;
            }
            if (filter.isShowSSTP && vpnGateConnection.isSSTPSupport()) {
                dataWithFilter.add(vpnGateConnection);
                continue;
            }
        }
        return dataWithFilter;
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

    public enum NumberFilterOperator {
        EQUAL,
        GREATER,
        GREATER_OR_EQUAL,
        LESS,
        LESS_OR_EQUAL
    }

    public static class Filter {
        public Boolean isShowTCP = true;
        public Boolean isShowUDP = true;
        public Boolean isShowL2TP = true;
        public Boolean isShowSSTP = true;
        public Integer ping;
        public NumberFilterOperator pingFilterOperator = NumberFilterOperator.LESS_OR_EQUAL;
        public Integer speed;
        public NumberFilterOperator speedFilterOperator = NumberFilterOperator.GREATER_OR_EQUAL;
        public Integer sessionCount;
        public NumberFilterOperator sessionCountFilterOperator = NumberFilterOperator.LESS_OR_EQUAL;
    }

    public static final class ORDER {
        public static final int ASC = 0;
        public static final int DESC = 1;
    }

    public static final class SortProperty {
        public static final String COUNTRY = "COUNTRY";
        public static final String SPEED = "SPEED";
        public static final String PING = "PING";
        public static final String SCORE = "SCORE";
        public static final String UPTIME = "UPTIME";
        public static final String SESSION = "SESSION";
    }
}
