package vn.unlimit.vpngate.models

import android.os.Build
import android.os.Build.VERSION_CODES
import android.os.Parcel
import android.os.Parcelable
import vn.unlimit.vpngate.App
import vn.unlimit.vpngate.utils.DataUtil

class PaidServer(inParcel: Parcel) : Parcelable {
    var _id: String = ""
    private var l2tpSupport: Int = 1
    private var sstpSupport: Int = 1
    var serverCountryCode: String = ""
    var isCommunity: Boolean = false
    private var public: Int = 1
    var serverName: String = ""
    var serverLocation = "Singapore"
    var serverDomain = ""
    var serverIp = ""
    var tcpPort = 0
    var udpPort = 0
    var maxSession = 0
    var sessionCount = 0
    private var ovpnContent = ""
    var serverStatus = ""

    companion object CREATOR : Parcelable.Creator<PaidServer?> {
        override fun createFromParcel(inParcel: Parcel): PaidServer {
            return PaidServer(inParcel)
        }

        override fun newArray(size: Int): Array<PaidServer?> {
            return arrayOfNulls(size)
        }
    }

    init {
        _id = inParcel.readString()!!
        l2tpSupport = inParcel.readInt()
        sstpSupport = inParcel.readInt()
        serverCountryCode = inParcel.readString()!!
        isCommunity = inParcel.readByte() == 1.toByte()
        public = inParcel.readInt()
        serverName = inParcel.readString()!!
        serverLocation = inParcel.readString()!!
        serverDomain = inParcel.readString()!!
        serverIp = inParcel.readString()!!
        tcpPort = inParcel.readInt()
        udpPort = inParcel.readInt()
        maxSession = inParcel.readInt()
        sessionCount = inParcel.readInt()
        ovpnContent = inParcel.readString()!!
        serverStatus = inParcel.readString()!!
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(out: Parcel, flags: Int) {
        out.writeString(_id)
        out.writeInt(l2tpSupport)
        out.writeInt(sstpSupport)
        out.writeString(serverCountryCode)
        out.writeByte(if (isCommunity) 1 else 0)
        out.writeInt(public)
        out.writeString(serverName)
        out.writeString(serverLocation)
        out.writeString(serverDomain)
        out.writeString(serverIp)
        out.writeInt(tcpPort)
        out.writeInt(udpPort)
        out.writeInt(maxSession)
        out.writeInt(sessionCount)
        out.writeString(ovpnContent)
        out.writeString(serverStatus)
    }

    fun getOpenVpnConfigDataUdp(): String {
        var openVpnConfigDataTmp: String = ovpnContent
        if (!App.instance!!.dataUtil!!.getBooleanSetting(DataUtil.USE_DOMAIN_TO_CONNECT, false)) {
            openVpnConfigDataTmp = openVpnConfigDataTmp.replace(serverDomain, serverIp)
        }
        return openVpnConfigDataTmp
    }

    fun getOpenVpnConfigData(): String {
        var openVpnConfigDataTcp: String = ovpnContent
        if (udpPort > 0) {
            // Current config is config for tcp need for udp
            openVpnConfigDataTcp = openVpnConfigDataTcp
                .replace("proto udp", "proto tcp")
                .replace("remote $serverDomain $tcpPort", "remote $serverDomain $udpPort")
        }
        if (!App.instance!!.dataUtil!!.getBooleanSetting(DataUtil.USE_DOMAIN_TO_CONNECT, false)) {
            openVpnConfigDataTcp = openVpnConfigDataTcp.replace(serverDomain, serverIp)
        }
        // Current config is udp only
        return openVpnConfigDataTcp
    }

    fun getName(useUdp: Boolean): String {
        var address: String = serverIp
        if (App.instance!!.dataUtil!!.getBooleanSetting(DataUtil.USE_DOMAIN_TO_CONNECT, false)) {
            address = serverDomain
        }
        return if (App.instance!!.dataUtil!!.getBooleanSetting(
                DataUtil.INCLUDE_UDP_SERVER,
                true
            )
        ) {
            if (tcpPort == 0 && udpPort == 0) {
                // Current profile from non udp but open status page with include udp option
                String.format("Paid-%s[%s]", serverLocation, address)
            } else String.format(
                "Paid-%s[%s][%s]",
                serverLocation,
                address,
                if (useUdp || tcpPort == 0) "UDP:$udpPort" else "TCP:$tcpPort"
            )
        } else String.format("Paid-%s[%s]", serverLocation, address)
    }

    fun isSSTPSupport(): Boolean {
        return Build.VERSION.SDK_INT >= VERSION_CODES.M && sstpSupport == 1
    }

    fun isL2TPSupport() : Boolean {
        return Build.VERSION.SDK_INT < VERSION_CODES.TIRAMISU && l2tpSupport == 1
    }
}