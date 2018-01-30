package vn.unlimit.vpngate.models;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Base64;

import java.text.DecimalFormat;

import vn.unlimit.vpngate.R;

/**
 * Created by dongh on 14/01/2018.
 */

public class VPNGateConnection implements Parcelable {
    public static final Parcelable.Creator<VPNGateConnection> CREATOR
            = new Parcelable.Creator<VPNGateConnection>() {
        public VPNGateConnection createFromParcel(Parcel in) {
            return new VPNGateConnection(in);
        }

        public VPNGateConnection[] newArray(int size) {
            return new VPNGateConnection[size];
        }
    };
    //HostName,IP,Score,Ping,Speed,CountryLong,CountryShort,NumVpnSessions,Uptime,TotalUsers,TotalTraffic,logType,Operator,Message,OpenVPN_ConfigData_Base64
    private String hostName;
    private String ip;
    private int score;
    private int ping;
    private int speed;
    private String countryLong;
    private String countryShort;
    private int numVpnSession;
    private int uptime;
    private int totalUser;
    private long totalTraffic;
    private String logType;
    private String operator;
    private String message;
    private String openVpnConfigData;

    private VPNGateConnection(Parcel in) {
        hostName = in.readString();
        ip = in.readString();
        score = in.readInt();
        ping = in.readInt();
        speed = in.readInt();
        countryLong = in.readString();
        countryShort = in.readString();
        numVpnSession = in.readInt();
        uptime = in.readInt();
        totalUser = in.readInt();
        totalTraffic = in.readLong();
        logType = in.readString();
        operator = in.readString();
        message = in.readString();
        message = in.readString();
        openVpnConfigData = in.readString();
    }

    //Empty constructor
    private VPNGateConnection() {

    }

    public static VPNGateConnection fromCsv(String csvLine) {
        String[] properties = csvLine.split(",");
        try {
            int index = 0;
            VPNGateConnection vpnGateConnection = new VPNGateConnection();
            vpnGateConnection.hostName = properties[index++];
            vpnGateConnection.ip = properties[index++];
            vpnGateConnection.score = Integer.parseInt(properties[index++]);
            vpnGateConnection.ping = Integer.parseInt(properties[index++]);
            vpnGateConnection.speed = Integer.parseInt(properties[index++]);
            vpnGateConnection.countryLong = properties[index++];
            vpnGateConnection.countryShort = properties[index++];
            vpnGateConnection.numVpnSession = Integer.parseInt(properties[index++]);
            vpnGateConnection.uptime = Integer.parseInt(properties[index++]);
            vpnGateConnection.totalUser = Integer.parseInt(properties[index++]);
            vpnGateConnection.totalTraffic = Long.parseLong(properties[index++]);
            vpnGateConnection.logType = properties[index++];
            vpnGateConnection.setOperator(properties[index++]);
            vpnGateConnection.message = properties[index++];
            vpnGateConnection.setOpenVpnConfigData(properties[index]);
            return vpnGateConnection;
        } catch (Exception e) {
            return null;
        }
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeString(hostName);
        out.writeString(ip);
        out.writeInt(score);
        out.writeInt(ping);
        out.writeInt(speed);
        out.writeString(countryLong);
        out.writeString(countryShort);
        out.writeInt(numVpnSession);
        out.writeInt(uptime);
        out.writeInt(totalUser);
        out.writeLong(totalTraffic);
        out.writeString(logType);
        out.writeString(operator);
        out.writeString(message);
        out.writeString(openVpnConfigData);
    }

    private String decodeBase64(String base64str) {
        try {
            byte[] plainBytes = Base64.decode(base64str, 1);
            return new String(plainBytes);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public String getCalculateHostName() {
        return hostName + ".opengw.net";
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public int getPing() {
        return ping;
    }

    public void setPing(int ping) {
        this.ping = ping;
    }

    public String getPingAsString() {
        return ping + "";
    }

    public String getCountryLong() {
        return countryLong;
    }

    public void setCountryLong(String countryLong) {
        this.countryLong = countryLong;
    }

    public String getCountryShort() {
        return countryShort;
    }

    public void setCountryShort(String countryShort) {
        this.countryShort = countryShort;
    }

    public int getNumVpnSession() {
        return numVpnSession;
    }

    public void setNumVpnSession(int numVpnSession) {
        this.numVpnSession = numVpnSession;
    }

    public String getNumVpnSessionAsString() {
        return numVpnSession + "";
    }

    public int getTotalUser() {
        return totalUser;
    }

    public void setTotalUser(int totalUser) {
        this.totalUser = totalUser;
    }

    public String getLogType() {
        return logType;
    }

    public void setLogType(String logType) {
        this.logType = logType;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        operator = operator.replace("'s owner", "");
        this.operator = operator;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getOpenVpnConfigData() {
        return openVpnConfigData;
    }

    public void setOpenVpnConfigData(String openVpnConfigData) {
        if (openVpnConfigData.contains("=")) {
            this.openVpnConfigData = decodeBase64(openVpnConfigData);
        } else {
            this.openVpnConfigData = openVpnConfigData;
        }

    }

    public int getSpeed() {
        return speed;
    }

    public void setSpeed(int speed) {
        this.speed = speed;
    }

    public String getCalculateSpeed() {
        return round((double) speed / (1000 * 1000));
    }

    public int getUptime() {
        return uptime;
    }

    public void setUptime(int uptime) {
        this.uptime = uptime;
    }

    public long getTotalTraffic() {
        return totalTraffic;
    }

    public void setTotalTraffic(long totalTraffic) {
        this.totalTraffic = totalTraffic;
    }

    public String getCalculateUpTime(Context context) {
        //Display as second
        if (uptime < 60000) {
            return round(uptime / 1000) + " " + context.getResources().getString(R.string.seconds);
        }
        //Display as minute
        if (uptime < 3600000) {
            return Math.round((double) uptime / 60000) + " " + context.getResources().getString(R.string.minutes);
        }
        //Display as hours
        if (uptime < 3600000 * 24) {
            return round((double) uptime / 3600000) + " " + context.getResources().getString(R.string.hours);
        }
        return round((double) uptime / (24 * 3600000)) + " " + context.getResources().getString(R.string.days);
    }

    private String round(double value) {
        DecimalFormat df = new DecimalFormat("####0.###");
        return df.format(value);
    }

    public int describeContents() {
        return 0;
    }
}
