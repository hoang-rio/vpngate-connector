package vn.unlimit.vpngate.models

import android.os.Build
import android.os.Parcel
import android.os.Parcelable
import androidx.sqlite.db.SimpleSQLiteQuery
import vn.unlimit.vpngate.App
import vn.unlimit.vpngate.db.VPNGateItemDao
import java.util.Locale

/**
 * Created by dongh on 14/01/2018.
 */
class VPNGateConnectionList : Parcelable {
    @JvmField
    var filter: Filter? = null
    private var data: MutableList<VPNGateConnection>?
    private var vpnGateItemDao: VPNGateItemDao? = null
    private var sortField: String? = null
    private var sortType: Int? = null

    constructor() {
        data = ArrayList()
        vpnGateItemDao = App.instance!!.vpnGateItemDao
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
        val result = VPNGateConnectionList().fromVPNGateItems(vpnGateItemDao!!.search("%$keyword%"))
        return result
    }

    private fun getOrderQuery(): String {
        if (sortType == ORDER.ASC) {
            return " ORDER BY $sortField ASC"
        }
        return " ORDER BY $sortField DESC"
    }

    /**
     * Get ordered list
     *
     * @param property
     * @param type     order type 0 = ASC, 1 = DESC
     * @return
     */
    fun sort(property: String?, type: Int, skipProcessSort: Boolean = false) {
        property.let {
            sortField = property
            sortType = type
            if (skipProcessSort) {
                return
            }
            val whereQuery = getWhereQuery()
            val query = "SELECT * FROM vpngateitem${if(whereQuery.isNotEmpty()) "WHERE $whereQuery " else ""}${getOrderQuery()}"
            val sortedData: List<VPNGateItem>? =
                vpnGateItemDao?.filterAndSort(SimpleSQLiteQuery(query))
            this.clear()
            sortedData!!.forEach { data!!.add(VPNGateConnection().fromVPNGateItem(it)) }
        }
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

    private fun getOperator(numberFilterOperator: NumberFilterOperator): String {
        return when (numberFilterOperator) {
            NumberFilterOperator.EQUAL -> "="
            NumberFilterOperator.GREATER -> ">"
            NumberFilterOperator.GREATER_OR_EQUAL -> ">="
            NumberFilterOperator.LESS -> "<"
            NumberFilterOperator.LESS_OR_EQUAL -> "<="
        }
    }

    private fun appendQuery(
        currentQuery: String,
        queryToAppend: String
    ): String {
        var query = currentQuery
        query += "${if (query.isEmpty()) "" else " AND "}$queryToAppend"
        return query
    }

    private fun getWhereQuery(): String {
        var whereQuery = ""
        if (filter?.ping != null) {
            whereQuery = "ping ${getOperator(filter!!.pingFilterOperator)} ${filter!!.ping}"
        }
        if (filter?.speed != null) {
            val speedInMb = filter!!.speed!! * 1024 * 1024
            whereQuery = appendQuery(
                whereQuery,
                "speed ${getOperator(filter!!.speedFilterOperator)} $speedInMb"
            )
        }
        if (filter?.sessionCount != null) {
            whereQuery = appendQuery(
                whereQuery,
                "numVpnSession ${getOperator(filter!!.sessionCountFilterOperator)} ${filter!!.sessionCount}"
            )
        }
        if (filter?.isShowTCP == true) {
            whereQuery = appendQuery(
                whereQuery, "tcpPort > 0"
            )
        }
        if (filter?.isShowUDP == true) {
            whereQuery = appendQuery(whereQuery, "udpPort > 0")
        }
        if (filter?.isShowL2TP == true && Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            whereQuery = appendQuery(whereQuery, "isL2TPSupport = 1")
        }
        if (filter?.isShowSSTP == true) {
            whereQuery = appendQuery(whereQuery, "isSSTPSupport = 1")
        }
        return whereQuery
    }

    fun advancedFilter(): VPNGateConnectionList {
        val selectQuery = "SELECT * FROM vpngateitem"
        val whereQuery = getWhereQuery()
        clear()
        val filteredResult: List<VPNGateItem>? = if (whereQuery.isNotEmpty()) {
            vpnGateItemDao?.filterAndSort(SimpleSQLiteQuery("$selectQuery WHERE $whereQuery${getOrderQuery()}"))
        } else {
            vpnGateItemDao?.filterAndSort(SimpleSQLiteQuery("$selectQuery${getOrderQuery()}"))
        }
        filteredResult?.forEach {
            data!!.add(VPNGateConnection().fromVPNGateItem(it))
        }
        return this
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(out: Parcel, flags: Int) {
        out.writeTypedList(data)
    }

    fun toVPNGateItems(): List<VPNGateItem> {
        val vpnGateItems = ArrayList<VPNGateItem>()
        data?.forEach {
            vpnGateItems.add(
                it.toVPNGateItem()
            )
        }
        return vpnGateItems
    }

    fun fromVPNGateItems(vpnGateItems: List<VPNGateItem>): VPNGateConnectionList {
        data?.clear()
        vpnGateItems.forEach {
            data?.add(VPNGateConnection().fromVPNGateItem(it))
        }
        return this
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
        const val COUNTRY: String = "countryShort"
        const val SPEED: String = "speed"
        const val PING: String = "ping"
        const val SCORE: String = "score"
        const val UPTIME: String = "uptime"
        const val SESSION: String = "numVpnSession"
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
