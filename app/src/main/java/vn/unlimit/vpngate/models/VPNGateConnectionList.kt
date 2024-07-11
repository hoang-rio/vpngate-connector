package vn.unlimit.vpngate.models

import android.os.Parcel
import android.os.Parcelable
import java.util.Locale

/**
 * Created by dongh on 14/01/2018.
 */
class VPNGateConnectionList : Parcelable {
    @JvmField
    var filter: Filter? = null
    private var data: MutableList<VPNGateConnection>?

    constructor() {
        data = ArrayList()
    }

    private constructor(`in`: Parcel) {
        data = `in`.createTypedArrayList(VPNGateConnection.CREATOR)
    }

    /**
     * Filter connection by keyword
     *
     * @param inKeyword keyword to filter
     * @return
     */
    fun filter(inKeyword: String): VPNGateConnectionList {
        var keyword = inKeyword
        keyword = keyword.lowercase(Locale.getDefault())
        val result = VPNGateConnectionList()
        for (vpnGateConnection in data!!) {
            if (vpnGateConnection.countryLong!!.lowercase(Locale.getDefault())
                    .contains(keyword) ||
                vpnGateConnection.hostName!!.lowercase(Locale.getDefault())
                    .contains(keyword) ||
                vpnGateConnection.operator!!.lowercase(Locale.getDefault())
                    .contains(keyword) ||
                vpnGateConnection.ip!!.contains(keyword)
            ) {
                result.add(vpnGateConnection)
            }
        }
        if (filter != null) {
            return result.advancedFilter(filter)
        }
        return result
    }

    /**
     * Get ordered list
     *
     * @param property
     * @param type     order type 0 = ASC, 1 = DESC
     * @return
     */
    fun sort(property: String?, type: Int) {
        data!!.sortWith(Comparator { o1: VPNGateConnection, o2: VPNGateConnection ->
            if (type == ORDER.ASC) {
                when (property) {
                    SortProperty.COUNTRY -> return@Comparator o1.countryLong!!
                        .compareTo(o2.countryLong!!)

                    SortProperty.SPEED -> return@Comparator o1.speed.compareTo(o2.speed)
                    SortProperty.PING -> return@Comparator o1.ping.compareTo(o2.ping)
                    SortProperty.SCORE -> return@Comparator o1.score.compareTo(o2.score)
                    SortProperty.UPTIME -> return@Comparator o1.uptime
                        .compareTo(o2.uptime)

                    SortProperty.SESSION -> return@Comparator o1.numVpnSession
                        .compareTo(o2.numVpnSession)

                    else -> return@Comparator 0
                }
            } else if (type == ORDER.DESC) {
                when (property) {
                    SortProperty.COUNTRY -> return@Comparator o2.countryLong!!
                        .compareTo(o1.countryLong!!)

                    SortProperty.SPEED -> return@Comparator o2.speed.compareTo(o1.speed)
                    SortProperty.PING -> return@Comparator o2.ping.compareTo(o1.ping)
                    SortProperty.SCORE -> return@Comparator o2.score.compareTo(o1.score)
                    SortProperty.UPTIME -> return@Comparator o2.uptime
                        .compareTo(o1.uptime)

                    SortProperty.SESSION -> return@Comparator o2.numVpnSession
                        .compareTo(o1.numVpnSession)

                    else -> return@Comparator 0
                }
            }
            0
        })
    }

    fun add(vpnGateConnection: VPNGateConnection) {
        data!!.add(vpnGateConnection)
    }

    fun clear() {
        data!!.clear()
    }

    fun addAll(list: VPNGateConnectionList) {
        data!!.addAll(list.data!!)
    }

    fun get(index: Int): VPNGateConnection {
        return data!![index]
    }

    fun size(): Int {
        return data!!.size
    }

    fun advancedFilter(filter: Filter?): VPNGateConnectionList {
        this.filter = filter
        return advancedFilter()
    }

    fun advancedFilter(): VPNGateConnectionList {
        if (filter == null) {
            return this
        }
        val dataWithFilter = VPNGateConnectionList()
        // Apply or filter conditional
        for (vpnGateConnection in data!!) {
            if (filter!!.ping != null) {
                when (filter!!.pingFilterOperator) {
                    NumberFilterOperator.EQUAL -> if (vpnGateConnection.ping != filter!!.ping) {
                        continue
                    }

                    NumberFilterOperator.GREATER -> if (vpnGateConnection.ping <= filter!!.ping!!) {
                        continue
                    }

                    NumberFilterOperator.GREATER_OR_EQUAL -> if (vpnGateConnection.ping < filter!!.ping!!) {
                        continue
                    }

                    NumberFilterOperator.LESS -> if (vpnGateConnection.ping >= filter!!.ping!!) {
                        continue
                    }

                    NumberFilterOperator.LESS_OR_EQUAL -> if (vpnGateConnection.ping > filter!!.ping!!) {
                        continue
                    }

                }
            }
            if (filter!!.speed != null) {
                val speedInMb = filter!!.speed!! * 1024 * 1024
                when (filter!!.speedFilterOperator) {
                    NumberFilterOperator.EQUAL -> if (vpnGateConnection.speed != speedInMb) {
                        continue
                    }

                    NumberFilterOperator.GREATER -> if (vpnGateConnection.speed <= speedInMb) {
                        continue
                    }

                    NumberFilterOperator.GREATER_OR_EQUAL -> if (vpnGateConnection.speed < speedInMb) {
                        continue
                    }

                    NumberFilterOperator.LESS -> if (vpnGateConnection.speed >= speedInMb) {
                        continue
                    }

                    NumberFilterOperator.LESS_OR_EQUAL -> if (vpnGateConnection.speed > speedInMb) {
                        continue
                    }

                }
            }
            if (filter!!.sessionCount != null) {
                when (filter!!.sessionCountFilterOperator) {
                    NumberFilterOperator.EQUAL -> if (vpnGateConnection.numVpnSession != filter!!.sessionCount) {
                        continue
                    }

                    NumberFilterOperator.GREATER -> if (vpnGateConnection.numVpnSession <= filter!!.sessionCount!!) {
                        continue
                    }

                    NumberFilterOperator.GREATER_OR_EQUAL -> if (vpnGateConnection.numVpnSession < filter!!.sessionCount!!) {
                        continue
                    }

                    NumberFilterOperator.LESS -> if (vpnGateConnection.numVpnSession >= filter!!.sessionCount!!) {
                        continue
                    }

                    NumberFilterOperator.LESS_OR_EQUAL -> if (vpnGateConnection.numVpnSession > filter!!.sessionCount!!) {
                        continue
                    }

                }
            }
            if (filter!!.isShowTCP && vpnGateConnection.tcpPort > 0) {
                dataWithFilter.add(vpnGateConnection)
                continue
            }
            if (filter!!.isShowUDP && vpnGateConnection.udpPort > 0) {
                dataWithFilter.add(vpnGateConnection)
                continue
            }
            if (filter!!.isShowL2TP && vpnGateConnection.isL2TPSupport()) {
                dataWithFilter.add(vpnGateConnection)
                continue
            }
            if (filter!!.isShowSSTP && vpnGateConnection.isSSTPSupport()) {
                dataWithFilter.add(vpnGateConnection)
                continue
            }
        }
        return dataWithFilter
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(out: Parcel, flags: Int) {
        out.writeTypedList(data)
    }

    enum class NumberFilterOperator {
        EQUAL,
        GREATER,
        GREATER_OR_EQUAL,
        LESS,
        LESS_OR_EQUAL
    }

    class Filter {
        var isShowTCP: Boolean = true
        var isShowUDP: Boolean = true
        var isShowL2TP: Boolean = true
        var isShowSSTP: Boolean = true
        var ping: Int? = null
        var pingFilterOperator: NumberFilterOperator = NumberFilterOperator.LESS_OR_EQUAL
        var speed: Int? = null
        var speedFilterOperator: NumberFilterOperator = NumberFilterOperator.GREATER_OR_EQUAL
        var sessionCount: Int? = null
        var sessionCountFilterOperator: NumberFilterOperator = NumberFilterOperator.LESS_OR_EQUAL
    }

    object ORDER {
        const val ASC: Int = 0
        const val DESC: Int = 1
    }

    object SortProperty {
        const val COUNTRY: String = "COUNTRY"
        const val SPEED: String = "SPEED"
        const val PING: String = "PING"
        const val SCORE: String = "SCORE"
        const val UPTIME: String = "UPTIME"
        const val SESSION: String = "SESSION"
    }

    companion object CREATOR : Parcelable.Creator<VPNGateConnectionList> {
        override fun createFromParcel(`in`: Parcel): VPNGateConnectionList {
            return VPNGateConnectionList(`in`)
        }

        override fun newArray(size: Int): Array<VPNGateConnectionList?> {
            return arrayOfNulls(size)
        }
    }
}
