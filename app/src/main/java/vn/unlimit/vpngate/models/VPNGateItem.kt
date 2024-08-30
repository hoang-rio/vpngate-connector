package vn.unlimit.vpngate.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class VPNGateItem(
    @PrimaryKey val hostName: String,
    @ColumnInfo val ip: String?,
    @ColumnInfo val score: Int = 0,
    @ColumnInfo val ping: Int = 0,
    @ColumnInfo val speed : Int = 0,
    @ColumnInfo val countryLong: String?,
    @ColumnInfo val countryShort: String?,
    @ColumnInfo val numVpnSession: Int = 0,
    @ColumnInfo val uptime: Int = 0,
    @ColumnInfo val totalUser: Int = 0,
    @ColumnInfo val totalTraffic: Long = 0,
    @ColumnInfo val logType: String?,
    @ColumnInfo val operator: String?,
    @ColumnInfo val message: String?,
    @ColumnInfo val openVpnConfigData: String?,
    @ColumnInfo val tcpPort: Int = 0,
    @ColumnInfo val udpPort: Int = 0,
    @ColumnInfo val isL2TPSupport: Boolean = false,
    @ColumnInfo val isSSTPSupport: Boolean = false
)
