package vn.unlimit.vpngate.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.RawQuery
import androidx.sqlite.db.SupportSQLiteQuery
import vn.unlimit.vpngate.models.VPNGateItem

@Dao
interface VPNGateItemDao {
    @Query("SELECT * FROM vpngateitem")
    fun getAll(): List<VPNGateItem>

    @RawQuery
    fun filterAndSort(query: SupportSQLiteQuery): List<VPNGateItem>

    @Insert
    fun insertAll(vararg vpnGateItem: VPNGateItem)

    @Query("DELETE FROM vpngateitem")
    fun deleteAll()

    @Query("SELECT COUNT(hostName) FROM vpngateitem")
    fun count(): Int
}