package vn.unlimit.vpngateclient.models;

import android.util.Base64;

/**
 * Created by dongh on 14/01/2018.
 */

public class VPNGateConnection {
    //HostName,IP,Score,Ping,Speed,CountryLong,CountryShort,NumVpnSessions,Uptime,TotalUsers,TotalTraffic,logType,Operator,Message,OpenVPN_ConfigData_Base64
    private String hostName;
    private String iP;
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

    public static VPNGateConnection fromCsv(String csvLine) {
        String[] properties = csvLine.split(",");
        try {
            int index = 0;
            VPNGateConnection vpnGateConnection = new VPNGateConnection();
            vpnGateConnection.hostName = properties[index++];
            vpnGateConnection.iP = properties[index++];
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
            vpnGateConnection.operator = properties[index++];
            vpnGateConnection.message = properties[index++];
            vpnGateConnection.setOpenVpnConfigData(properties[index]);
            return vpnGateConnection;
        } catch (Exception e) {
            return null;
        }
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

    public String getiP() {
        return iP;
    }

    public void setiP(String iP) {
        this.iP = iP;
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
}
