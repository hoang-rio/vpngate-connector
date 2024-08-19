package vn.unlimit.vpngate.models

import android.content.Context
import android.os.Build
import android.os.Parcel
import android.os.Parcelable
import android.util.Base64
import vn.unlimit.vpngate.App.Companion.instance
import vn.unlimit.vpngate.R
import vn.unlimit.vpngate.utils.DataUtil
import java.text.DecimalFormat

/**
 * Created by dongh on 14/01/2018.
 */
class VPNGateConnection : Parcelable {
    //HostName,IP,Score,Ping,Speed,CountryLong,CountryShort,NumVpnSessions,Uptime,TotalUsers,TotalTraffic,logType,Operator,Message,OpenVPN_ConfigData_Base64
    var hostName: String? = null
    var ip: String? = null
    var score = 0
    var ping = 0
    var speed = 0
    var countryLong: String? = null
    var countryShort: String? = null
    var numVpnSession = 0
    var uptime = 0
    var totalUser = 0
    var totalTraffic: Long = 0
    var logType: String? = null
    var operator: String? = null
    var message: String? = null
    var openVpnConfigData: String? = null
    var tcpPort = 0
    var udpPort = 0
    private var isL2TPSupport = 0
    private var isSSTPSupport = 0

    private constructor(`in`: Parcel) {
        hostName = `in`.readString()
        ip = `in`.readString()
        score = `in`.readInt()
        ping = `in`.readInt()
        speed = `in`.readInt()
        countryLong = `in`.readString()
        countryShort = `in`.readString()
        numVpnSession = `in`.readInt()
        uptime = `in`.readInt()
        totalUser = `in`.readInt()
        totalTraffic = `in`.readLong()
        logType = `in`.readString()
        operator = `in`.readString()
        message = `in`.readString()
        openVpnConfigData = `in`.readString()
        tcpPort = `in`.readInt()
        udpPort = `in`.readInt()
        isL2TPSupport = `in`.readInt()
        isSSTPSupport = `in`.readInt()
    }

    //Empty constructor
    constructor()

    override fun writeToParcel(out: Parcel, flags: Int) {
        out.writeString(hostName)
        out.writeString(ip)
        out.writeInt(score)
        out.writeInt(ping)
        out.writeInt(speed)
        out.writeString(countryLong)
        out.writeString(countryShort)
        out.writeInt(numVpnSession)
        out.writeInt(uptime)
        out.writeInt(totalUser)
        out.writeLong(totalTraffic)
        out.writeString(logType)
        out.writeString(operator)
        out.writeString(message)
        out.writeString(openVpnConfigData)
        out.writeInt(tcpPort)
        out.writeInt(udpPort)
        out.writeInt(isL2TPSupport)
        out.writeInt(isSSTPSupport)
    }

    private fun decodeBase64(base64str: String): String? {
        try {
            val plainBytes = Base64.decode(base64str, 1)
            return String(plainBytes)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    val calculateHostName: String
        get() = "$hostName.opengw.net"

    val scoreAsString: String
        get() = score.toString()

    val pingAsString: String
        get() = ping.toString() + ""

    val numVpnSessionAsString: String
        get() = numVpnSession.toString() + ""

    fun setOperatorString(operator: String) {
        var loperator = operator
        loperator = loperator.replace("'s owner", "")
        this.operator = loperator
    }

    fun getOpenVpnConfigDataString(): String? {
        var openVpnConfigDataTmp = openVpnConfigData
        if (instance!!.dataUtil!!.getBooleanSetting(DataUtil.USE_DOMAIN_TO_CONNECT, false)) {
            val dDomain = "$hostName.opengw.net"
            openVpnConfigDataTmp = openVpnConfigDataTmp!!.replace(ip!!, dDomain)
        }
        return openVpnConfigDataTmp
    }

    fun setOpenVpnConfigDataString(openVpnConfigData: String) {
        this.openVpnConfigData = decodeBase64(openVpnConfigData)
    }

    val openVpnConfigDataUdp: String?
        get() {
            var openVpnConfigDataUdp = openVpnConfigData
            if (this.tcpPort > 0) {
                // Current config is config for tcp need for udp
                openVpnConfigDataUdp = openVpnConfigDataUdp!!
                    .replace("proto tcp", "proto udp")
                    .replace("remote $ip $tcpPort", "remote $ip $udpPort")
            }
            if (instance!!.dataUtil!!.getBooleanSetting(DataUtil.USE_DOMAIN_TO_CONNECT, false)) {
                val dDomain = "$hostName.opengw.net"
                openVpnConfigDataUdp = openVpnConfigDataUdp!!.replace(ip!!, dDomain)
            }
            // Current config is udp only
            return openVpnConfigDataUdp
        }

    val calculateSpeed: String
        get() = round(speed.toDouble() / (1000 * 1000))

    val calculateTotalTraffic: String
        get() {
            val inMB = totalTraffic.toDouble() / (1000 * 1000)
            if (inMB < 1000) {
                return round(inMB) + " MB"
            }
            val inGB = inMB / 1000
            if (inGB < 1000) {
                return round(inMB / 1000) + " GB"
            }
            return round(inGB / 1000) + " TB"
        }

    fun getCalculateUpTime(context: Context): String {
        //Display as second
        if (uptime < 60000) {
            return round((uptime / 1000).toDouble()) + " " + context.resources.getString(R.string.seconds)
        }
        //Display as minute
        if (uptime < 3600000) {
            return Math.round(uptime.toDouble() / 60000)
                .toString() + " " + context.resources.getString(R.string.minutes)
        }
        //Display as hours
        if (uptime < 3600000 * 24) {
            return round(uptime.toDouble() / 3600000) + " " + context.resources.getString(R.string.hours)
        }
        return round(uptime.toDouble() / (24 * 3600000)) + " " + context.resources.getString(R.string.days)
    }

    private fun round(value: Double): String {
        val df = DecimalFormat("####0.###")
        return df.format(value)
    }

    override fun describeContents(): Int {
        return 0
    }

    fun toVPNGateItem(): VPNGateItem {
        return VPNGateItem(
            hostName = this.hostName!!,
            ip = this.ip,
            score = this.score,
            ping = this.ping,
            speed = this.speed,
            countryLong = this.countryLong,
            countryShort = this.countryShort,
            numVpnSession = this.numVpnSession,
            uptime = this.uptime,
            totalUser = this.totalUser,
            totalTraffic = this.totalTraffic,
            logType = this.logType,
            operator = this.operator,
            message = this.message,
            openVpnConfigData = this.openVpnConfigData,
            tcpPort = this.tcpPort,
            udpPort = this.udpPort,
            isL2TPSupport = this.isL2TPSupport(),
            isSSTPSupport = this.isSSTPSupport(),
        )
    }

    fun fromVPNGateItem(vpnGateItem: VPNGateItem): VPNGateConnection {
        hostName = vpnGateItem.hostName
        ip = vpnGateItem.ip
        score = vpnGateItem.score
        ping = vpnGateItem.ping
        speed = vpnGateItem.speed
        countryLong = vpnGateItem.countryLong
        countryShort = vpnGateItem.countryShort
        numVpnSession = vpnGateItem.numVpnSession
        uptime = vpnGateItem.uptime
        totalUser = vpnGateItem.totalUser
        totalTraffic = vpnGateItem.totalTraffic
        logType = vpnGateItem.logType
        operator = vpnGateItem.operator
        message = vpnGateItem.message
        openVpnConfigData = vpnGateItem.openVpnConfigData
        tcpPort = vpnGateItem.tcpPort
        udpPort = vpnGateItem.udpPort
        isL2TPSupport = if (vpnGateItem.isL2TPSupport) 1 else 0
        isSSTPSupport = if (vpnGateItem.isSSTPSupport) 1 else 0
        return this
    }

    val name: String
        get() = this.getName(false)

    fun getName(useUdp: Boolean): String {
        var address = ip
        if (instance!!.dataUtil!!.getBooleanSetting(DataUtil.USE_DOMAIN_TO_CONNECT, false)) {
            address = "$hostName.opengw.net"
        }
        if (instance!!.dataUtil!!.getBooleanSetting(DataUtil.INCLUDE_UDP_SERVER, true)) {
            if (tcpPort == 0 && udpPort == 0) {
                // Current profile from non udp but open status page with include udp option
                return String.format("%s[%s]", countryLong, address)
            }
            return String.format(
                "%s[%s][%s]",
                countryLong,
                address,
                if (useUdp || tcpPort == 0) "UDP:$udpPort" else "TCP:$tcpPort"
            )
        }
        return String.format("%s[%s]", countryLong, address)
    }

    fun isL2TPSupport(): Boolean {
        return isL2TPSupport == 1 && Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
    }

    fun isSSTPSupport(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && isSSTPSupport == 1
    }

    companion object {
        @JvmField
        val CREATOR
                : Parcelable.Creator<VPNGateConnection> =
            object : Parcelable.Creator<VPNGateConnection> {
                override fun createFromParcel(`in`: Parcel): VPNGateConnection {
                    return VPNGateConnection(`in`)
                }

                override fun newArray(size: Int): Array<VPNGateConnection?> {
                    return arrayOfNulls(size)
                }
            }

        fun fromCsv(csvLine: String): VPNGateConnection? {
            val properties =
                csvLine.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            try {
                var index = 0
                val vpnGateConnection = VPNGateConnection()
                vpnGateConnection.hostName = properties[index++]
                vpnGateConnection.ip = properties[index++]
                vpnGateConnection.score = properties[index++].toInt()
                vpnGateConnection.ping = properties[index++].toInt()
                vpnGateConnection.speed = properties[index++].toInt()
                vpnGateConnection.countryLong = properties[index++]
                vpnGateConnection.countryShort = properties[index++]
                vpnGateConnection.numVpnSession = properties[index++].toInt()
                vpnGateConnection.uptime = properties[index++].toInt()
                vpnGateConnection.totalUser = properties[index++].toInt()
                vpnGateConnection.totalTraffic = properties[index++].toLong()
                vpnGateConnection.logType = properties[index++]
                vpnGateConnection.setOperatorString(properties[index++])
                vpnGateConnection.message = properties[index++]
                vpnGateConnection.setOpenVpnConfigDataString(properties[index])
                if (instance!!.dataUtil!!.getBooleanSetting(
                        DataUtil.INCLUDE_UDP_SERVER,
                        true
                    ) && properties.size >= index + 2
                ) {
                    vpnGateConnection.tcpPort = properties[++index].toInt()
                    vpnGateConnection.udpPort = properties[++index].toInt()
                    if (properties.size > index + 1) {
                        vpnGateConnection.isL2TPSupport = properties[++index].toInt()
                    }
                    if (properties.size > index + 1) {
                        vpnGateConnection.isSSTPSupport = properties[++index].toInt()
                    }
                } else {
                    vpnGateConnection.tcpPort = 0
                    vpnGateConnection.udpPort = 0
                    vpnGateConnection.isL2TPSupport = 0
                    vpnGateConnection.isSSTPSupport = 0
                }
                return vpnGateConnection
            } catch (e: Exception) {
                return null
            }
        }
    }
}
