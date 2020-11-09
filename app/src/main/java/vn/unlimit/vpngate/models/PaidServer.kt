package vn.unlimit.vpngate.models

import android.os.Parcel
import android.os.Parcelable

class PaidServer: Parcelable {
    var l2tpSupport: Int = 1
    var serverCountryCode: String = ""
    var isCommunity: Boolean = false
    var public: Int = 1
    var serverName: String = ""
    var serverLocation = "Singapore"
    var serverDomain = ""
    var serverIp = ""
    var tcpPort = 0
    var udpPort = 0
    var maxSession = 0
    var sessionCount = 0
    var ovpnContent = ""
    var serverStatus = ""
    companion object CREATOR: Parcelable.Creator<PaidServer?> {
        override fun createFromParcel(inParcel: Parcel): PaidServer? {
            return PaidServer(inParcel)
        }

        override fun newArray(size: Int): Array<PaidServer?> {
            return arrayOfNulls(size)
        }
    }
    constructor(inParcel: Parcel) {
        l2tpSupport = inParcel.readInt()
        serverCountryCode = inParcel.readString()!!
        isCommunity = inParcel.readByte().equals(1)
        public = inParcel.readInt()
        serverName = inParcel.readString()!!
        serverLocation = inParcel.readString()!!
        serverDomain = inParcel.readString()!!
        serverIp = inParcel.readString()!!
        tcpPort = inParcel.readInt()
        udpPort = inParcel.readInt()
        maxSession = inParcel.readInt()
        sessionCount = inParcel.readInt()
        ovpnContent =  inParcel.readString()!!
        serverStatus = inParcel.readString()!!
    }
    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(out: Parcel, flags: Int) {
        out.writeInt(l2tpSupport)
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
}